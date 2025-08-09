package TestJava.testjava.examples;

import TestJava.testjava.services.ClericService;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;

/**
 * Exemples d'utilisation du ClericService
 * 
 * Ce fichier montre comment le service du Clerc fonctionne en pratique
 * avec des exemples concrets d'usage.
 */
public class ClericServiceExamples {

    /**
     * Exemple 1: Déclenchement automatique via TaxService
     * 
     * Le ClericService est automatiquement déclenché toutes les 5 minutes
     * lors de la collecte des taxes, après le paiement du salaire du Clerc.
     */
    public static void automaticTriggerExample() {
        // Cet exemple montre le flux automatique :
        
        // 1. TaxService.collectTaxes() s'exécute toutes les 5 minutes
        // 2. Pour chaque villageois avec profession CLERIC :
        //    - Paiement du salaire (18µ brut, 11.7µ net)
        //    - Déclenchement automatique : ClericService.triggerRandomBuffAfterSalary()
        
        System.out.println("=== EXEMPLE DÉCLENCHEMENT AUTOMATIQUE ===");
        System.out.println("1. Clerc reçoit son salaire : 18µ - 35% impôts = 11.7µ net");
        System.out.println("2. Service automatiquement déclenché");
        System.out.println("3. Scan des cibles dans le village");
        System.out.println("4. Sélection aléatoire d'une cible");
        System.out.println("5. Application d'un buff aléatoire");
        System.out.println("6. Messages envoyés aux joueurs concernés");
    }

    /**
     * Exemple 2: Déclenchement manuel pour test
     * 
     * Comment déclencher manuellement le service pour tests ou débogage.
     */
    public static void manualTriggerExample(String villagerUUID, String villageName) {
        // Récupération des données
        VillagerModel villagerModel = VillagerRepository.find(java.util.UUID.fromString(villagerUUID));
        if (villagerModel == null) {
            System.out.println("Erreur: Villageois introuvable");
            return;
        }

        VillageModel village = VillageRepository.get(villageName);
        if (village == null) {
            System.out.println("Erreur: Village introuvable");
            return;
        }

        // Récupération de l'entité Villager
        Villager clericEntity = (Villager) Bukkit.getServer().getEntity(villagerModel.getId());
        if (clericEntity == null) {
            System.out.println("Erreur: Entité villageois introuvable dans le monde");
            return;
        }

        // Vérification que c'est bien un Clerc
        if (clericEntity.getProfession() != Villager.Profession.CLERIC) {
            System.out.println("Erreur: Le villageois n'est pas un Clerc");
            return;
        }

        System.out.println("=== EXEMPLE DÉCLENCHEMENT MANUEL ===");
        System.out.println("Village: " + villageName);
        System.out.println("Clerc UUID: " + villagerUUID);
        System.out.println("Déclenchement du service...");

        // Déclenchement manuel
        ClericService.triggerRandomBuffAfterSalary(villagerModel, clericEntity);
        
        System.out.println("Service exécuté ! Vérifiez les logs pour les détails.");
    }

    /**
     * Exemple 3: Scénarios typiques de buff
     * 
     * Description des différents scénarios possibles lors de l'application des buffs.
     */
    public static void buffScenariosExample() {
        System.out.println("=== SCÉNARIOS TYPIQUES DE BUFF ===");
        
        System.out.println("\n--- Scénario 1: Buff sur le Propriétaire ---");
        System.out.println("• Propriétaire connecté et dans le village");
        System.out.println("• Sélection: Propriétaire Jean");
        System.out.println("• Effet: Régénération I pendant 15 minutes");
        System.out.println("• Message joueur: '✨ Clerc : Vous avez reçu Régénération I pendant 15 minutes !'");
        
        System.out.println("\n--- Scénario 2: Buff sur un Garde Squelette ---");
        System.out.println("• 3 gardes squelettes trouvés dans le village");
        System.out.println("• Sélection: [MonVillage] Garde Marcel");
        System.out.println("• Effet: Force I pendant 60 minutes");
        System.out.println("• Message propriétaire: '⚗️ Clerc : [MonVillage] Garde Marcel a reçu Force I pendant 60 minutes !'");
        
        System.out.println("\n--- Scénario 3: Village Vide ---");
        System.out.println("• Propriétaire déconnecté");
        System.out.println("• Aucun garde squelette trouvé");
        System.out.println("• Log: '[ClericService] Aucun personnage éligible trouvé dans le village MonVillage'");
        System.out.println("• Résultat: Service s'arrête silencieusement");
        
        System.out.println("\n--- Scénario 4: Buff sur l'Armée ---");
        System.out.println("• Squelettes d'armée détectés (système extensible)");
        System.out.println("• Sélection: [MonVillage] Soldat Pierre");
        System.out.println("• Effet: Vitesse I pendant 60 minutes");
        System.out.println("• Message propriétaire: '⚗️ Clerc : [MonVillage] Soldat Pierre a reçu Vitesse I pendant 60 minutes !'");
    }

