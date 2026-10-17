package cz.axorus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;

public class TargetInfo {
    private static PlayerEntity target;
    private static long lastHit;

    public static void setTarget(PlayerEntity p) {
        target = p;
        lastHit = System.currentTimeMillis();
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!Modules.TARGET_HUD.enabled || target == null || mc.player == null) return;
        long showMs = Long.parseLong(Modules.TARGET_HUD.setting("Duration").get().replace("s", "")) * 1000L;
        if (System.currentTimeMillis() - lastHit > showMs || target.isRemoved()) { target = null; return; }

        TextRenderer font = mc.textRenderer;
        int w = 175, h = 78;
        boolean right = Modules.TARGET_HUD.setting("Position").get().equals("Right");
        int x = right ? mc.getWindow().getScaledWidth() / 2 + 20 : mc.getWindow().getScaledWidth() / 2 - 20 - w;
        int y = mc.getWindow().getScaledHeight() / 2 - 30;

        int[] alphas = {0x40, 0x80, 0xBF, 0xFF};
        int alpha = alphas[Modules.TARGET_HUD.setting("Opacity").index];
        Themes.Theme th = Themes.get();
        ctx.fill(x, y, x + w, y + h, (alpha << 24) | (th.bg & 0xFFFFFF));
        ctx.fill(x, y, x + w, y + 2, th.accent);
        if (th.glow) Glow.box(ctx, x, y, x + w, y + h, th.accent);

        int tx = x + 6;
        if (Modules.SHOW_SKIN.enabled) {
            PlayerListEntry entry = mc.getNetworkHandler() == null ? null
                    : mc.getNetworkHandler().getPlayerListEntry(target.getUuid());
            if (entry != null) PlayerSkinDrawer.draw(ctx, entry.getSkinTextures(), x + 6, y + 8, 32);
            tx = x + 44;
        }

        ctx.drawText(font, target.getName().getString(), tx, y + 7, 0xFFFFFFFF, true);

        if (Modules.SHOW_HP.enabled) {
            float hp = target.getHealth() + target.getAbsorptionAmount();
            float max = Math.max(1f, target.getMaxHealth());
            ctx.drawText(font, String.format("HP: %.1f / %.0f", hp, max), tx, y + 19, 0xFFFF5555, true);
            int bw = w - (tx - x) - 6;
            ctx.fill(tx, y + 31, tx + bw, y + 35, 0xFF333340);
            ctx.fill(tx, y + 31, tx + (int) (bw * Math.min(1f, hp / max)), y + 35, 0xFF55FF55);
        }

        if (Modules.SHOW_TIER.enabled) {
            ctx.drawText(font, "Tier: " + TierFetcher.get(target.getName().getString()), tx, y + 40, 0xFFFFD35A, true);
        }

        if (Modules.SHOW_GEAR.enabled) {
            // Klient zná jen to, co server pošle: ruce + brnění (ne celý inventář).
            ItemStack[] items = {
                    target.getEquippedStack(EquipmentSlot.HEAD),
                    target.getEquippedStack(EquipmentSlot.CHEST),
                    target.getEquippedStack(EquipmentSlot.LEGS),
                    target.getEquippedStack(EquipmentSlot.FEET),
                    target.getMainHandStack(),
                    target.getOffHandStack()
            };
            int ix = x + 6, iy = y + 56;
            for (ItemStack s : items) {
                if (!s.isEmpty()) {
                    ctx.drawItem(s, ix, iy);
                    ctx.drawStackOverlay(font, s, ix, iy);
                }
                ix += 18;
            }
            boolean totem = target.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                    || target.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
            boolean pot = target.getMainHandStack().getItem() instanceof PotionItem
                    || target.getOffHandStack().getItem() instanceof PotionItem;
            ctx.drawText(font, (totem ? "Totem " : "") + (pot ? "Pot" : ""), ix + 4, iy + 5, 0xFFAAAAAA, true);
        }
    }
}
