package io.github.phs314.quicktts.synthesis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * synthesis 서비스. 봇 서버가 클러스터 안에서 음성을 만들어 달라고 부른다 (ADR 0061).
 * 음성을 만드는 코드는 이 서비스에만 있다 (ADR 0065).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SynthesisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynthesisServiceApplication.class, args);
    }
}
