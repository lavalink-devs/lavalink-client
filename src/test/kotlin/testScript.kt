import dev.arbjerg.lavalink.client.*
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent as JDAReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.net.URI

fun main() {
    val token = System.getenv("BOT_TOKEN")
    val client = LavalinkClient(
        getUserIdFromToken(token)
    )

    client.loadBalancer.addPenaltyProvider(VoiceRegionPenaltyProvider())

    client.on<ReadyEvent>()
        .subscribe { (node, event) ->
            println("Node '${node.name}' is ready, session id is '${event.sessionId}'!")
        }

    client.on<StatsEvent>()
        .subscribe { (node, event) ->
            println("Node '${node.name}' has stats, current players: ${event.playingPlayers}/${event.players}")
        }

    JDABuilder.createDefault(token)
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
                } else if (event is JDAReadyEvent) {
                    registerNode(client)

                    println("${event.jda.selfUser.asTag} is ready!")
                    event.jda.updateCommands()
                        .addCommands(
                            Commands.slash("custom-request", "Testing custom requests"),
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
    listOf(
        /*client.addNode(
            "Testnode",
            URI.create("ws://localhost:2333"),
            "youshallnotpass",
            RegionGroup.EUROPE
        ),*/

        client.addNode(
            "Mac-mini",
            URI.create("ws://192.168.1.139:2333/bepis"),
            "youshallnotpass",
            RegionGroup.US
        )
    )
        .forEach { node ->
            node.on<TrackStartEvent>()
                // .next() // Adding next turns this into a 'once' listener.
                .subscribe { (node, event) ->
                    // A new track is started!
                    println("${node.name}: track started: ${event.track.info}")
                }
        }
}

private fun joinHelper(event: SlashCommandInteractionEvent) {
    val member = event.member!!
    val memberVoice = member.voiceState!!

    if (memberVoice.inAudioChannel()) {
        event.jda.directAudioController.connect(memberVoice.channel!!)
    }

    event.reply("Joining your channel!").queue()
}

private fun handleSlash(lavalink: LavalinkClient, event: SlashCommandInteractionEvent) {
    when (event.fullCommandName) {
        "custom-request" -> {
            val guildId = event.guild!!.idLong
            val link = lavalink.getLink(guildId)

            link.node.customRequest {
                it.get()
                    .path("/version")
                    .header("Accept", "text/html")
            }.subscribe {
                it.body?.use { body ->
                    val bodyStr = body.string()

                    println(bodyStr)
                    event.reply("Response from version endpoint (with custom request): $bodyStr").queue()
                }
            }
        }

        "join" -> {
            joinHelper(event)
        }

        "leave" -> {
            // Disconnecting automatically destroys the player
            event.jda.directAudioController.disconnect(event.guild!!)
            event.reply("Leaving your channel!").queue()
        }

        "play" -> {
            if (!event.guild!!.selfMember.voiceState!!.inAudioChannel()) {
                joinHelper(event)
            } else {
                event.deferReply(false).queue()
            }

            val identifier = event.getOption("identifier")!!.asString
            val guildId = event.guild!!.idLong
            val link = lavalink.getLink(guildId)

            link.loadItem(identifier).subscribe loadItem@ { item ->
                when (item) {
                    is LoadResult.TrackLoaded -> {
                        link.createOrUpdatePlayer().setEncodedTrack(item.data.encoded)
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
                    is LoadResult.PlaylistLoaded -> {
                        event.hook.sendMessage("IMPLEMENT PLAYLIST LOADED").queue()
                    }
                    is LoadResult.SearchResult -> {
                        if (item.data.tracks.isEmpty()) {
                            event.hook.sendMessage("Nothing found").queue()
                            return@loadItem
                        }

                        val track = item.data.tracks.first()

                        link.createOrUpdatePlayer().setEncodedTrack(track.encoded)
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
