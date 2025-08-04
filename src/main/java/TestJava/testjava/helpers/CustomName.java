package TestJava.testjava.helpers;

import TestJava.testjava.TestJava;
import TestJava.testjava.classes.CustomEntity;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomName {
    @Nonnull
    public static Collection<LivingEntity> filter(@Nonnull Collection<LivingEntity> base) {
        Collection<LivingEntity> selection = new ArrayList<>();
        for (LivingEntity entity : base) {
            if (entity.isCustomNameVisible()) {
                selection.add(entity);
            }
        }
        return selection;
    }

    @Nonnull
    public static String squareBrackets(@Nonnull String name, @Nonnull Integer index) {
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(name);
        ArrayList<String> arr = new ArrayList<>();

        while (matcher.find()) {
            arr.add(matcher.group(1));
        }

        return arr.get(index);
    }

    /**
     * Extrait le nom du village d'un customName en utilisant la convention robuste
     * avec classes sociales dans des accolades {0} et villages dans des crochets [Village]
     * 
     * Formats supportés :
     * - Standard : "[VillageName] Prénom Nom"
     * - Avec classe sociale : "{0} [VillageName] Prénom Nom" 
     * - Avec couleurs : "§e{0}§r [VillageName] Prénom Nom"
     * - Ancien format (migration) : "[0][VillageName] Prénom Nom"
     */
    @Nonnull
    public static String extractVillageName(@Nonnull String customName) {
        if (customName == null || customName.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom personnalisé ne peut pas être null ou vide");
        }
        
        // Supprime les codes de couleur pour l'analyse
        String cleanName = org.bukkit.ChatColor.stripColor(customName);
        
        // Nouveau système robuste : Le village est TOUJOURS le premier élément entre crochets []
        // Les classes sociales sont dans des accolades {} et ne posent plus de problème
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(cleanName);
        
        if (matcher.find()) {
            String villageName = matcher.group(1);
            
            // Vérification de sécurité : s'assurer que ce n'est pas un ancien tag classe sociale
            if (villageName.matches("^[0-4]$")) {
                // Ancien format détecté, chercher le second élément
                if (matcher.find()) {
                    return matcher.group(1);
                } else {
                    throw new IllegalArgumentException("Format ancien détecté mais village manquant dans: " + customName);
                }
            }
            
            return villageName;
        }
        
        throw new IllegalArgumentException("Aucun village trouvé entre crochets dans: " + customName);
    }

    public static Collection<CustomEntity> getAll() {
        Collection<CustomEntity> cEntities = new ArrayList<>();
        
        // Vérification de sécurité pour le monde
        if (TestJava.world == null) {
            TestJava.plugin.getLogger().warning("TestJava.world est null dans CustomName.getAll()");
            return cEntities; // Retourner une collection vide
        }
        
        try {
            Collection<LivingEntity> entities = CustomName.filter(TestJava.world.getLivingEntities());
            for (LivingEntity entity : entities) {
                cEntities.add(new CustomEntity(entity));
            }
        } catch (Exception e) {
            TestJava.plugin.getLogger().warning("Erreur lors de la récupération des entités : " + e.getMessage());
        }
        return cEntities;
    }

    public static Collection<CustomEntity> whereVillage(String oldId) {
        Collection<CustomEntity> entities = CustomName.getAll();
        entities.removeIf(entity -> {
            try {
                return !CustomName.extractVillageName(entity.getEntity().getCustomName()).equals(oldId);
            } catch (Exception e) {
                // En cas d'erreur d'extraction, on considère que l'entité ne correspond pas
                TestJava.plugin.getLogger().warning("Erreur extraction village pour " + 
                    entity.getEntity().getCustomName() + ": " + e.getMessage());
                return true; // Retirer l'entité de la liste
            }
        });
        return entities;
    }

    public static String generate() {
        String fullname = "";

        while (fullname.equals("") || fullname.equals(" ")) {
            URL url = null;
            try {
                url = new URL("https://randomuser.me/api/");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            assert url != null;
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                assert con != null;
                con.setRequestMethod("GET");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }

            BufferedReader in = null;
            try {
                in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine = null;
            StringBuilder content = new StringBuilder();
            while (true) {
                try {
                    assert in != null;
                    if ((inputLine = in.readLine()) == null) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                content.append(inputLine);
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Pattern pattern = Pattern.compile("\"first\":\"(.*?)\"");
            Matcher matcher = pattern.matcher(content);
            matcher.find();
            String firstName = matcher.group(1);

            pattern = Pattern.compile("\"last\":\"(.*?)\"");
            matcher = pattern.matcher(content);
            matcher.find();
            String lastName = matcher.group(1);
            fullname = firstName.replaceAll("[^\\x20-\\x7e]", "") + " " + lastName.replaceAll("[^\\x20-\\x7e]", "");
        }

        return fullname;
    }
}
