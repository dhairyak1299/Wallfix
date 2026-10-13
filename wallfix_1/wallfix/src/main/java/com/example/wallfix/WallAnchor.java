package com.example.wallfix;

/** The wall row being watched: which plane, which Y, and the column the player is aiming at. */
public final class WallAnchor {
    public final SpanAxis span;
    public final int plane;
    public final int y;
    public final int seedSpan;

    public WallAnchor(SpanAxis span, int plane, int y, int seedSpan) {
        this.span = span; this.plane = plane; this.y = y; this.seedSpan = seedSpan;
    }
}
