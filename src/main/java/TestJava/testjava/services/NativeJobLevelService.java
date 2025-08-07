package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

/**
 * Service pour gérer les niveaux des métiers natifs des villageois
 * basé sur leur niveau d'éducation
 */
public class NativeJobLevelService {

    /**
     * Applique le niveau d'éducation au niveau de métier natif d'un villageois
     * Règles :
     * - Éducation 0-1 : Pas de changement de niveau
     * - Éducation 2-5 : Niveau = éducation
     * - Éducation 6+ : Niveau = 5 (maximum maître)
     * 
     * @param villagerModel Le modèle du villageois
     */
    public static void applyEducationToNativeJobLevel(VillagerModel villagerModel) {
        // Vérifier que le villageois a un métier natif
        if (!villagerModel.hasNativeJob()) {
            return;
        }

        Entity entity = TestJava.world != null ? TestJava.world.getEntity(villagerModel.getId()) : null;
        if (!(entity instanceof Villager villager)) {
            return;
        }

        // Vérifier que le villageois a une profession (pas NONE)
        if (villager.getProfession() == Villager.Profession.NONE) {
            return;
        }

        int education = villagerModel.getEducation();
        int targetLevel = calculateJobLevelFromEducation(education);
        
        // Appliquer le niveau seulement si l'éducation le justifie
        if (targetLevel > 0) {
            int currentLevel = villager.getVillagerLevel();
            
            if (currentLevel != targetLevel) {
                villager.setVillagerLevel(targetLevel);
                
                // CORRECTION BUG : Forcer la régénération des trades pour que le niveau se reflète
                forceTradeRegeneration(villager);
                
                Bukkit.getLogger().info("[NativeJobLevel] Villageois " + villagerModel.getId() + 
                                       " - Niveau métier natif mis à jour: " + currentLevel + " → " + targetLevel + 
                                       " (éducation: " + education + ")");
                
                String professionName = getProfessionDisplayName(villager.getProfession());
                Bukkit.getServer().broadcastMessage("§a✅ §6" + getVillagerDisplayName(villager) + 
                                                   "§f est maintenant §e" + professionName + " niveau " + targetLevel);
            }
        }
    }

    /**
     * Calcule le niveau de métier natif en fonction de l'éducation
     * 
     * @param education Le niveau d'éducation (0-8)
     * @return Le niveau de métier à appliquer (0 = pas de changement, 2-5 = niveau réel)
     */
    private static int calculateJobLevelFromEducation(int education) {
        if (education <= 1) {
            return 0; // Pas de changement
        } else if (education <= 5) {
            return education; // Niveau = éducation
        } else {
            return 5; // Maximum maître
        }
    }

    /**
     * Obtient le nom d'affichage de la profession
     */
    private static String getProfessionDisplayName(Villager.Profession profession) {
        if (profession == Villager.Profession.FARMER) {
            return "Fermier";
        } else if (profession == Villager.Profession.LIBRARIAN) {
            return "Bibliothécaire";
        } else if (profession == Villager.Profession.CLERIC) {
            return "Clerc";
        } else if (profession == Villager.Profession.CARTOGRAPHER) {
            return "Cartographe";
        } else if (profession == Villager.Profession.FISHERMAN) {
            return "Pêcheur";
        } else if (profession == Villager.Profession.FLETCHER) {
            return "Archer";
        } else if (profession == Villager.Profession.SHEPHERD) {
            return "Tisserand";
        } else if (profession == Villager.Profession.BUTCHER) {
            return "Boucher";
        } else if (profession == Villager.Profession.LEATHERWORKER) {
            return "Travailleur du Cuir";
        } else if (profession == Villager.Profession.MASON) {
            return "Tailleur de Pierre";
        } else if (profession == Villager.Profession.TOOLSMITH) {
            return "Forgeron d'Outils";
        } else if (profession == Villager.Profession.WEAPONSMITH) {
            return "Réparateur d'Armes";
        } else if (profession == Villager.Profession.ARMORER) {
            return "Armurier";
        } else {
            return profession.toString();
        }
    }

    /**
     * Extrait le nom d'affichage du villageois
     */
    private static String getVillagerDisplayName(Villager villager) {
        String customName = villager.getCustomName();
        if (customName != null) {
            return org.bukkit.ChatColor.stripColor(customName);
        }
        return "Un villageois";
    }

    /**
     * Force la régénération des trades d'un villageois pour refléter son nouveau niveau
     * Plusieurs approches sont utilisées pour maximiser les chances de succès
     */
    private static void forceTradeRegeneration(Villager villager) {
        try {
            // Approche 1 : Réinitialiser l'expérience du villageois
            // Cela force souvent la régénération des trades
            int currentXp = villager.getVillagerExperience();
            villager.setVillagerExperience(0);
            
            // Attendre un tick avant de restaurer l'XP
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                villager.setVillagerExperience(currentXp);
                
                // Approche 2 : Forcer un "restock" en manipulant les trades
                forceTradeRestock(villager);
                
            }, 1L);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NativeJobLevel] Erreur lors de la régénération des trades: " + e.getMessage());
        }
    }

    /**
     * Force le "restock" des trades pour générer de nouveaux trades selon le niveau
     */
    private static void forceTradeRestock(Villager villager) {
        try {
            // Approche 3 : Simuler un restock en réinitiialisant les utilisations
            if (villager.getRecipes() != null && !villager.getRecipes().isEmpty()) {
                // Dupliquer la liste pour éviter les ConcurrentModificationException
                var recipes = new java.util.ArrayList<>(villager.getRecipes());
                
                for (org.bukkit.inventory.MerchantRecipe recipe : recipes) {
                    recipe.setUses(0); // Réinitialiser les utilisations
                }
                
                // Forcer la mise à jour des trades avec un délai
                Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                    // Approche 4 : Temporairement changer la profession et la remettre
                    // pour forcer une régénération complète
                    Villager.Profession currentProfession = villager.getProfession();
                    villager.setProfession(Villager.Profession.NONE);
                    
                    Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                        villager.setProfession(currentProfession);
                    }, 2L);
                    
                }, 2L);
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NativeJobLevel] Erreur lors du force restock: " + e.getMessage());
        }
    }

    /**
     * Force l'application du niveau d'éducation pour tous les villageois avec métiers natifs
     * Utilisé lors du démarrage ou de corrections
     */
    public static void applyEducationToAllNativeJobVillagers() {
        java.util.Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int updatedCount = 0;
        
        for (VillagerModel villagerModel : allVillagers) {
            if (villagerModel.hasNativeJob()) {
                Entity entity = TestJava.world != null ? TestJava.world.getEntity(villagerModel.getId()) : null;
                if (entity instanceof Villager villager && villager.getProfession() != Villager.Profession.NONE) {
                    int oldLevel = villager.getVillagerLevel();
                    applyEducationToNativeJobLevel(villagerModel);
                    
                    if (villager.getVillagerLevel() != oldLevel) {
                        updatedCount++;
                    }
                }
            }
        }
        
        if (updatedCount > 0) {
            Bukkit.getLogger().info("[NativeJobLevel] Correction automatique: " + updatedCount + 
                                   " niveaux de métiers natifs mis à jour selon l'éducation");
        }
    }
}
