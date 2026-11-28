# 운영 안내서

배포한 서버에 문제가 생겼을 때 이 문서를 위에서부터 따라 합니다. 명령은 저장소 루트의 Git Bash 에서 실행합니다.
이 PC 배포(compose)의 명령 앞부분은 길어서 아래처럼 줄여 씁니다.

```bash
prod="docker compose -f compose.yaml -f compose.prod.yaml --profile app --profile tunnel"
```

## 알림을 받았을 때

알림은 디스코드 채널 웹훅으로 옵니다([ADR 0074](adr/0074-alerts-to-discord-webhook.md)). 알림마다 `runbook` 이 이 문서의 아래 제목을 가리킵니다. 고쳐지면 "RESOLVED" 알림이 한 번 더 옵니다. 지금 울리는 알림과 지표는 이 PC 에서 http://localhost:9090/alerts 에서 봅니다.

먼저 무엇이 떠 있는지 봅니다.

```bash
$prod ps
```

### 서비스가 답하지 않음

`QuickTtsServiceDown`: Prometheus 가 2분 넘게 그 서비스의 지표를 받지 못했습니다.

1. `$prod ps` 에서 그 서비스가 `restarting` 이나 `unhealthy` 인지 봅니다. 저절로 다시 뜨는 중이면 1~2분 기다립니다.
2. `$prod logs --tail 200 <서비스>` 에서 마지막 오류를 봅니다. 켜지다 멈추면 보통 설정 값(`.env`)이나 DB 연결입니다.
3. 방금 판을 바꿨다면 아래 "옛 판으로 되돌리기" 로 돌아갑니다.

### 디스코드 연결이 끊김

`QuickTtsDiscordDisconnected`: 샤드 일부가 5분 넘게 디스코드에 붙어 있지 않습니다. 그 샤드가 맡은 서버에서는 봇이 말하지 못합니다.

1. https://discordstatus.com 에서 디스코드 쪽 장애인지 봅니다. 그렇다면 JDA 가 저절로 다시 붙으니 기다립니다.
2. `$prod logs --tail 200 bot-server` 에서 `Invalid token`(토큰이 바뀜)이나 같은 토큰으로 다른 곳에서 켠 흔적(개발용 `bootRun`)이 있는지 봅니다.
3. 둘 다 아니면 `$prod restart bot-server`.

### quick chat 이 실패함

`QuickTtsQuickChatFailing`: 10분 동안 quick chat 이 음성 생성 실패(`speech-synthesis-failed`)나 예상 못 한 오류(`unexpected-failure`)로 3번 넘게 실패했습니다.

1. `speech-synthesis-failed` 면 Edge 와 Google 이 둘 다 막힌 것입니다. `$prod logs --tail 200 synthesis-service` 에서 엔진 오류를 봅니다. Edge 가 버전 때문에 거절하면 `apps/synthesis-service` 의 `edge-chromium-version` 을 최신 Edge 버전으로 올려 새 판을 냅니다.
2. `unexpected-failure` 면 버그입니다. `$prod logs bot-server | grep -B2 -A30 ERROR` 로 스택을 찾아 이슈로 남깁니다.

### TTS 엔진 차단기가 열림

`QuickTtsEngineCircuitOpen`: 그 엔진이 10분 넘게 계속 실패해서 차단기가 닫히지 않았습니다([ADR 0070](adr/0070-timeout-budget-and-engine-circuit-breakers.md)). Edge 면 그동안 Google 목소리로 읽고 있습니다. 사용자는 목소리가 바뀐 것만 압니다.

1. `$prod logs --tail 200 synthesis-service | grep -i edge` 에서 실패 이유를 봅니다.
2. 버전 거절이면 위와 같이 `edge-chromium-version` 을 올립니다. 연결 실패면 Microsoft 쪽 장애일 수 있으니 기다립니다.

### DB 백업이 실패함

이 PC 배포는 `mysql-backup` 이 실패할 때 바로 웹훅으로 알리고, 클러스터는 `QuickTtsBackupMissing`(26시간 넘게 성공 없음)이 알립니다.

1. `$prod logs --tail 50 mysql-backup` 에서 이유를 봅니다. 보통 백업 폴더(`QUICKTTS_BACKUP_DIR`)가 없거나 디스크가 찼습니다.
2. 고친 뒤 `$prod restart mysql-backup` 으로 바로 한 번 덤프합니다.

## DB 되살리기

등록된 기기, 목소리 설정, 허용 목록이 DB 에 있습니다. `mysql-backup` 이 하루에 한 번 `QUICKTTS_BACKUP_DIR`(기본 `backups/`)에 `quicktts-<UTC 시각>.sql.gz` 를 남기고 14일치를 둡니다([ADR 0073](adr/0073-db-backup-and-migration-rules.md)).

**언제**: DB 볼륨이 망가졌을 때, 잘못된 마이그레이션이나 실수로 데이터가 지워졌을 때, 다른 PC 로 옮길 때.

1. 되살릴 파일을 고릅니다. 가장 최근 것부터 봅니다.

   ```bash
   ls -lt "${QUICKTTS_BACKUP_DIR:-backups}" | head
   ```

2. 먼저 그 파일이 멀쩡한지 빈 MySQL 에 되살려 봅니다. 운영 DB 는 건드리지 않습니다. 표마다 줄 수가 나오면 됩니다.

   ```bash
   scripts/restore-drill.sh backups/quicktts-20261128T193000Z.sql.gz
   ```

3. 봇 서버를 멈춰서 되살리는 동안 새 데이터가 쓰이지 않게 합니다.

   ```bash
   $prod stop bot-server cloudflared
   ```

4. 덤프를 운영 DB 에 넣습니다. 덤프 안에 표마다 `DROP TABLE IF EXISTS` 가 있어서 지금 표를 덤프 내용으로 바꿉니다.

   ```bash
   gunzip -c backups/quicktts-20261128T193000Z.sql.gz | $prod exec -T mysql sh -c 'mysql -uquicktts -p"$MYSQL_PASSWORD" quicktts'
   ```

   DB 볼륨이 통째로 없어졌으면 `$prod up -d --wait mysql` 로 빈 MySQL 을 먼저 띄운 뒤 넣습니다.

5. 봇 서버를 다시 켭니다. 덤프보다 새 판의 서버라면 켜질 때 Flyway 가 남은 마이그레이션을 더합니다.

   ```bash
   $prod up -d
   ```

덤프한 시각 뒤에 연결한 기기는 다시 `/연결` 해야 합니다.

**연습**: 몇 달에 한 번, 그리고 백업 방식을 바꿨을 때 2번만 해 봅니다. 백업은 되살려 본 적이 있어야 믿을 수 있습니다.

## 옛 판으로 되돌리기

1. `.env` 의 `QUICKTTS_IMAGE_TAG` 를 앞 판의 `main-<커밋>` 으로 바꿉니다.
2. `$prod up -d`
3. 마이그레이션은 되돌리지 않습니다. 늘리기와 줄이기를 다른 판에 나누는 규칙(ADR 0073) 덕에 바로 앞 판은 새 스키마에서도 돕니다. `__contract_` 마이그레이션이 들어간 판보다 앞으로 되돌려야 하면 DB 도 그 판 전의 덤프로 되살립니다.
