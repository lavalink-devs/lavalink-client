import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup;
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import dev.arbjerg.lavalink.protocol.v4.LoadResult;
import dev.arbjerg.lavalink.protocol.v4.Message;
import dev.arbjerg.lavalink.protocol.v4.Track;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class JavaJDAExample extends ListenerAdapter {
    private final LavalinkClient client;

    public static void main(String[] args) throws InterruptedException {
        new JavaJDAExample();
    }

    public JavaJDAExample() throws InterruptedException {
        final var token = System.getenv("BOT_TOKEN");
        this.client = new LavalinkClient(Helpers.getUserIdFromToken(token));

        this.client.getLoadBalancer().addPenaltyProvider(new VoiceRegionPenaltyProvider());

        this.registerLavalinkListeners();
        this.registerLavalinkNodes();

        JDABuilder.createDefault(token)
                .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(this.client))
                .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                .enableCache(CacheFlag.VOICE_STATE)
                .addEventListeners(this)
                .build()
                .awaitReady();
    }

    private void registerLavalinkNodes() {
        List.of(
            /*client.addNode(
                "Testnode",
                URI.create("ws://localhost:2333"),
                "youshallnotpass",
                RegionGroup.EUROPE
            ),*/

            client.addNode(
                "Mac-mini",
                URI.create("ws://192.168.1.139:2333/bepis"),
                "youshallnotpass",
                RegionGroup.US
            )
        ).forEach((node) -> {
            node.on(TrackStartEvent.class).subscribe((data) -> {
                final LavalinkNode node1 = data.getNode();
                final var event = data.getEvent();

                System.out.printf(
                        "%s: track started: %s%n",
                        node1.getName(),
                        event.getTrack().getInfo()
                );
            });
        });
    }

    private void registerLavalinkListeners() {
        this.client.on(dev.arbjerg.lavalink.client.ReadyEvent.class).subscribe((data) -> {
            final LavalinkNode node = data.getNode();
            final Message.ReadyEvent event = data.getEvent();

            System.out.printf(
                    "Node '%s' is ready, session id is '%s'!%n",
                    node.getName(),
                    event.getSessionId()
            );
        });

        this.client.on(StatsEvent.class).subscribe((data) -> {
            final LavalinkNode node = data.getNode();
            final Message.StatsEvent event = data.getEvent();

            System.out.printf(
                    "Node '%s' has stats, current players: %d/%d%n",
                    node.getName(),
                    event.getPlayingPlayers(),
                    event.getPlayers()
            );
        });
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        System.out.println(event.getJDA().getSelfUser().getAsTag() + " is ready!");

        event.getJDA().updateCommands()
                .addCommands(
                    Commands.slash("custom-request", "Testing custom requests"),
                    Commands.slash("join", "Join the voice channel you are in."),
                    Commands.slash("leave", "Leaves the vc"),
                    Commands.slash("play", "Play a song")
                            .addOption(
                                OptionType.STRING,
                                "identifier",
                                "The identifier of the song you want to play",
                                true
                            )
                )
                .queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getFullCommandName()) {
            case "join":
                joinHelper(event);
                break;
            case "leave":
                event.getJDA().getDirectAudioController().disconnect(event.getGuild());
                event.reply("Leaving your channel!").queue();
                break;
            case "play":
                final Guild guild = event.getGuild();

                // We are already connected, go ahead and play
                if (guild.getSelfMember().getVoiceState().inAudioChannel()) {
                    event.deferReply(false).queue();
                } else {
                    // Connect to VC first
                    joinHelper(event);
                }

                final String identifier = event.getOption("identifier").getAsString();
                final long guildId = guild.getIdLong();
                final Link link = this.client.getLink(guildId);

                link.loadItem(identifier).subscribe((item) -> {
                    if (item instanceof LoadResult.TrackLoaded trackLoaded) {
                        final Track track = trackLoaded.getData();

                        link.createOrUpdatePlayer()
                                .setEncodedTrack(track.getEncoded())
                                .setVolume(35)
                                .asMono()
                                .subscribe((ignored) -> {
                                    event.getHook().sendMessage("Now playing: " + track.getInfo().getTitle()).queue();
                                });
                    } else if (item instanceof LoadResult.PlaylistLoaded playlistLoaded) {
                        final int trackCount = playlistLoaded.getData().getTracks().size();
                        event.getHook()
                                .sendMessage("This playlist has " + trackCount + " tracks!")
                                .queue();
                    } else if (item instanceof LoadResult.SearchResult searchResult) {
                        final List<Track> tracks = searchResult.getData().getTracks();

                        if (tracks.isEmpty()) {
                            event.getHook().sendMessage("No tracks found!").queue();
                            return;
                        }

                        final Track firstTrack = tracks.get(0);

                        // This is a different way of updating the player! Choose your preference!
                        // This method will also create a player if there is not one in the server yet
                        link.updatePlayer((update) -> update.setEncodedTrack(firstTrack.getEncoded()).setVolume(35) )
                                .subscribe((ignored) -> {
                                    event.getHook().sendMessage("Now playing: " + firstTrack.getInfo().getTitle()).queue();
                                });

                    } else if (item instanceof LoadResult.NoMatches) {
                        event.getHook().sendMessage("No matches found for your input!").queue();
                    } else if (item instanceof LoadResult.LoadFailed fail) {
                        event.getHook().sendMessage("Failed to load track! " + fail.getData().getMessage()).queue();
                    }
                });

                break;
            case "custom-request":
                // Weird variable names? This is why you don't use switch statements for this :)
                final long crGuildId = event.getGuild().getIdLong();
                final Link crLink = this.client.getLink(crGuildId);

                crLink.getNode().customRequest(
                        (builder) -> builder.get().path("/version").header("Accept", "text/plain")
                ).subscribe((response) -> {
                    try (ResponseBody body = response.body()) {
                        final String bodyText = body.string();

                        event.reply("Response from version endpoint (with custom request): " + bodyText).queue();
                    } catch (IOException e) {
                        event.reply("Something went wrong! " + e.getMessage()).queue();
                    }
                });
                break;
            default:
                event.reply("Unknown command???").queue();
                break;
        }
    }

    // Makes sure that the bot is in a voice channel!
    private void joinHelper(SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (memberVoiceState.inAudioChannel()) {
            event.getJDA().getDirectAudioController().connect(memberVoiceState.getChannel());
        }

        event.reply("Joining your channel!").queue();
    }
}
