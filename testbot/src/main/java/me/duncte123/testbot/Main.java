package me.duncte123.testbot;

import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup;
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final int SESSION_INVALID = 4006;

    public static void main(String[] args) throws InterruptedException {
        final var token = System.getenv("BOT_TOKEN");
        final LavalinkClient client = new LavalinkClient(Helpers.getUserIdFromToken(token));

        client.getLoadBalancer().addPenaltyProvider(new VoiceRegionPenaltyProvider());

        registerLavalinkListeners(client);
        registerLavalinkNodes(client);

        final var jda = JDABuilder.createDefault(token)
            .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(client))
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
            .enableCache(CacheFlag.VOICE_STATE)
            .addEventListeners(new JDAListener(client))
            .build()
            .awaitReady();

        // Got a lot of 4006 closecodes? Try this "fix"
        client.on(WebSocketClosedEvent.class).subscribe((event) -> {
            if (event.getCode() == SESSION_INVALID) {
                final var guildId = event.getGuildId();
                final var guild = jda.getGuildById(guildId);

                if (guild == null) {
                    return;
                }

                final var connectedChannel = guild.getSelfMember().getVoiceState().getChannel();

                // somehow
                if (connectedChannel == null) {
                    return;
                }

                jda.getDirectAudioController().reconnect(connectedChannel);
            }
        });
    }


    private static void registerLavalinkNodes(LavalinkClient client) {
        List.of(
            client.addNode(
                "Testnode",
                URI.create("ws://localhost:2333"),
                "youshallnotpass",
                RegionGroup.EUROPE
            ),

            client.addNode(
                "Pi-local",
                URI.create("ws://pi.local.duncte123.lgbt:2333"),
                "youshallnotpass",
                RegionGroup.US
            )
        ).forEach((node) -> {
            node.on(TrackStartEvent.class).subscribe((event) -> {
                final LavalinkNode node1 = event.getNode();

                LOG.info(
                    "{}: track started: {}",
                    node1.getName(),
                    event.getTrack().getInfo()
                );
            });
        });
    }

    private static void registerLavalinkListeners(LavalinkClient client) {
        client.on(ReadyEvent.class).subscribe((event) -> {
            final LavalinkNode node = event.getNode();

            LOG.info(
                "Node '{}' is ready, session id is '{}'!",
                node.getName(),
                event.getSessionId()
            );
        });

        client.on(StatsEvent.class).subscribe((event) -> {
            final LavalinkNode node = event.getNode();

            LOG.info(
                "Node '{}' has stats, current players: {}/{} (link count {})",
                node.getName(),
                event.getPlayingPlayers(),
                event.getPlayers(),
                client.getLinks().size()
            );
        });

        client.on(EmittedEvent.class).subscribe((event) -> {
            if (event instanceof TrackStartEvent) {
                LOG.info("Is a track start event!");
            }

            final var node = event.getNode();

            LOG.info(
                "Node '{}' emitted event: {}",
                node.getName(),
                event
            );
        });
    }
}
