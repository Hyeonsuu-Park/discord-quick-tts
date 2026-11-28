package io.github.phs314.quicktts.bot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phs314.quicktts.bot.shared.adapter.in.discord.DiscordSlashCommand;
import io.github.phs314.quicktts.bot.shared.adapter.in.discord.InteractionErrorHandler;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DiscordListenerRegistrarTest {

    private final ShardManager shardManager = mock(ShardManager.class);
    private final JDA shard = mock(JDA.class, RETURNS_DEEP_STUBS);

    @Test
    void 리스너마다_한_번씩_등록하고_슬래시_명령만_실패_답장으로_감싼다() {
        when(shardManager.getShards()).thenReturn(List.of(shard));
        EventListener listener = new ListenerAdapter() {
        };
        DiscordSlashCommand command = new DiscordSlashCommand() {
            @Override
            public SlashCommandData definition() {
                return Commands.slash("시험", "시험 명령");
            }

            @Override
            public void onEvent(GenericEvent event) {
            }
        };

        new DiscordListenerRegistrar(shardManager, List.of(listener, command), List.of(command)).register();

        ArgumentCaptor<Object> registered = ArgumentCaptor.forClass(Object.class);
        verify(shardManager, times(2)).addEventListener(registered.capture());
        assertThat(registered.getAllValues().getFirst()).isSameAs(listener);
        assertThat(registered.getAllValues().get(1)).isInstanceOf(InteractionErrorHandler.class);
    }

    /** 켜질 때는 명령을 올리지 않으므로 synthesis 가 없어도 켜진다. */
    @Test
    void 켜질_때는_리스너만_등록하고_명령은_올리지_않는다() {
        DiscordSlashCommand failing = failingCommand();

        new DiscordListenerRegistrar(shardManager, List.of(failing), List.of(failing)).register();

        verify(shardManager, never()).getShards();
    }

    @Test
    void 명령_정의를_만들지_못하면_올리지_않고_다음에_다시_한다() {
        DiscordSlashCommand failing = failingCommand();

        assertThatCode(() -> new DiscordListenerRegistrar(shardManager, List.of(), List.of(failing)).registerCommands())
                .doesNotThrowAnyException();

        verify(shardManager, never()).getShards();
    }

    @Test
    void 정의가_같으면_다시_올리지_않는다() {
        when(shardManager.getShards()).thenReturn(List.of(shard));
        CommandListUpdateAction update = mock(CommandListUpdateAction.class);
        when(shard.updateCommands()).thenReturn(update);
        when(update.addCommands(anyCollection())).thenReturn(update);
        doAnswer(invocation -> {
            invocation.<Consumer<List<Command>>>getArgument(0).accept(List.of());
            return null;
        }).when(update).queue(any(), any());
        DiscordSlashCommand command = command(() -> Commands.slash("시험", "시험 명령"));
        DiscordListenerRegistrar registrar = new DiscordListenerRegistrar(shardManager, List.of(), List.of(command));

        registrar.registerCommands();
        registrar.registerCommands();

        verify(shard, times(1)).updateCommands();
    }

    private static DiscordSlashCommand failingCommand() {
        return command(() -> {
            throw new IllegalStateException("synthesis 없음");
        });
    }

    private static DiscordSlashCommand command(Supplier<SlashCommandData> definition) {
        return new DiscordSlashCommand() {
            @Override
            public SlashCommandData definition() {
                return definition.get();
            }

            @Override
            public void onEvent(GenericEvent event) {
            }
        };
    }
}
