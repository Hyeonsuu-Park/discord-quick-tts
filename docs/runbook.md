# 운영 안내서

배포한 서버를 운영할 때 할 일과, 문제가 생겼을 때 따라 할 순서입니다. 명령은 저장소 루트의 Git Bash 에서 실행합니다.

| 상황 | 볼 곳 |
| --- | --- |
| 디스코드 채널에 알림이 옴 | [알림을 받았을 때](#알림을-받았을-때) |
| 데이터가 없어지거나 DB 가 망가짐 | [DB 되살리기](#db-되살리기) |
| 새 판을 올림, 올린 판이 이상함 | [새 판 올리기](#새-판-올리기), [옛 판으로 되돌리기](#옛-판으로-되돌리기) |
| 누가 무엇을 했는지 알아야 함 | [누가 무엇을 했는지 보기](#누가-무엇을-했는지-보기) |
| 누가 문장을 몰아 보냄 | [누가 몰아 보낼 때](#누가-몰아-보낼-때) |
| 토큰이나 비밀번호가 새었음 | [비밀이 새었을 때](#비밀이-새었을-때) |
| 친구들이 서버에 못 붙음 | [서버 주소가 바뀌었을 때](#서버-주소가-바뀌었을-때) |
| 옛 앱을 그만 받쳐 주려 함 | [옛 앱 판 내리기](#옛-앱-판-내리기) |

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

## 새 판 올리기

1. main 에 머지된 커밋의 CI 가 초록인지 봅니다(CI 가 통과해야 이미지가 올라갑니다, [ADR 0072](adr/0072-images-after-ci-and-smoke-run.md)).
2. GitHub 저장소 > **Packages > quicktts-bot-server** 에서 그 커밋의 `main-<커밋 7자리>` 태그를 확인합니다.
3. 지금 태그를 적어 둡니다(되돌릴 때 씁니다). `grep QUICKTTS_IMAGE_TAG .env`
4. `.env` 의 `QUICKTTS_IMAGE_TAG` 를 새 태그로 바꾸고 `$prod up -d`.
5. `$prod ps` 에서 `bot-server` 가 `healthy` 가 되는지, 디스코드에서 `/도움말` 이 답하는지 봅니다. 안 되면 바로 위 "옛 판으로 되돌리기".

## 누가 무엇을 했는지 보기

봇 서버는 보안에 관련된 행동을 `audit event=` 로 시작하는 한 줄로 남깁니다([ADR 0075](adr/0075-audit-log.md)). 비밀(기기 토큰, 연결 코드)은 남기지 않습니다.

```bash
$prod logs bot-server | grep "audit event="
$prod logs bot-server | grep "audit event=device"            # 기기 연결·해제만
$prod logs bot-server | grep "audit event=.* actor=123456789"  # 한 사람이 한 일
```

| 사건 | 남기는 때 | 키 |
| --- | --- | --- |
| `pairing-code.issued` | `/연결` 로 연결 코드를 받음 | `actor` |
| `device.registered` | 데스크톱 앱이 연결 코드로 기기를 등록함 | `device-name`, `token-hash`(저장된 해시 앞 12자), `ip` |
| `device.registration-rejected` | 틀리거나 만료된 연결 코드 | `reason`, `ip` |
| `device.unlinked` | `/연결해제` | `actor`, `device`, `count` |
| `device-token.rejected` | 없거나 틀린 기기 토큰으로 API 를 부름 | `path`, `ip`, `token-sent` |
| `request.rate-limited` | 사용량 제한에 걸림(같은 키는 1분에 한 줄) | `key`, `path`, `ip` |
| `allowlist.added`, `allowlist.removed` | `/허용목록 추가·삭제` | `actor`, `guild`, `changed` |
| `allowlist.denied` | 봇 주인이 아닌 사람이 `/허용목록` 을 씀 | `actor`, `subcommand` |
| `guild.left` | 허용 목록에 없는 서버에서 나감 | `guild`, `guild-name`, `reason` |

오래 안 쓴 기기는 DB 의 `last_used_at`(한 시간 단위, [ADR 0078](adr/0078-device-last-used-at.md))으로 찾습니다. 사용자는 `/연결해제` 메뉴에서 같은 날짜를 봅니다.

```bash
$prod exec mysql sh -c 'mysql -uquicktts -p"$MYSQL_PASSWORD" quicktts -e "select name, discord_user_id, registered_at, last_used_at from device where last_used_at is null or last_used_at < now() - interval 90 day order by last_used_at"'
```

컨테이너 로그는 서비스마다 30MB(10MB × 3)까지만 남습니다. 오래된 일을 찾으려면 그 전에 파일로 꺼내 둡니다. `$prod logs --no-color bot-server > bot-server-$(date +%F).log`

## 누가 몰아 보낼 때

1. 위의 `request.rate-limited` 줄에서 `key` 를 봅니다. `user:<디스코드 ID>` 면 그 사람, `registration:<IP>` 면 기기 등록을 몰아 시도하는 곳입니다.
2. 사용량 제한(1분 60번)이 이미 막고 있으므로 서버는 버팁니다. 계속되면 그 사람이 있는 디스코드 서버를 `/허용목록 삭제` 로 빼서 봇이 나가게 합니다.
3. 값이 너무 낮거나 높으면 `.env` 의 `QUICKTTS_RATE_LIMIT_REQUESTS_PER_MINUTE`, `QUICKTTS_RATE_LIMIT_REGISTRATIONS_PER_MINUTE` 를 바꾸고 `$prod up -d`.

## 비밀이 새었을 때

- **디스코드 봇 토큰**: [Discord Developer Portal](https://discord.com/developers/applications) > 이 봇 > Bot > **Reset Token**. 새 토큰을 `.env` 의 `DISCORD_TOKEN` 에 넣고 `$prod up -d`. 옛 토큰은 바로 쓸 수 없게 됩니다.
- **누군가의 기기 토큰**(PC 를 잃어버림 등): 그 사람이 디스코드에서 `/연결해제` 로 그 PC 를 끊습니다. 서버는 해시만 들고 있어서 DB 가 새어도 토큰은 나오지 않습니다.
- **알림 웹훅 주소**: 디스코드 채널 설정 > 연동 > 웹후크에서 그 웹후크를 지우고 새로 만든 뒤 `.env` 의 `QUICKTTS_ALERT_DISCORD_WEBHOOK_URL` 을 바꾸고 `$prod up -d`.
- **DB 비밀번호**: `$prod exec mysql mysql -uroot -p` 로 들어가 `ALTER USER 'quicktts'@'%' IDENTIFIED BY '<새 비밀번호>';` 뒤 `.env` 의 `QUICKTTS_PROD_DB_PASSWORD` 를 바꾸고 `$prod up -d`.

## 서버 주소가 바뀌었을 때

지금은 계정 없는 quick tunnel 이라 PC 를 다시 켜거나 `cloudflared` 가 다시 뜨면 주소가 바뀝니다.

```bash
$prod logs cloudflared | grep trycloudflare.com
```

새 주소를 쓰는 사람들에게 알리고, 각자 `~/.discord-quick-tts/client.properties` 의 `server-url` 을 바꾼 뒤 앱을 다시 켜게 합니다. 고정 주소는 Cloudflare 계정과 도메인으로 만드는 named tunnel 이 필요합니다.

## 옛 앱 판 내리기

API 를 깨는 방식으로 바꿔야 할 때(옛 `/api/v1` 경로 내리기 등)의 순서입니다([ADR 0076](adr/0076-api-compatibility-and-minimum-client-version.md)).

1. 바뀐 API 를 쓰는 새 앱 판을 Release 로 냅니다. 서버는 옛 경로와 새 경로를 함께 둡니다.
2. 지금 쓰이는 앱 판을 봅니다. `curl -s http://localhost:9090/api/v1/query --data-urlencode 'query=sum by (version) (increase(quicktts_client_requests_total[7d]))'`
3. `.env` 에 `QUICKTTS_MINIMUM_CLIENT_VERSION=<새 판>` 을 넣고 `$prod up -d`. 그보다 낮은 앱은 트레이 알림으로 업데이트를 안내받습니다.
4. 2번의 옛 판 요청이 충분히 줄면 옛 경로를 지우는 PR 을 냅니다. CI 의 API 호환성 검사가 막으면, 막힌 변경마다 `GET /api/v1/me/voice-channel removed the required property ...` 처럼 방법, 경로, 설명을 한 줄로 `libs/api-contract/openapi-breaking-allowed.txt` 에 적어 함께 커밋합니다.
