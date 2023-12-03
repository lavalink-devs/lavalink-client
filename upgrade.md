# Coming from the v3 client

First things first, there is no dependency on lavaplayer anymore. This means that all the track loading will have to be done via the server.
This also means that the following classes no longer exist:
- PlayerEventListenerAdapter

This client also uses project reactor, their docs are over at https://projectreactor.io/docs

## Classes that serve similar purposes
- `Lavalink`/`JDALavalink` -> `LavalinkClient`
- `Link`/`JDALink` -> `Link`
- `Link.State` -> `LinkState`
- `LavalinkSocket` -> `Node`
- `LavalinkRestClient` -> `Node`
- `LavalinkLoadBalancer` -> `DefaultLoadBalancer`
- `PenaltyProvider` -> `IPenaltyProvider`
- `IPlayer#stopTrack` -> Set encodedTrack to `null` on the player. 
- Any filters -> they are in `dev.arbjerg.lavalink.protocol.v4.Filters` class.

If you are missing anything from here, feel free to let me know in the lavalink discord
