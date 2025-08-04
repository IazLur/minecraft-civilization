package TestJava.testjava.enums;

import org.bukkit.ChatColor;

public enum SocialClass {
    MISERABLE(0, "Misérable", ChatColor.YELLOW, "{0}"),
    INACTIVE(1, "Inactive", ChatColor.GRAY, "{1}"),
    OUVRIERE(2, "Ouvrière", ChatColor.BLUE, "{2}"),
    MOYENNE(3, "Moyenne", ChatColor.GREEN, "{3}"),
    BOURGEOISIE(4, "Bourgeoisie", ChatColor.GOLD, "{4}");

    private final int level;
    private final String name;
    private final ChatColor color;
    private final String tag;

    SocialClass(int level, String name, ChatColor color, String tag) {
        this.level = level;
        this.name = name;
        this.color = color;
        this.tag = tag;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getTag() {
        return tag;
    }

    public String getColoredTag() {
        return color + tag + ChatColor.RESET;
    }

    public static SocialClass fromLevel(int level) {
        for (SocialClass socialClass : values()) {
            if (socialClass.level == level) {
                return socialClass;
            }
        }
        return MISERABLE; // Valeur par défaut
    }

    /**
     * Vérifie si cette classe sociale peut avoir un métier
     */
    public boolean canHaveJob() {
        return this.level >= 1; // Classe 1 et plus peuvent avoir un métier
    }

    /**
     * Vérifie si cette classe sociale doit perdre son métier
     */
    public boolean shouldLoseJob() {
        return this.level == 0; // Seule la classe 0 ne peut pas avoir de métier
    }
}