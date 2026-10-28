package com.eleco;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

/** Estado persistente del mod en el Overworld: cuándo empezó la cuenta de los 5 días. */
public class EcoState extends PersistentState {
    public long startTick = -1;
    public int lastAnnouncedDay = 0;

    public static EcoState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        EcoState s = new EcoState();
        s.startTick = nbt.getLong("startTick");
        s.lastAnnouncedDay = nbt.getInt("lastDay");
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putLong("startTick", startTick);
        nbt.putInt("lastDay", lastAnnouncedDay);
        return nbt;
    }

    public static EcoState get(MinecraftServer server) {
        PersistentStateManager m = server.getOverworld().getPersistentStateManager();
        EcoState s = m.getOrCreate(new PersistentState.Type<>(EcoState::new, EcoState::fromNbt, null), "eleco");
        if (s.startTick < 0) {
            s.startTick = server.getOverworld().getTime();
            s.markDirty();
        }
        return s;
    }

    /** Día 1..5 (el 5 dura hasta el final: persecución). */
    public int day(long worldTime) {
        long len = Math.max(200, EcoConfig.get().dayLengthTicks);
        long d = (worldTime - startTick) / len + 1;
        return (int) Math.max(1, Math.min(5, d));
    }

    public void setDay(int day, long worldTime) {
        startTick = worldTime - (long) (day - 1) * Math.max(200, EcoConfig.get().dayLengthTicks);
        lastAnnouncedDay = 0; // fuerza el aviso
        markDirty();
    }
}
