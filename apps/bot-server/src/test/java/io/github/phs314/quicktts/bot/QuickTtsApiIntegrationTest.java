package io.github.phs314.quicktts.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.github.phs314.quicktts.api.ApiErrorCode;
import io.github.phs314.quicktts.api.DeviceRegistrationRequest;
import io.github.phs314.quicktts.api.DeviceRegistrationResponse;
import io.github.phs314.quicktts.api.QuickChatRequest;
import io.github.phs314.quicktts.api.QuickTtsApi;
import io.github.phs314.quicktts.api.VoiceChannelResponse;
import io.github.phs314.quicktts.bot.config.DiscordListenerRegistrar;
import io.github.phs314.quicktts.bot.guild.application.port.out.BotOwnerPort;
import io.github.phs314.quicktts.bot.guild.application.port.out.JoinedGuildPort;
import io.github.phs314.quicktts.bot.identity.application.port.in.IssuePairingCodeUseCase;
import io.github.phs314.quicktts.bot.identity.application.port.in.UnlinkAllDevicesUseCase;
import io.github.phs314.quicktts.bot.identity.application.port.in.command.IssuePairingCodeCommand;
import io.github.phs314.quicktts.bot.identity.application.port.in.command.UnlinkAllDevicesCommand;
import io.github.phs314.quicktts.bot.settings.application.port.in.ChangeVoiceUseCase;
import io.github.phs314.quicktts.bot.settings.application.port.in.command.ChangeVoiceCommand;
import io.github.phs314.quicktts.bot.settings.application.port.out.FakeVoiceChoicesPort;
import io.github.phs314.quicktts.bot.settings.application.port.out.VoiceChoicesPort;
import io.github.phs314.quicktts.bot.speech.application.port.in.dto.VoiceChannelDto;
import io.github.phs314.quicktts.bot.speech.application.port.out.FakeSpeechPlayerPort;
import io.github.phs314.quicktts.bot.speech.application.port.out.FakeTextToSpeechPort;
import io.github.phs314.quicktts.bot.speech.application.port.out.FakeVoiceStatePort;
import io.github.phs314.quicktts.bot.speech.application.port.out.SpeechPlayerPort;
import io.github.phs314.quicktts.bot.speech.application.port.out.TextToSpeechPort;
import io.github.phs314.quicktts.bot.speech.application.port.out.VoiceStatePort;
import io.github.phs314.quicktts.bot.speech.domain.vo.ChannelId;
import io.github.phs314.quicktts.bot.speech.domain.vo.VoiceChannel;
import io.github.phs314.quicktts.bot.shared.domain.vo.DiscordUserId;
import io.github.phs314.quicktts.bot.shared.domain.vo.GuildId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 봇 서버를 통째로 띄우고 데스크톱 클라이언트처럼 HTTP 로 /api/v1 을 불러 응답까지 확인한다.
 * 컨트롤러부터 서비스, 도메인, DB(Flyway 마이그레이션 포함)까지는 실제 것을 쓴다.
 * 디스코드와 synthesis 서비스는 출력 포트 대역으로 바꿔서 디스코드에 접속하지 않고 소리도 나지 않는다.
 * 디스코드 /연결, /연결해제, /목소리 명령이 할 일은 그 명령이 부르는 입력 포트를 직접 부른다.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "quicktts.discord.token=test-token",
        // PC 서버와 같은 H2(MySQL 모드)를 파일 대신 메모리에 띄운다. MySQL 쪽 SQL 은 저장소 테스트가 확인한다.
        "spring.datasource.url=jdbc:h2:mem:api-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // 테스트마다 기기를 새로 연결하므로 사용량 제한은 넉넉하게 둔다. 제한 자체는 RateLimitFilterTest 가 확인한다.
        "quicktts.rate-limit.requests-per-minute=10000",
        "quicktts.rate-limit.registrations-per-minute=10000",
        "quicktts.client.minimum-version=0.3.0"
})
// 진짜 ShardManager 를 만들면 디스코드에 로그인하므로 빈 껍데기로 바꾸고, 켜질 때 디스코드를 부르는 빈도 함께 바꾼다.
@MockitoBean(types = {ShardManager.class, DiscordListenerRegistrar.class, BotOwnerPort.class, JoinedGuildPort.class})
class QuickTtsApiIntegrationTest {

    private static final FakeVoiceStatePort VOICE_CHANNELS = new FakeVoiceStatePort();
    private static final FakeSpeechPlayerPort PLAYER = new FakeSpeechPlayerPort();
    private static final FakeTextToSpeechPort TTS = new FakeTextToSpeechPort();
    private static final FakeVoiceChoicesPort VOICES = new FakeVoiceChoicesPort();

