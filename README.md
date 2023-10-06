[VERSION]: https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.arbjerg.dev%2Fsnapshots%2Fdev%2Farbjerg%2Flavalink-client%2Fmaven-metadata.xml

# Java Lavalink client
This is a client for Lavalink v4 only. You cannot use this with Lavalink v3.

Feature overview:
- Works with any discord library (as long as they allow for sending direct request to discord)
- Load balancing based on server metrics and voice server region.
- Make your own custom load balancers and penalty providers!

Current version (remove the `v` prefix): ![Latest version][VERSION]

Or copy/download it [here](https://maven.arbjerg.dev/#/snapshots/dev/arbjerg/lavalink-client)

### Gradle instructions
```gradle
repositories {
    maven("https://maven.arbjerg.dev/snapshots")
}

dependencies {
    implementation("dev.arbjerg:lavalink-client:VERSION")
}
```

### Maven instructions
```maven
<repository>
    <id>arbjerg</id>
    <name>arbjerg</name>
    <url>https://maven.arbjerg.dev/snapshots</url>
</repository>

<dependency>
  <groupId>dev.arbjerg</groupId>
  <artifactId>lavalink-client</artifactId>
  <version>VERSION</version>
</dependency>
```

## Installation and usage with JDA

Lavalink client ships with a voice interceptor for JDA

```java
// Helpers is a class provided by this lib!
String botToken = System.getenv("BOT_TOKEN");
LavalinkClient client = new LavalinkClient(
    Helpers.getUserIdFromToken(botToken)
);

JDABuilder.createDefault(botToken)
    // .... your jda configuration
    .setVoiceDispatchInterceptor(JDAVoiceUpdateListener(client))
    // .... your jda configuration
    .build();
```

then connect to vc by using the [direct audio controller](https://ci.dv8tion.net/job/JDA5/javadoc/net/dv8tion/jda/api/JDA.html#getDirectAudioController()) like this:
```java
jda.getDirectAudioController().connect(voiceChannel);
```


Note: Using `Guild#getAudioManager()` will ***NOT*** work.

## Installation and usage with Discord4j

Installation for D4J is a little bit different from usual, mainly because D4j does not have a voice intercepting system.

```java
// Helpers is a class provided by this lib!
String botToken = System.getenv("BOT_TOKEN");
LavalinkClient client = new LavalinkClient(
    Helpers.getUserIdFromToken(botToken)
);
        
DiscordClient discord = DiscordClientBuilder.create(botToken)
    .build()
    .gateway()
    .setEnabledIntents(IntentSet.all())
    .login()
    .block();

D4JVoiceHandler.install(discord, client);
```

or if you're using kotlin

```kotlin
discord.installVoiceHandler(client)
```

Then connect like this
```java
var voiceState = /* Get the voice state of the member that ran the command */;
var memberVoice = voiceState.getChannel().block();

memberVoice.sendConnectVoiceState(false, false).subscribe();
```

And disconnect like this
```java
var voiceState = /* Get the voice state of the member that ran the command */;
var memberVoice = voiceState.getChannel().block();

memberVoice.sendDisconnectVoiceState().subscribe();
```

Alternatively, you can use `Discord4JUtils.leave(gatewayClient, guildId);` as that does not access any voice states.


## Standalone usage
This library is made to not rely on and discord libraries and can be used as such.
In order to connect to a voice channel without any library you will need a [voice server update event from discord](https://discord.com/developers/docs/topics/voice-connections#retrieving-voice-server-information-example-voice-server-update-payload) containing the token, endpoint and session id.
In this example it is assumed that you have this information as an object named `event`.

Sample code for connecting to a voice channel without a discord library:

```java
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion;
import dev.arbjerg.lavalink.protocol.v4.VoiceState;

// This is your lavalink client
LavalinkClient client = new LavalinkClient(/* your bot user id */);

// This is sample code, it will need modifications to work for you
public void onVoiceServerUpdate(VoiceServerUpdateEvent event) {
    VoiceState lavalinkVoiceState = new VoiceState(
        event.getToken(),
        event.getEndpoint(),
        event.getSessionId()
    );

    // If you want load-balancing based on the region of the voice server, use the enum.
    VoiceRegion region = VoiceRegion.fromEndpoint(event.getEndpoint());

    // You can omit the region parameter if you dont need region balancing.
    LavalinkNode node = lavalink.getLink(event.getGuildId(), region).getNode();

    // Finally, tell lavalink to connect.
    node.createOrUpdatePlayer(event.getGuildId())
        .setVoiceState(lavalinkVoiceState)
        .asMono()
        .block()
}
```
