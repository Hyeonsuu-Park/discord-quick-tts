package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.phs314.quicktts.api.QuickTtsApi;
import io.github.phs314.quicktts.bot.shared.adapter.in.AuditLog;
import io.github.phs314.quicktts.bot.shared.domain.vo.DiscordUserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 웹 API 사용량 제한 (ADR 0069). 기기 토큰 인증 뒤에 돈다.
 * <ul>
 *   <li>기기 토큰이 있는 {@code /api} 요청은 디스코드 사용자마다 센다. 한 사람이 PC 를 여럿 연결해도 같이 센다.</li>
 *   <li>기기 등록은 토큰이 없으므로 보낸 곳(IP)마다 센다. Cloudflare Tunnel 뒤에서는 {@code CF-Connecting-IP} 가 보낸 곳이다.</li>
 *   <li>상태 확인, API 문서처럼 그 밖의 요청은 세지 않는다.</li>
 * </ul>
 * 넘으면 {@code Retry-After}(초)를 붙여 429 로 답한다. 응답 모양은 다른 실패와 같게 스프링 MVC 의 예외 처리에 넘긴다.
 * 봇 서버는 한 벌만 뜨므로(ADR 0059) 버킷은 이 프로세스 메모리에 두고, 10분 동안 안 쓴 버킷은 지운다.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** 웹 API 경로. 버전(/api/v1, /api/v2 ...)과 상관없이 센다. */
    private static final String API_PREFIX = "/api/";
    private static final String USER_KEY_PREFIX = "user:";
    private static final String REGISTRATION_KEY_PREFIX = "registration:";

    private static final Duration IDLE_BUCKET_EXPIRY = Duration.ofMinutes(10);
    private static final long MAX_BUCKETS = 100_000;

    private final RateLimitProperties properties;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(IDLE_BUCKET_EXPIRY)
            .maximumSize(MAX_BUCKETS)
            .build();
    /** 몰아 보내는 동안 거절마다 감사 로그가 쌓이지 않게, 같은 키는 1분에 한 줄만 남긴다. */
    private final Cache<String, Boolean> recentlyAudited = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(MAX_BUCKETS)
            .build();

    public RateLimitFilter(RateLimitProperties properties, HandlerExceptionResolver handlerExceptionResolver) {
        this.properties = properties;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> key = bucketKeyOf(request);
        if (key.isPresent()) {
            ConsumptionProbe probe = buckets.get(key.get(), this::newBucket).tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                if (recentlyAudited.asMap().putIfAbsent(key.get(), Boolean.TRUE) == null) {
                    AuditLog.record("request.rate-limited", "key", key.get(), "path", request.getRequestURI(),
                            "ip", ClientAddress.of(request));
                }
                long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
                response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
                handlerExceptionResolver.resolveException(request, response, null,
                        new TooManyRequestsException(retryAfterSeconds));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static Optional<String> bucketKeyOf(HttpServletRequest request) {
        Optional<DiscordUserId> user = signedInUser();
        if (user.isPresent() && request.getRequestURI().startsWith(API_PREFIX)) {
            return Optional.of(USER_KEY_PREFIX + user.get().value());
        }
        if (HttpMethod.POST.matches(request.getMethod()) && QuickTtsApi.DEVICES_PATH.equals(request.getRequestURI())) {
            return Optional.of(REGISTRATION_KEY_PREFIX + ClientAddress.of(request));
        }
        return Optional.empty();
    }

    private Bucket newBucket(String key) {
        return perMinute(key.startsWith(USER_KEY_PREFIX)
                ? properties.requestsPerMinute()
                : properties.registrationsPerMinute());
    }

    private static Optional<DiscordUserId> signedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof DiscordUserId userId
                ? Optional.of(userId)
                : Optional.empty();
    }

    /** 1분에 {@code limit} 번. 다 쓰면 1분에 걸쳐 조금씩 다시 찬다. */
    private static Bucket perMinute(int limit) {
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth.capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)))
                .build();
    }
}
