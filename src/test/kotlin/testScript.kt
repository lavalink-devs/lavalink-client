import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.protocol.v4.Message
import java.net.URI

fun main() {
    val client = LavalinkClient(
        405777831730085889L,
    )

    val node = client.addNode("Testnode", URI.create("ws://localhost:2333"), "youshallnotpass")

    node.loadItem("https://www.youtube.com/watch?v=G87p148EOjo&pp=ygUJdG9tIGNhcmR5").subscribe {
        println(it)
    }

    node.on<Message.EmittedEvent.TrackStartEvent>()
        .next()
        .subscribe {
            // A new track is started!
            println("A new track is started!")
            println(it.track.info)
        }
}
