import com.github.topi314.lavasearch.protocol.SearchResult as TopiSearchResult
import dev.arbjerg.lavalink.client.*
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider
import dev.arbjerg.lavalink.client.protocol.*
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
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
        .subscribe { event ->
            println("Node '${event.node.name}' is ready, session id is '${event.sessionId}'!")
        }

    // Testing for exceptions thrown in the handler
    var counter = 0

    client.on<StatsEvent>()
        .subscribe({ event ->
            counter++
            println("Node '${event.node.name}' has stats, current players: ${event.playingPlayers}/${event.players}")

            // WARNING: if you do not catch your exception here, the subscription will be canceled.
            if (counter == 2) {
                throw RuntimeException("Counter has reached 2")
            }
        }) { err ->
            err.printStackTrace()
        }

    client.on<StatsEvent>()
        .subscribe { event ->
            println("[event 2] Node '${event.node.name}' has stats, current players: ${event.playingPlayers}/${event.players}")
        }

    client.on<EmittedEvent<*>>()
        .subscribe { event ->
            if (event is TrackStartEvent) {
                println("Is a track start event!")
            }

            val node = event.node

            println("Node '${node.name}' emitted event: $event")
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
                            Commands.slash("custom-json-request", "Testing custom json requests"),
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
            URI.create("ws://mac-mini.local.duncte123.lgbt:2333/bepis"),
            "youshallnotpass",
            RegionGroup.US
        )
    )
        .forEach { node ->
            node.on<TrackStartEvent>()
                // .next() // Adding next turns this into a 'once' listener.
                .subscribe { event ->
                    // A new track is started!
                    println("${event.node.name}: track started: ${event.track.info}")
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

        "custon-json-request" -> {
            val guildId = event.guild!!.idLong
            val link = lavalink.getLink(guildId)

            link.node.customJsonRequest<TopiSearchResult>{
                it.get().path("/v4/loadtracks?identifier=ytsearch%3Anever%20gonna%20give%20you%20up")
            }.doOnSuccess {
                if (it == null) {
                    event.reply("Failed to load tracks!").queue()
                    return@doOnSuccess
                }
                event.reply(
                    """
                        Response from loadsearch endpoint.
                        tracks: ${it.tracks.size}
                        albums: ${it.albums.size}
                        artists: ${it.artists.size}
                        playlists: ${it.playlists.size}
                        texts: ${it.texts.size}
                        """.trimIndent()
                ).queue()
            }.subscribe()
        }

        "join" -> {
            joinHelper(event)
        }

        "leave" -> {
            // Disconnecting automatically destroys the player
            event.jda.directAudioController.disconnect(event.guild!!)
            event.reply("Leaving your channel!").queue()

            val guildId = event.guild!!.idLong
            val link = lavalink.getLink(guildId)

            println(link.node.playerCache)
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
                    is TrackLoaded -> {
                        link.createOrUpdatePlayer().setTrack(item.track)
                            .subscribe {
                                event.hook.sendMessage("Now playing ${item.track.info.title}!").queue()
                            }
                    }

                    is LoadFailed -> {
                        event.hook.sendMessage("Failed to load track! ${item.exception.message}").queue()
                    }
                    is NoMatches -> {
                        event.hook.sendMessage("No matches found for your input!").queue()
                    }
                    is PlaylistLoaded -> {
                        event.hook.sendMessage("IMPLEMENT PLAYLIST LOADED").queue()
                    }
                    is SearchResult -> {
                        if (item.tracks.isEmpty()) {
                            event.hook.sendMessage("Nothing found").queue()
                            return@loadItem
                        }

                        val track = item.tracks.first()

                        link.createOrUpdatePlayer().setTrack(track)
                            .subscribe {
                                event.hook.sendMessage("Now playing ${track.info.title}!").queue()
                            }
                    }
                }
            }
        }
    }
}
