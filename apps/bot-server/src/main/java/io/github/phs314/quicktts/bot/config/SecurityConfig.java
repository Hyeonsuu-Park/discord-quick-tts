package io.github.phs314.quicktts.bot.config;

import io.github.phs314.quicktts.api.QuickTtsApi;
import io.github.phs314.quicktts.bot.identity.adapter.in.web.DeviceTokenAuthenticationEntryPoint;
import io.github.phs314.quicktts.bot.identity.adapter.in.web.DeviceTokenAuthenticationFilter;
import io.github.phs314.quicktts.bot.identity.application.port.in.AuthenticateDeviceUseCase;
import io.github.phs314.quicktts.bot.shared.adapter.in.web.RateLimitFilter;
import io.github.phs314.quicktts.bot.shared.adapter.in.web.RateLimitProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 웹 API 의 인증 규칙 (ADR 0031). 기기 등록, 상태 확인, API 문서만 토큰 없이 열고, 나머지는 모두 기기 토큰이 있어야 한다.
 * 클라이언트는 매 요청에 토큰을 싣는 프로그램이라 세션, 로그인 화면, CSRF 토큰을 쓰지 않는다.
 * 관리 포트({@code management.server.port})를 따로 열면 그 포트로 온 요청은 모두 토큰 없이 연다.
 * 그 포트는 클러스터 안의 프로브와 Prometheus 만 닿는다 (ADR 0057).
 * 기기 토큰으로 사람을 알아낸 다음에 사용량 제한을 센다 (ADR 0069).
 */
@Configuration
@Import(ManagementPortRequestMatcher.class)
@EnableWebSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            AuthenticateDeviceUseCase authenticateDeviceUseCase,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver,
            ManagementPortRequestMatcher managementPort,
            RateLimitProperties rateLimitProperties) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(managementPort).permitAll()
                        .requestMatchers(HttpMethod.POST, QuickTtsApi.DEVICES_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, QuickTtsApi.HEALTH_PATH, QuickTtsApi.HEALTH_PATH + "/**").permitAll()
                        // API 문서는 경로와 요청 모양만 보여 주고, 문서에서 API 를 부를 때는 기기 토큰이 있어야 한다 (ADR 0054).
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new DeviceTokenAuthenticationEntryPoint(handlerExceptionResolver)))
                .addFilterBefore(new DeviceTokenAuthenticationFilter(authenticateDeviceUseCase),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new RateLimitFilter(rateLimitProperties, handlerExceptionResolver),
                        DeviceTokenAuthenticationFilter.class)
                .build();
    }

}
