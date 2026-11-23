package io.github.phs314.quicktts.synthesis.adapter.out.tts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** TTS 설정 값의 필수 값이 비면 켜질 때 멈추고 고치는 방법을 알려 주는지 확인한다. */
class TtsPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues("quicktts.tts.default-voice=edge:ko-KR-SunHiNeural",
                    "quicktts.tts.edge-chromium-version=143.0.3650.75");

    @Test
    void 설정_값이_있으면_켜진다() {
        runner.run(context -> assertThat(context.getBean(TtsProperties.class).defaultVoice())
                .isEqualTo("edge:ko-KR-SunHiNeural"));
    }

    @Test
    void 기본_목소리가_비어_있으면_켜지지_않고_고치는_방법을_알려준다() {
        runner.withPropertyValues("quicktts.tts.default-voice=")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause().hasMessageContaining("QUICKTTS_DEFAULT_VOICE 가 비어 있습니다"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TtsProperties.class)
    static class PropertiesConfig {
    }
}
