package com.example.wallfix;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Module (locked-Y mode): finds the wall's axis + plane from the player's position and facing.
 *  - Looks NEAREST plane first, across nearby columns, so a breach in the exact column you face
 *    can never make the search run on to some unrelated wall deeper in the structure.
 *  - Tries both wall orientations and keeps the one with the longer run of solid wall along it,
 *    so facing a wall at an angle can't be mistaken for a wall running the other way.
 */
public final class PlaneLocator {
    private static final int PLANE_SEARCH_RADIUS = 8;
    private static final int SPAN_SEARCH_RADIUS  = Config.MAX_HOLE_WIDTH / 2 + 2; // must reach the flank of the widest hole centred on you

    public WallAnchor locate(EntityPlayerSP p, World w, int y) {
        Vec3 look = p.getLookVec();
        SpanAxis primary = AxisResolver.resolve(look);
        SpanAxis[] order = { primary, primary == SpanAxis.X ? SpanAxis.Z : SpanAxis.X };

        WallAnchor best = null; int bestScore = -1;
        for (SpanAxis ax : order) {
            double lookPlane = ax == SpanAxis.X ? look.zCoord : look.xCoord;
            if (Math.abs(lookPlane) < 0.05) continue;
            int dir = lookPlane > 0 ? 1 : -1;
            int seedSpan   = (int) Math.floor(ax == SpanAxis.X ? p.posX : p.posZ);
            int startPlane = (int) Math.floor(ax == SpanAxis.X ? p.posZ : p.posX);

            int[] hit = null;
            search:
            for (int d = 1; d <= PLANE_SEARCH_RADIUS; d++) {          // nearest plane first
                int plane = startPlane + dir * d;
                for (int ds : spanOffsets()) {                         // then nearest column
                    if (WorldQuery.isSolidWall(w, ax.make(seedSpan + ds, y, plane))) { hit = new int[] { plane, seedSpan + ds }; break search; }
                }
            }
            if (hit == null) continue;
            int score = WorldQuery.solidNeighbours(w, ax, hit[0], y, hit[1], Config.WALL_SCORE_RADIUS);
            if (score >= Config.MIN_WALL_SCORE && score > bestScore) { bestScore = score; best = new WallAnchor(ax, hit[0], y, seedSpan); }
        }
        return best;
    }

    private int[] spanOffsets() {
        int[] out = new int[SPAN_SEARCH_RADIUS * 2 + 1];
        out[0] = 0; int idx = 1;
        for (int i = 1; i <= SPAN_SEARCH_RADIUS; i++) { out[idx++] = -i; out[idx++] = i; }
        return out;
    }
}
