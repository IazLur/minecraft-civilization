package TestJava.testjava.threads;

import TestJava.testjava.classes.CustomEntity;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;

import java.util.Collection;
import java.util.Objects;

public class DefenderThread implements Runnable {
    @Override
    public void run() {
        Collection<CustomEntity> entities = CustomName.getAll();
        entities = entities.stream().filter(entity -> entity.getEntity() instanceof Skeleton).toList();
        entities.forEach(entity -> {
            Collection<Entity> targets = entity.getEntity().getNearbyEntities(10D, 10D, 10D);
            targets.forEach(target -> {
                if(!target.isCustomNameVisible() && !(target instanceof Player)) {
                    return;
                }
                String targetVillage = CustomName.squareBrackets(target.getCustomName(), 0);
                String entityVillage = CustomName.squareBrackets(entity.getEntity().getCustomName(), 0);
                if(!targetVillage.equals(entityVillage)) {
                    if(target instanceof Player) {
                        VillageModel village = VillageRepository.get(entityVillage);
                        if(Objects.equals(village.getPlayerName(), ((Player) target).getDisplayName())) {
                            return;
                        }
                    }
                    entity.getEntity().attack(target);
                }
            });
        });
    }
}
