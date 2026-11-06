package com.example.wallfix;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Module: approves a placement only if the cell is inside the hole, on the right row/plane, still open,
 * has a safe support block (full cube, no GUI/tile entity), is within reach, and vanilla's own
 * canPlaceBlockOnSide agrees. The click point is the NEAREST point of the support's face to your eyes
 * (vanilla lets you click anywhere on a face), and of all valid supports the closest one is used.
 */
public final class PlacementValidator {
    private static final double INSET = 0.05;

    public PlacementPlan validate(EntityPlayerSP p, World w, Hole hole, BlockPos cell, ItemStack stack) {
        WallAnchor a = hole.anchor;
        if (!hole.contains(cell)) return null;
        if (cell.getY() < a.y - Config.ROWS_BELOW || cell.getY() > a.y + Config.ROWS_ABOVE) return null;
        if (a.span.plane(cell) != a.plane) return null;
        if (!WorldQuery.isOpen(w, cell)) return null;
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return null;

        ItemBlock ib = (ItemBlock) stack.getItem();
        Vec3 eye = p.getPositionEyes(1.0F);

        PlacementPlan best = null; double bestDist = Double.MAX_VALUE;
        for (EnumFacing dir : EnumFacing.values()) {
            BlockPos sup = cell.offset(dir);
            if (!WorldQuery.isSupport(w, sup)) continue;

            EnumFacing face = dir.getOpposite();                 // face of the support that points at the cell
            Vec3 hv = nearestOnFace(eye, sup, face);
            double dist = eye.distanceTo(hv);
            if (dist > Config.REACH || dist >= bestDist) continue;
            if (!ib.canPlaceBlockOnSide(w, sup, face, p, stack)) continue;
            best = new PlacementPlan(cell, sup, face, hv); bestDist = dist;
        }
        return best;
    }

    private Vec3 nearestOnFace(Vec3 eye, BlockPos sup, EnumFacing face) {
        int nx = face.getFrontOffsetX(), ny = face.getFrontOffsetY(), nz = face.getFrontOffsetZ();
        double x = nx != 0 ? sup.getX() + (nx > 0 ? 1 : 0) : clamp(eye.xCoord, sup.getX());
        double y = ny != 0 ? sup.getY() + (ny > 0 ? 1 : 0) : clamp(eye.yCoord, sup.getY());
        double z = nz != 0 ? sup.getZ() + (nz > 0 ? 1 : 0) : clamp(eye.zCoord, sup.getZ());
        return new Vec3(x, y, z);
    }

    private double clamp(double v, int blockMin) {
        return Math.max(blockMin + INSET, Math.min(blockMin + 1 - INSET, v));
    }
}
