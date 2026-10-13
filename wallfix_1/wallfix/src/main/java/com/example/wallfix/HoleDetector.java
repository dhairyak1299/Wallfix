package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Module: live scan, no history needed. Any open (air/water) cell sitting right next to
 * a surviving obsidian block, within range of the watched anchor, is a target - whether
 * that gap has existed for ten seconds or ten minutes before the mod was armed.
 */
public final class HoleDetector {

    public Hole find(World w, WallAnchor a) {
        List<BlockPos> cells = new ArrayList<BlockPos>();
        Block wall = null;
        int reach = Config.MAX_HOLE_WIDTH + 2;

        for (int dy : rowOrder()) {
            int y = a.y + dy;
            for (int s = a.seedSpan - reach; s <= a.seedSpan + reach; s++) {
                BlockPos cell = a.span.make(s, y, a.plane);
                if (!WorldQuery.isOpen(w, cell)) continue;

                BlockPos left = a.span.make(s - 1, y, a.plane);
                BlockPos right = a.span.make(s + 1, y, a.plane);
                boolean leftWall = WorldQuery.isSolidWall(w, left);
                boolean rightWall = WorldQuery.isSolidWall(w, right);
                if (!leftWall && !rightWall) continue; // not touching any surviving obsidian - not our concern

                if (wall == null) wall = w.getBlockState(leftWall ? left : right).getBlock();
                cells.add(cell);
            }
        }
        if (cells.size() < Config.MIN_HOLE_CELLS || cells.isEmpty()) return null;
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
