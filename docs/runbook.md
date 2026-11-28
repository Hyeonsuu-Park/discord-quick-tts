# 운영 안내서

배포한 서버에 문제가 생겼을 때 이 문서를 위에서부터 따라 합니다. 명령은 저장소 루트의 Git Bash 에서 실행합니다.
이 PC 배포(compose)의 명령 앞부분은 길어서 아래처럼 줄여 씁니다.

```bash
prod="docker compose -f compose.yaml -f compose.prod.yaml --profile app --profile tunnel"
```

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
