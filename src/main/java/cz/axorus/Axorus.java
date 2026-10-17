package cz.axorus;

import cz.axorus.gui.ClickGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class Axorus implements ClientModInitializer {
    private boolean guiKeyDown = false;

    @Override
    public void onInitializeClient() {
        ConfigStore.load();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.getWindow() == null) return;
            long win = mc.getWindow().getHandle();

            // Right Shift -> Click GUI
            boolean down = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            if (down && !guiKeyDown) {
                if (mc.currentScreen instanceof ClickGui) mc.setScreen(null);
                else if (mc.currentScreen == null) mc.setScreen(new ClickGui());
            }
            guiKeyDown = down;

            // Keybindy modulů (jen když není otevřená žádná obrazovka)
            for (Module m : Modules.ALL) {
                if (m.key < 0) continue;
                boolean pressed = mc.currentScreen == null
                        && GLFW.glfwGetKey(win, m.key) == GLFW.GLFW_PRESS;
                if (m.hold) {
                    if (mc.currentScreen == null) m.enabled = pressed;
                } else if (pressed && !m.keyDown) {
                    m.enabled = !m.enabled;
                }
                m.keyDown = pressed;
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof PlayerEntity target && target != player) {
                TargetInfo.setTarget(target);
            }
            return ActionResult.PASS;
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.of("axorus", "target_hud"),
                (ctx, tickCounter) -> TargetInfo.render(ctx, MinecraftClient.getInstance()));
    }
}
