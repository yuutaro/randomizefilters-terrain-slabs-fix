package dev.yuutaro.randomizefiltersterrainslabsfix.smoketest;

import com.createrandomizefilters.component.*;
import com.createrandomizefilters.item.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.yuutaro.randomizefiltersterrainslabsfix.*;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Executes real transformed roller methods in a disposable dedicated-server world. */
@Mod("terrain_slabs_fix_smoketest")
public final class SmokeTestMod {
    private static int assertions;
    public SmokeTestMod() { NeoForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            run(event.getServer().overworld());
            System.out.println("TERRAIN_SLABS_FIX_SMOKE_PASS assertions=" + assertions);
        } catch (Throwable failure) {
            System.err.println("TERRAIN_SLABS_FIX_SMOKE_FAIL");
            failure.printStackTrace();
        } finally {
            event.getServer().halt(false);
        }
    }

    private void run(ServerLevel world) throws Throwable {
        var access = MethodHandles.privateLookupIn(RollerMovementBehaviour.class, MethodHandles.lookup());
        MethodHandle lookup = access.findStatic(RollerMovementBehaviour.class, "findSlabBlockForFullBlock",
            MethodType.methodType(Block.class, Block.class));
        MethodHandle slab = access.findVirtual(RollerMovementBehaviour.class, "getStateToPaveWithAsSlab",
            MethodType.methodType(BlockState.class, MovementContext.class));
        MethodHandle fill = access.findVirtual(RollerMovementBehaviour.class, "tryFill",
            MethodType.methodType(Class.forName(RollerMovementBehaviour.class.getName() + "$PaveResult"),
                MovementContext.class, BlockPos.class, BlockState.class));
        RollerMovementBehaviour roller = new RollerMovementBehaviour();
        Map<String, String> pairs;
        try (var reader = new InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream("/terrain-slab-recipe-pairs.json")),
            StandardCharsets.UTF_8
        )) {
            pairs = new Gson().fromJson(reader, new TypeToken<LinkedHashMap<String, String>>() {}.getType());
        }
        BlockPos origin = new BlockPos(0, 180, 0);
        int index = 0;
        for (var pair : pairs.entrySet()) {
            Block material = block(pair.getKey());
            Block expected = block(pair.getValue());
            check(lookup.invoke(material) == expected, "lookup " + pair.getKey());
            for (boolean advanced : List.of(false, true)) {
                ItemStack filter = item(advanced ? AdvancedRandomFilterItem.class : RandomFilterItem.class);
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(material);
                if (advanced) {
                    filter.set(ModDataComponents.WEIGHTED_BLOCK_LIST.get(),
                        new WeightedBlockList(List.of(new WeightedBlockList.WeightedBlock(id, 1, false))));
                } else {
                    filter.set(ModDataComponents.FILTER_BLOCK_LIST.get(), new FilterBlockList(List.of(id), 0));
                }
                ItemStackHandler inventory = new ItemStackHandler(1);
                inventory.setStackInSlot(0, new ItemStack(material, 4));
                MovementContext context = context(world, filter, inventory);
                BlockPos target = origin.offset(index++, 0, 0);
                world.getChunkAt(target);
                world.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
                world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                context.position = Vec3.atCenterOf(target);
                BlockState requested = (BlockState) slab.invoke(roller, context);
                check(requested != null && requested.is(expected), "preview " + pair.getKey());
                check(fill.invoke(roller, context, target, requested).toString().equals("SUCCESS"), "place");
                check(world.getBlockState(target).is(expected), "placed " + pair.getValue());
                check(inventory.getStackInSlot(0).getCount() == 3, "consumed one material");
            }
        }

        int mapped = 0;
        for (Block fullBlock : BuiltInRegistries.BLOCK) {
            Block expected = net.countered.terrainslabs.block.ModSlabsMap.getSlabForBlock(fullBlock);
            if (expected != null) {
                check(lookup.invoke(fullBlock) == expected, "native mapping " + BuiltInRegistries.BLOCK.getKey(fullBlock));
                mapped++;
            }
        }
        System.out.println("TERRAIN_SLABS_FIX_NATIVE_MAPPINGS_CHECKED=" + mapped);
        // Outside Terrain Slabs' mapping, preserve the original lookup.
        check(lookup.invoke(Blocks.SMOOTH_QUARTZ) == Blocks.SMOOTH_QUARTZ_SLAB, "vanilla lookup unchanged");
        check(lookup.invoke(Blocks.BEDROCK) == null, "unmapped block unchanged");
        System.out.println("TERRAIN_SLABS_FIX_RECIPES_CHECKED=" + pairs.size());
    }

    private static MovementContext context(ServerLevel world, ItemStack filter, ItemStackHandler inventory) {
        BearingContraption contraption = new BearingContraption();
        contraption.getStorage().initialize();
        contraption.getStorage().attachExternal(inventory);
        CompoundTag data = new CompoundTag();
        data.put("Filter", filter.save(world.registryAccess()));
        return new MovementContext(world,
            new StructureBlockInfo(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), data), contraption);
    }

    private static ItemStack item(Class<? extends Item> type) {
        for (Item item : BuiltInRegistries.ITEM) if (type.isInstance(item)) return new ItemStack(item);
        throw new AssertionError("Missing filter item " + type.getName());
    }
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElseThrow();
    }
    private static void check(boolean success, String message) {
        assertions++;
        if (!success) throw new AssertionError(message);
    }
}
