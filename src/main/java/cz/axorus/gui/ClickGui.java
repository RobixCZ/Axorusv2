package cz.axorus.gui;

import cz.axorus.*;
import cz.axorus.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ClickGui extends Screen {
    private static class Panel {
        String title; int x, y; final int w;
        List<Module> modules = new ArrayList<>();
        Panel(String t, int x, int y, int w) { title = t; this.x = x; this.y = y; this.w = w; }
    }

    private static final List<Panel> PANELS = new ArrayList<>();
    private static int themePage = 0;
    private static final int PER_PAGE = 9;

    private boolean lmbDown, rmbDown;
    private final boolean[] prevKeys = new boolean[350];
    private Panel dragging; private int dragDX, dragDY;
    private Module listening = null;
    private boolean clicked, rclicked;

    public ClickGui() {
        super(Text.literal("Axorus"));
        if (PANELS.isEmpty()) {
            Panel visual = new Panel("Visual", 20, 20, 120);
            for (Module m : Modules.ALL) visual.modules.add(m);
            PANELS.add(visual);
            PANELS.add(new Panel("Configs", 150, 20, 130));
            PANELS.add(new Panel("Menu", 290, 20, 120));
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return listening == null; }
    @Override public void removed() { ConfigStore.save(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        clicked = l && !lmbDown; rclicked = r && !rmbDown;
        lmbDown = l; rmbDown = r;
        if (!l) dragging = null;

        pollKeybind(win);

        Themes.Theme th = Themes.get();
        ctx.fill(0, 0, width, height, 0x77000000);
        if (dragging != null) { dragging.x = mx - dragDX; dragging.y = my - dragDY; }

        for (Panel p : PANELS) {
            ctx.fill(p.x, p.y, p.x + p.w, p.y + 16, th.header);
            ctx.fill(p.x, p.y + 15, p.x + p.w, p.y + 16, th.accent);
            ctx.drawText(textRenderer, p.title, p.x + 6, p.y + 4, 0xFFFFFFFF, true);
            if (clicked && inside(mx, my, p.x, p.y, p.w, 16)) {
                dragging = p; dragDX = mx - p.x; dragDY = my - p.y; clicked = false;
            }
            int cy = p.y + 16;
            int end = switch (p.title) {
                case "Configs" -> drawConfigs(ctx, p, cy, mx, my, th);
                case "Menu" -> drawMenu(ctx, p, cy, th);
                default -> drawModules(ctx, p, cy, mx, my, th);
            };
            if (th.glow) Glow.box(ctx, p.x, p.y, p.x + p.w, end, th.accent);
        }

        ctx.drawText(textRenderer, "AXORUS", 8, 6, th.accent, true);

        if (listening != null) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "Stiskni klávesu pro " + listening.name + "  (ESC = zrušit, Backspace = smazat)",
                    width / 2, height - 20, 0xFFFFFFFF);
        }
    }

    // ---------- moduly ----------
    private int drawModules(DrawContext ctx, Panel p, int cy, int mx, int my, Themes.Theme th) {
        for (Module m : p.modules) {
            boolean hov = inside(mx, my, p.x, cy, p.w, 16);
            int bg = m.enabled ? (th.accent & 0xFFFFFF) | 0xB0000000 : (hov ? lighten(th.bg) : th.bg);
            ctx.fill(p.x, cy, p.x + p.w, cy + 16, bg);
            ctx.drawText(textRenderer, m.name, p.x + 6, cy + 4, m.enabled ? 0xFFFFFFFF : 0xFFAAAAAA, true);
            String tag = m.key >= 0 ? "[" + keyName(m.key) + "]" : (m.expanded ? "-" : "+");
            ctx.drawText(textRenderer, tag, p.x + p.w - 6 - textRenderer.getWidth(tag), cy + 4, 0xFF888888, true);
            if (hov && clicked) { m.enabled = !m.enabled; clicked = false; }
            if (hov && rclicked) { m.expanded = !m.expanded; rclicked = false; }
            cy += 16;

            if (m.expanded) {
                if (row(ctx, p, cy, "Keybind", listening == m ? "..." : (m.key < 0 ? "None" : keyName(m.key)), mx, my, th)) {
                    listening = (listening == m) ? null : m;
                    System.arraycopy(new boolean[350], 0, prevKeys, 0, 350);
                    snapshotKeys();
                }
                cy += 14;
                if (row(ctx, p, cy, "Mode", m.hold ? "Hold" : "Toggle", mx, my, th)) m.hold = !m.hold;
                cy += 14;
                for (Setting s : m.settings) {
                    if (row(ctx, p, cy, s.name, s.get(), mx, my, th)) s.cycle();
                    cy += 14;
                }
            }
        }
        return cy;
    }

    private boolean row(DrawContext ctx, Panel p, int cy, String label, String value, int mx, int my, Themes.Theme th) {
        boolean hov = inside(mx, my, p.x, cy, p.w, 14);
        ctx.fill(p.x, cy, p.x + p.w, cy + 14, hov ? lighten(th.bg) : darken(th.bg));
        ctx.fill(p.x, cy, p.x + 2, cy + 14, th.accent);
        ctx.drawText(textRenderer, label, p.x + 8, cy + 3, 0xFFBBBBBB, true);
        ctx.drawText(textRenderer, value, p.x + p.w - 6 - textRenderer.getWidth(value), cy + 3, th.accent, true);
        if (hov && clicked) { clicked = false; return true; }
        return false;
    }

    // ---------- configy / témata ----------
    private int drawConfigs(DrawContext ctx, Panel p, int cy, int mx, int my, Themes.Theme th) {
        int pages = (Themes.ALL.length + PER_PAGE - 1) / PER_PAGE;
        int start = themePage * PER_PAGE;
        for (int i = start; i < Math.min(Themes.ALL.length, start + PER_PAGE); i++) {
            Themes.Theme t = Themes.ALL[i];
            boolean hov = inside(mx, my, p.x, cy, p.w, 16);
            boolean sel = i == Themes.current;
            ctx.fill(p.x, cy, p.x + p.w, cy + 16, hov ? lighten(th.bg) : th.bg);
            ctx.fill(p.x + 5, cy + 3, p.x + 15, cy + 13, t.accent);
            ctx.fill(p.x + 15, cy + 3, p.x + 25, cy + 13, t.bg);
            ctx.drawText(textRenderer, t.name, p.x + 30, cy + 4, sel ? 0xFFFFFFFF : 0xFFAAAAAA, true);
            if (sel) ctx.fill(p.x, cy, p.x + 2, cy + 16, th.accent);
            if (hov && clicked) { Themes.current = i; clicked = false; }
            cy += 16;
        }
        // stránkování
        ctx.fill(p.x, cy, p.x + p.w, cy + 16, th.header);
        boolean hp = inside(mx, my, p.x, cy, p.w / 2, 16), hn = inside(mx, my, p.x + p.w / 2, cy, p.w / 2, 16);
        ctx.drawText(textRenderer, "< Prev", p.x + 6, cy + 4, hp ? 0xFFFFFFFF : 0xFF999999, true);
        String nx = "Next >";
        ctx.drawText(textRenderer, nx, p.x + p.w - 6 - textRenderer.getWidth(nx), cy + 4, hn ? 0xFFFFFFFF : 0xFF999999, true);
        String pg = (themePage + 1) + "/" + pages;
        ctx.drawText(textRenderer, pg, p.x + (p.w - textRenderer.getWidth(pg)) / 2, cy + 4, th.accent, true);
        if (clicked && hp) { themePage = (themePage - 1 + pages) % pages; clicked = false; }
        if (clicked && hn) { themePage = (themePage + 1) % pages; clicked = false; }
        return cy + 16;
    }

    private int drawMenu(DrawContext ctx, Panel p, int cy, Themes.Theme th) {
        ctx.fill(p.x, cy, p.x + p.w, cy + 50, th.bg);
        ctx.drawText(textRenderer, "Menu Key: R-Shift", p.x + 6, cy + 5, 0xFFAAAAAA, true);
        ctx.drawText(textRenderer, "Theme: " + th.name, p.x + 6, cy + 18, th.accent, true);
        ctx.drawText(textRenderer, "RMB na modul = config", p.x + 6, cy + 31, 0xFF777777, true);
        return cy + 50;
    }

    // ---------- keybind ----------
    private void snapshotKeys() {
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        for (int k = 32; k < 349; k++) prevKeys[k] = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
    }

    private void pollKeybind(long win) {
        if (listening == null) return;
        for (int k = 32; k < 349; k++) {
            boolean down = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
            if (down && !prevKeys[k]) {
                if (k == GLFW.GLFW_KEY_RIGHT_SHIFT) { prevKeys[k] = true; continue; } // rezervováno pro GUI
                if (k == GLFW.GLFW_KEY_ESCAPE) { listening = null; }
                else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) { listening.key = -1; listening = null; }
                else { listening.key = k; listening = null; }
                prevKeys[k] = true;
                break;
            }
            prevKeys[k] = down;
        }
    }

    static String keyName(int k) {
        switch (k) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RAlt";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            default:
        }
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(k, 0);
        return n != null ? n.toUpperCase() : "K" + k;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    private static int lighten(int c) { return add(c, 14); }
    private static int darken(int c) { return add(c, -3); }
    private static int add(int c, int d) {
        int r = Math.max(0, Math.min(255, ((c >> 16) & 255) + d));
        int g = Math.max(0, Math.min(255, ((c >> 8) & 255) + d));
        int b = Math.max(0, Math.min(255, (c & 255) + d));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
