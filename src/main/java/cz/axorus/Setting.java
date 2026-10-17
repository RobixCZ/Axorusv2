package cz.axorus;

/** Nastavení modulu, které se přepíná klikem mezi možnostmi. */
public class Setting {
    public final String name;
    public final String[] options;
    public int index;

    public Setting(String name, int defaultIndex, String... options) {
        this.name = name;
        this.options = options;
        this.index = defaultIndex;
    }

    public String get() { return options[index]; }
    public void cycle() { index = (index + 1) % options.length; }
}
