package com.example.wallfix;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** Module: settings popup (key P). Sets the locked repair Y-level and the blocks-per-second rate. Writes RepairController / Settings only. */
public final class PatchGui extends GuiScreen {
    private final RepairController controller;
    private GuiTextField yField, rateField;
    private String status = "";

    private static final int BTN_SET_Y = 0, BTN_AUTO = 1, BTN_CLOSE = 2, BTN_MINUS = 3, BTN_PLUS = 4, BTN_SET_RATE = 5;

    public PatchGui(RepairController controller) { this.controller = controller; }

    // vertical layout, offsets from screen centre (cy):
    //  -100 title | -80 Y label | -70 Y field | -44 Set Y/Auto | -18 current | +2 rate label
    //  +12 [-] rate field [+] | +38 Set Blocks/Second | +66 Close | +94 status
    @Override
    public void initGui() {
        int cx = width / 2, cy = height / 2;

        yField = new GuiTextField(0, fontRendererObj, cx - 60, cy - 70, 120, 20);
        yField.setMaxStringLength(3);
        yField.setText(controller.getLockedY() == null ? "" : String.valueOf(controller.getLockedY()));
        yField.setFocused(true);

        rateField = new GuiTextField(1, fontRendererObj, cx - 76, cy + 12, 152, 20);
        rateField.setMaxStringLength(5);
        rateField.setText(RepairController.fmt(controller.getPlacementsPerSecond()));
        rateField.setFocused(false);

        buttonList.clear();
        buttonList.add(new GuiButton(BTN_SET_Y, cx - 100, cy - 44, 95, 20, "Set Y"));
        buttonList.add(new GuiButton(BTN_AUTO, cx + 5, cy - 44, 95, 20, "Auto"));
        buttonList.add(new GuiButton(BTN_MINUS, cx - 100, cy + 12, 20, 20, "-"));
        buttonList.add(new GuiButton(BTN_PLUS, cx + 80, cy + 12, 20, 20, "+"));
        buttonList.add(new GuiButton(BTN_SET_RATE, cx - 100, cy + 38, 200, 20, "Set Blocks / Second"));
        buttonList.add(new GuiButton(BTN_CLOSE, cx - 40, cy + 66, 80, 20, "Close"));
    }

    @Override
    public boolean doesGuiPauseGame() { return false; } // don't freeze a multiplayer session

    @Override
    protected void actionPerformed(GuiButton b) {
        if (b.id == BTN_SET_Y) trySetY();
        else if (b.id == BTN_AUTO) { controller.setLockedY(null); status = "Y set to auto."; }
        else if (b.id == BTN_MINUS) adjustRate(-1);
        else if (b.id == BTN_PLUS) adjustRate(+1);
        else if (b.id == BTN_SET_RATE) trySetRate();
        else if (b.id == BTN_CLOSE) mc.displayGuiScreen(null);
    }

    private void trySetY() {
        String t = yField.getText().trim();
        if (t.isEmpty()) { status = "Enter a Y between 0 and 255."; return; }
        try {
            int y = Integer.parseInt(t);
            if (y < 0 || y > 255) { status = "Y must be 0-255."; return; }
            controller.setLockedY(y);
            status = "Locked to Y=" + y;
        } catch (NumberFormatException e) {
            status = "Y is not a number.";
        }
    }

    private void trySetRate() {
        try {
            double r = Double.parseDouble(rateField.getText().trim());
            if (Double.isNaN(r) || r < Config.MIN_RATE || r > Config.MAX_RATE) {
                status = "Rate must be " + RepairController.fmt(Config.MIN_RATE) + " to " + RepairController.fmt(Config.MAX_RATE) + ".";
                return;
            }
            applyRate(r);
        } catch (NumberFormatException e) {
            status = "Rate is not a number.";
        }
    }

    private void adjustRate(int delta) {
        double r = controller.getPlacementsPerSecond() + delta;
        r = Math.max(Config.MIN_RATE, Math.min(Config.MAX_RATE, r));
        applyRate(r);
    }

    private void applyRate(double r) {
        controller.setPlacementsPerSecond(r);
        Settings.saveRate(controller.getPlacementsPerSecond());
        rateField.setText(RepairController.fmt(controller.getPlacementsPerSecond()));
        status = "Placing up to " + RepairController.fmt(controller.getPlacementsPerSecond()) + " blocks per second.";
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        if (key == Keyboard.KEY_TAB) {                       // switch between the two fields
            boolean toRate = yField.isFocused();
            yField.setFocused(!toRate);
            rateField.setFocused(toRate);
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            if (rateField.isFocused()) trySetRate(); else trySetY();
            return;
        }
        if (yField.isFocused()) yField.textboxKeyTyped(c, key);
        else if (rateField.isFocused()) rateField.textboxKeyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws java.io.IOException {
        super.mouseClicked(mx, my, btn);
        yField.mouseClicked(mx, my, btn);
        rateField.mouseClicked(mx, my, btn);
    }

    @Override
    public void updateScreen() { yField.updateCursorCounter(); rateField.updateCursorCounter(); }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        int cx = width / 2, cy = height / 2;
        drawCenteredString(fontRendererObj, "WallFix Settings", cx, cy - 100, 0xFFFFFF);

        drawCenteredString(fontRendererObj, "Repair Y-level (0-255)", cx, cy - 80, 0xCCCCCC);
        yField.drawTextBox();
        Integer cur = controller.getLockedY();
        drawCenteredString(fontRendererObj, "Current: " + (cur == null ? "auto (from aim)" : "Y=" + cur), cx, cy - 18, 0x888888);

        drawCenteredString(fontRendererObj, "Blocks per second (" + RepairController.fmt(Config.MIN_RATE) + " - " + RepairController.fmt(Config.MAX_RATE) + ")  lower = slower", cx, cy + 2, 0xCCCCCC);
        rateField.drawTextBox();

        if (!status.isEmpty()) drawCenteredString(fontRendererObj, status, cx, cy + 94, 0x55FF55);
        super.drawScreen(mx, my, partial);
    }
}
