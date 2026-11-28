-- 기기마다 마지막으로 쓴 시각을 남긴다(ADR 0078). 오래 안 쓴 PC 를 /연결해제 목록에서 가려 끊을 수 있게 한다.
-- 늘리기만 하는 마이그레이션이다(ADR 0073). 앞 판 서버는 이 열을 모르고, null 을 허용하므로 그대로 돈다.
alter table device add column last_used_at datetime(6) null;
