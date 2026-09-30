package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Module: snapshot which wall cells are solid, later report which of them are now open
 * and bounded by surviving blocks on both sides. Pre-existing gaps never count.
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

        for (int dy : rowOrder()) {
            int y = a.y + dy;
            int[] run = scanRow(w, a, y);
            if (run == null) continue;

            if (wall == null) wall = w.getBlockState(a.span.make(run[0] - 1, y, a.plane)).getBlock();
            for (int k = 0; run[0] + k <= run[1] - k; k++) {          // edge-first, alternating
                addIfWasSolid(cells, baseline, a, run[0] + k, y);
                if (run[1] - k != run[0] + k) addIfWasSolid(cells, baseline, a, run[1] - k, y);
            }
        }
        return cells.size() < Config.MIN_HOLE_CELLS || cells.isEmpty() ? null : new Hole(a, cells, wall);
    }

    private void addIfWasSolid(List<BlockPos> out, Set<Long> baseline, WallAnchor a, int s, int y) {
        BlockPos p = a.span.make(s, y, a.plane);
        if (baseline.contains(p.toLong())) out.add(p);
    }

    private int[] scanRow(World w, WallAnchor a, int y) {
        int start = Integer.MIN_VALUE;
        for (int d = 0; d <= Config.SEED_TOLERANCE && start == Integer.MIN_VALUE; d++) {
            if (WorldQuery.isOpen(w, a.span.make(a.seedSpan + d, y, a.plane))) start = a.seedSpan + d;
            else if (WorldQuery.isOpen(w, a.span.make(a.seedSpan - d, y, a.plane))) start = a.seedSpan - d;
        }
        if (start == Integer.MIN_VALUE) return null;

        int lo = start, hi = start;
        while (WorldQuery.isOpen(w, a.span.make(lo - 1, y, a.plane))) { lo--; if (start - lo > Config.MAX_HOLE_WIDTH) return null; }
        while (WorldQuery.isOpen(w, a.span.make(hi + 1, y, a.plane))) { hi++; if (hi - start > Config.MAX_HOLE_WIDTH) return null; }
        if (hi - lo + 1 > Config.MAX_HOLE_WIDTH) return null;

        // Both ends must be surviving wall, otherwise this is open space, not a hole.
        if (!WorldQuery.isSolidWall(w, a.span.make(lo - 1, y, a.plane))) return null;
        if (!WorldQuery.isSolidWall(w, a.span.make(hi + 1, y, a.plane))) return null;
        return new int[] { lo, hi };
    }

    private List<Integer> rowOrder() {
        List<Integer> r = new ArrayList<Integer>();
        r.add(0);
        for (int i = 1; i <= Config.ROWS_BELOW; i++) r.add(-i);
        for (int i = 1; i <= Config.ROWS_ABOVE; i++) r.add(i);
        return r;
    }
}
