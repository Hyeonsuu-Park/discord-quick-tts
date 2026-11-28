package io.github.phs314.quicktts.bot.shared.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 앱 판 지표의 태그 값이 정해진 모양으로만 남는지 본다. */
class ClientVersionFilterTest {

    @Test
    void 앱이_보낸_판을_꺼낸다() {
        assertThat(ClientVersionFilter.versionOf("QuickTTS/0.3.0")).isEqualTo("0.3.0");
        assertThat(ClientVersionFilter.versionOf("QuickTTS/dev")).isEqualTo("dev");
    }

    @Test
    void 판을_모르는_요청은_other_로_센다() {
        assertThat(ClientVersionFilter.versionOf(null)).isEqualTo("other");
        assertThat(ClientVersionFilter.versionOf("curl/8.5.0")).isEqualTo("other");
        assertThat(ClientVersionFilter.versionOf("Java-http-client/25")).isEqualTo("other");
        assertThat(ClientVersionFilter.versionOf("QuickTTS/0.3.0-악의적인-긴-값")).isEqualTo("other");
    }
}
