import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.TrackStartEvent
import dev.arbjerg.lavalink.client.getUserIdFromToken
import dev.arbjerg.lavalink.libraries.discord4j.installVoiceHandler
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.gateway.intent.IntentSet
import reactor.core.publisher.Mono
import java.net.URI

fun main() {
    val token = System.getenv("BOT_TOKEN")
    val client = LavalinkClient(
        getUserIdFromToken(token)
    )
    val discord = DiscordClientBuilder.create(token)
        .build()
        .gateway()
        .setEnabledIntents(IntentSet.nonPrivileged())
        .login().block()!!

    registerNodeD4j(client)

    val appId = discord.restClient.applicationId.block()!!
    val join = ApplicationCommandRequest.builder()
        .name("join")
        .description("Join the voice channel you are in.")
        .build()
    val leave = ApplicationCommandRequest.builder()
        .name("leave")
        .description("Leaves the vc")
        .build()
    val play = ApplicationCommandRequest.builder()
        .name("play")
        .description("Play a song")
        .addOption(
            ApplicationCommandOptionData.builder()
                .name("identifier")
                .description("The identifier of the song you want to play")
                .type(ApplicationCommandOption.Type.STRING.value)
                .required(true)
                .build()
        )
        .build()
    discord.restClient.applicationService.bulkOverwriteGlobalApplicationCommand(
        appId,
        listOf(join, leave, play)
    ).subscribe()

    discord.on(ChatInputInteractionEvent::class.java) {
        handleSlash(client, it)
        Mono.empty<Unit>()
    }.subscribe()

    discord.installVoiceHandler(client)
    readln()
}

fun registerNodeD4j(client: LavalinkClient) {
    val node = client.addNode(
        "Testnode",
        URI.create("ws://localhost:2333"),
        "youshallnotpass"
    )
    node.on<TrackStartEvent>()
        .next()
        .subscribe { event ->
            // A new track is started!
            println("A new track is started!")
            println(event.track.info)
        }
}

private fun handleSlash(lavalink: LavalinkClient, event: ChatInputInteractionEvent) {
    when (event.commandName) {
        "join" -> {
            val member = event.interaction.member.get()
            val voiceState = member.voiceState.block()!!
            val memberVoice = voiceState.channel.block()

            memberVoice?.sendConnectVoiceState(false, false)?.subscribe()

            event.reply("Joining your channel!").subscribe()
        }

        "leave" -> {
            // Disconnecting automatically destroys the player
            val member = event.interaction.member.get()
            val voiceState = member.voiceState.block()!!
            val memberVoice = voiceState.channel.block()

            memberVoice?.sendDisconnectVoiceState()?.subscribe()

            event.reply("Leaving your channel!").subscribe()
        }

        "play" -> {
            val input = event.getOption("identifier")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get()
            val link = lavalink.getOrCreateLink(event.interaction.guildId.get().asLong())
            link.getPlayer().block()!!.setIdentifier(input)
                .subscribe()
            event.reply("Playing!!").subscribe()
        }
    }
}
