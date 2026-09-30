package com.example.wallfix;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/** Axis the opening spans along. X = wall lies in a fixed-Z plane; Z = wall lies in a fixed-X plane. */
public enum SpanAxis {
    X, Z;

    public int span(BlockPos p)  { return this == X ? p.getX() : p.getZ(); }
    public int plane(BlockPos p) { return this == X ? p.getZ() : p.getX(); }

    public BlockPos make(int span, int y, int plane) {
        return this == X ? new BlockPos(span, y, plane) : new BlockPos(plane, y, span);
    }
    public EnumFacing spanNeg()  { return this == X ? EnumFacing.WEST  : EnumFacing.NORTH; }
    public EnumFacing spanPos()  { return this == X ? EnumFacing.EAST  : EnumFacing.SOUTH; }
    public EnumFacing planeNeg() { return this == X ? EnumFacing.NORTH : EnumFacing.WEST; }
    public EnumFacing planePos() { return this == X ? EnumFacing.SOUTH : EnumFacing.EAST; }
}
