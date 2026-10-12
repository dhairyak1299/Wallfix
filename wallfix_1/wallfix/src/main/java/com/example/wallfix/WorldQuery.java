package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

final class WorldQuery {
    private WorldQuery() {}

    /** Air, liquid, tall grass etc. Something a block can be placed into.
     *  Uses Block#isReplaceable, the actual method vanilla placement logic checks -
     *  material.isReplaceable() alone misses water/lava, which override this on the block itself. */
    static boolean isOpen(World w, BlockPos p) {
        if (!w.isBlockLoaded(p)) return false;
        return w.getBlockState(p).getBlock().isReplaceable(w, p);
    }

    /** A surviving wall block. */
    static boolean isSolidWall(World w, BlockPos p) {
        if (!w.isBlockLoaded(p)) return false;
        Block b = w.getBlockState(p).getBlock();
        return !b.getMaterial().isReplaceable() && b.getMaterial().isSolid();
    }

    /** A block safe to right-click against: full cube, no GUI, no tile entity (dispensers, chests, furnaces). */
    static boolean isSupport(World w, BlockPos p) {
        if (!isSolidWall(w, p)) return false;
        IBlockState s = w.getBlockState(p);
        Block b = s.getBlock();
        return b.isFullCube() && !b.hasTileEntity(s) && !(b instanceof BlockWorkbench);
    }
}
