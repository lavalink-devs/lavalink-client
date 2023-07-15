import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Message
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.net.URI

private const val COMMAND_GUILD_ID = 1082302532421943407L

fun main() {
    val client = LavalinkClient()

    JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
        .setVoiceDispatchInterceptor(JDAVoiceUpdateListener(client))
        .enableIntents(
            GatewayIntent.GUILD_VOICE_STATES,
        )
        .enableCache(
            CacheFlag.VOICE_STATE,
        )
        .addEventListeners(object : EventListener {
            override fun onEvent(event: GenericEvent) {
                if (event is SlashCommandInteractionEvent) {
                    handleSlash(client, event)
                } else if (event is ReadyEvent) {
                    client.userId = event.jda.selfUser.idLong
                    registerNode(client)

                    println("${event.jda.selfUser.asTag} is ready!")
                    event.jda.getGuildById(COMMAND_GUILD_ID)!!
                        .updateCommands()
                        .addCommands(
                            Commands.slash("join", "Join the voice channel you are in."),
                            Commands.slash("leave", "Leaves the vc"),
                            Commands.slash("play", "Play a song")
                                .addOption(
                                    OptionType.STRING,
                                    "identifier",
                                    "The identifier of the song you want to play",
                                    true
                                )
                        )
                        .queue()
                }
            }
        })
        .build()
        .awaitReady()
}

fun registerNode(client: LavalinkClient) {
    val node = client.addNode(
        "Testnode",
        URI.create("ws://localhost:2333"),
        "youshallnotpass"
    )

    val node2 = client.addNode(
        "Mac-mini",
        URI.create("ws://192.168.1.139:2333"),
        "youshallnotpass"
    )

    node.on<Message.EmittedEvent.TrackStartEvent>()
        .next()
        .subscribe {
            // A new track is started!
            println("A new track is started!")
            println(it.track.info)
        }

    node2.on<Message.EmittedEvent.TrackStartEvent>()
        .next()
        .subscribe {
            // A new track is started!
            println("MAC_MINI: track started: ${it.track.info}")
        }
}

fun handleSlash(lavalink: LavalinkClient, event: SlashCommandInteractionEvent) {
    when (event.fullCommandName) {
        "join" -> {
            val member = event.member!!
            val memberVoice = member.voiceState!!

            if (memberVoice.inAudioChannel()) {
                event.jda.directAudioController.connect(memberVoice.channel!!)
            }

            event.reply("Joining your channel!").queue()
        }

        "leave" -> {
            // Disconnecting automatically destroys the player
            event.jda.directAudioController.disconnect(event.guild!!)
            event.reply("Leaving your channel!").queue()
        }

        "play" -> {
            val identifier = event.getOption("identifier")!!.asString
            val guildId = event.guild!!.idLong
            val link = lavalink.getLink(guildId)
            val node = link.node

            event.deferReply(false).queue()

            node.loadItem(identifier).subscribe { item ->
                link.getPlayer().subscribe getPlayer@{ player ->
                    when (item) {
                        is LoadResult.TrackLoaded -> {
                            player.setEncodedTrack(item.data.encoded)
                                .asMono()
                                .subscribe {
                                    event.hook.sendMessage("Now playing ${item.data.info.title}!").queue()
                                }
                        }

                        is LoadResult.LoadFailed -> {
                            event.hook.sendMessage("Failed to load track! ${item.data.message}").queue()
                        }
                        is LoadResult.NoMatches -> {
                            event.hook.sendMessage("No matches found for your input!").queue()
                        }
                        is LoadResult.PlaylistLoaded -> TODO()
                        is LoadResult.SearchResult -> {
                            if (item.data.tracks.isEmpty()) {
                                event.hook.sendMessage("Nothing found").queue()
                                return@getPlayer
                            }

                            val track = item.data.tracks.first()

                            player.setEncodedTrack(track.encoded)
                                .asMono()
                                .subscribe {
                                    event.hook.sendMessage("Now playing ${track.info.title}!").queue()
                                }
                        }
                    }
                }
            }
        }
    }
}
