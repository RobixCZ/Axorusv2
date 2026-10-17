package cz.axorus;

import java.util.List;

public class Modules {
    public static final Module TARGET_HUD = new Module("Target HUD", "Visual", true)
            .add(new Setting("Duration", 1, "3s", "6s", "10s", "15s"))
            .add(new Setting("Opacity", 2, "25%", "50%", "75%", "100%"))
            .add(new Setting("Position", 0, "Right", "Left"));
    public static final Module SHOW_SKIN = new Module("Skin",    "Visual", true);
    public static final Module SHOW_HP   = new Module("HP",      "Visual", true);
    public static final Module SHOW_TIER = new Module("MCTiers", "Visual", true);
    public static final Module SHOW_GEAR = new Module("Gear",    "Visual", true);

    public static final List<Module> ALL = List.of(TARGET_HUD, SHOW_SKIN, SHOW_HP, SHOW_TIER, SHOW_GEAR);
}
