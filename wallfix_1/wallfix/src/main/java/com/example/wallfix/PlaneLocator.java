package com.example.wallfix;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * Module: finds the wall's axis + plane coordinate from the player's position and facing,
 * instead of a raycast. Used whenever Y is locked, so aiming through an open hole can never
 * make the anchor drift onto some unrelated surface behind it.
 */
public final class PlaneLocator {
    private static final int SEARCH_RADIUS = 6;

    public WallAnchor locate(EntityPlayerSP p, World w, int y) {
        SpanAxis axis = AxisResolver.resolve(p.getLookVec());
        int seedSpan = axis == SpanAxis.X ? (int) Math.floor(p.posX) : (int) Math.floor(p.posZ);
        int startPlane = axis == SpanAxis.X ? (int) Math.floor(p.posZ) : (int) Math.floor(p.posX);

        // yaw tells us which way along the plane axis the player is facing
        double yaw = p.rotationYaw % 360;
        if (yaw < 0) yaw += 360;
        int dir = planeDirection(axis, yaw);

        for (int d = 0; d <= SEARCH_RADIUS; d++) {
            int plane = startPlane + dir * d;
            BlockPos probe = axis.make(seedSpan, y, plane);
            if (WorldQuery.isSolidWall(w, probe)) return new WallAnchor(axis, plane, y, seedSpan);
        }
        return null; // nothing obsidian-solid found in front of you at that Y - stand closer / face the wall
    }

    private int planeDirection(SpanAxis axis, double yaw) {
        // yaw 0 = south(+Z), 90 = west(-X), 180 = north(-Z), 270 = east(+X)
        if (axis == SpanAxis.X) return (yaw > 90 && yaw < 270) ? -1 : 1;      // facing north vs south -> plane is Z
        return (yaw > 0 && yaw < 180) ? -1 : 1;                              // facing west vs east -> plane is X
    }
}
