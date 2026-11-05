package com.example.wallfix;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Module (auto mode): the wall row comes from where the crosshair crosses each candidate wall plane.
 * No "first block the ray hits" - that is what let a ray sail through a hole and lock onto a far wall.
 * The first plane (nearest) where the crossing cell is wall, or sits in a gap bounded by wall, wins.
 * Both orientations are tried; the one with the longer run of solid wall is kept. Read-only.
 */
public final class ImpactDetector {
    private static final int PLANE_RADIUS = 14;
    private static final int SCORE_RADIUS = 6;

    public WallAnchor detect(EntityPlayerSP p, WorldClient w) {
        Vec3 eye = p.getPositionEyes(1.0F);
        Vec3 look = p.getLookVec();
        SpanAxis primary = AxisResolver.resolve(look);
        SpanAxis[] order = { primary, primary == SpanAxis.X ? SpanAxis.Z : SpanAxis.X };

        WallAnchor best = null; int bestScore = -1;
        for (SpanAxis ax : order) {
            WallAnchor a = scan(ax, eye, look, w);
            if (a == null) continue;
            int score = WorldQuery.solidNeighbours(w, ax, a.plane, a.y, a.seedSpan, SCORE_RADIUS);
            if (score > bestScore) { bestScore = score; best = a; }
        }
        return best;
    }

    private WallAnchor scan(SpanAxis ax, Vec3 eye, Vec3 look, World w) {
        double lookPlane = ax == SpanAxis.X ? look.zCoord : look.xCoord;
        double lookSpan  = ax == SpanAxis.X ? look.xCoord : look.zCoord;
        double eyePlane  = ax == SpanAxis.X ? eye.zCoord  : eye.xCoord;
        double eyeSpan   = ax == SpanAxis.X ? eye.xCoord  : eye.zCoord;
        if (Math.abs(lookPlane) < 0.05) return null;
        int dir = lookPlane > 0 ? 1 : -1;
        int start = (int) Math.floor(eyePlane);

        for (int d = 1; d <= PLANE_RADIUS; d++) {
            int plane = start + dir * d;
            double face = dir > 0 ? plane : plane + 1;          // face of the cell the ray enters through
            double t = (face - eyePlane) / lookPlane;
            if (t <= 0) continue;
            int y    = (int) Math.floor(eye.yCoord + look.yCoord * t);
            int span = (int) Math.floor(eyeSpan + lookSpan * t);
            if (WorldQuery.isWallLike(w, ax, plane, y, span, Config.MAX_HOLE_WIDTH)) return new WallAnchor(ax, plane, y, span);
        }
        return null;
    }
}
