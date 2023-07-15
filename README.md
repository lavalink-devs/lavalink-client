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
