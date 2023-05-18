import dev.arbjerg.lavalink.client.LavalinkPlayer

val player = LavalinkPlayer()


fun doThing() {

    player.setVolume(5.0)
        .setPaused(true)
        .asMono()
        .subscribe()


}
