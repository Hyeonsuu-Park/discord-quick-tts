package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import io.github.phs314.quicktts.api.QuickTtsApi;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 나눠 준 데스크톱 앱의 판을 다룬다 (ADR 0076).
 * <ul>
 *   <li>{@code /api} 응답마다 이 서버가 받쳐 주는 가장 낮은 앱 판을 {@link QuickTtsApi#MINIMUM_CLIENT_VERSION_HEADER} 로 알린다.
 *       인증 전에 붙이므로 401, 429 응답에도 붙는다.</li>
 *   <li>앱이 {@code User-Agent: QuickTTS/1.2.3} 으로 보낸 판을 {@code quicktts.client.requests{version}} 으로 센다.
 *       최소 판을 올리기 전에 아직 옛 판을 쓰는 사람이 얼마나 되는지 본다.</li>
 * </ul>
 */
public class ClientVersionFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";
    private static final String METRIC = "quicktts.client.requests";

    private final String minimumVersion;
    private final MeterRegistry meterRegistry;

    public ClientVersionFilter(ClientVersionProperties properties, MeterRegistry meterRegistry) {
        this.minimumVersion = properties.minimumVersion();
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!minimumVersion.isEmpty()) {
            response.setHeader(QuickTtsApi.MINIMUM_CLIENT_VERSION_HEADER, minimumVersion);
        }
        Counter.builder(METRIC)
                .description("데스크톱 앱 판마다 받은 웹 API 요청 수")
                .tag("version", versionOf(request.getHeader(HttpHeaders.USER_AGENT)))
                .register(meterRegistry)
                .increment();
        chain.doFilter(request, response);
    }

    /**
     * {@code QuickTTS/1.2.3} 에서 판을 꺼낸다. 판을 모르는 개발 중 앱은 {@code dev}, 그 밖의 프로그램은 {@code other} 다.
     * 지표의 태그 값이 끝없이 늘지 않도록 정해진 모양만 그대로 쓴다.
     */
    static String versionOf(String userAgent) {
        if (userAgent == null) {
            return "other";
        }
        Matcher matcher = QuickTtsApi.USER_AGENT_PATTERN.matcher(userAgent.strip());
        return matcher.matches() ? matcher.group(1) : "other";
    }
}
