package io.github.phs314.quicktts.synthesis.application.port.in.command;

import io.github.phs314.quicktts.synthesis.domain.vo.SpokenText;
import io.github.phs314.quicktts.synthesis.domain.vo.VoiceId;

/**
 * @param text    소리 내 읽을 꼴로 다듬은 문장
 * @param voiceId 읽을 목소리
 */
public record SynthesizeSpeechCommand(SpokenText text, VoiceId voiceId) {
}
