package com.eleco;

import com.eleco.net.DayPayload;
import com.eleco.worldgen.ModStructures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElEco implements ModInitializer {
    public static final String MOD_ID = "eleco";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        EcoConfig.load();
        ModBlocks.register();
        ModEntities.register();
        ModItems.register();
        ModParticles.register();
        ModStructures.register();

        PayloadTypeRegistry.playS2C().register(DayPayload.ID, DayPayload.CODEC);

        // Respaldo de la noche eterna por si el datapack de dimension_type es sobrescrito por otro mod.
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
                world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, server);
                if (world.getTimeOfDay() % 24000L != 18000L) world.setTimeOfDay(18000L);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(EcoDirector::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.player, new DayPayload(EcoDirector.currentDay(server))));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> EcoCommands.register(dispatcher));

        LOGGER.info("El Eco despierta... no mires atrás.");
    }
}
