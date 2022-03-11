package TestJava.testjava.threads;

import TestJava.testjava.classes.CustomEntity;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.entity.*;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefenderThread implements Runnable {
    @Override
    public void run() {
        Collection<CustomEntity> entities = CustomName.getAll();
        entities = entities.stream().filter(entity -> entity.getEntity() instanceof Skeleton).toList();
        entities.forEach(entity -> {
            AtomicBoolean haveTarget = new AtomicBoolean(false);
            Collection<Entity> targets = entity.getEntity().getNearbyEntities(20D, 20D, 20D);
            targets.forEach(target -> {
                if (!target.isCustomNameVisible() && !(target instanceof Player)) {
                    return;
                }
                String entityVillage = CustomName.squareBrackets(entity.getEntity().getCustomName(), 0);
                if (target instanceof Player) {
                    VillageModel village = VillageRepository.get(entityVillage);
                    if (Objects.equals(village.getPlayerName(), ((Player) target).getDisplayName())) {
                        return;
                    }
                    haveTarget.set(true);
                    ((Mob) entity.getEntity()).setTarget((LivingEntity) target);
                    return;
                }
                String targetVillage = CustomName.squareBrackets(target.getCustomName(), 0);
                if (!targetVillage.equals(entityVillage)) {
                    haveTarget.set(true);
                    ((Mob) entity.getEntity()).setTarget((LivingEntity) target);
                }
            });
            if (!haveTarget.get()) {
                ((Mob) entity.getEntity()).setTarget(null);
            }
        });
    }
}
