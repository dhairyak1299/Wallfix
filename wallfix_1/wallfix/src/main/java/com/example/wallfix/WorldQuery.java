package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

final class WorldQuery {
    private WorldQuery() {}

    /** Something a block can be placed into: air, water, lava, tall grass, snow...
     *  Vanilla placement (World#canBlockBePlaced) decides this with the MATERIAL's replaceable flag,
     *  which is true for air AND liquids. Block#isReplaceable is only overridden by air/plants/snow,
     *  NOT by BlockLiquid, so it must never be the only check (that bug hid every water-filled gap).
     *  Either check passing is enough. */
    static boolean isOpen(World w, BlockPos p) {
        if (!w.isBlockLoaded(p)) return false;
        Block b = w.getBlockState(p).getBlock();
        return b.getMaterial().isReplaceable() || b.isReplaceable(w, p);
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

    /** Count of solid wall cells within +-radius of span, same row and plane (the centre cell is not counted). A real wall has many; its thickness axis has ~none. */
    static int solidNeighbours(World w, SpanAxis ax, int plane, int y, int span, int radius) {
        int n = 0;
        for (int i = 1; i <= radius; i++) {
            if (isSolidWall(w, ax.make(span - i, y, plane))) n++;
            if (isSolidWall(w, ax.make(span + i, y, plane))) n++;
        }
        return n;
    }

    /** Solid wall, or an open cell sitting in a gap that is bounded by solid wall within maxGap cells (through open cells only). */
    static boolean isWallLike(World w, SpanAxis ax, int plane, int y, int span, int maxGap) {
        if (isSolidWall(w, ax.make(span, y, plane))) return true;
        if (!isOpen(w, ax.make(span, y, plane))) return false;
        for (int dir = -1; dir <= 1; dir += 2) {
            for (int i = 1; i <= maxGap; i++) {
                BlockPos c = ax.make(span + dir * i, y, plane);
                if (isSolidWall(w, c)) return true;
                if (!isOpen(w, c)) break;
            }
        }
        return false;
    }
}
