package io.github.phs314.quicktts.bot.config;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.api.QuickTtsApi;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 웹 API 문서(OpenAPI)의 머리말과 인증 방식. 경로와 요청·응답 모양은 springdoc 이 컨트롤러에서 읽는다.
 * 문서는 {@code /v3/api-docs}, 화면은 {@code /swagger-ui.html} 이다 (ADR 0054).
 */
@Configuration
public class OpenApiConfig {

    /** 기기 토큰을 {@code Authorization: Bearer ...} 로 싣는 인증. 따로 표시한 곳 말고는 모든 API 가 쓴다. */
    public static final String DEVICE_TOKEN = "deviceToken";

    /** 오류 응답(ProblemDetail, RFC 9457) 모양. 컨트롤러의 오류 응답이 {@code #/components/schemas/Problem} 으로 가리킨다. */
    public static final String PROBLEM = "Problem";

    @Bean
    public OpenAPI quickTtsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quick TTS 봇 서버 API")
                        .version("v1")
                        .description("데스크톱 앱이 문장을 보내고, 읽힐 음성 채널을 묻고, 기기를 연결하는 API."))
                .components(new Components()
                        .addSecuritySchemes(DEVICE_TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("디스코드 /연결 코드로 기기를 등록하면 받는 기기 토큰"))
                        .addSchemas(PROBLEM, problemSchema()))
                .addSecurityItem(new SecurityRequirement().addList(DEVICE_TOKEN));
    }

    /**
     * {@code libs/api-contract} 의 요청·응답 record 는 필드를 늘 다 싣고 다니므로(null 도 그대로 싣는다) 모든 필드를 필수로 적는다.
     * {@code @Nullable} 필드는 null 도 될 수 있다고 적는다. 그래야 CI 의 API 차이 검사가 응답 필드를 빼거나
     * 요청에 필수 필드를 더하는 것을 나눠 준 앱을 깨는 변경으로 잡는다 (ADR 0076).
     */
    @Bean
    public OpenApiCustomizer contractRecordsAreRequired() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) ->
                contractRecord(name).ifPresent(type -> describeRecord(type, schema)));
    }

    private static Optional<Class<?>> contractRecord(String schemaName) {
        try {
            Class<?> type = Class.forName(QuickTtsApi.class.getPackageName() + "." + schemaName);
            return type.isRecord() ? Optional.of(type) : Optional.empty();
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private static void describeRecord(Class<?> type, Schema<?> schema) {
        schema.setRequired(Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList());
        for (RecordComponent component : type.getRecordComponents()) {
            Schema<?> property = schema.getProperties() == null ? null : schema.getProperties().get(component.getName());
            if (property != null && component.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                Set<String> types = new LinkedHashSet<>(property.getTypes() == null ? Set.of() : property.getTypes());
                types.add("null");
                property.setTypes(types);
            }
        }
    }

    private static Schema<?> problemSchema() {
        return new ObjectSchema()
                .description("오류 응답. 클라이언트는 status 대신 code 로 안내 문구를 고른다 (ADR 0013).")
                .addProperty("type", new StringSchema())
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty(ApiErrorCode.PROPERTY, new StringSchema()
                        .description("오류 코드 (ApiErrorCode). 예: invalid-value, invalid-device-token"));
    }
}
