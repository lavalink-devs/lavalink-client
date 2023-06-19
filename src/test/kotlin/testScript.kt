import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Message
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.net.URI

fun main() {
    val client = LavalinkClient(
        405777831730085889L,
    )

    val jda = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
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
                    println("${event.jda.selfUser.asTag} is ready!")
                    event.jda.getGuildById(191245668617158656L)!!
                        .updateCommands()
                        .addCommands(
                            Commands.slash("join", "Join the voice channel you are in."),
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

    val node = client.addNode("Testnode", URI.create("ws://localhost:2333"), "youshallnotpass")

    /*node.loadItem("https://www.youtube.com/watch?v=G87p148EOjo&pp=ygUJdG9tIGNhcmR5").subscribe {
        println(it)
    }*/

    node.on<Message.EmittedEvent.TrackStartEvent>()
        .next()
        .subscribe {
            // A new track is started!
            println("A new track is started!")
            println(it.track.info)
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
        "play" -> {
            val identifier = event.getOption("input")!!.asString
            val guildId = event.guild!!.idLong
            val link = lavalink.getLink(guildId)
            val node = link.node

            node.loadItem(identifier).subscribe { item ->
                link.getPlayer().subscribe { player ->
                    when (item) {
                        is LoadResult.TrackLoaded -> {
                            player.setEncodedTrack(item.data.encoded)
                                .asMono()
                                .subscribe {
                                    println("Player has been updated")
                                }
                        }

                        is LoadResult.LoadFailed -> TODO()
                        is LoadResult.NoMatches -> TODO()
                        is LoadResult.PlaylistLoaded -> TODO()
                        is LoadResult.SearchResult -> TODO()
                    }
                }
            }
        }
    }
}
