package io.github.phs314.quicktts.bot.speech.adapter.out.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import io.github.phs314.quicktts.bot.speech.application.exception.BotCannotJoinVoiceChannelException;
import io.github.phs314.quicktts.bot.speech.application.exception.UnplayableAudioException;
import io.github.phs314.quicktts.bot.speech.domain.vo.Audio;
import io.github.phs314.quicktts.bot.speech.domain.vo.ChannelId;
import io.github.phs314.quicktts.bot.speech.domain.vo.VoiceChannel;
import io.github.phs314.quicktts.bot.shared.domain.vo.GuildId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.Test;

class SpeechPlayerDiscordAdapterTest {

    @Test
    void 메모리의_mp3_바이트로_바로_재생되는_트랙을_만든다() throws Exception {
        AudioTrack track = SpeechPlayerDiscordAdapter.toTrack(new Audio(sampleMp3(), "mp3"));

        DefaultAudioPlayerManager manager = new DefaultAudioPlayerManager();
        try {
            AudioPlayer player = manager.createPlayer();
            player.startTrack(track, false);
            AudioFrame frame = player.provide(5, TimeUnit.SECONDS);

            assertThat(frame).isNotNull();
            assertThat(frame.getDataLength()).isPositive();
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void 같은_음성으로_트랙을_여러_번_만들어도_바이트를_바꾸지_않는다() throws IOException {
        byte[] audio = sampleMp3();
        byte[] before = audio.clone();

        SpeechPlayerDiscordAdapter.toTrack(new Audio(audio, "mp3"));
        SpeechPlayerDiscordAdapter.toTrack(new Audio(audio, "mp3"));

        assertThat(audio).isEqualTo(before);
    }

    @Test
    void 음성이_아닌_바이트면_음성_생성_실패로_알린다() {
        Audio broken = new Audio("음성이 아님".getBytes(StandardCharsets.UTF_8), "mp3");

        assertThatThrownBy(() -> SpeechPlayerDiscordAdapter.toTrack(broken))
                .isInstanceOf(UnplayableAudioException.class);
    }

    /** Edge 목소리로 만든 mp3 의 앞 1초쯤. */
    private static byte[] sampleMp3() throws IOException {
        try (InputStream in = SpeechPlayerDiscordAdapterTest.class.getResourceAsStream("/speech/sample.mp3")) {
            return in.readAllBytes();
        }
    }

    private static final VoiceChannel CHANNEL = new VoiceChannel(new GuildId(1L), new ChannelId(2L));

    /** 조용히 넘어가면 서비스가 재생 순서에 넣고 202 로 답해서, 앱은 성공을 보는데 소리가 나지 않는다. */
    @Test
    void 디스코드_서버를_찾지_못하면_들어갈_수_없다고_알린다() {
        ShardManager shardManager = mock(ShardManager.class);
        SpeechPlayerDiscordAdapter adapter = new SpeechPlayerDiscordAdapter(shardManager, new SimpleMeterRegistry());

        assertThatThrownBy(() -> adapter.join(CHANNEL)).isInstanceOf(BotCannotJoinVoiceChannelException.class);
        adapter.shutdown();
    }

    @Test
    void 음성_채널을_찾지_못하면_들어갈_수_없다고_알린다() {
        ShardManager shardManager = mock(ShardManager.class);
        Guild guild = mock(Guild.class);
        when(shardManager.getGuildById(anyLong())).thenReturn(guild);
        when(guild.getChannelById(AudioChannel.class, 2L)).thenReturn(null);
        SpeechPlayerDiscordAdapter adapter = new SpeechPlayerDiscordAdapter(shardManager, new SimpleMeterRegistry());

        assertThatThrownBy(() -> adapter.join(CHANNEL)).isInstanceOf(BotCannotJoinVoiceChannelException.class);
        adapter.shutdown();
    }
}
