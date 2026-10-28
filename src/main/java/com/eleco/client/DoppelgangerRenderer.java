package com.eleco.client;

import com.eleco.entity.DoppelgangerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Dibuja al doble con la skin (y modelo ancho/delgado) del jugador copiado; el eco hueco es una silueta translúcida. */
public class DoppelgangerRenderer extends LivingEntityRenderer<DoppelgangerEntity, PlayerEntityModel<DoppelgangerEntity>> {
    private static final Identifier HOLLOW = Identifier.of("eleco", "textures/entity/doppelganger_hollow.png");
    private final PlayerEntityModel<DoppelgangerEntity> wide;
    private final PlayerEntityModel<DoppelgangerEntity> slim;

    public DoppelgangerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.wide = this.model;
        this.slim = new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
    }

    private SkinTextures skin(DoppelgangerEntity e) {
        UUID id = e.getOwnerUuid();
        if (id == null) return DefaultSkinHelper.getSkinTextures(e.getUuid());
        var nh = MinecraftClient.getInstance().getNetworkHandler();
        PlayerListEntry entry = nh == null ? null : nh.getPlayerListEntry(id);
        return entry != null ? entry.getSkinTextures() : DefaultSkinHelper.getSkinTextures(id);
    }

    @Override
    public Identifier getTexture(DoppelgangerEntity e) {
        return e.isHollow() ? HOLLOW : skin(e).texture();
    }

    @Override
    protected RenderLayer getRenderLayer(DoppelgangerEntity e, boolean showBody, boolean translucent, boolean showOutline) {
        if (e.isHollow()) return RenderLayer.getEntityTranslucent(HOLLOW);
        return super.getRenderLayer(e, showBody, translucent, showOutline);
    }

    @Override
    public void render(DoppelgangerEntity e, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light) {
        this.model = skin(e).model() == SkinTextures.Model.SLIM ? slim : wide;
        this.model.sneaking = e.isInSneakingPose();
        this.model.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
        this.model.rightArmPose = BipedEntityModel.ArmPose.EMPTY;
        // el modelo de capas del jugador visible salvo en el eco hueco
        this.model.setVisible(true);
        super.render(e, yaw, tickDelta, matrices, vcp, e.isHollow() ? 0xF000F0 : light);
    }

    @Override
    protected boolean hasLabel(DoppelgangerEntity e) { return false; }
}
