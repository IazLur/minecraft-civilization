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

    public static Collection<CustomEntity> getAll() {
        Collection<LivingEntity> entities = CustomName.filter(TestJava.world.getLivingEntities());
        Collection<CustomEntity> cEntities = new ArrayList<>();
        for (LivingEntity entity : entities) {
            cEntities.add(new CustomEntity(entity));
        }
        return cEntities;
    }

    public static Collection<CustomEntity> whereVillage(String oldId) {
        Collection<CustomEntity> entities = CustomName.getAll();
        entities.removeIf(entity ->
                !CustomName.squareBrackets(entity.getEntity().getCustomName(), 0).equals(oldId));
        return entities;
    }

    public static String generate() {
        String fullname = "";

        while (fullname.equals("")) {
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
