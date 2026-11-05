package io.github.yuutaro.randomizefiltersterrainslabsfix.mixin;

import io.github.yuutaro.randomizefiltersterrainslabsfix.TerrainSlabLookup;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shared lookup used by Random and Advanced Random filters, including actual placement. */
@Mixin(
    targets = "com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour",
    priority = 900,
    remap = false
)
public abstract class RollerSlabLookupMixin {
    @Dynamic("Added by com.createrandomizefilters.mixin.RollerMovementBehaviourMixin (1.0.7)")
    @Inject(
        method = "findSlabBlockForFullBlock(Lnet/minecraft/world/level/block/Block;)Lnet/minecraft/world/level/block/Block;",
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        remap = false
    )
    private static void terrainSlabsFix$findTerrainSlab(
        Block fullBlock,
        CallbackInfoReturnable<Block> callback
    ) {
        Block slab = TerrainSlabLookup.terrainSlab(fullBlock);
        if (slab != null) {
            callback.setReturnValue(slab);
        }
    }
}
