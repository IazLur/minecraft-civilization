package TestJava.testjava.examples;

import TestJava.testjava.services.ForestGuardService;
import TestJava.testjava.models.VillagerModel;
import org.bukkit.entity.Villager;

/**
 * Exemples d'utilisation du ForestGuardService
 * 
 * Le ForestGuardService est automatiquement déclenché toutes les 5 minutes
 * après le paiement des taxes dans le VillagerTaxThread.
 * 
 * Ce système permet aux gardes forestiers de planter automatiquement des arbres
 * autour de leur lieu de travail, créant des forêts progressivement.
 */
public class ForestGuardServiceExamples {
    
    /**
     * EXEMPLE 1: Déclenchement Automatique
     * 
     * Le ForestGuardService est automatiquement déclenché toutes les 5 minutes
     * via le VillagerTaxThread -> TaxService.collectTaxes()
     * 
     * Workflow automatique:
     * 1. VillagerTaxThread s'exécute (toutes les 5 minutes)
     * 2. TaxService.collectTaxes() traite tous les villageois
     * 3. Pour chaque garde forestier: paiement salaire + collecte taxes
     * 4. Déclenchement automatique: ForestGuardService.triggerTreePlantingAfterSalary()
     * 5. Le garde se déplace vers son bâtiment
     * 6. Recherche d'un endroit libre pour planter
     * 7. Déplacement vers l'endroit de plantation
     * 8. Plantation du sapling + croissance magique après 3 secondes
     */
    public static void exempleDeDeClenchementAutomatique() {
        /*
         * LE CODE CI-DESSOUS EST PUREMENT INFORMATIF
         * Le système fonctionne automatiquement sans intervention manuelle
         */
        
        // Dans TaxService.collectTaxes(), cette logique s'exécute automatiquement:
        /*
        if (villager.hasCustomJob()) {
            String customJobName = villager.getCurrentJobName();
            
            if ("garde forestier".equals(customJobName)) {
                ForestGuardService.triggerTreePlantingAfterSalary(villager, entity);
            }
        }
        */
    }
    
    /**
     * EXEMPLE 2: Déclenchement Manuel (pour tests ou cas spéciaux)
     * 
     * Si vous voulez déclencher manuellement la plantation d'arbres
     * par un garde forestier (par exemple dans une commande admin)
     */
    public static void exempleDeDeClenchementManuel(VillagerModel villagerModel, Villager villagerEntity) {
        // Vérifier que c'est bien un garde forestier
        if (villagerModel.hasCustomJob() && "garde forestier".equals(villagerModel.getCurrentJobName())) {
            
            // Déclencher manuellement la plantation
            ForestGuardService.triggerTreePlantingAfterSalary(villagerModel, villagerEntity);
            
            // Messages informatifs
            System.out.println("🌲 Plantation d'arbre déclenchée manuellement pour le garde forestier: " + 
                             villagerModel.getId());
        } else {
            System.out.println("❌ Ce villageois n'est pas un garde forestier");
        }
    }
    
    /**
     * EXEMPLE 3: Configuration et Fonctionnement
     * 
     * Le garde forestier utilise la configuration définie dans metiers_custom.json:
     */
    public static void exempleDeConfiguration() {
        /*
         * Configuration dans metiers_custom.json:
         * {
         *   "buildingType": "garde forestier",
         *   "distanceMin": 77,
         *   "distanceMax": 128,
         *   "description": "Garde forestier pour générer une forêt",
         *   "costToBuild": 500,
         *   "costPerDay": 10,
         *   "costPerUpgrade": 100,
         *   "costUpgradeMultiplier": 1.2,
         *   "nombreEmployesMax": 3,
         *   "salaireEmploye": 12,
         *   "tauxTaxeEmploye": 0.25
         * }
         * 
         * Cela signifie:
         * - Salaire: 12µ toutes les 5 minutes
         * - Impôt: 25% (3µ collectés)
         * - Revenu net pour le garde: 9µ
         * - Maximum 3 gardes par bâtiment
         * - Coût de construction: 500µ
         * - Coût d'entretien: 10µ toutes les 4 minutes
         */
    }
    
