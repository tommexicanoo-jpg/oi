package com.eleco;

import com.eleco.block.ExtinguishedTorchBlock;
import com.eleco.block.ExtinguishedWallTorchBlock;
import com.eleco.entity.DoppelgangerEntity;
import com.eleco.entity.ObserverEntity;
import com.eleco.net.DayPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Director del terror: avanza los 5 días y orquesta susurros, ojos, dobles, Observador y antorchas. */
public final class EcoDirector {
    private static final Map<UUID, Long> NEXT_DOPPEL = new HashMap<>();
    private static final SoundEvent[] WHISPERS = {
            SoundEvents.ENTITY_ENDERMAN_STARE, SoundEvents.ENTITY_VEX_AMBIENT,
            SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundEvents.ENTITY_PHANTOM_AMBIENT
    };

    private EcoDirector() {}

    public static int currentDay(MinecraftServer server) {
        return EcoState.get(server).day(server.getOverworld().getTime());
    }

    public static int currentDay(ServerWorld world) {
        return EcoState.get(world.getServer()).day(world.getServer().getOverworld().getTime());
    }

    // -------------------------------------------------------------------- tick
    public static void tick(MinecraftServer server) {
        ServerWorld ow = server.getOverworld();
        long t = ow.getTime();
        if (t % 20 != 0) return; // lógica una vez por segundo
        EcoState st = EcoState.get(server);
        int day = st.day(t);
        if (day != st.lastAnnouncedDay) announce(server, st, day);
        if (t % 200 == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
                ServerPlayNetworking.send(p, new DayPayload(day));
        }
        for (ServerPlayerEntity p : ow.getPlayers()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            perPlayer(ow, p, day, t);
        }
    }

