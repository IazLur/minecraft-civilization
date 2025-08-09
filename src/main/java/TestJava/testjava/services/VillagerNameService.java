package TestJava.testjava.services;

import TestJava.testjava.enums.SocialClass;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillagerModel;
import org.bukkit.ChatColor;
import org.bukkit.entity.Villager;

import java.util.Locale;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class VillagerNameService {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOCIAL_CLASS_TAG_PATTERN = Pattern.compile("(\\[\\d\\]|\\{\\d\\})\\s*");
    private static final Pattern BRACKET_PREFIX_PATTERN = Pattern.compile("^(\\s*\\[[^\\]]+\\]\\s*)+");
    private static final Pattern WEALTH_SUFFIX_PATTERN = Pattern.compile("\\s*\\d+(?:[\\.,]\\d+)?µ\\s*$");

    public static String buildDisplayName(VillagerModel model, Villager entity, String fallbackFullName) {
        String village = model.getVillageName() != null ? model.getVillageName() : "?";
        String jobOrClass = computeJobOrClass(model, entity);
        String baseName = extractOrGenerateBaseName(entity, fallbackFullName);
        String person = formatPersonName(baseName);
        String wealth = formatWealth(model.getRichesse());

        return ChatColor.BLUE + "[" + village + "] "
             + ChatColor.YELLOW + "[" + jobOrClass + "] "
             + ChatColor.WHITE + person + " "
             + ChatColor.GREEN + wealth;
    }

    private static String computeJobOrClass(VillagerModel model, Villager entity) {
        // Custom job takes precedence
        if (model != null && model.hasCustomJob() && model.getCurrentJobName() != null && !model.getCurrentJobName().isBlank()) {
            return model.getCurrentJobName();
        }

        // Native job with level
        if (entity != null && entity.getProfession() != null
                && entity.getProfession() != Villager.Profession.NONE
                && entity.getProfession() != Villager.Profession.NITWIT) {
            String prof = entity.getProfession().toString().toLowerCase(Locale.ROOT);
            int level = entity.getVillagerLevel();
            return prof + " (niv " + level + ")";
        }

        // Fallback to social class label
        SocialClass sc = model != null ? model.getSocialClassEnum() : SocialClass.MISERABLE;
        return sc.getName();
    }

    private static String extractOrGenerateBaseName(Villager entity, String fallbackFullName) {
        // Try extracting existing base name from current custom name
        if (entity != null && entity.getCustomName() != null && !entity.getCustomName().isBlank()) {
            String extracted = extractBaseFullName(entity.getCustomName());
            if (extracted != null && !extracted.isBlank()) {
                return extracted.trim();
            }
        }
        // Use provided fallback
        if (fallbackFullName != null && !fallbackFullName.isBlank()) {
            return fallbackFullName.trim();
        }
        // Generate a new one
        return CustomName.generate();
    }

    private static String extractBaseFullName(String fullDisplayName) {
        if (fullDisplayName == null) return null;
        String name = fullDisplayName;
        // Strip color codes
        name = ChatColor.stripColor(name);
        name = COLOR_CODE_PATTERN.matcher(name).replaceAll("");
        // Remove social class legacy tags and any leading [ ... ] groups
        name = SOCIAL_CLASS_TAG_PATTERN.matcher(name).replaceAll("").trim();
        name = BRACKET_PREFIX_PATTERN.matcher(name).replaceFirst("").trim();
        // Remove trailing wealth (e.g., 123.4µ)
        name = WEALTH_SUFFIX_PATTERN.matcher(name).replaceFirst("").trim();
        return name;
    }

    private static String formatPersonName(String baseFullName) {
        if (baseFullName == null || baseFullName.isBlank()) {
            return "?";
        }
        String trimmed = baseFullName.trim();
        // If already in initial form like "J. Martin", keep as is
        if (trimmed.matches("^[A-Za-zÀ-ÖØ-öø-ÿ]\\.\\s.+")) {
            return trimmed;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0];
            String last = parts[parts.length - 1];
            String initial = first.substring(0, 1).toUpperCase(Locale.ROOT);
            return initial + ". " + last;
        }
        // Single token fallback
        return trimmed;
    }

    private static String formatWealth(Float richesse) {
        float value = richesse != null ? richesse : 0.0f;
        // One decimal like examples (e.g., 334.6µ)
        return String.format(Locale.US, "%.1fµ", value);
    }
}
