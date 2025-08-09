package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.BuildingDistanceConfig;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.JobDistanceConfig;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.enums.SocialClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
                
                // CORRECTION BUG : Vérifier que l'empire peut payer le salaire
                String villageName = villager.getVillageName();
                VillageModel village = VillageRepository.get(villageName);
                if (village == null) {
                    continue; // Village introuvable
                }
                
                EmpireModel empire = EmpireRepository.getForPlayer(village.getPlayerName());
                if (empire == null) {
                    continue; // Empire introuvable
                }
                
                // Vérifier si l'empire a assez d'argent pour payer le salaire
                if (empire.getJuridictionCount() < salary) {
                    // FAILLITE : L'empire ne peut pas payer, le villageois perd son métier
                    handleJobLossFromBankruptcy(villager, entity, salary, empire.getJuridictionCount());
                    continue;
                }
                
                // L'empire peut payer : prélever le salaire de l'empire
                empire.setJuridictionCount(empire.getJuridictionCount() - salary);
                
                // Payer le salaire au villageois (salaire - impôts)
                float netSalary = salary - tax;
                villager.setRichesse(villager.getRichesse() + netSalary);
                
                // Verser les impôts à l'empire (récupération partielle)
                empire.setJuridictionCount(empire.getJuridictionCount() + tax);
                EmpireRepository.update(empire);
                
                totalTaxCollected += tax;
                totalTaxedVillagers++;

                // Statistiques par village
                villageStats.putIfAbsent(villageName, new VillageTaxStats());
                VillageTaxStats stats = villageStats.get(villageName);
                stats.taxCollected += tax;
                stats.taxedVillagers++;
                stats.villageName = villageName;

                // Log pour debug
                Bukkit.getLogger().info("[TaxService] Salaire payé: " + villager.getId() + 
                                       " - Village: " + villageName +
                                       " - Métier: " + jobType + 
                                       " - Salaire brut: " + salary + "µ" +
                                       " - Salaire net: " + String.format("%.2f", netSalary) + "µ" +
                                       " - Impôt collecté: " + String.format("%.2f", tax) + "µ");

                // Sauvegarder le villageois
                VillagerRepository.update(villager);

                // ACTION MÉTIER NATIF: Forgeron d'Outils répare les golems après paiement
                try {
                    if (!villager.hasCustomJob() && entity.getProfession() == Villager.Profession.TOOLSMITH) {
                        ToolsmithService.triggerRepairsAfterSalary(villager, entity);
                    }
                } catch (Throwable t) {
                    Bukkit.getLogger().warning("[TaxService] Erreur ToolsmithService: " + t.getMessage());
                }

                // ACTION MÉTIER NATIF: Fletcher équipe les gardes squelettes avec armure d'or après paiement
                try {
                    if (!villager.hasCustomJob() && entity.getProfession() == Villager.Profession.FLETCHER) {
                        FletcherService.triggerArmorEquippingAfterSalary(villager, entity);
                    }
                } catch (Throwable t) {
                    Bukkit.getLogger().warning("[TaxService] Erreur FletcherService: " + t.getMessage());
                }

                // ACTION MÉTIER NATIF: Armurier améliore l'armure du joueur après paiement
                try {
                    if (!villager.hasCustomJob() && entity.getProfession() == Villager.Profession.ARMORER) {
                        VillageModel villageModel = VillageRepository.get(villager.getVillageName());
                        if (villageModel != null) {
                            ArmorierService.triggerArmorUpgradeAfterSalary(villager, villageModel);
                        }
                    }
                } catch (Throwable t) {
                    Bukkit.getLogger().warning("[TaxService] Erreur ArmorierService: " + t.getMessage());
                }

            } catch (Exception e) {
                Bukkit.getLogger().warning("[TaxService] Erreur lors de la collecte d'impôts pour " + 
                                         villager.getId() + ": " + e.getMessage());
            }
        }

        // NOUVELLE FONCTIONNALITÉ: Redistribution 25% des taxes aux misérables
        float redistributionAmount = 0.0f;
        Map<String, RedistributionStats> redistributionByVillage = new HashMap<>();
        
        if (totalTaxCollected > 0) {
            redistributionAmount = totalTaxCollected * 0.25f;
            redistributionByVillage = redistributeToMiserableVillagers(redistributionAmount);
        }

        // Messages par village et enregistrement historique
        if (totalTaxCollected > 0) {
            // Message global résumé
            String summaryMsg =
                Colorize.name("💰 Paie des salaires terminée") + ": " +
                Colorize.name(String.format("%.2fµ", totalTaxCollected)) +
                " d'impôts collectés auprès de " + Colorize.name(totalTaxedVillagers + " travailleurs");
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(summaryMsg);
            }
            
            // Messages détaillés par village
            for (VillageTaxStats stats : villageStats.values()) {
                if (stats.taxCollected > 0) {
                    VillageModel village = VillageRepository.get(stats.villageName);
                    if (village != null) {
                        String ownerName = village.getPlayerName();
                        String detailMsg =
                            Colorize.name(stats.villageName) + ": " +
                            Colorize.name(String.format("%.2fµ", stats.taxCollected)) +
                            " pour " + Colorize.name(stats.taxedVillagers + " travailleurs");
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage(detailMsg);
                        }
                        
                        // Message de redistribution pour le propriétaire du village
                        RedistributionStats redistribution = redistributionByVillage.get(stats.villageName);
                        if (redistribution != null && redistribution.miserableCount > 0) {
                            // Envoyer le message au propriétaire du village uniquement s'il est connecté
                            if (Bukkit.getPlayerExact(ownerName) != null) {
                                Bukkit.getPlayerExact(ownerName).sendMessage(
                                    Colorize.name("25%(") + 
                                    Colorize.name(String.format("%.2fµ", redistribution.amountDistributed)) + 
                                    Colorize.name(") redistribuées à ") + 
                                    Colorize.name(redistribution.miserableCount + " misérables")
                                );
                            }
                        }
                        
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
     * Redistribue un montant aux villageois misérables de tous les villages
     * @param totalAmount Montant total à redistribuer (25% des taxes collectées)
     * @return Map avec les statistiques de redistribution par village
     */
    private static Map<String, RedistributionStats> redistributeToMiserableVillagers(float totalAmount) {
        Map<String, RedistributionStats> redistributionByVillage = new HashMap<>();
        
        // Trouver tous les villageois misérables
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        Map<String, java.util.List<VillagerModel>> miserablesByVillage = new HashMap<>();
        int totalMiserables = 0;
        
        for (VillagerModel villager : allVillagers) {
            if (villager.getSocialClassEnum() == SocialClass.MISERABLE) {
                String villageName = villager.getVillageName();
                miserablesByVillage.putIfAbsent(villageName, new java.util.ArrayList<>());
                miserablesByVillage.get(villageName).add(villager);
                totalMiserables++;
            }
        }
        
        // Si aucun misérable, rien à redistribuer
        if (totalMiserables == 0) {
            Bukkit.getLogger().info("[TaxService] Aucun villageois misérable trouvé pour la redistribution");
            return redistributionByVillage;
        }
        
        // Calculer le montant par misérable
        float amountPerMiserable = totalAmount / totalMiserables;
        
        // Redistribuer par village
        for (Map.Entry<String, java.util.List<VillagerModel>> entry : miserablesByVillage.entrySet()) {
            String villageName = entry.getKey();
            java.util.List<VillagerModel> miserables = entry.getValue();
            
            RedistributionStats stats = new RedistributionStats();
            stats.miserableCount = miserables.size();
            stats.amountDistributed = amountPerMiserable * miserables.size();
            
            // Distribuer l'argent à chaque misérable
            for (VillagerModel miserable : miserables) {
                miserable.setRichesse(miserable.getRichesse() + amountPerMiserable);
                VillagerRepository.update(miserable);
                
                // Log pour debug
                Bukkit.getLogger().info("[TaxService] Redistribution: " + miserable.getId() + 
                                       " (Village: " + villageName + ") a reçu " + 
                                       String.format("%.2f", amountPerMiserable) + "µ");
            }
            
            redistributionByVillage.put(villageName, stats);
        }
        
        // Log global de redistribution
        Bukkit.getLogger().info("[TaxService] Redistribution terminée: " + 
                               String.format("%.2f", totalAmount) + "µ redistribués à " + 
                               totalMiserables + " misérables dans " + 
                               miserablesByVillage.size() + " villages");
        
        return redistributionByVillage;
    }
    
    /**
     * Gère la perte d'emploi d'un villageois due à la faillite de son empire
     */
    private static void handleJobLossFromBankruptcy(VillagerModel villager, Villager entity, int requiredSalary, float availableFunds) {
        String villageName = villager.getVillageName();
        String jobType = "";
        
        // Déterminer le type de métier
        if (villager.hasCustomJob()) {
            jobType = "métier custom (" + villager.getCurrentJobName() + ")";
            
            // Retirer le métier custom
            villager.clearJob();
            
            // Retirer l'armure de cuir si c'est un métier custom
            if (entity != null) {
                // TODO: Appeler CustomJobArmorService.removeCustomJobArmor si nécessaire
            }
        } else if (villager.hasNativeJob()) {
            jobType = "métier natif (" + entity.getProfession().toString() + ")";
            
            // Retirer le métier natif
            villager.clearJob();
            if (entity != null) {
                entity.setProfession(Villager.Profession.NONE);
            }
        }
        
        // Rétrograder à la classe Inactive
        SocialClassService.demoteToInactiveOnJobLoss(villager);
        
        // Messages informatifs
        String villagerName = "Un villageois";
    if (entity != null) { villagerName = entity.getName(); }
        
        // Message global de faillite
        String bankruptcyMsg = "💸 " + Colorize.name("FAILLITE") + " à " + Colorize.name(villageName) +
            " : " + villagerName + " a perdu son " + jobType +
            " (salaire requis: " + requiredSalary + "µ, disponible: " + String.format("%.2f", availableFunds) + "µ)";
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(bankruptcyMsg);
        }
        
        // Enregistrer dans l'historique
        HistoryService.recordJobChange(villager, "Licenciement pour faillite");
        
        // Log détaillé
        Bukkit.getLogger().warning("[TaxService] FAILLITE: " + villager.getId() + 
                                 " - Village: " + villageName +
                                 " - Métier perdu: " + jobType + 
                                 " - Salaire requis: " + requiredSalary + "µ" +
                                 " - Fonds disponibles: " + String.format("%.2f", availableFunds) + "µ");
        
        // Sauvegarder les changements
        VillagerRepository.update(villager);
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
     * Classe pour les statistiques de redistribution par village
     */
    private static class RedistributionStats {
        public int miserableCount = 0;
        public float amountDistributed = 0.0f;
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