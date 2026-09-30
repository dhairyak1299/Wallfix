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
import java.util.Set;

/**
 * Module: toggle + state machine.
 *  ARMED     - each tick: check the watched row for a new hole, otherwise refresh anchor + snapshot from the aim ray.
 *  REPAIRING - anchor and snapshot are frozen, refill until sealed, out of time, or out of blocks.
 * It never touches movement, rotation or position.
 */
public final class RepairController {
    private enum State { DISABLED, ARMED, REPAIRING }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final KeyBinding toggleKey;
    private final ImpactDetector impact = new ImpactDetector();
    private final HoleDetector holes = new HoleDetector();
    private final PlacementValidator validator = new PlacementValidator();
    private final PlacementExecutor executor = new PlacementExecutor();
    private final CycleClock clock = new CycleClock();
    private final Map<Long, long[]> attempts = new HashMap<Long, long[]>(); // cell -> {lastMs, count}

    private State state = State.DISABLED;
    public boolean isEnabled() { return state != State.DISABLED; }
    public boolean isRepairing() { return state == State.REPAIRING; }
    private WallAnchor anchor;
    private Set<Long> baseline;
    private volatile Integer lockedY = null; // null = auto from aim

    public void setLockedY(Integer y) { lockedY = y; if (state == State.ARMED) { anchor = null; baseline = null; } }
    public Integer getLockedY() { return lockedY; }

    public RepairController(KeyBinding toggleKey) { this.toggleKey = toggleKey; }

    public void tick() {
        while (toggleKey.isPressed()) toggle();
        if (state == State.DISABLED) return;

        EntityPlayerSP p = mc.thePlayer;
        WorldClient w = mc.theWorld;
        if (p == null || w == null || mc.currentScreen != null) return;
        long now = System.currentTimeMillis();

        if (state == State.ARMED) tickArmed(p, w, now);
        else tickRepairing(p, w, now);
    }

    private void tickArmed(EntityPlayerSP p, WorldClient w, long now) {
        if (anchor != null && baseline != null) {
            Hole h = holes.find(w, anchor, baseline);
            if (h != null && clock.acceptImpact(now)) {
                clock.registerImpact(now);
                attempts.clear();
                state = State.REPAIRING;
                tickRepairing(p, w, now); // same tick, zero added latency
                return;
            }
        }
        WallAnchor a = impact.detect(p, w);
        if (a != null) {
            Integer ly = lockedY;
            if (ly != null) a = new WallAnchor(a.span, a.plane, ly, a.seedSpan);
            anchor = a; baseline = holes.snapshot(w, a);
        }
    }

    private void tickRepairing(EntityPlayerSP p, WorldClient w, long now) {
        Hole hole = holes.find(w, anchor, baseline);
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
    }

    private void rearm() { state = State.ARMED; anchor = null; baseline = null; attempts.clear(); }

    private void toggle() {
        if (state == State.DISABLED) { rearm(); clock.reset(); chat("WallFix: ON"); }
        else { state = State.DISABLED; anchor = null; baseline = null; attempts.clear(); chat("WallFix: OFF"); }
    }

    private void chat(String s) { if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(s)); }
}
