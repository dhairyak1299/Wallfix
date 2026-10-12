package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Module: snapshot which wall cells are solid, later report which are now open.
 * A cell counts as a breach the moment a surviving obsidian block has an open
 * (air/water) neighbour along the span axis on a cell this snapshot once held solid -
 * no requirement that the far side also be bounded. Matches a sealed box: any
 * exposed obsidian face IS a breach, there are no legitimate open "wall ends" inside one.
 */
public final class HoleDetector {

    public Set<Long> snapshot(World w, WallAnchor a) {
        Set<Long> solid = new HashSet<Long>();
        int reach = Config.MAX_HOLE_WIDTH + 2;
        for (int y = a.y - Config.ROWS_BELOW; y <= a.y + Config.ROWS_ABOVE; y++) {
            for (int s = a.seedSpan - reach; s <= a.seedSpan + reach; s++) {
                BlockPos p = a.span.make(s, y, a.plane);
                if (WorldQuery.isSolidWall(w, p)) solid.add(p.toLong());
            }
        }
        return solid;
    }

    public Hole find(World w, WallAnchor a, Set<Long> baseline) {
        List<BlockPos> cells = new ArrayList<BlockPos>();
        Block wall = null;
        int reach = Config.MAX_HOLE_WIDTH + 2;

        for (int dy : rowOrder()) {
            int y = a.y + dy;
            for (int s = a.seedSpan - reach; s <= a.seedSpan + reach; s++) {
                BlockPos wallCell = a.span.make(s, y, a.plane);
                long key = wallCell.toLong();
                if (!baseline.contains(key)) continue;          // wasn't part of the wall we snapshotted
                if (WorldQuery.isSolidWall(w, wallCell)) continue; // still standing, nothing to do

                // this baseline cell is now open - that IS the target, refill it directly
                if (wall == null) wall = neighbourWallBlock(w, a, s, y);
                cells.add(wallCell);
            }
        }
        orderEdgeFirst(cells, a);
        return cells.size() < Config.MIN_HOLE_CELLS || cells.isEmpty() ? null : new Hole(a, cells, wall);
    }

    private Block neighbourWallBlock(World w, WallAnchor a, int s, int y) {
        BlockPos left = a.span.make(s - 1, y, a.plane);
        if (WorldQuery.isSolidWall(w, left)) return w.getBlockState(left).getBlock();
        BlockPos right = a.span.make(s + 1, y, a.plane);
        if (WorldQuery.isSolidWall(w, right)) return w.getBlockState(right).getBlock();
        return null;
    }

    /** Cells next to a surviving neighbour go first, so PlacementValidator always has support to click against. */
    private void orderEdgeFirst(List<BlockPos> cells, WallAnchor a) {
        final Set<Long> set = new HashSet<Long>();
        for (BlockPos p : cells) set.add(p.toLong());
        java.util.Collections.sort(cells, new java.util.Comparator<BlockPos>() {
            public int compare(BlockPos x, BlockPos y2) { return rank(x) - rank(y2); }
            private int rank(BlockPos p) {
                int s = a.span.span(p);
                boolean edge = !set.contains(a.span.make(s - 1, p.getY(), a.plane).toLong())
                            || !set.contains(a.span.make(s + 1, p.getY(), a.plane).toLong());
                return edge ? 0 : 1;
            }
        });
    }

    private List<Integer> rowOrder() {
        List<Integer> r = new ArrayList<Integer>();
        r.add(0);
        for (int i = 1; i <= Config.ROWS_BELOW; i++) r.add(-i);
        for (int i = 1; i <= Config.ROWS_ABOVE; i++) r.add(i);
        return r;
    }
}
