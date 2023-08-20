# Lavalink client
Proposal for a v4 lavalink client

## Installation in JDA

Lavalink client ships with a voice interceptor for JDA

```java
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

then connect to vc as you would normally.

## Installation in discord4j
Good luck

```java
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
