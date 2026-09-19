package it.randomuccello.ghirlande;

public enum GarlandColor {
    RED("red"),
    YELLOW("yellow"),
    BLUE("blue"),
    WHITE("white"),
    ORANGE("orange"),
    PINK("pink"),
    MAGENTA("magenta"),
    LIGHT_BLUE("light_blue"),
    LIGHT_GRAY("light_gray"),
    CYAN("cyan"),
    BLACK("black"),
    GRAY("gray"),
    MIXED("mixed");

    private final String id;

    GarlandColor(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static GarlandColor byId(String id) {
        for (GarlandColor value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return MIXED;
    }

    public int initialCharges() {
        return switch (this) {
            case PINK -> 10;
            case GRAY -> 20;
            default -> -1;
        };
    }
}
