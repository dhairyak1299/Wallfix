package com.example.wallfix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import java.util.HashMap;
import java.util.Map;

/**
 * Module: toggle + state machine.
 *  ARMED     - every tick: keep the wall anchor valid, check the watched row live for a gap, react the same tick.
 *  REPAIRING - refill until sealed (or out of blocks). Placement is paced by a token bucket (blocks/second).
 *
 * Latency design:
 *  - The anchor (axis, plane, row) is KEPT between repairs, so detection never waits for you to face the wall again.
 *  - In locked-Y mode the watched window follows your position along the wall.
 *  - Nothing delays a repair: no impact gate, and no abort before the next shot (Config.STOP_BEFORE_NEXT_SHOT).
 *  - Rejected placements are remembered across cycles with a short backoff instead of stalling a whole cycle.
 * Never touches movement, rotation or position.
 */
public final class RepairController {
    private enum State { DISABLED, ARMED, REPAIRING }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding[] toggleKeys;
    private final ImpactDetector impact = new ImpactDetector();
    private final PlaneLocator planeLocator = new PlaneLocator();
    private final HoleDetector holes = new HoleDetector();
    private final PlacementValidator validator = new PlacementValidator();
    private final PlacementExecutor executor = new PlacementExecutor();
    private final CycleClock clock = new CycleClock();
    private final Map<Long, long[]> attempts = new HashMap<Long, long[]>(); // cell -> {lastTryMs, tryCount}

    private State state = State.DISABLED;
    public boolean isEnabled() { return state != State.DISABLED; }
    public boolean isRepairing() { return state == State.REPAIRING; }
    private WallAnchor anchor;
    private volatile Integer lockedY = null; // null = auto from aim
    private boolean warnedNoWall = false;

    // ----- what the HUD shows -----
    private volatile String note = "";
    private String lastSummary = "";
    private long gapSeenAt = 0, firstPlacedAt = 0;
    /** One-line explanation of what the tool sees right now, so a silent failure is never a mystery. */
    public String getNote() { return state == State.DISABLED ? "" : note; }

    // ----- placement pacing (blocks per second) -----
    private volatile double rate = Config.DEFAULT_PLACE_PER_SEC;
    private double tokens = 1.0;
    private long lastRefill = 0;
    public void setPlacementsPerSecond(double r) { rate = Math.max(Config.MIN_RATE, Math.min(Config.MAX_RATE, r)); }
    public double getPlacementsPerSecond() { return rate; }

    private void refill(long now) {
        if (lastRefill == 0) { lastRefill = now; return; }
        double r = rate;
        double cap = Math.max(1.0, Math.ceil(r / 20.0));     // ~1 block of burst; 2 only when the rate itself needs it
        tokens = Math.min(cap, tokens + (now - lastRefill) * r / 1000.0);
        lastRefill = now;
    }

    public void setLockedY(Integer y) { lockedY = y; if (state == State.ARMED) anchor = null; }
    public Integer getLockedY() { return lockedY; }

    public RepairController(KeyBinding... toggleKeys) { this.toggleKeys = toggleKeys; }

    public void tick() {
        for (KeyBinding k : toggleKeys) while (k.isPressed()) toggle();
        if (state == State.DISABLED) return;

        long now = System.currentTimeMillis();
        refill(now);

        EntityPlayerSP p = mc.thePlayer;
        WorldClient w = mc.theWorld;
        if (p == null || w == null || mc.currentScreen != null) return;

        if (state == State.ARMED) tickArmed(p, w, now);
        else tickRepairing(p, w, now);
    }

    // ---------------------------------------------------------------- anchor upkeep

    private static int spanOf(WallAnchor a, EntityPlayerSP p) { return (int) Math.floor(a.span == SpanAxis.X ? p.posX : p.posZ); }

    /** Locked mode: slide the watched window to where the player is now (same wall, same row). */
    private WallAnchor recentre(WallAnchor a, EntityPlayerSP p) {
        int ps = spanOf(a, p);
        return ps == a.seedSpan ? a : new WallAnchor(a.span, a.plane, a.y, ps);
    }

    /** Locked mode: is there still wall in this row near the player? If not (you walked to another wall), re-find it. */
    private boolean anchorStillValid(WallAnchor a, EntityPlayerSP p, WorldClient w) {
        int ps = spanOf(a, p), reach = Config.MAX_HOLE_WIDTH + 2;
        for (int s = ps - reach; s <= ps + reach; s++) if (WorldQuery.isSolidWall(w, a.span.make(s, a.y, a.plane))) return true;
        return false;
    }