    /**
     * EXEMPLE 4: Cycle de Vie Complet d'un Garde Forestier
     */
    public static void exempleDeCycleDeVie() {
        /*
         * 1. CONSTRUCTION DU BÂTIMENT
         * - Joueur utilise: /build garde forestier
         * - Coût: 500µ déduits de l'empire
         * - Bâtiment créé avec BuildingModel dans la base
         * 
         * 2. ASSIGNATION AUTOMATIQUE D'EMPLOYÉS
         * - AutomaticJobAssignmentThread trouve des villageois inactifs
         * - Assigne automatiquement jusqu'à 3 gardes forestiers
         * - Chaque garde reçoit l'armure de cuir
         * 
         * 3. CYCLE DE TRAVAIL (toutes les 5 minutes)
         * - VillagerTaxThread s'exécute
         * - Chaque garde forestier:
         *   a) Reçoit 12µ de salaire
         *   b) Paie 3µ d'impôts (25%)
         *   c) Déclenche ForestGuardService.triggerTreePlantingAfterSalary()
         *   d) Se déplace vers le bâtiment garde forestier
         *   e) Cherche un endroit libre dans un rayon de 50 blocs
         *   f) Se déplace vers l'endroit de plantation
         *   g) Plante un sapling aléatoire (chêne, bouleau, épicéa, etc.)
         *   h) Attend 3 secondes puis fait pousser l'arbre magiquement
         * 
         * 4. ENTRETIEN DU BÂTIMENT
         * - DailyBuildingCostThread (toutes les 4 minutes)
         * - Coût: 10µ déduits de l'empire du propriétaire
         * - Si l'empire n'a pas assez: bâtiment désactivé
         * 
         * 5. MAINTENANCE DES EMPLOYÉS
         * - CustomJobMaintenanceThread (toutes les 7 minutes)
         * - Vérifie que les gardes portent leur armure de cuir
         * - Ajuste le nombre d'employés selon les besoins
         * - Nettoie les incohérences
         */
    }
    
    /**
     * EXEMPLE 5: Types d'Arbres et Probabilités
     */
    public static void exempleDeTypesDArbres() {
        /*
         * Le ForestGuardService plante des arbres selon ces probabilités:
         * 
         * - Chêne (Oak): 30% - Le plus commun
         * - Bouleau (Birch): 25% - Commun  
         * - Épicéa (Spruce): 20% - Commun
         * - Jungle: 10% - Peu commun
         * - Acacia: 8% - Peu commun
         * - Chêne Noir (Dark Oak): 5% - Rare
         * - Cerisier (Cherry): 2% - Très rare
         * 
         * Ces probabilités créent une diversité naturelle dans les forêts générées
         * tout en favorisant les types d'arbres les plus communs.
         */
    }
    
    /**
     * EXEMPLE 6: Messages et Notifications
     */
    public static void exempleDeMessages() {
        /*
         * Le système affiche automatiquement des messages sur le serveur:
         * 
         * PLANTATION:
         * "🌱 [Nom du Garde] a planté un chêne qui va bientôt pousser..."
         * 
         * CROISSANCE RÉUSSIE:
         * "🌳✨ [Nom du Garde] a fait pousser un magnifique chêne par magie !"
         * 
         * ÉCHECS:
         * "🌲 [Nom du Garde] n'a pas pu rejoindre son poste de garde forestier"
         * "🌲 [Nom du Garde] ne trouve pas d'endroit libre pour planter un arbre"
         * "🌲 [Nom du Garde] n'a pas pu atteindre l'endroit de plantation"
         * 
         * Ces messages permettent aux joueurs de suivre l'activité des gardes forestiers
         * et de comprendre pourquoi une plantation pourrait échouer.
         */
    }
    
    /**
     * EXEMPLE 7: Optimisation et Performance
     */
    public static void exempleOptimisation() {
        /*
         * Le ForestGuardService est optimisé pour les performances:
         * 
         * 1. RECHERCHE D'EMPLACEMENT INTELLIGENTE:
         * - Évite les zones déjà boisées (rayon 8 blocs)
         * - Vérifie le type de sol adapté
         * - Limite la recherche à 50 blocs du bâtiment
         * 
         * 2. DÉPLACEMENTS FIABLES:
         * - Utilise VillagerMovementManager pour éviter les blocages
         * - Timeout automatique (60s lieu de travail, 45s plantation)
         * - Retry automatique en cas d'échec de pathfinding
         * 
         * 3. CROISSANCE OPTIMISÉE:
         * - Utilise d'abord Minecraft native (applyBoneMeal)
         * - Fallback manuel uniquement si nécessaire
         * - Construction d'arbre simple (4-6 blocs + feuilles)
         * 
         * 4. GESTION D'ERREURS:
         * - Try-catch pour chaque étape critique
         * - Logs détaillés pour le debugging
         * - Pas de crash en cas d'erreur inattendue
         */
    }
}
