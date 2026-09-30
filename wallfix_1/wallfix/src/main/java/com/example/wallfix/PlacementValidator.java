package com.example.wallfix;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Module: a placement is approved only if the cell is inside the hole, on the right Y-level,
 * still open, has a safe support block, is in reach, is actually visible from the eyes,
 * and vanilla's own canPlaceBlockOnSide agrees.
 */
public final class PlacementValidator {

    private static final double[] OFFS = { 0.0, 0.25, -0.25 };

    public PlacementPlan validate(EntityPlayerSP p, World w, Hole hole, BlockPos cell, ItemStack stack) {
        WallAnchor a = hole.anchor;
        if (!hole.contains(cell)) return null;
        if (cell.getY() < a.y - Config.ROWS_BELOW || cell.getY() > a.y + Config.ROWS_ABOVE) return null;
        if (a.span.plane(cell) != a.plane) return null;
        if (!WorldQuery.isOpen(w, cell)) return null;
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return null;

        ItemBlock ib = (ItemBlock) stack.getItem();
        Vec3 eye = p.getPositionEyes(1.0F);

        EnumFacing[] order = { a.span.spanNeg(), a.span.spanPos(), EnumFacing.DOWN, EnumFacing.UP, a.span.planeNeg(), a.span.planePos() };
        for (EnumFacing dir : order) {
            BlockPos sup = cell.offset(dir);
            if (!WorldQuery.isSupport(w, sup)) continue;

            EnumFacing face = dir.getOpposite(); // face of the support that points at the cell
            Vec3 hv = findVisibleHit(w, eye, sup, face);
            if (hv == null) continue;
            if (!ib.canPlaceBlockOnSide(w, sup, face, p, stack)) continue;
            return new PlacementPlan(cell, sup, face, hv);
        }
        return null;
    }

    private Vec3 findVisibleHit(World w, Vec3 eye, BlockPos sup, EnumFacing face) {
        int nx = face.getFrontOffsetX(), ny = face.getFrontOffsetY(), nz = face.getFrontOffsetZ();
        double cx = sup.getX() + 0.5 + nx * 0.5, cy = sup.getY() + 0.5 + ny * 0.5, cz = sup.getZ() + 0.5 + nz * 0.5;

        // tangent axes (the two axes not equal to the face normal)
        double[][] tan = nx != 0 ? new double[][] { {0,1,0}, {0,0,1} }
                       : ny != 0 ? new double[][] { {1,0,0}, {0,0,1} }
                       :           new double[][] { {1,0,0}, {0,1,0} };

        for (double u : OFFS) for (double v : OFFS) {
            Vec3 hv = new Vec3(cx + tan[0][0]*u + tan[1][0]*v,
                               cy + tan[0][1]*u + tan[1][1]*v,
                               cz + tan[0][2]*u + tan[1][2]*v);
            if (eye.distanceTo(hv) > Config.REACH) continue;

            Vec3 end = hv.addVector(-nx * 0.05, -ny * 0.05, -nz * 0.05); // just inside the support block
            MovingObjectPosition r = w.rayTraceBlocks(eye, end, false, false, false);
            if (r != null && r.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && r.getBlockPos().equals(sup) && r.sideHit == face) {
                return hv;
            }
        }
        return null;
    }
}
