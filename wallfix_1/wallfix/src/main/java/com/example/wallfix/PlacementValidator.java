package com.example.wallfix;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Module: a placement is approved only if the cell is inside the hole, on the right Y-level,
 * still open, has a safe support block, is in reach, and vanilla's own canPlaceBlockOnSide agrees.
 * Deliberately simple: the face center is used directly as the click point, same as other
 * placement-automation tools do - no custom line-of-sight raytrace, since that's one more thing
 * that can silently reject every placement if the math is even slightly off.
 */
public final class PlacementValidator {

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
            Vec3 hv = faceCenter(sup, face);
            if (eye.distanceTo(hv) > Config.REACH) continue;
            if (!ib.canPlaceBlockOnSide(w, sup, face, p, stack)) continue;
            return new PlacementPlan(cell, sup, face, hv);
        }
        return null;
    }

    private Vec3 faceCenter(BlockPos sup, EnumFacing face) {
        return new Vec3(
            sup.getX() + 0.5 + face.getFrontOffsetX() * 0.5,
            sup.getY() + 0.5 + face.getFrontOffsetY() * 0.5,
            sup.getZ() + 0.5 + face.getFrontOffsetZ() * 0.5
        );
    }
}
