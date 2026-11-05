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
 *  ARMED     - each tick: establish/keep an anchor (wall row), check it live for any breach.
 *  REPAIRING - anchor frozen, refill until sealed, out of time, or out of blocks.
 * Detection is fully live (no snapshot/history) so arming the mod after damage
 * already exists works exactly the same as arming it before. Never touches
 * movement, rotation or position.
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
    private final Map<Long, long[]> attempts = new HashMap<Long, long[]>(); // cell -> {lastMs, count}

    private State state = State.DISABLED;
    public boolean isEnabled() { return state != State.DISABLED; }
    public boolean isRepairing() { return state == State.REPAIRING; }
    private WallAnchor anchor;
    private volatile Integer lockedY = null; // null = auto from aim
    private boolean warnedNoWall = false;
    private volatile String note = "";
    /** One-line explanation of what the tool sees right now; shown under the status tag so a silent failure is never a mystery. */
    public String getNote() { return state == State.DISABLED ? "" : note; }

    public void setLockedY(Integer y) { lockedY = y; if (state == State.ARMED) anchor = null; }
    public Integer getLockedY() { return lockedY; }

    public RepairController(KeyBinding... toggleKeys) { this.toggleKeys = toggleKeys; }

    public void tick() {
        for (KeyBinding k : toggleKeys) while (k.isPressed()) toggle();
        if (state == State.DISABLED) return;

        EntityPlayerSP p = mc.thePlayer;
        WorldClient w = mc.theWorld;
        if (p == null || w == null || mc.currentScreen != null) return;
        long now = System.currentTimeMillis();

        if (state == State.ARMED) tickArmed(p, w, now);
        else tickRepairing(p, w, now);
    }

    private void tickArmed(EntityPlayerSP p, WorldClient w, long now) {
        Integer ly = lockedY;
        if (ly != null) {
            // Locked Y: find the wall ONCE from position+facing, then freeze it.
            if (anchor == null) {
                WallAnchor a = planeLocator.locate(p, w, ly);
                if (a != null) { anchor = a; warnedNoWall = false; }
                else if (!warnedNoWall) { chat("WallFix: no obsidian found near Y=" + ly + " in front of you."); warnedNoWall = true; }
            }
        } else {
            // Auto: track the wall row where the crosshair crosses it.
            WallAnchor a = impact.detect(p, w);
            if (a != null) anchor = a;
        }

        if (anchor == null) { note = "no wall found - face the wall"; return; }
        Hole h = holes.find(w, anchor);
        note = h == null ? "wall " + anchor.span + "-axis plane=" + anchor.plane + " y=" + anchor.y + ": no gap" : "gap of " + h.cells.size() + " cell(s)";
        if (h != null && clock.acceptImpact(now)) {
            clock.registerImpact(now);
            attempts.clear();
            state = State.REPAIRING;
            tickRepairing(p, w, now); // same tick, zero added latency
        }
    }

    private void tickRepairing(EntityPlayerSP p, WorldClient w, long now) {
        Hole hole = holes.find(w, anchor);
        if (hole == null) { rearm(); return; }                 // sealed
        if (now > clock.repairDeadline()) { rearm(); return; } // next shot is close, stop interfering

        int placed = 0;
        for (BlockPos cell : hole.cells) {
            if (placed >= Config.MAX_PER_TICK) break;

            long[] rec = attempts.get(cell.toLong());
            if (rec != null && (rec[1] >= Config.MAX_ATTEMPTS || now - rec[0] < Config.RETRY_MS)) continue;

            ItemStack stack = executor.ensureBlockInHand(p, hole.wallBlock);
            if (stack == null) { chat("WallFix: no blocks in hotbar."); rearm(); return; }

            PlacementPlan plan = validator.validate(p, w, hole, cell, stack);
            if (plan == null) continue;

            if (executor.place(mc, plan, stack)) {
                if (rec == null) { rec = new long[] { 0, 0 }; attempts.put(cell.toLong(), rec); }
                rec[0] = now; rec[1]++;
                placed++;
            }
        }
        note = placed > 0 ? "placing (" + hole.cells.size() + " cell gap)" : hole.cells.size() + " cell gap: nothing placeable (reach/support/retry)";
    }

    private void rearm() { state = State.ARMED; anchor = null; attempts.clear(); }

    private void toggle() {
        if (state == State.DISABLED) { rearm(); clock.reset(); chat("WallFix: ON"); }
        else { state = State.DISABLED; anchor = null; attempts.clear(); chat("WallFix: OFF"); }
    }

    private void chat(String s) { if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(s)); }
}
