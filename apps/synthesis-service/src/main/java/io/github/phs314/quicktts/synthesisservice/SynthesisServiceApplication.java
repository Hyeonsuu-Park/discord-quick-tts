package io.github.phs314.quicktts.synthesisservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * synthesis 서비스. 봇 서버가 클러스터 안에서 음성을 만들어 달라고 부른다 (ADR 0061).
 */
@SpringBootApplication
public class SynthesisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SynthesisServiceApplication.class, args);
    }
}
