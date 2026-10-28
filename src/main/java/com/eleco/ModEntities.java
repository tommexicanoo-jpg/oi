package com.eleco;

import com.eleco.entity.DoppelgangerEntity;
import com.eleco.entity.ObserverEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<ObserverEntity> OBSERVER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(ElEco.MOD_ID, "observer"),
            EntityType.Builder.create(ObserverEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 2.9f).maxTrackingRange(12).makeFireImmune().build("observer"));

    public static final EntityType<DoppelgangerEntity> DOPPELGANGER = Registry.register(Registries.ENTITY_TYPE,
            Identifier.of(ElEco.MOD_ID, "doppelganger"),
            EntityType.Builder.create(DoppelgangerEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f).maxTrackingRange(10).build("doppelganger"));

    public static void register() {
        FabricDefaultAttributeRegistry.register(OBSERVER, ObserverEntity.createObserverAttributes());
        FabricDefaultAttributeRegistry.register(DOPPELGANGER, DoppelgangerEntity.createDoppelAttributes());
    }
}
