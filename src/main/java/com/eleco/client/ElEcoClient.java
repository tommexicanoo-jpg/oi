package com.eleco.client;

import com.eleco.ModEntities;
import com.eleco.ModParticles;
import com.eleco.net.DayPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Environment(EnvType.CLIENT)
public class ElEcoClient implements ClientModInitializer {
    private static final String SHADER_RES = "assets/eleco/shaderpack/ElEcoShaders.zip";

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.OBSERVER, ObserverRenderer::new);
        EntityRendererRegistry.register(ModEntities.DOPPELGANGER, DoppelgangerRenderer::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.EYES, EyesParticle.Factory::new);

        ClientPlayNetworking.registerGlobalReceiver(DayPayload.ID, (payload, ctx) -> ClientEco.setDay(payload.day()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientEco.reset());
        ClientTickEvents.END_CLIENT_TICK.register(c -> ClientEco.tick());

        // Shaderpack OPCIONAL: /ecoshader install copia el zip a la carpeta shaderpacks (requiere Iris/OptiFine)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("ecoshader")
                        .then(ClientCommandManager.literal("install").executes(c -> {
                            try {
                                Path dir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
                                Files.createDirectories(dir);
                                Path out = dir.resolve("ElEco-Shaders.zip");
                                var path = FabricLoader.getInstance().getModContainer("eleco").orElseThrow().findPath(SHADER_RES);
                                try (InputStream in = Files.newInputStream(path.orElseThrow())) {
                                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                                }
                                c.getSource().sendFeedback(Text.translatable("eleco.shader.copied", out.toString()));
                                return 1;
                            } catch (Exception e) {
                                c.getSource().sendFeedback(Text.translatable("eleco.shader.error", e.toString()));
                                return 0;
                            }
                        }))
                        .then(ClientCommandManager.literal("status").executes(c -> {
                            c.getSource().sendFeedback(Text.translatable("eleco.shader.status", ClientEco.active ? ClientEco.day : 0));
                            return 1;
                        }))));
    }
}
