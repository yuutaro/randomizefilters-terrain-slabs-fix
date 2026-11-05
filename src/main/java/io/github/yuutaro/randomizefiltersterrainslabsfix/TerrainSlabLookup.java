package io.github.yuutaro.randomizefiltersterrainslabsfix;

import net.countered.terrainslabs.block.ModSlabsMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;

/** Uses Terrain Slabs' own mapping, including irregular names and custom stone slabs. */
public final class TerrainSlabLookup {
    private TerrainSlabLookup() {}

    public static Block terrainSlab(Block fullBlock) {
        if (fullBlock == null || fullBlock == Blocks.AIR) {
            return null;
        }
        Block slab = ModSlabsMap.getSlabForBlock(fullBlock);
        return slab != null && slab.defaultBlockState().hasProperty(SlabBlock.TYPE) ? slab : null;
    }
}
