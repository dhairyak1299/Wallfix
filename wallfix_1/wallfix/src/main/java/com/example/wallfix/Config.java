package com.example.wallfix;

/** Central tuning. Edit and rebuild. */
public final class Config {
    private Config() {}

    // Detection
    public static final double RAY_RANGE       = 40.0;  // how far the aim ray looks for the wall
    public static final int    ROWS_BELOW      = 0;     // strict single Y-level by default
    public static final int    ROWS_ABOVE      = 0;
    public static final int    MAX_HOLE_WIDTH  = 9;     // wider than this = open space, not a hole
    public static final int    SEED_TOLERANCE  = 6;     // how far from the aim column a hole may start
    public static final int    MIN_HOLE_CELLS  = 1;     // raise to 2 to ignore single blocks you mine yourself

    // Placement
    public static final double REACH           = 4.0;   // conservative vs 4.5 vanilla survival reach
    public static final int    MAX_PER_TICK    = 2;     // placements per client tick (50 ms)
    public static final long   RETRY_MS        = 150;   // wait before re-trying a cell the server hasn't confirmed
    public static final int    MAX_ATTEMPTS    = 3;
    public static final boolean AUTO_SWITCH    = true;  // swap hotbar slot to a block if hand isn't holding one

    // Timing
    public static final double NOMINAL_CYCLE_MS = 3000.0;
    public static final long   SAFETY_MS        = 250;  // stop repairing this long before the next expected shot
    public static final long   MIN_IMPACT_GAP   = 1200; // ignore "new impacts" closer than this
}
