package io.github.phs314.quicktts.bot.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 관리 포트({@code management.server.port})를 따로 열었을 때 그 포트로 온 요청을 가린다 (ADR 0057).
 * 포트 번호는 관리 서버가 실제로 뜬 뒤에 알 수 있어서(0 이면 아무 번호) 뜰 때 받아 둔다.
 * 따로 열지 않았으면(개발, exe) 관리 서버가 없으므로 아무 요청도 가리지 않는다.
 * 보안 설정의 일부라서 {@link SecurityConfig} 가 함께 등록한다(컨트롤러 슬라이스 테스트에도 같이 뜨게).
 */
public class ManagementPortRequestMatcher implements RequestMatcher, ApplicationListener<WebServerInitializedEvent> {

    private static final String MANAGEMENT_NAMESPACE = "management";

    private volatile int port = -1;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (WebServerApplicationContext.hasServerNamespace(event.getApplicationContext(), MANAGEMENT_NAMESPACE)) {
            port = event.getWebServer().getPort();
        }
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return port > 0 && request.getLocalPort() == port;
    }
}
