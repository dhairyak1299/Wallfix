package com.example.wallfix;

import net.minecraft.util.Vec3;

/** Module: decide whether the opening spans X or Z from the player's look direction. */
public final class AxisResolver {
    private AxisResolver() {}

    public static SpanAxis resolve(Vec3 look) {
        // Looking mostly along X means the wall lies in a fixed-X plane and the opening spans Z.
        return Math.abs(look.xCoord) > Math.abs(look.zCoord) ? SpanAxis.Z : SpanAxis.X;
    }
}
