package io.github.phs314.quicktts.bot.config;

import io.github.phs314.quicktts.bot.shared.adapter.in.discord.DiscordSlashCommand;
import io.github.phs314.quicktts.bot.shared.adapter.in.discord.InteractionErrorHandler;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.data.DataArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 디스코드 이벤트를 받는 빈을 모두 모아 모든 샤드에 한 번씩 등록한다. 리스너는 스스로 등록하지 않는다.
 * {@link DiscordSlashCommand} 는 명령이 잡지 않은 실패에도 답이 가도록 {@link InteractionErrorHandler} 로 감싸고,
 * 디스코드는 명령 목록을 통째로 바꾸므로 명령 정의도 여기서 한 번에 등록한다. 슬래시 명령은 샤드가 아니라 봇 앱에 붙으므로 한 샤드로 한 번만 보낸다.
 * <p>
 * 명령 정의는 synthesis 서비스의 목소리 목록을 담으므로 켜질 때가 아니라 켜진 뒤 1분마다 만든다(ADR 0071).
 * synthesis 가 없어도 봇 서버는 켜지고, 디스코드에는 전에 올린 명령이 그대로 남는다. 정의가 바뀌었을 때만 다시 올린다.
 */
@Component
@RequiredArgsConstructor
public class DiscordListenerRegistrar {

    private static final Logger log = LoggerFactory.getLogger(DiscordListenerRegistrar.class);

    private final ShardManager shardManager;
    private final List<EventListener> listeners;
    private final List<DiscordSlashCommand> commands;
    private volatile String lastRegistered;

    @PostConstruct
    void register() {
        listeners.forEach(listener -> shardManager.addEventListener(listener instanceof DiscordSlashCommand
                ? new InteractionErrorHandler(listener)
                : listener));
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void registerCommands() {
        List<SlashCommandData> definitions;
        try {
            definitions = commands.stream().map(DiscordSlashCommand::definition).toList();
        } catch (RuntimeException e) {
            log.warn("슬래시 명령 정의를 만들지 못해 1분 뒤 다시 합니다. 디스코드에는 전에 올린 명령이 그대로 있습니다.", e);
            return;
        }
        String json = new String(DataArray.fromCollection(definitions.stream().map(SlashCommandData::toData).toList())
                .toJson(), StandardCharsets.UTF_8);
        if (json.equals(lastRegistered)) {
            return;
        }
        shardManager.getShards().getFirst().updateCommands()
                .addCommands(definitions)
                .queue(registered -> lastRegistered = json,
                        failure -> log.warn("슬래시 명령을 올리지 못해 1분 뒤 다시 합니다.", failure));
    }
}
