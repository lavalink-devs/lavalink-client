package me.duncte123.testbot;

import dev.arbjerg.lavalink.client.LavalinkClient;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EvalEngine {
    private final ExecutorService evalThread = Executors.newThreadPerTaskExecutor(
        (it) -> Thread.ofVirtual()
            .name("eval-thread")
            .unstarted(it)
    );

    private final ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("kts");

    private final LavalinkClient client;

    public EvalEngine(LavalinkClient client) {
        this.client = client;
        this.initEngine();
    }

    public Object eval(SlashCommandInteractionEvent slashEvent, String code) {
        final var bindings = new SimpleBindings();

        bindings.put("client", this.client);
        bindings.put("event", slashEvent);
        bindings.put("guild", slashEvent.getGuild());
        bindings.put("jda", slashEvent.getJDA());

        final Future<Object> future = this.evalThread.submit(() -> {
           try {
               return this.engine.eval(code, bindings);
           } catch (final Exception e) {
               return e;
           }
        });


        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            future.cancel(true);
            return ex;
        }
    }

    private void initEngine() {
        final var packageImports = List.of(
            "java.io",
            "java.lang",
            "java.math",
            "java.time",
            "java.util",
            "java.util.concurrent",
            "java.util.stream",
            "net.dv8tion.jda.api",
            "net.dv8tion.jda.internal.entities",
            "net.dv8tion.jda.api.entities",
            "net.dv8tion.jda.api.entities.channel",
            "net.dv8tion.jda.api.entities.channel.attribute",
            "net.dv8tion.jda.api.entities.channel.middleman",
            "net.dv8tion.jda.api.entities.channel.concrete",
            "net.dv8tion.jda.api.managers",
            "net.dv8tion.jda.internal.managers",
            "net.dv8tion.jda.api.utils",
            "dev.arbjerg.lavalink.client",
            "dev.arbjerg.lavalink.client.player",
            "dev.arbjerg.lavalink.client.http",
            "dev.arbjerg.lavalink.client.event"
        );

        final List<String> classImports = List.of(
            // Nothing needed for now
        );

        // classImports.joinToString(separator = "\nimport ", postfix = "\n")

        final var importString = packageImports.stream()
            .map((i) -> "import " + i + ".*")
            .collect(Collectors.joining("\n"))/* + classImports.stream().collect(Collectors.joining(
            "", "\nimport ", "\n"
        ))*/;


        try {
            this.engine.eval(importString);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
