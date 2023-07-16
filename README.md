# Lavalink client
Proposal for a v4 lavalink client

## Installation in JDA

Lavalink client ships with a voice interceptor for JDA

```java
LavalinkClient client = new LavalinkClient(
    0000000:, // your bot's id, can also be set from the ready event.
);


JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
    // .... your jda configuration
    .setVoiceDispatchInterceptor(JDAVoiceUpdateListener(client))
    // .... your jda configuration
    .build();
```

then connect to vc as you would normally.

## Installation in discord4j
Good luck

```java
LavalinkClient client = new LavalinkClient();
DiscordClient discord = DiscordClientBuilder.create(System.getenv("BOT_TOKEN"))
    .build()
    .gateway()
    .setEnabledIntents(IntentSet.all())
    .login()
    .block();

client.setUserId(discord.selfId.asLong());

D4JVoiceHandler.install(discord, client);
```

or if you're using kotlin

```kotlin
discord.installVoiceHandler(client)
```

Then connect like this
```java
var voiceState = /* Get the voice state of the member that ran the command */;
var memberVoice = voiceState.channel.block();

memberVoice.sendConnectVoiceState(false, false).subscribe();
```

And disconnect like this
```java
val voiceState = /* Get the voice state of the member that ran the command */;
val memberVoice = voiceState.channel.block();

memberVoice.sendDisconnectVoiceState().subscribe();
```
