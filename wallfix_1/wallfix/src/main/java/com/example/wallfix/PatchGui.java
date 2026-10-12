package com.example.wallfix;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** Module: small popup to set or clear the locked Y-level by hand. Read/writes RepairController only. */
public final class PatchGui extends GuiScreen {
    private final RepairController controller;
    private GuiTextField yField;
    private String status = "";

    private static final int BTN_SET = 0, BTN_AUTO = 1, BTN_CLOSE = 2;

    public PatchGui(RepairController controller) { this.controller = controller; }

    @Override
    public void initGui() {
        int cx = width / 2, cy = height / 2;

        yField = new GuiTextField(0, fontRendererObj, cx - 60, cy - 30, 120, 20);
        yField.setMaxStringLength(3);
        yField.setText(controller.getLockedY() == null ? "" : String.valueOf(controller.getLockedY()));
        yField.setFocused(true);

        buttonList.clear();
        buttonList.add(new GuiButton(BTN_SET, cx - 100, cy, 90, 20, "Set Y"));
        buttonList.add(new GuiButton(BTN_AUTO, cx + 10, cy, 90, 20, "Auto"));
        buttonList.add(new GuiButton(BTN_CLOSE, cx - 40, cy + 30, 80, 20, "Close"));
    }

    @Override
    public boolean doesGuiPauseGame() { return false; } // don't freeze a multiplayer session

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == BTN_SET) trySet();
        else if (b.id == BTN_AUTO) { controller.setLockedY(null); status = "Set to auto."; }
        else if (b.id == BTN_CLOSE) mc.displayGuiScreen(null);
    }

    private void trySet() {
        String t = yField.getText().trim();
        if (t.isEmpty()) { status = "Enter a number 0-255."; return; }
        try {
            int y = Integer.parseInt(t);
            if (y < 0 || y > 255) { status = "Y must be 0-255."; return; }
            controller.setLockedY(y);
            status = "Locked to Y=" + y;
        } catch (NumberFormatException e) {
            status = "Not a number.";
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) { trySet(); return; }
        if (yField.isFocused()) yField.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws java.io.IOException {
        super.mouseClicked(mx, my, btn);
        yField.mouseClicked(mx, my, btn);
    }

    @Override
    public void updateScreen() { yField.updateCursorCounter(); }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        int cx = width / 2, cy = height / 2;
        drawCenteredString(fontRendererObj, "WallFix \u2014 Set Repair Y-Level", cx, cy - 55, 0xFFFFFF);
        yField.drawTextBox();

        Integer cur = controller.getLockedY();
        String curStr = "Current: " + (cur == null ? "auto (from aim)" : "Y=" + cur);
        drawCenteredString(fontRendererObj, curStr, cx, cy - 12, 0xAAAAAA);
        if (!status.isEmpty()) drawCenteredString(fontRendererObj, status, cx, cy + 55, 0x55FF55);

        super.drawScreen(mx, my, partial);
    }
}
