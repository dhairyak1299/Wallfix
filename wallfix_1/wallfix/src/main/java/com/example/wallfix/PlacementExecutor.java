package com.example.wallfix;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** Module: performs the real right-click placement through PlayerControllerMP (sends C08, swings arm). */
public final class PlacementExecutor {

    public boolean place(Minecraft mc, PlacementPlan plan, ItemStack stack) {
        boolean ok = mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, plan.support, plan.face, plan.hitVec);
        if (ok) mc.thePlayer.swingItem();
        return ok;
    }

    /** Returns a usable block stack in hand, switching hotbar slot if allowed. Prefers the wall's own block type. */
    public ItemStack ensureBlockInHand(EntityPlayerSP p, Block preferred) {
        ItemStack held = p.inventory.getCurrentItem();
        if (isUsable(held) && (preferred == null || ((ItemBlock) held.getItem()).getBlock() == preferred)) return held;
        if (!Config.AUTO_SWITCH) return isUsable(held) ? held : null;

        int fallback = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.inventory.mainInventory[i];
            if (!isUsable(s)) continue;
            if (preferred != null && ((ItemBlock) s.getItem()).getBlock() == preferred) { p.inventory.currentItem = i; return s; }
            if (fallback < 0) fallback = i;
        }
        if (isUsable(held)) return held;
        if (fallback >= 0) { p.inventory.currentItem = fallback; return p.inventory.mainInventory[fallback]; }
        return null;
    }

    private boolean isUsable(ItemStack s) {
        return s != null && s.stackSize > 0 && s.getItem() instanceof ItemBlock
                && ((ItemBlock) s.getItem()).getBlock().isFullCube();
    }
}
