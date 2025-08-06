package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.JobDistanceConfig;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.services.HistoryService;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service pour gérer le système d'impôts des villageois
 */
public class TaxService {

    /**
     * Collecte les impôts de tous les villageois ayant un métier
     */
    public static void collectTaxes() {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        
        // Statistiques par village
        Map<String, VillageTaxStats> villageStats = new HashMap<>();
        float totalTaxCollected = 0.0f;
        int totalTaxedVillagers = 0;

        for (VillagerModel villager : allVillagers) {
            try {
                // Récupérer l'entité Villager du monde
                Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
                if (entity == null) {
                    continue; // Villageois fantôme
                }

                int salary = 0;
                float taxRate = 0.0f;
                boolean hasJob = false;
                String jobType = "";

                // Vérifier d'abord les métiers custom
                if (villager.hasCustomJob()) {
                    BuildingDistanceConfig customConfig = DistanceConfigService.getBuildingConfig(villager.getCurrentJobName());
                    if (customConfig != null) {
                        salary = customConfig.getSalaireEmploye();
                        taxRate = customConfig.getTauxTaxeEmploye();
                        hasJob = true;
                        jobType = "custom (" + villager.getCurrentJobName() + ")";
                    }
                }
                // Sinon vérifier les métiers natifs
                else if (entity.getProfession() != Villager.Profession.NONE && 
                         entity.getProfession() != Villager.Profession.NITWIT) {
                    
                    JobDistanceConfig jobConfig = getJobConfigFromProfession(entity.getProfession());
                    if (jobConfig != null) {
                        salary = jobConfig.getSalaire();
                        taxRate = jobConfig.getTauxImpot();
                        hasJob = true;
                        jobType = "natif (" + jobConfig.getJobName() + ")";
                        
                        // Marquer comme métier natif dans la base de données si pas déjà fait
                        if (!villager.hasNativeJob()) {
                            villager.assignNativeJob();
                        }
                    }
                }

                // Pas de métier = pas d'impôts
                if (!hasJob) {
                    continue;
                }

                // Calculer l'impôt
                float tax = salary * taxRate;

                // Payer le salaire au villageois
                villager.setRichesse(villager.getRichesse() + salary);

                // Collecter les impôts
                villager.setRichesse(villager.getRichesse() - tax);
                totalTaxCollected += tax;
                totalTaxedVillagers++;

                // Statistiques par village
                String villageName = villager.getVillageName();
                villageStats.putIfAbsent(villageName, new VillageTaxStats());
                VillageTaxStats stats = villageStats.get(villageName);
                stats.taxCollected += tax;
                stats.taxedVillagers++;
                stats.villageName = villageName;

                // Log pour debug
                Bukkit.getLogger().info("[TaxService] Impôt collecté: " + villager.getId() + 
                                       " - Village: " + villageName +
                                       " - Métier: " + jobType + 
                                       " - Salaire: " + salary + "µ" +
                                       " - Impôt: " + String.format("%.2f", tax) + "µ");

                // Verser les impôts à l'empire du propriétaire du village
                VillageModel village = VillageRepository.get(villageName);
                if (village != null) {
                    EmpireModel empire = EmpireRepository.getForPlayer(village.getPlayerName());
                    if (empire != null) {
                        empire.setJuridictionCount(empire.getJuridictionCount() + tax);
                        EmpireRepository.update(empire);
                    }
                }

                // Sauvegarder le villageois
                VillagerRepository.update(villager);

            } catch (Exception e) {
                Bukkit.getLogger().warning("[TaxService] Erreur lors de la collecte d'impôts pour " + 
                                         villager.getId() + ": " + e.getMessage());
            }
        }

        // Messages par village et enregistrement historique
        if (totalTaxCollected > 0) {
            // Message global résumé
            Bukkit.getServer().broadcastMessage(
                Colorize.name("💰 Collecte d'impôts terminée") + ": " + 
                Colorize.name(String.format("%.2fµ", totalTaxCollected)) + 
                " collectés au total auprès de " + Colorize.name(totalTaxedVillagers + " villageois")
            );
            
            // Messages détaillés par village
            for (VillageTaxStats stats : villageStats.values()) {
                if (stats.taxCollected > 0) {
                    VillageModel village = VillageRepository.get(stats.villageName);
                    if (village != null) {
                        String ownerName = village.getPlayerName();
                        Bukkit.getServer().broadcastMessage(
                            Colorize.name("🏘️ " + stats.villageName) + " (" + ownerName + "): " +
                            Colorize.name(String.format("%.2fµ", stats.taxCollected)) + 
                            " collectés auprès de " + Colorize.name(stats.taxedVillagers + " villageois")
                        );
                        
                        // Enregistrer la collecte d'impôts dans l'historique
                        EmpireModel empire = EmpireRepository.getForPlayer(ownerName);
                        if (empire != null) {
                            HistoryService.recordTaxCollection(stats.villageName, stats.taxCollected, empire.getJuridictionCount());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Classe pour les statistiques d'impôts par village
     */
    private static class VillageTaxStats {
        public String villageName;
        public float taxCollected = 0.0f;
        public int taxedVillagers = 0;
    }

    /**
     * Obtient la configuration d'un métier à partir de la profession du villageois
     */
    private static JobDistanceConfig getJobConfigFromProfession(Villager.Profession profession) {
        Material jobBlock = getJobBlockFromProfession(profession);
        if (jobBlock == null) {
            return null;
        }
        
        return DistanceConfigService.getJobConfig(jobBlock);
    }

    /**
     * Convertit une profession Minecraft en bloc de métier correspondant
     */
    private static Material getJobBlockFromProfession(Villager.Profession profession) {
        String professionName = profession.toString().toUpperCase();
        
        if (professionName.equals("CARTOGRAPHER")) {
            return Material.CARTOGRAPHY_TABLE;
        } else if (professionName.equals("CLERIC")) {
            return Material.BREWING_STAND;
        } else if (professionName.equals("TOOLSMITH")) {
            return Material.SMITHING_TABLE;
        } else if (professionName.equals("FLETCHER")) {
            return Material.FLETCHING_TABLE;
        } else if (professionName.equals("SHEPHERD")) {
            return Material.LOOM;
        } else if (professionName.equals("MASON")) {
            return Material.STONECUTTER;
        } else if (professionName.equals("FARMER")) {
            return Material.COMPOSTER;
        } else if (professionName.equals("FISHERMAN")) {
            return Material.BARREL;
        } else if (professionName.equals("BUTCHER")) {
            return Material.SMOKER;
        } else if (professionName.equals("ARMORER")) {
            return Material.BLAST_FURNACE;
        } else if (professionName.equals("LIBRARIAN")) {
            return Material.LECTERN;
        } else if (professionName.equals("WEAPONSMITH")) {
            return Material.GRINDSTONE;
        } else if (professionName.equals("NONE") || professionName.equals("NITWIT")) {
            return null;
        } else if (professionName.contains("ENCHANT")) {
            return Material.ENCHANTING_TABLE;
        } else if (professionName.contains("BREW") || professionName.contains("APOTH")) {
            return Material.CAULDRON;
        } else if (professionName.contains("ANVIL") || professionName.contains("FORGE")) {
            return Material.ANVIL;
        } else {
            return null;
        }
    }

    /**
     * Calcule la richesse totale de tous les villageois d'un village
     */
    public static float getTotalVillagerWealthInVillage(String villageName) {
        String query = String.format("/.[villageName='%s']", villageName);
        Collection<VillagerModel> villagers = TestJava.database.find(query, VillagerModel.class);
        
        float totalWealth = 0.0f;
        for (VillagerModel villager : villagers) {
            totalWealth += villager.getRichesse();
        }
        
        return totalWealth;
    }

    /**
     * Obtient les statistiques de collecte d'impôts
     */
    public static TaxStats getTaxStats() {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int totalVillagers = 0;
        int workingVillagers = 0;
        float totalWealth = 0.0f;
        float estimatedDailyTax = 0.0f;

        for (VillagerModel villager : allVillagers) {
            totalVillagers++;
            totalWealth += villager.getRichesse();

            // Vérifier si a un métier
            Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
            if (entity != null && entity.getProfession() != Villager.Profession.NONE && 
                entity.getProfession() != Villager.Profession.NITWIT) {
                
                workingVillagers++;
                JobDistanceConfig jobConfig = getJobConfigFromProfession(entity.getProfession());
                if (jobConfig != null) {
                    estimatedDailyTax += jobConfig.getSalaire() * jobConfig.getTauxImpot() * 288; // 288 = 24h / 5min
                }
            }
        }

        return new TaxStats(totalVillagers, workingVillagers, totalWealth, estimatedDailyTax);
    }

    /**
     * Classe pour les statistiques d'impôts
     */
    public static class TaxStats {
        public final int totalVillagers;
        public final int workingVillagers;
        public final float totalWealth;
        public final float estimatedDailyTax;

        public TaxStats(int totalVillagers, int workingVillagers, float totalWealth, float estimatedDailyTax) {
            this.totalVillagers = totalVillagers;
            this.workingVillagers = workingVillagers;
            this.totalWealth = totalWealth;
            this.estimatedDailyTax = estimatedDailyTax;
        }
    }
}