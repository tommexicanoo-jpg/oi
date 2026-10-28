package com.eleco.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Doble del jugador: misma skin (cliente, por UUID), armadura, objetos y postura.
 * Silencioso e inmóvil, solo mira. Si te acercas o apartas la vista, se desvanece
 * y queda un "eco hueco" (silueta translúcida) unos segundos.
 */
public class DoppelgangerEntity extends MobEntity {
    private static final TrackedData<Optional<UUID>> OWNER =
            DataTracker.registerData(DoppelgangerEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> HOLLOW =
            DataTracker.registerData(DoppelgangerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int seenTicks = 0;
    private int hollowTicks = 0;

    public DoppelgangerEntity(EntityType<? extends DoppelgangerEntity> type, World world) {
        super(type, world);
        setSilent(true);
        setPersistent();
        setAiDisabled(true);
        for (EquipmentSlot s : EquipmentSlot.values()) setEquipmentDropChance(s, 0.0f);
    }

    public static DefaultAttributeContainer.Builder createDoppelAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER, Optional.empty());
        builder.add(HOLLOW, false);
    }

    public UUID getOwnerUuid() { return dataTracker.get(OWNER).orElse(null); }
    public boolean isHollow() { return dataTracker.get(HOLLOW); }

    /** Copia skin (por UUID), armadura, objetos en mano y postura del jugador. */
    public void copyFrom(PlayerEntity p) {
        dataTracker.set(OWNER, Optional.of(p.getUuid()));
        for (EquipmentSlot s : EquipmentSlot.values()) {
            ItemStack st = p.getEquippedStack(s);
            if (!st.isEmpty()) equipStack(s, st.copy());
        }
        if (p.isInSneakingPose()) setPose(EntityPose.CROUCHING);
        setCustomNameVisible(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld sw)) return;
        if (isHollow()) {
            if (--hollowTicks <= 0) discard();
            return;
        }
        UUID id = getOwnerUuid();
        PlayerEntity owner = id == null ? null : sw.getPlayerByUuid(id);
        if (owner == null || !owner.isAlive() || owner.getWorld() != getWorld()) { discard(); return; }

        // mirar fijamente al jugador
        Vec3d d = owner.getEyePos().subtract(getEyePos());
        float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        setYaw(yaw); setBodyYaw(yaw); setHeadYaw(yaw); setPitch(pitch);
        prevYaw = yaw; prevBodyYaw = yaw; prevHeadYaw = yaw; prevPitch = pitch;

        Vec3d toMe = getEyePos().subtract(owner.getEyePos()).normalize();
        double dot = owner.getRotationVec(1.0f).dotProduct(toMe);
        double dist = distanceTo(owner);

        if (dot > 0.80 && owner.canSee(this)) seenTicks++;
        if (dist < 6.0) { vanish(sw); return; }                   // te acercas demasiado
        if (seenTicks >= 12 && dot < 0.55) { vanish(sw); return; } // apartas la vista ("parpadeo")
        if ((seenTicks == 0 && age > 400) || age > 900) { vanish(sw); }  // nunca lo miraste / demasiado tiempo
    }

    /** Se desvanece al instante dejando un eco hueco. */
    public void vanish(ServerWorld sw) {
        if (isHollow()) return;
        dataTracker.set(HOLLOW, true);
        hollowTicks = 100;
        for (EquipmentSlot s : EquipmentSlot.values()) equipStack(s, ItemStack.EMPTY);
        sw.spawnParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 0.9, getZ(), 18, 0.25, 0.7, 0.25, 0.01);
        sw.spawnParticles(ParticleTypes.ASH, getX(), getY() + 0.9, getZ(), 30, 0.3, 0.8, 0.3, 0.0);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient && !isHollow() && source.getAttacker() != null) vanish((ServerWorld) getWorld());
        return false;
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean canImmediatelyDespawn(double d) { return false; }
    @Override public boolean isCollidable() { return false; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        UUID id = getOwnerUuid();
        if (id != null) nbt.putUuid("Owner", id);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("Owner")) dataTracker.set(OWNER, Optional.of(nbt.getUuid("Owner")));
        else discard(); // un doble sin dueño no tiene sentido tras recargar el mundo
    }
}
