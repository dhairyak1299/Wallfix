package com.example.wallfix;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/** Client-side command (never sent to the server): /patch <y> | /patch auto | /patch */
public final class PatchCommand extends CommandBase {
    private final RepairController controller;

    public PatchCommand(RepairController controller) { this.controller = controller; }

    @Override public String getCommandName() { return "patch"; }
    @Override public String getCommandUsage(ICommandSender s) { return "/patch <y> | /patch auto"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            Integer y = controller.getLockedY();
            say(sender, y == null ? "WallFix Y: auto (from aim)" : "WallFix Y: locked to " + y);
        } else if (args[0].equalsIgnoreCase("auto")) {
            controller.setLockedY(null);
            say(sender, "WallFix Y: auto (from aim)");
        } else {
            try {
                int y = Integer.parseInt(args[0]);
                if (y < 0 || y > 255) { say(sender, "Y must be 0-255."); return; }
                controller.setLockedY(y);
                say(sender, "WallFix Y: locked to " + y);
            } catch (NumberFormatException e) {
                say(sender, "Usage: /patch <y> | /patch auto");
            }
        }
    }

    private void say(ICommandSender s, String m) { s.addChatMessage(new ChatComponentText(m)); }
}
