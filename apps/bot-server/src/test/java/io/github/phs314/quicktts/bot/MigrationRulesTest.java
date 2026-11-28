package io.github.phs314.quicktts.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 마이그레이션은 늘리기(expand)와 줄이기(contract)를 다른 판에 나눈다(ADR 0073).
 * 바로 앞 판의 서버가 새 스키마에서도 돌아야 되돌릴 수 있기 때문이다.
 * 테이블·열을 지우거나 이름을 바꾸거나 타입을 바꾸는 파일은 이름에 {@code __contract_} 를 붙여
 * 리뷰에서 "앞 판이 더는 이것을 안 쓰는가" 를 따로 보게 한다. 이 규칙 전에 만든 V5 까지는 보지 않는다.
 */
class MigrationRulesTest {

    private static final int RULE_STARTS_AFTER = 5;
    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__(.+)\\.sql$");
    private static final Pattern DESTRUCTIVE = Pattern.compile(
            "\\b(drop\\s+(table|column|index|constraint|primary\\s+key)|rename\\s+(table|column|to)"
                    + "|modify\\s+column|change\\s+column|alter\\s+column)\\b",
            Pattern.CASE_INSENSITIVE);

    @Test
    void 지우거나_바꾸는_마이그레이션은_contract_라고_이름_붙인다() throws IOException {
        Resource[] migrations = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/V*.sql");
        assertThat(migrations).isNotEmpty();

        List<String> unnamed = new ArrayList<>();
        for (Resource migration : migrations) {
            String file = migration.getFilename();
            Matcher name = VERSION.matcher(file);
            assertThat(name.matches()).as("마이그레이션 파일 이름 모양: %s", file).isTrue();
            if (Integer.parseInt(name.group(1)) <= RULE_STARTS_AFTER) {
                continue;
            }
            String sql = withoutComments(migration.getContentAsString(StandardCharsets.UTF_8));
            if (DESTRUCTIVE.matcher(sql).find() && !name.group(2).startsWith("contract_")) {
                unnamed.add(file);
            }
        }
        assertThat(unnamed)
                .as("지우거나 바꾸는 파일은 V<n>__contract_<설명>.sql 로 이름을 붙이고, 앞 판이 쓰지 않는지 확인하세요")
                .isEmpty();
    }

    @Test
    void 지우거나_바꾸는_문장을_알아본다() {
        assertThat(DESTRUCTIVE.matcher("ALTER TABLE device DROP COLUMN name;").find()).isTrue();
        assertThat(DESTRUCTIVE.matcher("alter table a rename column b to c").find()).isTrue();
        assertThat(DESTRUCTIVE.matcher("ALTER TABLE a MODIFY COLUMN b varchar(10)").find()).isTrue();
        assertThat(DESTRUCTIVE.matcher("ALTER TABLE device ADD COLUMN last_used_at datetime(6) NULL;").find()).isFalse();
        assertThat(DESTRUCTIVE.matcher(withoutComments("-- 다음 판에서 drop column 한다\nCREATE TABLE x (id int);"))
                .find()).isFalse();
    }

    private static String withoutComments(String sql) {
        return sql.replaceAll("(?m)--.*$", "").replaceAll("(?s)/\\*.*?\\*/", "");
    }
}
