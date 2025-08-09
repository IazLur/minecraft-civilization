package TestJava.testjava.examples;

import TestJava.testjava.services.VillagerMovementManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import java.util.UUID;

/**
 * Exemples d'utilisation du gestionnaire centralisé VillagerMovementManager
 * Ces exemples montrent comment migrer l'ancien code dispersé vers le nouveau système
 */
public class VillagerMovementExamples {

    /**
     * Exemple 1: Déplacement simple avec action à l'arrivée
     */
    public static void simpleMovementExample(Villager villager, Location destination) {
        VillagerMovementManager.moveVillager(villager, destination)
            .onSuccess(() -> {
                // Action à effectuer quand le villageois arrive
                System.out.println("Villageois arrivé à destination !");
            })
            .withName("SimpleMovement")
            .start();
    }

    /**
     * Exemple 2: Déplacement vers un joueur avec vérifications
     */
    public static void moveToPlayerExample(Villager villager, Player target) {
        VillagerMovementManager.moveVillager(villager, target.getLocation())
            .withSuccessDistance(3.0) // Se rapprocher à 3 blocs
            .withTimeout(30) // 30 secondes maximum
            .withName("MoveToPlayer_" + target.getName())
            .onSuccess(() -> {
                // Interaction avec le joueur
                if (target.isOnline()) {
                    target.sendMessage("Un villageois vous a rejoint !");
                    performPlayerInteraction(villager, target);
                }
            })
            .onFailure(() -> {
                // Gérer l'échec
                if (target.isOnline()) {
                    target.sendMessage("Le villageois n'a pas pu vous rejoindre.");
                }
            })
            .start();
    }

    /**
     * Exemple 3: Déplacement avec suivi de progression
     */
    public static void movementWithProgressExample(Villager villager, Location destination) {
        VillagerMovementManager.moveVillager(villager, destination)
            .withName("ProgressTrackedMovement")
            .onPositionUpdate((distance, attempts) -> {
                // Suivi de progression
                if (attempts % 5 == 0) { // Toutes les 5 tentatives
                    System.out.println("Distance restante: " + String.format("%.2f", distance) + 
                                     " blocs (tentative " + attempts + ")");
                }
                
                // Condition d'arrêt personnalisée
                if (attempts > 20 && distance > 50) {
                    System.out.println("Le villageois est trop loin, arrêt du mouvement");
                    // Le timeout va gérer l'arrêt automatiquement
                }
            })
            .onSuccess(() -> {
                System.out.println("Déplacement terminé avec succès !");
            })
            .onFailure(() -> {
                System.out.println("Déplacement échoué (timeout ou erreur)");
            })
            .start();
    }

    /**
     * Exemple 4: Migration d'ancien code de boucle répétitive
     */
    public static void migratedFromOldSystemExample(Villager villager, Location target) {
        // ANCIEN SYSTÈME (à éviter):
        /*
        UUID taskId = UUID.randomUUID();
        TestJava.threads.put(taskId, Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            attempts++;
            if (attempts > maxAttempts) {
                cleanup();
                return;
            }
            if (villager.isDead()) {
                cleanup();
                return;
            }
            villager.getPathfinder().moveTo(target, 1.0);
            if (villager.getLocation().distance(target) <= 3.0) {
                performAction();
                cleanup();
            }
        }, 0, 20));
        */

        // NOUVEAU SYSTÈME (recommandé):
        VillagerMovementManager.moveVillager(villager, target)
            .withSuccessDistance(3.0)
            .withMoveSpeed(1.0)
            .withTimeout(60)
            .withName("MigratedTask")
            .onSuccess(() -> {
                performAction();
            })
            .onFailure(() -> {
                handleFailure();
            })
            .start();
    }

    /**
     * Exemple 5: Gestion avancée avec annulation
     */
    public static void advancedManagementExample(Villager villager, Location destination) {
        // Annuler tout mouvement existant pour ce villageois
        VillagerMovementManager.cancelMovementForVillager(villager);
        
        // Démarrer un nouveau mouvement avec ID pour suivi
        UUID taskId = VillagerMovementManager.moveVillager(villager, destination)
            .withName("AdvancedTask")
            .withoutRetry() // Pas de relance automatique du pathfinding
            .onSuccess(() -> {
                System.out.println("Tâche avancée terminée !");
            })
            .start();
        
        // Exemple d'annulation conditionnelle plus tard
        scheduledCancellationExample(taskId, villager);
    }

