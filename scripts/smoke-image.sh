#!/usr/bin/env bash
# 만든 서버 이미지를 쿠버네티스와 같은 조건(읽기 전용 루트, /tmp 만 쓰기, root 아닌 사용자)으로 띄워
# liveness 와 API 하나를 불러 본다. 테스트로는 안 보이는, 실행할 때만 나는 문제를 올리기 전에 잡는다 (ADR 0072).
#   scripts/smoke-image.sh <서비스> <이미지>
# 봇 서버는 디스코드에 붙어야 켜지므로 SMOKE_DISCORD_TOKEN(시험용 봇 토큰)이 있을 때만 띄운다.
set -euo pipefail

service=$1
image=$2
port=18080
name=smoke-$service
args=(--detach --name "$name" --read-only --tmpfs /tmp --publish "127.0.0.1:$port:8080")

case $service in
  synthesis-service)
    # 목소리 목록은 바깥 TTS 엔진을 부르지 않으므로 네트워크 없이도 답한다.
    check_path=/internal/v1/voices
    ;;
  bot-server)
    if [ -z "${SMOKE_DISCORD_TOKEN:-}" ]; then
      echo "::notice::SMOKE_DISCORD_TOKEN 비밀값이 없어 봇 서버 이미지는 띄워 보지 않습니다."
      exit 0
    fi
    # synthesis 없이도 켜진다(ADR 0071). DB 는 메모리 H2 를 쓴다.
    args+=(--env DISCORD_TOKEN="$SMOKE_DISCORD_TOKEN" --env "QUICKTTS_DB_URL=jdbc:h2:mem:smoke;MODE=MySQL")
    # 준비됨은 DB 와 디스코드 샤드까지 본다.
    check_path=/actuator/health/readiness
    ;;
  *)
    echo "모르는 서비스: $service" >&2
    exit 2
    ;;
esac

cleanup() {
  docker logs "$name" > "smoke-$service.log" 2>&1 || true
  docker rm --force "$name" > /dev/null 2>&1 || true
}
trap cleanup EXIT

docker run "${args[@]}" "$image" > /dev/null

# 컨테이너가 죽으면 기다리지 않고 바로 실패한다.
wait_for() {
  local path=$1
  for _ in $(seq 1 90); do
    if curl --silent --fail --max-time 2 "http://127.0.0.1:$port$path" > /dev/null; then
      return 0
    fi
    if [ "$(docker inspect --format '{{.State.Running}}' "$name")" != true ]; then
      echo "컨테이너가 멈췄습니다." >&2
      break
    fi
    sleep 2
  done
  echo "$path 가 3분 안에 답하지 않았습니다. 로그:" >&2
  docker logs --tail 200 "$name" >&2 || true
  return 1
}

wait_for /actuator/health/liveness
wait_for "$check_path"
echo "$service 이미지가 떴고 $check_path 가 답했습니다."
