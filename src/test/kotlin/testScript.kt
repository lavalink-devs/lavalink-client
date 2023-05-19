import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.protocol.v4.Message
import java.net.URI


fun doThing() {
    val client = LavalinkClient()

    val node = client.addNode("", URI.create(""), "")

    node.on<Message.EmittedEvent.TrackStartEvent>()
        .next()
        .subscribe {
            // A new track is started!
        }
}
