package me.duncte123.testbot;

import dev.arbjerg.lavalink.client.LavalinkClient;
import dev.arbjerg.lavalink.client.Link;
import dev.arbjerg.lavalink.client.protocol.FilterBuilder;
import dev.arbjerg.lavalink.protocol.v4.Karaoke;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JDAListener extends ListenerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(JDAListener.class);

    private final LavalinkClient client;

    public JDAListener(LavalinkClient client) {
        this.client = client;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        LOG.info(event.getJDA().getSelfUser().getAsTag() + " is ready!");

        event.getJDA().updateCommands()
            .addCommands(
                Commands.slash("custom-request", "Testing custom requests"),
                Commands.slash("join", "Join the voice channel you are in."),
                Commands.slash("leave", "Leaves the vc"),
                Commands.slash("stop", "Stops the current track"),
                Commands.slash("pause", "Pause or unpause the plauer"),
                Commands.slash("play", "Play a song")
                    .addOption(
                        OptionType.STRING,
                        "identifier",
                        "The identifier of the song you want to play",
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
        switch (event.getFullCommandName()) {
            case "join":
                joinHelper(event);
                break;
            case "stop":
                this.client.getLink(event.getGuild().getIdLong())
                    .updatePlayer(
                        (update) -> update.setTrack(null).setPaused(false)
                    )
                    .subscribe((__) -> {
                        event.reply("Stopped the current track").queue();
                    });
                break;
            case "leave":
                event.getJDA().getDirectAudioController().disconnect(event.getGuild());
                event.reply("Leaving your channel!").queue();
                break;
            case "pause":
                this.client.getLink(event.getGuild().getIdLong())
                    .getPlayer()
                    .flatMap((player) -> player.setPaused(!player.getPaused()))
                    .subscribe((player) -> {
                        event.reply("Player has been " + (player.getPaused() ? "paused" : "resumed") + "!").queue();
                    });
                break;
            case "karaoke on": {
                final Guild guild = event.getGuild();
                final long guildId = guild.getIdLong();
                final Link link = this.client.getLink(guildId);

                link.createOrUpdatePlayer()
                    .setFilters(
                        new FilterBuilder()
                            .setKaraoke(
                                new Karaoke()
                            )
                            .build()
                    )
                    .subscribe();
                break;
            }
            case "karaoke off": {
                final Guild guild = event.getGuild();
                final long guildId = guild.getIdLong();
                final Link link = this.client.getLink(guildId);

                link.createOrUpdatePlayer()
                    .setFilters(
                        new FilterBuilder()
                            .setKaraoke(null)
                            .build()
                    )
                    .subscribe();
                break;
            }
            case "play": {
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

                link.loadItem(identifier).subscribe(new AudioLoader(link, event));

                break;
            }
            case "custom-request": {
                final Link link = this.client.getLink(event.getGuild().getIdLong());

                link.getNode().customRequest(
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
            }
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
