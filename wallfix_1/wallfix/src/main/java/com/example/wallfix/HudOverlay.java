package com.example.wallfix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

/** Module: small always-visible status line, top-left. Draws only, never touches game state. */
public final class HudOverlay {
    private final RepairController controller;

    public HudOverlay(RepairController controller) { this.controller = controller; }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo) return; // don't fight the F3 screen

        String status;
        int color;
        if (!controller.isEnabled())      { status = "WallFix: OFF";       color = 0xFF5555; }
        else if (controller.isRepairing()) { status = "WallFix: REPAIRING"; color = 0xFFFF55; }
        else                                { status = "WallFix: ARMED";    color = 0x55FF55; }

        Integer y = controller.getLockedY();
        status += y == null ? "  [Y: auto]" : "  [Y: " + y + " locked]";

        FontRenderer fr = mc.fontRendererObj;
        String note = controller.getNote();
        int x = 4, top = 4;
        int width = Math.max(fr.getStringWidth(status), fr.getStringWidth(note));
        int bottom = top + fr.FONT_HEIGHT + 1 + (note.isEmpty() ? 0 : fr.FONT_HEIGHT + 2);
        GuiUtils.drawGradientRect(0, x - 2, top - 2, x + width + 2, bottom, 0x90000000, 0x90000000);
        fr.drawStringWithShadow(status, x, top, color);
        if (!note.isEmpty()) fr.drawStringWithShadow(note, x, top + fr.FONT_HEIGHT + 2, 0xAAAAAA);
    }
}
