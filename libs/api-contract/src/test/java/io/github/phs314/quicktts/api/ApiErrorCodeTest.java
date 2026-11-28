package io.github.phs314.quicktts.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 오류 코드 목록 파일이 {@link ApiErrorCode} 와 같은지 본다. 나눠 준 앱은 이 문자열로 안내 문구를 고르는데,
 * OpenAPI 차이 검사는 문자열 값을 보지 않는다. 그래서 목록을 파일로 두고 CI 가 main 의 목록과 비교한다 (ADR 0076).
 */
class ApiErrorCodeTest {

    private static final Path LIST = Path.of("error-codes.txt");

    @Test
    void 오류_코드_목록_파일이_코드와_같다() throws IOException {
        List<String> listed = Files.readAllLines(LIST, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();

        assertThat(codes())
                .as("ApiErrorCode 와 %s 가 다릅니다. 새 코드는 파일에 더하고, 이미 있는 코드는 지우거나 바꾸지 마세요.", LIST)
                .containsExactlyInAnyOrderElementsOf(listed);
    }

    private static List<String> codes() {
        return Arrays.stream(ApiErrorCode.class.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
                .filter(field -> !field.getName().equals("PROPERTY"))
                .map(ApiErrorCodeTest::value)
                .toList();
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
