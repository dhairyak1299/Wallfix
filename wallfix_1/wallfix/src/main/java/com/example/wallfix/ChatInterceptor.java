package com.example.wallfix;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Module: lets ".patch <y>" work like a cheat-client command, caught and cancelled
 * before the chat packet is ever sent to the server. "/patch" still works too,
 * handled separately by PatchCommand through vanilla's own command system.
 */
public final class ChatInterceptor {
    private final RepairController controller;

    public ChatInterceptor(RepairController controller) { this.controller = controller; }

    @SubscribeEvent
    public void onChat(ClientChatEvent e) {
        String msg = e.message;
        if (msg == null || !msg.toLowerCase().startsWith(".patch")) return;

        e.setCanceled(true); // never reaches the server
        String[] parts = msg.trim().split("\\s+");
        String arg = parts.length > 1 ? parts[1] : "";

        if (arg.isEmpty()) {
            Integer y = controller.getLockedY();
            say(y == null ? "WallFix Y: auto (from aim)" : "WallFix Y: locked to " + y);
        } else if (arg.equalsIgnoreCase("auto")) {
            controller.setLockedY(null);
            say("WallFix Y: auto (from aim)");
        } else {
            try {
                int y = Integer.parseInt(arg);
                if (y < 0 || y > 255) { say("Y must be 0-255."); return; }
                controller.setLockedY(y);
                say("WallFix Y: locked to " + y);
            } catch (NumberFormatException ex) {
                say("Usage: .patch <y> | .patch auto");
            }
        }
    }

    private void say(String s) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(s));
    }
}