    /** 테스트끼리 봇 음성 상태, 목소리 설정, 기기가 섞이지 않게 사용자, 디스코드 서버, 채널마다 새 ID 를 쓴다. */
    private static final AtomicLong IDS = new AtomicLong(1_000);

    @TestBean(methodName = "voiceChannels")
    VoiceStatePort voiceStatePort;

    @TestBean(methodName = "player")
    SpeechPlayerPort speechPlayerPort;

    @TestBean(methodName = "tts")
    TextToSpeechPort textToSpeechPort;

    @TestBean(methodName = "voices")
    VoiceChoicesPort voiceChoicesPort;

    @LocalServerPort
    int port;

    @Autowired
    IssuePairingCodeUseCase issuePairingCodeUseCase;

    @Autowired
    UnlinkAllDevicesUseCase unlinkAllDevicesUseCase;

    @Autowired
    ChangeVoiceUseCase changeVoiceUseCase;

    @Autowired
    ShardManager shardManager;

    RestTestClient client;

    static VoiceStatePort voiceChannels() {
        return VOICE_CHANNELS;
    }

    static SpeechPlayerPort player() {
        return PLAYER;
    }

    static TextToSpeechPort tts() {
        return TTS;
    }

    static VoiceChoicesPort voices() {
        return VOICES;
    }

    @BeforeEach
    void setUp() {
        VOICE_CHANNELS.reset();
        PLAYER.reset();
        TTS.reset();
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void 연결_코드로_PC_를_등록하면_201_과_기기_토큰을_주고_그_토큰으로_API_를_쓸_수_있다() {
        String token = registerDevice(newUser());

        client.get().uri(QuickTtsApi.MY_VOICE_CHANNEL_PATH)
                .header(HttpHeaders.AUTHORIZATION, QuickTtsApi.BEARER_PREFIX + token)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void 한_번_쓴_연결_코드는_400_과_invalid_pairing_code() {
        String code = issuePairingCodeUseCase.issue(new IssuePairingCodeCommand(newUser())).code().display();
        postDevice(code).expectStatus().isCreated();

        postDevice(code)
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.INVALID_PAIRING_CODE);
    }

    @Test
    void 연결_코드_모양이_틀리면_400_과_invalid_value() {
        postDevice("ABC")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.INVALID_VALUE);
    }

