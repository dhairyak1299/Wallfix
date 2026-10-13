package com.example.wallfix;

/** Module: tracks the TNT cycle. Learns the real period, rejects too-close impacts, sets the repair deadline. */
public final class CycleClock {
    private long lastImpact = 0;
    private double period = Config.NOMINAL_CYCLE_MS;

    public boolean acceptImpact(long now) {
        return lastImpact == 0 || now - lastImpact >= Config.MIN_IMPACT_GAP;
    }

    public void registerImpact(long now) {
        if (lastImpact != 0) {
            long dt = now - lastImpact;
            if (dt > Config.NOMINAL_CYCLE_MS * 0.5 && dt < Config.NOMINAL_CYCLE_MS * 2.0) period = period * 0.7 + dt * 0.3;
        }
        lastImpact = now;
    }

    public long repairDeadline() { return lastImpact + (long) (period - Config.SAFETY_MS); }
    public void reset() { lastImpact = 0; period = Config.NOMINAL_CYCLE_MS; }
}
