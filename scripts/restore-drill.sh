#!/usr/bin/env bash
# 덤프 파일 하나를 잠깐 띄운 빈 MySQL 에 되살려 보고, 표마다 줄 수와 마지막 마이그레이션 판을 보여 준다.
# 운영 DB 는 건드리지 않는다. 몇 달에 한 번, 그리고 백업 방식을 바꿨을 때 돌린다 (ADR 0073, docs/runbook.md).
#   scripts/restore-drill.sh backups/quicktts-20261128T193000Z.sql.gz
set -euo pipefail

dump=${1:?되살려 볼 덤프 파일(.sql.gz)을 주세요}
if [ ! -f "$dump" ]; then
  echo "덤프 파일이 없습니다: $dump" >&2
  exit 2
fi

name=quicktts-restore-drill
password=restore-drill
trap 'docker rm --force "$name" > /dev/null 2>&1 || true' EXIT

docker run --detach --name "$name" \
  --env MYSQL_ROOT_PASSWORD="$password" --env MYSQL_DATABASE=quicktts \
  mysql:8.4 > /dev/null

# 처음 켤 때는 TCP 를 닫은 임시 서버가 먼저 뜨므로, 127.0.0.1 로 닿을 때까지 기다린다.
ready=
for _ in $(seq 1 60); do
  if docker exec "$name" mysqladmin ping -h 127.0.0.1 -uroot -p"$password" --silent > /dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done
if [ -z "$ready" ]; then
  echo "시험용 MySQL 이 2분 안에 뜨지 않았습니다." >&2
  docker logs --tail 50 "$name" >&2
  exit 1
fi

sql() {
  docker exec --env MYSQL_PWD="$password" "$name" mysql -h 127.0.0.1 -uroot --batch --skip-column-names quicktts -e "$1"
}

gunzip -c "$dump" | docker exec --interactive --env MYSQL_PWD="$password" "$name" mysql -h 127.0.0.1 -uroot quicktts

echo "되살린 표와 줄 수:"
tables=$(sql "SHOW TABLES")
if [ -z "$tables" ]; then
  echo "표가 하나도 없습니다. 덤프가 비어 있습니다." >&2
  exit 1
fi
for table in $tables; do
  printf '  %-28s %s\n' "$table" "$(sql "SELECT COUNT(*) FROM \`$table\`")"
done

if echo "$tables" | grep -qx flyway_schema_history; then
  echo "마지막 마이그레이션: V$(sql "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1")"
fi
echo "되살리기 연습이 끝났습니다. 시험용 MySQL 은 지웁니다."