    @Test
    void 내_음성_채널을_물으면_디스코드_서버와_채널_이름을_준다() {
        DiscordUserId user = newUser();
        String token = registerDevice(user);
        VOICE_CHANNELS.enter(user, details(newChannel(newGuild()), "게임"));

        VoiceChannelResponse response = client.get().uri(QuickTtsApi.MY_VOICE_CHANNEL_PATH)
                .header(HttpHeaders.AUTHORIZATION, QuickTtsApi.BEARER_PREFIX + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(VoiceChannelResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isEqualTo(new VoiceChannelResponse("우리 서버", "https://example.com/icon.png", "게임"));
    }

    @Test
    void 보낸_문장은_보낸_사람의_음성_채널에서_고른_목소리로_읽는다() {
        DiscordUserId user = newUser();
        String token = registerDevice(user);
        VoiceChannel channel = newChannel(newGuild());
        VOICE_CHANNELS.enter(user, details(channel, "게임"));
        changeVoiceUseCase.changeVoice(new ChangeVoiceCommand(user, FakeVoiceChoicesPort.INJOON.id()));

        sendQuickChat(token, "안녕").expectStatus().isAccepted();

        assertThat(PLAYER.actions()).containsExactly("join:" + channel.channelId().value(), "play:fake:injoon:안녕");
    }

    @Test
    void 기기_토큰이_없으면_401_과_device_unauthorized() {
        client.post().uri(QuickTtsApi.QUICK_CHAT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new QuickChatRequest("안녕"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.INVALID_DEVICE_TOKEN);
    }

    @Test
    void 내_음성_채널을_물을_때도_기기_토큰이_없으면_401_과_device_unauthorized() {
        client.get().uri(QuickTtsApi.MY_VOICE_CHANNEL_PATH)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.INVALID_DEVICE_TOKEN);
    }

    @Test
    void 연결을_해제한_PC_의_토큰이면_401_과_device_unauthorized() {
        DiscordUserId user = newUser();
        String token = registerDevice(user);
        VOICE_CHANNELS.enter(user, details(newChannel(newGuild()), "게임"));
        unlinkAllDevicesUseCase.unlinkAll(new UnlinkAllDevicesCommand(user));

        sendQuickChat(token, "안녕")
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.INVALID_DEVICE_TOKEN);
        assertThat(PLAYER.actions()).isEmpty();
    }

    @Test
    void 빈_문장이나_너무_긴_문장은_400_과_invalid_value() {
        DiscordUserId user = newUser();
        String token = registerDevice(user);
        VOICE_CHANNELS.enter(user, details(newChannel(newGuild()), "게임"));

        sendQuickChat(token, " ")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.INVALID_VALUE);
        sendQuickChat(token, "가".repeat(QuickTtsApi.MAX_CONTENT_LENGTH + 1))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.INVALID_VALUE);
        assertThat(PLAYER.actions()).isEmpty();
    }

    @Test
    void 보낸_사람이_음성_채널에_없으면_409_와_speaker_not_in_voice_channel() {
        String token = registerDevice(newUser());

        sendQuickChat(token, "안녕")
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.AUTHOR_NOT_IN_VOICE_CHANNEL);
    }

    @Test
    void 같은_디스코드_서버의_다른_채널에서_쓰이고_있으면_409_와_voice_channel_in_use() {
        GuildId guildId = newGuild();
        VoiceChannel first = newChannel(guildId);
        DiscordUserId firstUser = newUser();
        DiscordUserId secondUser = newUser();
        String firstToken = registerDevice(firstUser);
        String secondToken = registerDevice(secondUser);
        VOICE_CHANNELS.enter(firstUser, details(first, "게임"));
        VOICE_CHANNELS.enter(secondUser, details(newChannel(guildId), "공부"));

        sendQuickChat(firstToken, "먼저").expectStatus().isAccepted();
        sendQuickChat(secondToken, "나중")
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo(ApiErrorCode.VOICE_CHANNEL_IN_USE)
                .jsonPath("$.detail").value(String.class, detail -> assertThat(detail).contains("게임"));

        assertThat(PLAYER.actions()).containsExactly("join:" + first.channelId().value(), "play:fake:sunhi:먼저");
    }

    @Test
    void synthesis_서비스가_음성을_만들지_못하면_502_와_speech_synthesis_failed() {
        DiscordUserId user = newUser();
        String token = registerDevice(user);
        VOICE_CHANNELS.enter(user, details(newChannel(newGuild()), "게임"));
        TTS.failing(true);

        sendQuickChat(token, "안녕")
                .expectStatus().isEqualTo(502)
                .expectBody().jsonPath("$.code").isEqualTo(ApiErrorCode.SPEECH_SYNTHESIS_FAILED);
        assertThat(PLAYER.actions()).isEmpty();
    }

    @Test
    void 상태_확인은_토큰_없이_열려_있고_디스코드에_붙어_있으면_UP() {
        connectShard(JDA.Status.CONNECTED);

        client.get().uri(QuickTtsApi.HEALTH_PATH)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void 디스코드에서_끊겨_있으면_상태_확인이_503_과_DOWN() {
        connectShard(JDA.Status.ATTEMPTING_TO_RECONNECT);

        client.get().uri(QuickTtsApi.HEALTH_PATH)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.status").isEqualTo("DOWN");
    }

    @Test
    void 지표는_기기_토큰이_있어야_본다() {
        String memoryUsed = "/actuator/metrics/jvm.memory.used";
        client.get().uri(memoryUsed).exchange().expectStatus().isUnauthorized();

        client.get().uri(memoryUsed)
                .header(HttpHeaders.AUTHORIZATION, QuickTtsApi.BEARER_PREFIX + registerDevice(newUser()))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void API_문서는_토큰_없이_열려_있고_기기_등록만_토큰_없이_부른다고_적는다() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.components.securitySchemes.deviceToken.scheme").isEqualTo("bearer")
                .jsonPath("$.security[0].deviceToken").exists()
                .jsonPath("$.paths['" + QuickTtsApi.QUICK_CHAT_PATH + "'].post").exists()
                .jsonPath("$.paths['" + QuickTtsApi.QUICK_CHAT_PATH + "'].post.parameters").doesNotExist()
                .jsonPath("$.paths['" + QuickTtsApi.MY_VOICE_CHANNEL_PATH + "'].get").exists()
                .jsonPath("$.paths['" + QuickTtsApi.DEVICES_PATH + "'].post.security").isEmpty();
    }

    @Test
    void API_응답마다_최소_앱_판을_알리고_보낸_앱_판을_센다() {
        client.get().uri(QuickTtsApi.MY_VOICE_CHANNEL_PATH)
                .header(HttpHeaders.USER_AGENT, QuickTtsApi.userAgent("0.2.0"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(QuickTtsApi.MINIMUM_CLIENT_VERSION_HEADER, "0.3.0");

        client.get().uri("/actuator/prometheus")
                .header(HttpHeaders.AUTHORIZATION, QuickTtsApi.BEARER_PREFIX + registerDevice(newUser()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("quicktts_client_requests_total{version=\"0.2.0\"}"));
    }

    @Test
    void 상태_확인에는_최소_앱_판을_붙이지_않는다() {
        client.get().uri(QuickTtsApi.HEALTH_PATH)
                .exchange()
                .expectHeader().doesNotExist(QuickTtsApi.MINIMUM_CLIENT_VERSION_HEADER);
    }

    /**
     * 컨트롤러에서 만든 API 문서가 저장소의 {@code libs/api-contract/openapi.json} 과 같은지 본다.
     * CI 는 main 의 이 파일과 PR 의 이 파일을 비교해 나눠 준 앱을 깨는 변경을 막는다 (ADR 0076).
     * API 를 바꿨으면 {@code ./gradlew :apps:bot-server:test --tests '*QuickTtsApiIntegrationTest*' -PupdateOpenApi} 로
     * 파일을 다시 만들어 함께 커밋한다.
     */
    @Test
    void API_문서가_저장소의_API_약속_파일과_같다() throws IOException {
        JsonMapper json = JsonMapper.builder().enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED).build();
        String body = client.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();
        ObjectNode actual = (ObjectNode) json.readTree(body);
        // 서버 주소는 테스트마다 바뀌는 포트라 약속에서 뺀다.
        actual.remove("servers");

        Path snapshot = Path.of(System.getProperty("quicktts.openapi.snapshot", "../../libs/api-contract/openapi.json"));
        if (Boolean.getBoolean("quicktts.openapi.update")) {
            Files.writeString(snapshot, json.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n",
                    StandardCharsets.UTF_8);
            return;
        }
        assertThat(snapshot)
                .as("API 약속 파일이 없습니다. -PupdateOpenApi 로 만드세요: %s", snapshot)
                .exists();
        JsonNode expected = json.readTree(Files.readString(snapshot, StandardCharsets.UTF_8));
        assertThat(actual)
                .as("API 가 libs/api-contract/openapi.json 과 다릅니다. 바꾼 것이 맞으면 -PupdateOpenApi 로 다시 만들어 함께 커밋하세요.")
                .isEqualTo(expected);
    }

    /** 디스코드 /연결 로 코드를 받아 클라이언트 첫 실행처럼 등록하고 기기 토큰을 돌려준다. */
    /** 샤드 하나가 이 상태로 디스코드에 붙어 있는 것처럼 만든다. */
    private void connectShard(JDA.Status status) {
        JDA shard = mock(JDA.class, RETURNS_DEEP_STUBS);
        when(shard.getStatus()).thenReturn(status);
        when(shardManager.getShards()).thenReturn(List.of(shard));
    }

    private String registerDevice(DiscordUserId userId) {
        String code = issuePairingCodeUseCase.issue(new IssuePairingCodeCommand(userId)).code().display();
        DeviceRegistrationResponse response = postDevice(code)
                .expectStatus().isCreated()
                .expectBody(DeviceRegistrationResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.deviceToken()).isNotBlank();
        return response.deviceToken();
    }

    private RestTestClient.ResponseSpec postDevice(String pairingCode) {
        return client.post().uri(QuickTtsApi.DEVICES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeviceRegistrationRequest(pairingCode, "테스트 PC"))
                .exchange();
    }

    private RestTestClient.ResponseSpec sendQuickChat(String token, String text) {
        return client.post().uri(QuickTtsApi.QUICK_CHAT_PATH)
                .header(HttpHeaders.AUTHORIZATION, QuickTtsApi.BEARER_PREFIX + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new QuickChatRequest(text))
                .exchange();
    }

    private static DiscordUserId newUser() {
        return new DiscordUserId(IDS.incrementAndGet());
    }

    private static GuildId newGuild() {
        return new GuildId(IDS.incrementAndGet());
    }

    private static VoiceChannel newChannel(GuildId guildId) {
        return new VoiceChannel(guildId, new ChannelId(IDS.incrementAndGet()));
    }

    private static VoiceChannelDto details(VoiceChannel channel, String channelName) {
        return new VoiceChannelDto(channel, "우리 서버", "https://example.com/icon.png", channelName);
    }
}
