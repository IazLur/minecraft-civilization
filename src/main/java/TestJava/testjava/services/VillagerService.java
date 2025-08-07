package TestJava.testjava.services;

import TestJava.testjava.Config;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.EatableModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EatableRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.UUID;

public class VillagerService {
    public void testIfGrowIsEatable(BlockGrowEvent e) {
        if (!(e.getNewState().getBlockData() instanceof Ageable age)) {
            return;
        }
        if (e.getNewState().getBlock().getBlockData().getMaterial() == Material.WHEAT) {
            if (age.getAge() == age.getMaximumAge()) {
                // Block is eatable
                VillageModel nearby = VillageRepository.getNearestOf(e.getBlock().getLocation());
                double distance = VillageRepository.getBellLocation(nearby).distance(e.getBlock().getLocation());
                if (distance > Config.VILLAGE_PROTECTION_RADIUS) {
                    return;
                }
                EatableModel eatable = new EatableModel();
                UUID uniq = UUID.randomUUID();
                eatable.setId(uniq);
                eatable.setVillage(nearby.getId());
                eatable.setX(e.getBlock().getX());
                eatable.setY(e.getBlock().getY());
                eatable.setZ(e.getBlock().getZ());
                EatableRepository.update(eatable);
            }
        }
    }

    /**
     * Crée un modèle VillagerModel à partir d'une entité Villager
     * @param villager L'entité Villager Minecraft
     * @return Le modèle VillagerModel créé, ou null si le villageois n'a pas de nom personnalisé valide
     */
    public static VillagerModel createVillagerModelFromEntity(Villager villager) {
        if (villager.customName() == null) {
            return null;
        }

        try {
            String villageName = CustomName.extractVillageName(villager.getCustomName());
            
            VillagerModel model = new VillagerModel();
            model.setId(villager.getUniqueId());
            model.setVillageName(villageName);
            model.setFood(20); // Nourriture maximale au départ
            model.setEating(false);
            model.setSocialClass(0); // Misérable par défaut
            model.setRichesse(0.0f); // Pas de richesse au départ
            model.setEducation(0); // Niveau d'éducation initial
            
            // Gestion des métiers
            model.setCurrentJobType(null);
            model.setCurrentJobName(null);
            model.setCurrentBuildingId(null);
            model.setHasLeatherArmor(false);

            return model;
        } catch (IllegalArgumentException e) {
            return null; // Format de nom invalide
        }
    }
}