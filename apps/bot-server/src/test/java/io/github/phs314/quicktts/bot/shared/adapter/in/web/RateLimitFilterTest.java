package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.api.QuickTtsApi;
import io.github.phs314.quicktts.bot.shared.domain.BusinessException;
import io.github.phs314.quicktts.bot.shared.domain.vo.DiscordUserId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * 사용자마다, 기기 등록은 보낸 곳마다 1분 상한을 세고, 넘으면 Retry-After 를 붙여 사용량 제한 실패로 넘기는지 확인한다.
 */
class RateLimitFilterTest {

    private final List<Exception> resolved = new ArrayList<>();
    private final HandlerExceptionResolver resolver = (request, response, handler, e) -> {
        resolved.add(e);
        return new ModelAndView();
    };
    private final RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(3, 2), resolver);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 기기_토큰이_있는_요청은_사용자마다_상한까지_받고_넘으면_막는다() throws Exception {
        signIn(1L);
        for (int i = 0; i < 3; i++) {
            assertThat(passes(post(QuickTtsApi.QUICK_CHAT_PATH))).isTrue();
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThat(passes(post(QuickTtsApi.QUICK_CHAT_PATH), response)).isFalse();

        assertThat(resolved).singleElement()
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ApiErrorCode.TOO_MANY_REQUESTS));
        assertThat(Long.parseLong(response.getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1L, 60L);
    }

    @Test
    void 다른_사용자는_따로_센다() throws Exception {
        signIn(1L);
        for (int i = 0; i < 3; i++) {
            passes(post(QuickTtsApi.QUICK_CHAT_PATH));
        }

        signIn(2L);
        assertThat(passes(post(QuickTtsApi.QUICK_CHAT_PATH))).isTrue();
    }

    @Test
    void 기기_등록은_보낸_곳마다_센다() throws Exception {
        assertThat(passes(registration("203.0.113.1"))).isTrue();
        assertThat(passes(registration("203.0.113.1"))).isTrue();
        assertThat(passes(registration("203.0.113.1"))).isFalse();

        assertThat(passes(registration("203.0.113.2"))).isTrue();
    }

    @Test
    void 상태_확인처럼_웹_API_가_아닌_요청은_세지_않는다() throws Exception {
        signIn(1L);
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest health = new MockHttpServletRequest("GET", QuickTtsApi.HEALTH_PATH);
            health.setRequestURI(QuickTtsApi.HEALTH_PATH);
            assertThat(passes(health)).isTrue();
        }
        SecurityContextHolder.clearContext();
        for (int i = 0; i < 10; i++) {
            assertThat(passes(post(QuickTtsApi.QUICK_CHAT_PATH))).isTrue();
        }
        assertThat(resolved).isEmpty();
    }

    private static void signIn(long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(new DiscordUserId(userId), null, Set.of()));
    }

    private static MockHttpServletRequest post(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        return request;
    }

    private static MockHttpServletRequest registration(String clientIp) {
        MockHttpServletRequest request = post(QuickTtsApi.DEVICES_PATH);
        request.addHeader(ClientAddress.CLIENT_IP_HEADER, clientIp);
        return request;
    }

    private boolean passes(MockHttpServletRequest request) throws Exception {
        return passes(request, new MockHttpServletResponse());
    }

    private boolean passes(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain.getRequest() != null;
    }
}