    /**
     * Exemple 4: Configuration et paramètres
     * 
     * Explication des paramètres configurables du service.
     */
    public static void configurationExample() {
        System.out.println("=== CONFIGURATION DU SERVICE ===");
        
        System.out.println("\n--- Effets Disponibles ---");
        System.out.println("• Régénération I (REGENERATION)");
        System.out.println("• Force I (STRENGTH)");
        System.out.println("• Vitesse I (SPEED)");
        
        System.out.println("\n--- Durées ---");
        System.out.println("• Joueurs: 15 minutes (18 000 ticks)");
        System.out.println("• Entités: 60 minutes (72 000 ticks)");
        
        System.out.println("\n--- Rayon de Recherche ---");
        System.out.println("• Utilise Config.VILLAGE_PROTECTION_RADIUS");
        System.out.println("• Recherche centrée sur la cloche du village");
        
        System.out.println("\n--- Métier Clerc (metiers.json) ---");
        System.out.println("• Material: BREWING_STAND");
        System.out.println("• Salaire: 18µ (le plus élevé)");
        System.out.println("• Taux impôt: 35%");
        System.out.println("• Distance placement: 0-13 blocs");
    }

    /**
     * Exemple 5: Gestion d'erreurs
     * 
     * Comment le service gère les cas d'erreur et situations exceptionnelles.
     */
    public static void errorHandlingExample() {
        System.out.println("=== GESTION D'ERREURS ===");
        
        System.out.println("\n--- Erreurs Courantes ---");
        System.out.println("• Villageois null -> Log d'avertissement, arrêt du service");
        System.out.println("• Village introuvable -> Log d'avertissement, arrêt du service");
        System.out.println("• Monde null -> Log d'avertissement, arrêt du service");
        System.out.println("• Exception lors de l'application -> Catch dans TaxService, log d'erreur");
        
        System.out.println("\n--- Logs de Débogage ---");
        System.out.println("• '[ClericService] Clerc <UUID> a appliqué <Effet> à <Cible> dans le village <Village>'");
        System.out.println("• '[ClericService] Aucun personnage éligible trouvé dans le village <Village>'");
        System.out.println("• '[TaxService] Erreur ClericService: <message>'");
        
        System.out.println("\n--- Récupération Automatique ---");
        System.out.println("• Les erreurs n'interrompent pas le cycle de collecte des taxes");
        System.out.println("• Chaque Clerc est traité indépendamment");
        System.out.println("• Les exceptions sont isolées par villageois");
    }

    /**
     * Exemple d'utilisation principale pour démonstration
     */
    public static void demonstrateClericService() {
        System.out.println("==========================================");
        System.out.println("    DÉMONSTRATION CLERIC SERVICE");
        System.out.println("==========================================");
        
        automaticTriggerExample();
        System.out.println();
        
        buffScenariosExample();
        System.out.println();
        
        configurationExample();
        System.out.println();
        
        errorHandlingExample();
        
        System.out.println("\n==========================================");
        System.out.println("  POUR UTILISER LE SERVICE EN JEU:");
        System.out.println("  1. Placez un BREWING_STAND près du village");
        System.out.println("  2. Un villageois inactif prendra automatiquement");
        System.out.println("     la profession CLERIC");
        System.out.println("  3. Toutes les 5 minutes, le Clerc appliquera");
        System.out.println("     un buff aléatoire après son salaire");
        System.out.println("==========================================");
    }
}
