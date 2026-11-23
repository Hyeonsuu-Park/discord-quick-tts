package io.github.phs314.quicktts.synthesis.contract;

import java.util.List;

/**
 * @param voices         고를 수 있는 목소리
 * @param defaultVoiceId 목소리를 고르지 않았을 때 쓰는 목소리
 */
public record VoiceCatalogResponse(List<VoiceResponse> voices, String defaultVoiceId) {

    /**
     * @param id    목소리 식별자
     * @param label 사용자에게 보여 줄 이름
     */
    public record VoiceResponse(String id, String label) {
    }
}