    /**
     * Exemple 6: Utilisation des méthodes de convenance
     */
    public static void convenienceMethodsExample(Villager villager, Location destination) {
        // Déplacement simple
        UUID taskId1 = VillagerMovementManager.moveToLocation(villager, destination, "ConvenienceTask");
        
        // Déplacement avec action
        VillagerMovementManager.moveToLocationAndExecute(villager, destination, 
            () -> System.out.println("Action exécutée !"), 
            "ConvenienceWithAction");
    }
    
    /**
     * Exemple 7: Gestion avancée des conflits et validation
     */
    public static void conflictManagementExample(Villager villager, Location destination) {
        // Vérifier si le villageois est déjà en mouvement
        if (VillagerMovementManager.isMoving(villager)) {
            System.out.println("Villageois déjà en mouvement, annulation du mouvement actuel...");
            // Le nouveau mouvement annulera automatiquement l'ancien
        }
        
        // Démarrer un mouvement avec validation intégrée
        UUID taskId = VillagerMovementManager.moveVillager(villager, destination)
            .withName("ConflictAwareTask")
            .withTimeout(45)
            .onSuccess(() -> {
                System.out.println("Mouvement réussi sans conflit !");
            })
            .onFailure(() -> {
                System.out.println("Mouvement échoué - vérification des conflits...");
                // Le système a automatiquement validé et géré les conflits
                checkForMovementIssues(villager);
            })
            .onPositionUpdate((distance, attempts) -> {
                // Monitoring pour détecter les conflits potentiels
                if (attempts > 10 && distance > 20) {
                    System.out.println("Possible conflit détecté - distance: " + 
                                     String.format("%.2f", distance));
                }
            })
            .start();
        
        // Si le mouvement a échoué à la validation (retourne null)
        if (taskId == null) {
            System.out.println("Mouvement rejeté par la validation - villageois ou destination invalide");
        }
    }
    
    /**
     * Exemple 8: Libération forcée et debug
     */
    public static void debugAndReleaseExample(Villager villager) {
        // Obtenir des informations de debug
        String debugInfo = VillagerMovementManager.getDebugInfo();
        System.out.println(debugInfo);
        
        // Libération forcée en cas de problème
        VillagerMovementManager.forceReleaseVillager(villager, "Debug manual release");
        
        // Vérifier l'état après libération
        boolean stillMoving = VillagerMovementManager.isMoving(villager);
        System.out.println("Villageois encore en mouvement après libération: " + stillMoving);
    }

    // Méthodes d'aide pour les exemples
    private static void performPlayerInteraction(Villager villager, Player player) {
        // Logique d'interaction personnalisée
    }

    private static void performAction() {
        // Action à effectuer
    }

    private static void handleFailure() {
        // Gestion d'échec
    }
    
    private static void checkForMovementIssues(Villager villager) {
        // Vérification des problèmes de mouvement
        if (villager.isDead()) {
            System.out.println("Villageois mort - cause de l'échec du mouvement");
        } else if (!villager.isValid()) {
            System.out.println("Villageois invalide - entité corrompue");
        } else {
            System.out.println("Villageois valide - échec dû à timeout ou pathfinding");
        }
    }

    private static void scheduledCancellationExample(UUID taskId, Villager villager) {
        // Exemple : annuler la tâche après 10 secondes si une condition est remplie
        // (Dans un vrai cas d'usage, ceci serait dans un scheduler)
        new Thread(() -> {
            try {
                Thread.sleep(10000); // 10 secondes
                if (someCustomCondition()) {
                    boolean cancelled = VillagerMovementManager.cancelMovement(taskId);
                    if (cancelled) {
                        System.out.println("Tâche annulée après 10 secondes");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static boolean someCustomCondition() {
        // Logique de condition personnalisée
        return false;
    }
}