    // ---------------------------------------------------------------- states

    private void tickArmed(EntityPlayerSP p, WorldClient w, long now) {
        Integer ly = lockedY;
        if (ly != null) {
            // Locked Y: find the wall once, then KEEP it and follow the player along it.
            if (anchor != null && (anchor.y != ly || !anchorStillValid(anchor, p, w))) anchor = null;
            if (anchor == null) {
                WallAnchor a = planeLocator.locate(p, w, ly);
                if (a != null) { anchor = a; warnedNoWall = false; }
                else if (!warnedNoWall) { chat("WallFix: no obsidian found near Y=" + ly + " in front of you."); warnedNoWall = true; }
            } else {
                anchor = recentre(anchor, p);
            }
        } else {
            // Auto: follow the wall row where the crosshair crosses it; when the crosshair is off the wall, keep the last good one.
            WallAnchor a = impact.detect(p, w);
            if (a != null) anchor = a;
        }

        if (anchor == null) { note = "no wall found - face the wall"; return; }
        Hole h = holes.find(w, anchor);
        if (h == null) {
            note = "wall " + anchor.span + "-axis plane=" + anchor.plane + " y=" + anchor.y + ": no gap" + (lastSummary.isEmpty() ? "" : " | " + lastSummary);
            return;
        }
        clock.noteGap(now);
        gapSeenAt = now; firstPlacedAt = 0;
        state = State.REPAIRING;
        tickRepairing(p, w, now); // same tick, zero added latency
    }

    private void tickRepairing(EntityPlayerSP p, WorldClient w, long now) {
        if (lockedY != null && anchor != null) anchor = recentre(anchor, p);
        Hole hole = holes.find(w, anchor);
        if (hole == null) {                                      // sealed
            if (firstPlacedAt != 0) lastSummary = "last: 1st block +" + (firstPlacedAt - gapSeenAt) + "ms, sealed +" + (now - gapSeenAt) + "ms";
            rearm();
            return;
        }
        if (Config.STOP_BEFORE_NEXT_SHOT && now > clock.repairDeadline()) { rearm(); return; }

        int placed = 0;
        boolean throttled = false;
        for (BlockPos cell : hole.cells) {
            if (placed >= Config.MAX_PER_TICK) break;

            long key = cell.toLong();
            long[] rec = attempts.get(key);
            if (rec != null) {
                long since = now - rec[0];
                if (since > Config.ATTEMPT_MEMORY_MS) { attempts.remove(key); rec = null; }       // old news, start fresh
                else if (since < Config.RETRY_MS) continue;                                      // just tried it
                else if (rec[1] >= Config.MAX_ATTEMPTS) { if (since < Config.BACKOFF_MS) continue; rec[1] = 0; } // server keeps refusing: pause, then try again
            }

            ItemStack stack = executor.ensureBlockInHand(p, hole.wallBlock);
            if (stack == null) { chat("WallFix: no blocks in hotbar."); rearm(); return; }

            PlacementPlan plan = validator.validate(p, w, hole, cell, stack);
            if (plan == null) continue;

            if (tokens < 1.0) { throttled = true; break; }      // something is placeable, but the rate limit says wait

            if (executor.place(mc, plan, stack)) {
                tokens -= 1.0;
                if (rec == null) { rec = new long[] { 0, 0 }; attempts.put(key, rec); }
                rec[0] = now; rec[1]++;
                placed++;
                if (firstPlacedAt == 0) firstPlacedAt = now;
            }
        }
        String rl = fmt(rate) + "/s";
        note = placed > 0 ? "placing @ " + rl + " (" + hole.cells.size() + " cell gap)"
             : throttled ? "pacing @ " + rl + " (" + hole.cells.size() + " cell gap)"
             : hole.cells.size() + " cell gap: nothing placeable (reach/support/retry)";
    }

    static String fmt(double r) { return r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r); }

    /** Back to watching. The anchor is deliberately KEPT, so the next gap is caught instantly whichever way you are facing. */
    private void rearm() { state = State.ARMED; }

    private void toggle() {
        if (state == State.DISABLED) { state = State.ARMED; anchor = null; attempts.clear(); clock.reset(); tokens = 1.0; lastRefill = 0; lastSummary = ""; chat("WallFix: ON"); }
        else { state = State.DISABLED; anchor = null; attempts.clear(); chat("WallFix: OFF"); }
    }

    private void chat(String s) { if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(s)); }
}
