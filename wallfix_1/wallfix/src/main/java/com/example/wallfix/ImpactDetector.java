package com.example.wallfix;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/** Module: find the wall row (plane + Y + aim column) from where the player is looking. Read-only. */
public final class ImpactDetector {

    public WallAnchor detect(EntityPlayerSP p, WorldClient w) {
        Vec3 eye = p.getPositionEyes(1.0F);
        Vec3 look = p.getLook(1.0F);
        Vec3 end = eye.addVector(look.xCoord * Config.RAY_RANGE, look.yCoord * Config.RAY_RANGE, look.zCoord * Config.RAY_RANGE);

        MovingObjectPosition hit = w.rayTraceBlocks(eye, end, false, true, false);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        if (!hit.sideHit.getAxis().isHorizontal()) return null; // aiming at a floor/ceiling, not a wall

        SpanAxis span = AxisResolver.resolve(look);
        BlockPos bp = hit.getBlockPos();
        return new WallAnchor(span, span.plane(bp), bp.getY(), span.span(bp));
    }
}
