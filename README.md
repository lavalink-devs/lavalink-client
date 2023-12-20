[VERSION]: https://img.shields.io/maven-central/v/dev.arbjerg/lavalink-client

# Java Lavalink client

> [!WARNING]
> This is a client for Lavalink v4 only.
> You cannot use this with Lavalink v3.


Feature overview:
- Works with any discord library (as long as they allow for sending direct request to discord)
- Load balancing based on server metrics and voice server region.
- Make your own custom load balancers and penalty providers!
- Lightweight
- Compiled for java 17

Current version (remove the `v` prefix): ![Latest version][VERSION]

Or copy/download it [here](https://maven.arbjerg.dev/#/releases/dev/arbjerg/lavalink-client)

Documentation can be found over at [https://client.lavalink.dev/](https://client.lavalink.dev/)

If you prefer javadoc-style documentation, you can find those [here](https://client.lavalink.dev/javadoc/)

### Gradle instructions
```gradle
repositories {
    maven("https://maven.lavalink.dev/releases") // Required for the protocol library
}

dependencies {
    implementation("dev.arbjerg:lavalink-client:VERSION")
}
```

### Maven instructions
```maven
<repositories>
    <repository>
        <id>ll-releases</id>
        <name>Lavalink Releases</name>
        <url>https://maven.lavalink.dev/releases</url>
    </repository>
</repositories>

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
    .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(client))
    // .... your jda configuration
    .build();
```

then connect to vc by using the [direct audio controller](https://ci.dv8tion.net/job/JDA5/javadoc/net/dv8tion/jda/api/JDA.html#getDirectAudioController()) like this:
```java
jda.getDirectAudioController().connect(voiceChannel);
```

> [!IMPORTANT]
> Using `Guild#getAudioManager()` will ***NOT*** work. This is because the audio manager makes **JDA** connect to the voice channel, and we want to send the event to LavaLink.
> You can however use `Member#getVoiceState` perfectly fine, this is also how you get the voice channel that your bot is in.
>
> You can get the current voice channel of your bot by calling `Guild#getSelfMember()#getVoiceState()#getChannel`

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


## Examples
The following examples are minimal implementations but show how the library works.
- Java examples
  - JDA (simple): [link](src/test/java/JavaJDAExample.java)
  - JDA (more complex example): [link](testbot/src/main/java/me/duncte123/testbot/Main.java)
- Kotlin examples
  - JDA: [link](src/test/kotlin/testScript.kt)
  - Discord4J: [link](src/test/kotlin/d4jTestScript.kt)

## Standalone usage
This library is made to not rely on and discord libraries and can be used as such.
In order to connect to a voice channel without any library you will need a [voice server update event from discord](https://discord.com/developers/docs/topics/voice-connections#retrieving-voice-server-information-example-voice-server-update-payload) containing the token, endpoint and session id.
In this example it is assumed that you have this information as an object named `event`.

Sample code for connecting to a voice channel without a discord library:

```java
import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion;
import dev.arbjerg.lavalink.protocol.v4.VoiceState;

// This is your lavalink client
LavalinkClient client = new LavalinkClient(/* your bot user id */);

// This is sample code, it will need modifications to work for you
public void onDiscordVoiceServerUpdate(VoiceServerUpdateEvent event) {
    VoiceState lavalinkVoiceState = new VoiceState(
        event.getToken(),
        event.getEndpoint(),
        event.getSessionId()
    );

    // If you want load-balancing based on the region of the voice server, use the enum.
    VoiceRegion region = VoiceRegion.fromEndpoint(event.getEndpoint());

    // You can omit the region parameter if you dont need region balancing.
    Link link = lavalink.getLink(event.getGuildId(), region);

    // Finally, tell lavalink to connect.
    link.onVoiceServerUpdate(lavalinkVoiceState)
}
```
