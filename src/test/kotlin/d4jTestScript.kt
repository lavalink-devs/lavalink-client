import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.libraries.discord4j.installVoiceHandler
import dev.arbjerg.lavalink.libraries.discord4j.joinChannel
import dev.arbjerg.lavalink.libraries.discord4j.leave
import dev.arbjerg.lavalink.protocol.v4.Message
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
    val client = LavalinkClient()
    val discord = DiscordClientBuilder.create(System.getenv("BOT_TOKEN"))
        .build()
        .gateway()
        .setEnabledIntents(IntentSet.all())
        .login().block()!!

    client.userId = discord.selfId.asLong()
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
    node.on<Message.EmittedEvent.TrackStartEvent>()
        .next()
        .subscribe {
            // A new track is started!
            println("A new track is started!")
            println(it.track.info)
        }
}

fun handleSlash(lavalink: LavalinkClient, event: ChatInputInteractionEvent) {
    when (event.commandName) {
        "join" -> {
            val guild = event.interaction.guild.block()!!
            val voiceState = guild.voiceStates
                .blockFirst()!!
            val memberVoice = voiceState.channel.block()

            if (memberVoice != null) {
                event.client.joinChannel(memberVoice).subscribe()
            }


            event.reply("Joining your channel!").subscribe()
        }

        "leave" -> {
            // Disconnecting automatically destroys the player
            event.client.leave(event.interaction.guildId.get()).subscribe()
            event.reply("Leaving your channel!").subscribe()
        }

        "play" -> {
            val input = event.getOption("identifier")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .get()
            val link = lavalink.getLink(event.interaction.guildId.get().asLong())
            link.getPlayer().block()!!.setIdentifier(input)
                .asMono()
                .block()
            event.reply("Playing!!").subscribe()
        }
    }
}
