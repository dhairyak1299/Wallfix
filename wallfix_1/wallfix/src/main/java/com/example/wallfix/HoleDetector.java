package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;

/**
 * Module: live scan, no history. A "hole" is a run of open cells (air/water) in the watched row
 * with surviving wall on BOTH ends - so open space past the end of a wall is never treated as damage
 * (REQUIRE_BOUNDED_GAP). Cells are ordered from both ends inward, so every placement has a neighbour
 * to click against and two placements per tick close the gap twice as fast.
 */
public final class HoleDetector {

    public Hole find(World w, WallAnchor a) {
        List<BlockPos> cells = new ArrayList<BlockPos>();
        Block wall = null;
        int reach = Config.MAX_HOLE_WIDTH + 2;
        int lo = a.seedSpan - reach, hi = a.seedSpan + reach;

        for (int dy : rowOrder()) {
            int y = a.y + dy;
            int s = lo;
            while (s <= hi) {
                if (!WorldQuery.isOpen(w, a.span.make(s, y, a.plane))) { s++; continue; }
                int start = s;
                while (s <= hi && WorldQuery.isOpen(w, a.span.make(s, y, a.plane))) s++;
                int end = s - 1;                                   // open run = [start, end]

                BlockPos left = a.span.make(start - 1, y, a.plane), right = a.span.make(end + 1, y, a.plane);
                boolean leftSolid = WorldQuery.isSolidWall(w, left), rightSolid = WorldQuery.isSolidWall(w, right);
                if (!leftSolid && !rightSolid) continue;           // not touching any surviving wall

                if (Config.REQUIRE_BOUNDED_GAP) {
                    if (!(leftSolid && rightSolid)) continue;      // open on one side = end of wall, not a hole
                    if (end - start + 1 > Config.MAX_HOLE_WIDTH) continue;
                    for (int i = 0; start + i <= end - i; i++) {   // both ends inward
                        cells.add(a.span.make(start + i, y, a.plane));
                        if (end - i != start + i) cells.add(a.span.make(end - i, y, a.plane));
                    }
                } else {
                    if (leftSolid) cells.add(a.span.make(start, y, a.plane));
                    if (rightSolid && end != start) cells.add(a.span.make(end, y, a.plane));
                }
                if (wall == null) wall = w.getBlockState(leftSolid ? left : right).getBlock();
            }
        }
        if (cells.isEmpty() || cells.size() < Config.MIN_HOLE_CELLS) return null;
        return new Hole(a, cells, wall);
    }

    private List<Integer> rowOrder() {
        List<Integer> r = new ArrayList<Integer>();
        r.add(0);
        for (int i = 1; i <= Config.ROWS_BELOW; i++) r.add(-i);
        for (int i = 1; i <= Config.ROWS_ABOVE; i++) r.add(i);
        return r;
    }
}
