package cz.axorus;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public final String name;
    public final String category;
    public boolean enabled;
    public int key = -1;          // GLFW keycode, -1 = nic
    public boolean hold = false;  // false = Toggle, true = Hold
    public boolean expanded = false;
    public boolean keyDown = false;
    public final List<Setting> settings = new ArrayList<>();

    public Module(String name, String category, boolean enabled) {
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public Module add(Setting s) { settings.add(s); return this; }

    public Setting setting(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s;
        return null;
    }
}
