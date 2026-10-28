package com.eleco;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class EcoCommands {
    private EcoCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("eco").requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("day").then(CommandManager.argument("n", IntegerArgumentType.integer(1, 5))
                        .executes(c -> {
                            int n = IntegerArgumentType.getInteger(c, "n");
                            EcoDirector.forceDay(c.getSource().getServer(), n);
                            c.getSource().sendFeedback(() -> Text.translatable("eleco.cmd.day", n), true);
                            return n;
                        })))
                .then(CommandManager.literal("status").executes(c -> {
                    var server = c.getSource().getServer();
                    int day = EcoDirector.currentDay(server);
                    long tick = server.getOverworld().getTime();
                    c.getSource().sendFeedback(() -> Text.translatable("eleco.cmd.status", day, tick), false);
                    return day;
                }))
                .then(CommandManager.literal("observer").executes(c -> {
                    ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                    boolean ok = EcoDirector.spawnObserverNear(p.getServerWorld(), p);
                    c.getSource().sendFeedback(() -> Text.translatable(ok ? "eleco.cmd.spawned" : "eleco.cmd.nospace"), false);
                    return ok ? 1 : 0;
                }))
                .then(CommandManager.literal("doppelganger").executes(c -> {
                    ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
                    boolean ok = EcoDirector.spawnDoppelganger(p.getServerWorld(), p);
                    c.getSource().sendFeedback(() -> Text.translatable(ok ? "eleco.cmd.spawned" : "eleco.cmd.nospace"), false);
                    return ok ? 1 : 0;
                })));
    }
}
