package io.github.phs314.quicktts.synthesis.contract;

/**
 * @param text    소리 내 읽을 꼴로 다듬은 문장
 * @param voiceId 읽을 목소리 (예: {@code edge:ko-KR-SunHiNeural})
 */
public record SynthesizeRequest(String text, String voiceId) {
}
