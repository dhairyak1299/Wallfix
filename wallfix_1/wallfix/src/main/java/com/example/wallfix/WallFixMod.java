package com.example.wallfix;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = WallFixMod.MODID, version = "1.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class WallFixMod {
    public static final String MODID = "wallfix";
    private RepairController controller;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        KeyBinding toggleX = new KeyBinding("Toggle WallFix (X)", Keyboard.KEY_X, "WallFix");
        KeyBinding toggleF = new KeyBinding("Toggle WallFix (F)", Keyboard.KEY_F, "WallFix");
        KeyBinding guiKey  = new KeyBinding("Open WallFix Y GUI", Keyboard.KEY_P, "WallFix");
        ClientRegistry.registerKeyBinding(toggleX);
        ClientRegistry.registerKeyBinding(toggleF);
        ClientRegistry.registerKeyBinding(guiKey);
        controller = new RepairController(toggleX, toggleF);
        ClientCommandHandler.instance.registerCommand(new PatchCommand(controller));
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HudOverlay(controller));
        MinecraftForge.EVENT_BUS.register(new GuiKeyHandler(guiKey, controller));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) controller.tick();
    }
}
