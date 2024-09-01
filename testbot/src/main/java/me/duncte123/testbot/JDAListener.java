package me.duncte123.testbot;

import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.player.FilterBuilder;
import dev.arbjerg.lavalink.protocol.v4.Karaoke;
import me.duncte123.lyrics.model.Lyrics;
import me.duncte123.lyrics.model.TextLyrics;
import me.duncte123.lyrics.model.TimedLyrics;
import me.duncte123.testbot.music.AudioLoader;
import me.duncte123.testbot.music.GuildMusicManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JDAListener extends ListenerAdapter {
    private static final long DUNCTE = 191231307290771456L;

    private static final Logger LOG = LoggerFactory.getLogger(JDAListener.class);

    public final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();
    private final LavalinkClient client;
    private final EvalEngine evalEngine;

    public JDAListener(LavalinkClient client) {
        this.client = client;
        this.evalEngine = new EvalEngine(client);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        LOG.info("{} is ready!", event.getJDA().getSelfUser().getAsTag());

        event.getJDA().updateCommands()
            .addCommands(
                Commands.slash("lyrics", "Testing custom requests"),
                Commands.slash("node", "What node am I on?"),
                Commands.slash("eval", "test out some code")
                    .addOption(
                        OptionType.STRING,
                        "script",
                        "Script to eval",
                        true
                    ),
                Commands.slash("join", "Join the voice channel you are in."),
                Commands.slash("leave", "Leaves the vc"),
                Commands.slash("stop", "Stops the current track"),
                Commands.slash("pause", "Pause or unpause the player"),
                Commands.slash("now-playing", "Shows what is currently playing"),
                Commands.slash("play", "Play a song")
                    .addOption(
                        OptionType.STRING,
                        "identifier",
                        "The identifier of the song you want to play",
                        true
                    ),
                Commands.slash("play-file", "Play a song from a file")
                    .addOption(
                        OptionType.ATTACHMENT,
                        "file",
                        "the file to play",
                        true
                    ),
                Commands.slash("karaoke", "Turn karaoke on or off")
                    .addSubcommands(
                        new SubcommandData("on", "Turn karaoke on"),
                        new SubcommandData("off", "Turn karaoke on")
                    )
            )
            .queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();

        if (guild == null) {
            return;
        }

        switch (event.getFullCommandName()) {
            case "node": {
                final var link = this.client.getLinkIfCached(guild.getIdLong());

                if (link == null) {
                    event.reply("No link for this guild").queue();
                    break;
                }

                event.reply("Connected to: " + link.getNode().getName()).queue();

                break;
            }
            case "eval": {
                final var user = event.getUser();

                if (user.getIdLong() != DUNCTE) {
                    event.replyFormat("I'm sorry %s, I'm afraid I can't let you do that", user.getAsMention()).queue();
                    return;
                }

                event.deferReply().queue();

                Thread.ofVirtual().start(() -> {
                    try {
                        final var script = event.getOption("script").getAsString();
                        final var result = this.evalEngine.eval(event, script);
                        this.parseEvalResult(event, result);
                    } catch (final Exception e) {
                        LOG.error("Failed to eval", e);
                    }
                });
                break;
            }
            case "join":
                joinHelper(event);
                break;
            case "stop":
                event.reply("Stopped the current track and clearing the queue").queue();
                this.getOrCreateMusicManager(event.getGuild().getIdLong()).stop();
                break;
            case "leave":
                event.getJDA().getDirectAudioController().disconnect(guild);
                event.reply("Leaving your channel!").queue();
                break;
            case "now-playing": {
                final var link = this.client.getOrCreateLink(guild.getIdLong());
                final var player = link.getCachedPlayer();

                if (player == null) {
                    event.reply("Not connected or no player available!").queue();
                    break;
                }

                final var track = player.getTrack();

                if (track == null) {
                    event.reply("Nothing playing currently!").queue();
                    break;
                }

                final var trackInfo = track.getInfo();

                event.reply(
                    "Currently playing: %s\nDuration: %s/%s\nRequester: <@%s>".formatted(
                        trackInfo.getTitle(),
                        player.getPosition(),
                        trackInfo.getLength(),
                        track.getUserData(MyUserData.class).requester()
                    )
                ).queue();
                break;
            }
            case "pause":
                this.client.getOrCreateLink(guild.getIdLong())
                    .getPlayer()
                    .flatMap((player) -> player.setPaused(!player.getPaused()))
                    .subscribe((player) -> {
                        event.reply("Player has been " + (player.getPaused() ? "paused" : "resumed") + "!").queue();
                    });
                break;
            case "karaoke on": {
                final long guildId = guild.getIdLong();
                final Link link = this.client.getOrCreateLink(guildId);

                link.createOrUpdatePlayer()
                    .setFilters(
                        new FilterBuilder()
                            .setKaraoke(
                                new Karaoke()
                            )
                            .build()
                    )
                    .subscribe();
                event.reply("turning karaoke on!").queue();
                break;
            }
            case "karaoke off": {
                final long guildId = guild.getIdLong();
                final Link link = this.client.getOrCreateLink(guildId);

                link.createOrUpdatePlayer()
                    .setFilters(
                        new FilterBuilder()
                            .setKaraoke(null)
                            .build()
                    )
                    .subscribe();
                event.reply("turning karaoke off!").queue();
                break;
            }
            case "play": {
                // We are already connected, go ahead and play
                if (guild.getSelfMember().getVoiceState().inAudioChannel()) {
                    event.deferReply(false).queue();
                } else {
                    // Connect to VC first
                    joinHelper(event);
                }

                final String identifier = event.getOption("identifier").getAsString();
                final long guildId = guild.getIdLong();
                final Link link = this.client.getOrCreateLink(guildId);
                final var mngr = this.getOrCreateMusicManager(guildId);

                link.loadItem(identifier).subscribe(new AudioLoader(event, mngr));

                break;
            }
            case "play-file": {
                // We are already connected, go ahead and play
                if (guild.getSelfMember().getVoiceState().inAudioChannel()) {
                    event.deferReply(false).queue();
                } else {
                    // Connect to VC first
                    joinHelper(event);
                }

                final var file = event.getOption("file").getAsAttachment();
                final long guildId = guild.getIdLong();
                final Link link = this.client.getOrCreateLink(guildId);
                final var mngr = this.getOrCreateMusicManager(guildId);

                link.loadItem(file.getUrl()).subscribe(new AudioLoader(event, mngr));

                break;
            }
            // Required plugin: https://github.com/DuncteBot/java-timed-lyrics
            case "lyrics": {
                final Link link = this.client.getOrCreateLink(guild.getIdLong());
                final var node = link.getNode();
                final var sessionId = node.getSessionId();
                final var guildId = guild.getId();

                node.customJsonRequest(
                    Lyrics.class,
                    (builder) -> builder.get().path(
                        "/v4/sessions/%s/players/%s/lyrics".formatted(sessionId, guildId)
                    )
                ).subscribe((response) -> {
                    switch (response) {
                        case TextLyrics lyrics -> {
                            event.replyEmbeds(
                                new EmbedBuilder()
                                    .setDescription(lyrics.text())
                                    .build()
                            ).queue();
                        }
                        case TimedLyrics lyrics -> {
                            final var position = link.getCachedPlayer().getState().getPosition();
                            final var builder = new StringBuilder();

                            lyrics.lines().forEach((line) -> {
                                if (line.range().start() <= position && position <= line.range().end()) {
                                    builder.append("__**%s**__\n".formatted(line.line()));
                                } else {
                                    builder.append(line.line()).append("\n");
                                }
                            });

                            event.replyEmbeds(
                                new EmbedBuilder()
                                    .setDescription(builder.toString())
                                    .build()
                            ).queue();
                        }
                        default -> event.reply("Unknown lyrics: " + response.getClass()).queue();
                    }
                }, (err) -> {
                    event.reply(err.getMessage()).queue();
                });
                break;
            }
            default:
                event.reply("Unknown command???").queue();
                break;
        }
    }

    private void parseEvalResult(SlashCommandInteractionEvent event, Object result) {
        final var hook = event.getHook();

        if (result == null) {
            hook.sendMessage("No result").queue();
            return;
        }

        switch (result) {
            case Throwable thr: {
                hook.sendMessage("ERROR: " + thr).queue();
                break;
            }

            case RestAction<?> ra: {
                ra.queue(
                    (res) -> {
                        hook.sendMessage("Rest action success: " + res).queue();
                    },
                    (err) -> {
                        hook.sendMessage("Rest action error: " + err).queue();
                    }
                );
                break;
            }

            default: {
                hook.sendMessage("result: " + result).queue();
                break;
            }
        }
    }

    private GuildMusicManager getOrCreateMusicManager(long guildId) {
        synchronized(this) {
            var mng = this.musicManagers.get(guildId);

            if (mng == null) {
                mng = new GuildMusicManager(guildId, this.client);
                this.musicManagers.put(guildId, mng);
            }

            return mng;
        }
    }

    // Makes sure that the bot is in a voice channel!
    private void joinHelper(SlashCommandInteractionEvent event) {
        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (memberVoiceState.inAudioChannel()) {
            event.getJDA().getDirectAudioController().connect(memberVoiceState.getChannel());
        }

        this.getOrCreateMusicManager(member.getGuild().getIdLong());

        event.reply("Joining your channel!").queue();
    }
}
