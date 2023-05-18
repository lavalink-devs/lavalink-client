import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.Message
import java.net.URI

val client = LavalinkClient()

val player = LavalinkPlayer()


fun doThing() {

    val node = client.addNode("", URI.create(""), "")

    node.on<Message.EmittedEvent.TrackStartEvent>()
        .next()
        .subscribe {
            // A new track is started!
        }

    player.setVolume(5.0)
        .setPaused(true)
        .asMono()
        .subscribe()


}
