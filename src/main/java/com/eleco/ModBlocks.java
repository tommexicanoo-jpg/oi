package com.eleco;

import com.eleco.block.ExtinguishedTorchBlock;
import com.eleco.block.ExtinguishedWallTorchBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    private static AbstractBlock.Settings torchSettings() {
        return AbstractBlock.Settings.create().noCollision().breakInstantly()
                .sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY);
    }

    public static final Block EXTINGUISHED_TORCH = Registry.register(Registries.BLOCK,
            Identifier.of(ElEco.MOD_ID, "extinguished_torch"), new ExtinguishedTorchBlock(torchSettings()));
    public static final Block EXTINGUISHED_WALL_TORCH = Registry.register(Registries.BLOCK,
            Identifier.of(ElEco.MOD_ID, "extinguished_wall_torch"), new ExtinguishedWallTorchBlock(torchSettings().dropsLike(EXTINGUISHED_TORCH)));

    public static void register() { /* carga de la clase */ }
}
