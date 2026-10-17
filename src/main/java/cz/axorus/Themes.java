package cz.axorus;

/** 45 barevných témat: název | pozadí | akcent. */
public class Themes {
    public static class Theme {
        public final String name; public final int bg, header, accent; public final boolean glow;
        Theme(String n, int bg, int accent) {
            this.name = n; this.glow = n.equals("Axorus"); this.accent = 0xFF000000 | accent;
            this.bg = 0xFF000000 | bg;
            int r = Math.max(0, ((bg >> 16) & 255) - 4), g = Math.max(0, ((bg >> 8) & 255) - 4), b = Math.max(0, (bg & 255) - 4);
            this.header = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    private static final String[] RAW = {
        "Axorus|0A0A0A|FF1E2D", "Matte Violet-Blue|0E0E14|6C7BFF", "Rose|160F13|FF6F9C", "Aqua|0B1416|3DE8E0",
        "Ocean|0A1220|3B82F6", "Sunset|180F0C|FF7A45", "Crimson|150C0E|E03A3E",
        "Emerald|0A1510|2ECC71", "Lavender|120F18|B79CFF", "Mint|0C1512|7FFFC4",
        "Gold|15120A|FFC93C", "Cherry|160A10|FF2E63", "Ice|0D1319|9FD8FF",
        "Neon Green|0A120A|39FF14", "Neon Pink|160A14|FF2FD0", "Purple Haze|120A1A|9B30FF",
        "Peach|171110|FFB38A", "Slate|11141A|7A8CA5", "Cyberpunk|0D0A14|FCEE0A",
        "Forest|0B130C|4CAF50", "Coral|160E0E|FF7F6B", "Sky|0B1118|5CC8FF",
        "Grape|130A17|A64DFF", "Teal|0A1414|14B8A6", "Magenta|150A12|E040FB",
        "Lime|0F1408|A3E635", "Amber|161008|FFB000", "Indigo|0C0C18|5C6BFF",
        "Blush|171012|FFA6C1", "Steel|101216|8FA3B8", "Lava|160A08|FF4500",
        "Orchid|140E16|DA70D6", "Turquoise|0A1415|40E0D0", "Berry|140A12|C2185B",
        "Mocha|140F0C|C69C6D", "Arctic|0C1216|B8F2FF", "Royal|0A0C18|4169E1",
        "Toxic|0C130A|9CFF00", "Blood|120808|B3001B", "Cotton Candy|131018|FFB7F5",
        "Dusk|110E18|8E7CC3", "Spring|0E1410|A8E6A1", "Plasma|0F0A18|7B2FFF",
        "Sakura|161013|FFC0D9", "Ember|140C08|FF8C1A"
    };

    public static final Theme[] ALL = new Theme[RAW.length];
    public static int current = 0;

    static {
        for (int i = 0; i < RAW.length; i++) {
            String[] p = RAW[i].split("\\|");
            ALL[i] = new Theme(p[0], Integer.parseInt(p[1], 16), Integer.parseInt(p[2], 16));
        }
    }

    public static Theme get() { return ALL[Math.max(0, Math.min(ALL.length - 1, current))]; }
}
