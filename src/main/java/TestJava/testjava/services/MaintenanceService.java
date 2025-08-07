package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.threads.TraderMaintenanceThread;


import java.util.logging.Logger;

public class MaintenanceService {
    private static final Logger LOGGER = TestJava.plugin.getLogger();

    public static void performFullMaintenance() {
        LOGGER.info("🔧 Démarrage de la maintenance complète...");

        try {
            // 1. Nettoyage des entités
            if (TestJava.world != null) {
                LOGGER.info("Nettoyage des entités...");
                TestJava.playerService.killAllDelegators();
                TestJava.playerService.killAllBandits();
                TestJava.playerService.resetAllWars();
                TestJava.playerService.killAllWanderingTraders();
                SheepService.removeNaturalSheep();
            }

            // 2. Synchronisation des villageois
            LOGGER.info("Synchronisation des villageois...");
            VillagerSynchronizationService.synchronizeWorldVillagersWithDatabase();

            // 3. Migration et initialisation des classes sociales
            LOGGER.info("Mise à jour des classes sociales...");
            SocialClassService.migrateSocialClassTagsToNewFormat();
            SocialClassService.initializeSocialClassForExistingVillagers();

            // 4. Synchronisation des métiers personnalisés
            LOGGER.info("Synchronisation des métiers personnalisés...");
            CustomJobSynchronizationService.synchronizeCustomJobsOnStartup();

            // 5. Vérification et correction des "Home" des villageois
            LOGGER.info("Vérification des positions de domicile...");
            VillagerHomeService.validateAndCorrectAllVillagerHomes();

            // 6. Correction des populations
            LOGGER.info("Correction des populations...");
            VillagePopulationCorrectionService.correctAllVillagePopulations();

            // 7. Migration des juridictions
            LOGGER.info("Migration des juridictions...");
            for (EmpireModel empire : EmpireRepository.getAll()) {
                try {
                    empire.getJuridictionCount();
                } catch (NullPointerException ex) {
                    empire.setJuridictionCount(0);
                    EmpireRepository.update(empire);
                }
            }

            // 8. Initialisation des ressources
            LOGGER.info("Initialisation des ressources...");
            ResourceInitializationService.initializeResourcesIfEmpty();

            // 9. Chargement des configurations
            LOGGER.info("Chargement des configurations...");
            DistanceConfigService.loadAllConfigurations();

            // 10. Maintenance des marchands
            LOGGER.info("Maintenance des marchands...");
            new TraderMaintenanceThread().run();

            // 11. Maintenance des métiers personnalisés
            LOGGER.info("Maintenance des métiers personnalisés...");
            CustomJobArmorService.equipAllCustomJobVillagers();

            LOGGER.info("✅ Maintenance complète terminée avec succès !");

        } catch (Exception e) {
            LOGGER.severe("❌ Erreur lors de la maintenance : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
