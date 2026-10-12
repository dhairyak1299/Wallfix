package com.example.wallfix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Module: opens PatchGui on keypress. Independent of enable/disable state, so you can set Y before arming. */
public final class GuiKeyHandler {
    private final KeyBinding key;
    private final RepairController controller;

    public GuiKeyHandler(KeyBinding key, RepairController controller) { this.key = key; this.controller = controller; }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        while (key.isPressed()) {
            if (mc.currentScreen == null) mc.displayGuiScreen(new PatchGui(controller));
        }
    }
}