    private static void announce(MinecraftServer server, EcoState st, int day) {
        st.lastAnnouncedDay = day;
        st.markDirty();
        ServerWorld ow = server.getOverworld();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, new DayPayload(day));
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(20, 80, 40));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("eleco.day.sub." + day)));
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("eleco.day.title", day)));
            ow.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                    SoundCategory.AMBIENT, 1.0f, 0.6f);
        }
    }

    private static void perPlayer(ServerWorld ow, ServerPlayerEntity p, int day, long t) {
        Random rnd = ow.getRandom();
        EcoConfig cfg = EcoConfig.get();

        // ---- susurros (aumentan con los días) ----
        if (rnd.nextDouble() < cfg.whisperChancePerSecond * day * day * 0.6 + cfg.whisperChancePerSecond) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double r = 2.5 + rnd.nextDouble() * 4.0;
            SoundEvent s = WHISPERS[rnd.nextInt(WHISPERS.length)];
            ow.playSound(null, p.getX() + Math.cos(ang) * r, p.getY() + 1.0, p.getZ() + Math.sin(ang) * r, s,
                    SoundCategory.AMBIENT, 0.25f + 0.08f * day, 0.45f + rnd.nextFloat() * 0.2f);
        }

        // ---- ojos rojos en hojas / ventanas / superficies oscuras ----
        if (rnd.nextDouble() < 0.35 + 0.12 * day) {
            for (int i = 0; i < 2 + day / 2; i++) trySpawnEyes(ow, p, rnd);
        }

        // ---- el Observador ----
        if (t % 100 == 0 && findObservers(ow, p).isEmpty()) spawnObserverNear(ow, p);

        // ---- dobles ----
        long next = NEXT_DOPPEL.computeIfAbsent(p.getUuid(), k -> t + 20L * (40 + rnd.nextInt(40)));
        if (t >= next) {
            if (!findObservers(ow, p).isEmpty() && !p.isTouchingWater()) spawnDoppelganger(ow, p);
            double secs = cfg.doppelgangerBaseSeconds * (1.0 - 0.17 * (day - 1)) * (0.6 + rnd.nextDouble() * 0.8);
            NEXT_DOPPEL.put(p.getUuid(), t + (long) (20 * Math.max(15, secs)));
        }

        // ---- antorchas que se apagan ----
        torchTick(ow, p, day, rnd, cfg);
    }

    // ------------------------------------------------------------------ Observador
    private static java.util.List<ObserverEntity> findObservers(ServerWorld ow, ServerPlayerEntity p) {
        return ow.getEntitiesByType(ModEntities.OBSERVER, p.getBoundingBox().expand(140), e -> true);
    }

    public static boolean spawnObserverNear(ServerWorld ow, ServerPlayerEntity p) {
        int day = currentDay(ow);
        double dist = Math.max(30.0, 66.0 - (day - 1) * 10.0);
        Random rnd = ow.getRandom();
        for (int i = 0; i < 16; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double x = p.getX() + Math.cos(ang) * dist;
            double z = p.getZ() + Math.sin(ang) * dist;
            BlockPos col = BlockPos.ofFloored(x, 0, z);
            if (!ow.isChunkLoaded(col)) continue;
            int y = ow.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, col.getX(), col.getZ());
            BlockPos below = new BlockPos(col.getX(), y - 1, col.getZ());
            if (!ow.getBlockState(below).isSolid() || !ow.getFluidState(below).isEmpty()) continue;
            ObserverEntity o = ModEntities.OBSERVER.create(ow);
            if (o == null) return false;
            o.refreshPositionAndAngles(x, y, z, rnd.nextFloat() * 360f, 0f);
            ow.spawnEntity(o);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ Dobles
    public static boolean spawnDoppelganger(ServerWorld ow, ServerPlayerEntity p) {
        if (!ow.getEntitiesByType(ModEntities.DOPPELGANGER, p.getBoundingBox().expand(60), e -> !e.isHollow()).isEmpty()) return false;
        Random rnd = ow.getRandom();
        Vec3d eye = p.getEyePos();
        for (int i = 0; i < 40; i++) {
            double ang = Math.toRadians(p.getYaw() + (rnd.nextDouble() - 0.5) * 90.0);   // dentro de su campo de visión
            double dist = 13 + rnd.nextDouble() * 16;
            double x = p.getX() - Math.sin(ang) * dist;
            double z = p.getZ() + Math.cos(ang) * dist;
            BlockPos col = BlockPos.ofFloored(x, p.getY(), z);
            if (!ow.isChunkLoaded(col)) continue;
            BlockPos spot = null;
            for (int dy = 3; dy >= -4 && spot == null; dy--) {
                BlockPos c = new BlockPos(col.getX(), p.getBlockY() + dy, col.getZ());
                if (ow.getBlockState(c).isAir() && ow.getBlockState(c.up()).isAir()
                        && ow.getBlockState(c.down()).isSolid()) spot = c;
            }
            if (spot == null || ow.getLightLevel(LightType.BLOCK, spot) > 5) continue;
            if (!(nearTree(ow, spot) || inCorridor(ow, spot) || nearWindow(ow, spot))) continue;
            Vec3d target = Vec3d.ofCenter(spot).add(0, 1.0, 0);
            BlockHitResult hit = ow.raycast(new RaycastContext(eye, target, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getPos().distanceTo(target) > 2.5) continue;

            DoppelgangerEntity d = ModEntities.DOPPELGANGER.create(ow);
            if (d == null) return false;
            d.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0f, 0f);
            d.copyFrom(p);
            ow.spawnEntity(d);   // aparece en silencio: sin sonido ni partículas
            return true;
        }
        return false;
    }

    private static boolean nearTree(ServerWorld w, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 0; dy <= 2; dy++)
            if (w.getBlockState(pos.add(dx, dy, dz)).isIn(BlockTags.LOGS)) return true;
        return false;
    }

    private static boolean inCorridor(ServerWorld w, BlockPos pos) {
        BlockPos m = pos.up();
        boolean x = w.getBlockState(m.east()).isSolid() && w.getBlockState(m.west()).isSolid();
        boolean z = w.getBlockState(m.north()).isSolid() && w.getBlockState(m.south()).isSolid();
        return (x ^ z) && w.getBlockState(pos.up(2)).isSolid();
    }

    private static boolean nearWindow(ServerWorld w, BlockPos pos) {
        for (Direction d : Direction.Type.HORIZONTAL) {
            for (int k = 1; k <= 2; k++) {
                if (w.getBlockState(pos.up().offset(d, k)).getBlock() instanceof PaneBlock) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ Ojos
    private static void trySpawnEyes(ServerWorld ow, ServerPlayerEntity p, Random rnd) {
        double ang = rnd.nextDouble() * Math.PI * 2;
        double r = 7 + rnd.nextDouble() * 17;
        BlockPos pos = BlockPos.ofFloored(p.getX() + Math.cos(ang) * r, p.getY() + rnd.nextInt(12) - 3, p.getZ() + Math.sin(ang) * r);
        if (!ow.isChunkLoaded(pos)) return;
        BlockState s = ow.getBlockState(pos);
        boolean ok = s.isIn(BlockTags.LEAVES) || s.getBlock() instanceof PaneBlock
                || (s.isOpaqueFullCube(ow, pos) && ow.getLightLevel(LightType.BLOCK, pos.up()) <= 2 && rnd.nextInt(3) == 0);
        if (!ok) return;
        Direction face = Direction.Type.HORIZONTAL.random(rnd);
        BlockPos n = pos.offset(face);
        if (!ow.getBlockState(n).isAir() || ow.getLightLevel(LightType.BLOCK, n) > 4) return;
        // Se ve desde el lado contrario al jugador: solo si esa cara mira hacia él
        Vec3d toPlayer = p.getPos().subtract(Vec3d.ofCenter(pos));
        if (toPlayer.x * face.getOffsetX() + toPlayer.z * face.getOffsetZ() < 0) return;
        double x = pos.getX() + 0.5 + face.getOffsetX() * 0.52;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5 + face.getOffsetZ() * 0.52;
        ow.spawnParticles(p, ModParticles.EYES, true, x, y, z, 1, 0, 0, 0, 0);
    }

    // ------------------------------------------------------------------ Antorchas
    private static void torchTick(ServerWorld ow, ServerPlayerEntity p, int day, Random rnd, EcoConfig cfg) {
        double life = Math.max(15.0, cfg.torchAverageSeconds * (1.0 - 0.13 * (day - 1)));
        double pOut = 1.0 / life;           // por segundo y antorcha
        double pFlicker = pOut * 2.5;

        // antorchas colocadas cerca del jugador
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockPos c = p.getBlockPos();
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) for (int dy = -5; dy <= 6; dy++) {
            m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
            BlockState s = ow.getBlockState(m);
            boolean standing = s.isOf(Blocks.TORCH);
            boolean wall = s.isOf(Blocks.WALL_TORCH);
            if (!standing && !wall) continue;
            double r = rnd.nextDouble();
            if (r >= pOut + pFlicker) continue;
            boolean flicker = r < pFlicker;
            BlockPos pos = m.toImmutable();
            var block = standing ? ModBlocks.EXTINGUISHED_TORCH : ModBlocks.EXTINGUISHED_WALL_TORCH;
            BlockState ns = block.getDefaultState().with(ExtinguishedTorchBlock.FLICKER, flicker);
            if (wall) ns = ns.with(ExtinguishedWallTorchBlock.FACING, s.get(WallTorchBlock.FACING));
            ow.setBlockState(pos, ns);
            if (flicker) ow.scheduleBlockTick(pos, block, 3 + rnd.nextInt(14));
            ow.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.3f, 1.3f);
            ow.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 4, 0.05, 0.1, 0.05, 0.01);
        }

        // antorcha en la mano
        for (Hand hand : Hand.values()) {
            ItemStack st = p.getStackInHand(hand);
            if (!st.isOf(Items.TORCH)) continue;
            double r = rnd.nextDouble();
            if (r >= (pOut + pFlicker) * 1.2) continue;
            ow.playSound(null, p.getX(), p.getY() + 1, p.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.35f, 1.4f);
            ow.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.15, 0.1, 0.15, 0.01);
            if (r < pFlicker * 1.2) {
                // parpadeo: apagada ~0.7 s y vuelve (se simula con ceguera visual breve de oscuridad)
                p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.DARKNESS, 14, 0, false, false, false));
            } else {
                ItemStack out = new ItemStack(ModItems.EXTINGUISHED_TORCH);
                if (st.getCount() == 1) p.setStackInHand(hand, out);
                else { st.decrement(1); p.getInventory().offerOrDrop(out); }
            }
            break;
        }
    }

    // ------------------------------------------------------------------ Ayudas (comandos)
    public static void forceDay(MinecraftServer server, int day) {
        EcoState st = EcoState.get(server);
        st.setDay(day, server.getOverworld().getTime());
    }
}
