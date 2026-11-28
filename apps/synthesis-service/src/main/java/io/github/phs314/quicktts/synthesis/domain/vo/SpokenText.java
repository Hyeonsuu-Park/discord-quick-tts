package io.github.phs314.quicktts.synthesis.domain.vo;

import io.github.phs314.quicktts.synthesis.domain.InvalidDomainValueException;

/**
 * 소리 내 읽을 꼴로 다듬은 문장. 봇 서버가 다듬어 보내지만, 이 서비스도 스스로 비었는지와 길이를 본다.
 * 봇 서버를 거치지 않고 이 서비스에 닿는 요청이 길이 제한 없는 TTS 로 쓰이지 않게 하려는 것이다.
 */
public record SpokenText(String value) {

    /**
     * 한 번에 읽을 수 있는 최대 글자 수. 봇 서버는 200자 문장을 다듬어 보내는데, 다듬으면 글자가 늘 수 있어서
     * (ㅇ → 이응, 짧은 링크 → "링크를 보냈어요") 넉넉히 둔다. 이 서비스에 바로 닿는 요청을 막는 상한이다.
     */
    public static final int MAX_LENGTH = 1000;

    public SpokenText {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainValueException("읽을 내용이 없습니다.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("문장이 너무 깁니다. (최대 " + MAX_LENGTH + "자)");
        }
    }
}
