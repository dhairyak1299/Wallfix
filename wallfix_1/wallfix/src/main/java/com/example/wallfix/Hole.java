package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cells to refill, already ordered edge-first so each placement has a neighbour to click. */
public final class Hole {
    public final WallAnchor anchor;
    public final List<BlockPos> cells;
    public final Block wallBlock; // block type of a surviving neighbour, may be null
    private final Set<Long> lookup = new HashSet<Long>();

    public Hole(WallAnchor anchor, List<BlockPos> cells, Block wallBlock) {
        this.anchor = anchor; this.cells = cells; this.wallBlock = wallBlock;
        for (BlockPos p : cells) lookup.add(p.toLong());
    }
    public boolean contains(BlockPos p) { return lookup.contains(p.toLong()); }
}
