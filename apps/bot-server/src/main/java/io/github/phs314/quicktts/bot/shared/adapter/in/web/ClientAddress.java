package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청을 보낸 곳의 IP. Cloudflare Tunnel 뒤에서는 {@value #CLIENT_IP_HEADER} 헤더가 보낸 곳이다(ADR 0069).
 * 이 헤더는 터널을 거치지 않고 봇 서버에 바로 닿는 곳(이 PC 안)에서는 꾸밀 수 있다.
 */
public final class ClientAddress {

    public static final String CLIENT_IP_HEADER = "CF-Connecting-IP";

    private ClientAddress() {
    }

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader(CLIENT_IP_HEADER);
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.strip();
    }
}
