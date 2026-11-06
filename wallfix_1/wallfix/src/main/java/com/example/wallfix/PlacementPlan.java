package com.example.wallfix;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public final class PlacementPlan {
    public final BlockPos target, support;
    public final EnumFacing face;
    public final Vec3 hitVec;

    public PlacementPlan(BlockPos target, BlockPos support, EnumFacing face, Vec3 hitVec) {
        this.target = target; this.support = support; this.face = face; this.hitVec = hitVec;
    }
}
