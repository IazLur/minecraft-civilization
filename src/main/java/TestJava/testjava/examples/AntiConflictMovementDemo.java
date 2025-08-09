package TestJava.testjava.examples;

import TestJava.testjava.services.VillagerMovementManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;
import TestJava.testjava.TestJava;

import java.util.UUID;

/**
 * Démonstration pratique du système anti-conflit de déplacement
 * 
 * Cette classe montre comment utiliser le nouveau système centralisé
 * avec gestion automatique des conflits pour des cas d'usage réels.
 */
public class AntiConflictMovementDemo {
    
    /**
     * Démonstration complète : Villageois qui doit aller chercher des ressources
     * puis revenir, avec gestion des interruptions et conflits
     */
    public static void demonstrateResourceCollection(Villager villager, Location resourceLocation, Location homeLocation) {
        System.out.println("=== Démonstration Collection de Ressources ===");
        
        // Étape 1: Aller aux ressources
        UUID collectTaskId = VillagerMovementManager.moveVillager(villager, resourceLocation)
            .withName("CollectResources_" + villager.getUniqueId().toString().substring(0, 8))
            .withSuccessDistance(2.0)
            .withTimeout(45)
            .onSuccess(() -> {
                System.out.println("✅ Arrivé aux ressources, début de la collecte...");
                
                // Simuler collecte de ressources (3 secondes)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        System.out.println("📦 Ressources collectées !");
                        
                        // Étape 2: Retour à la base
                        returnToBase(villager, homeLocation);
                    }
                }.runTaskLater(TestJava.plugin, 60); // 3 secondes
                
            })
            .onFailure(() -> {
                System.out.println("❌ Échec d'atteindre les ressources");
                handleCollectionFailure(villager, resourceLocation, homeLocation);
            })
            .onPositionUpdate((distance, attempts) -> {
                if (attempts % 10 == 0) { // Toutes les 10 tentatives (10 secondes)
                    System.out.println("🚶 Progression: " + String.format("%.1f", distance) + 
                                     " blocs restants (tentative " + attempts + ")");
                }
            })
            .start();
        
        if (collectTaskId == null) {
            System.out.println("❌ Villageois invalide pour la collecte de ressources");
        } else {
            System.out.println("🎯 Mission de collecte démarrée : " + collectTaskId);
        }
    }
    
    /**
     * Phase de retour avec gestion des conflits potentiels
     */
    private static void returnToBase(Villager villager, Location homeLocation) {
        System.out.println("🏠 Début du retour à la base...");
        
        UUID returnTaskId = VillagerMovementManager.moveVillager(villager, homeLocation)
            .withName("ReturnHome_" + villager.getUniqueId().toString().substring(0, 8))
            .withSuccessDistance(3.0)
            .withTimeout(60)
            .onSuccess(() -> {
                System.out.println("✅ Retour réussi ! Mission terminée.");
                System.out.println("📊 État final du système :");
                System.out.println(VillagerMovementManager.getDebugInfo());
            })
            .onFailure(() -> {
                System.out.println("❌ Échec du retour - libération du villageois...");
                VillagerMovementManager.forceReleaseVillager(villager, "Échec retour mission");
            })
            .start();
        
        if (returnTaskId != null) {
            System.out.println("🔄 Retour en cours : " + returnTaskId);
        }
    }
    
    /**
     * Gestion des échecs avec retry intelligent
     */
    private static void handleCollectionFailure(Villager villager, Location resourceLocation, Location homeLocation) {
        System.out.println("🔧 Analyse de l'échec...");
        
        // Vérifier l'état du villageois
        if (villager.isDead()) {
            System.out.println("💀 Villageois mort - mission abandonnée");
            return;
        }
        
        if (!villager.isValid()) {
            System.out.println("🚫 Villageois invalide - mission abandonnée");
            return;
        }
        
        // Calculer distance actuelle
        double currentDistance = villager.getLocation().distance(resourceLocation);
        System.out.println("📏 Distance actuelle aux ressources: " + String.format("%.1f", currentDistance) + " blocs");
        
        if (currentDistance < 10.0) {
            System.out.println("📍 Proche des ressources, tentative de collecte sur place...");
            // Simuler collecte directe puis retour
            new BukkitRunnable() {
                @Override
                public void run() {
                    System.out.println("📦 Collecte d'urgence effectuée !");
                    returnToBase(villager, homeLocation);
                }
            }.runTaskLater(TestJava.plugin, 40); // 2 secondes
            
        } else {
            System.out.println("🔄 Trop loin, nouvelle tentative de déplacement...");
            
            // Retry avec paramètres plus souples
            UUID retryTaskId = VillagerMovementManager.moveVillager(villager, resourceLocation)
                .withName("RetryCollect_" + villager.getUniqueId().toString().substring(0, 8))
                .withSuccessDistance(5.0) // Distance plus souple
                .withMoveSpeed(0.8) // Plus lent mais plus fiable
                .withTimeout(90) // Plus de temps
                .onSuccess(() -> {
                    System.out.println("✅ Retry réussi !");
                    returnToBase(villager, homeLocation);
                })
                .onFailure(() -> {
                    System.out.println("❌ Retry échoué - retour direct à la base");
                    returnToBase(villager, homeLocation);
                })
                .start();
            
            if (retryTaskId != null) {
                System.out.println("🔄 Retry en cours : " + retryTaskId);
            }
        }
    }
    
    /**
     * Démonstration de gestion d'une équipe de villageois
     * avec coordination et évitement de conflits
     */
    public static void demonstrateTeamMovement(Villager[] team, Location[] destinations) {
        System.out.println("=== Démonstration Mouvement d'Équipe ===");
        System.out.println("👥 Équipe de " + team.length + " villageois");
        
        for (int i = 0; i < team.length && i < destinations.length; i++) {
            final int index = i;
            final Villager villager = team[i];
            final Location destination = destinations[i];
            
            // Délai échelonné pour éviter les embouteillages
            new BukkitRunnable() {
                @Override
                public void run() {
                    UUID taskId = VillagerMovementManager.moveVillager(villager, destination)
                        .withName("TeamMember_" + (index + 1))
                        .withSuccessDistance(2.0)
                        .withTimeout(90)
                        .onSuccess(() -> {
                            System.out.println("✅ Membre " + (index + 1) + " arrivé à destination");
                            checkTeamCompletion(team, destinations);
                        })
                        .onFailure(() -> {
                            System.out.println("❌ Membre " + (index + 1) + " a échoué");
                            handleTeamMemberFailure(villager, index + 1);
                        })
                        .start();
                    
                    if (taskId != null) {
                        System.out.println("🎯 Membre " + (index + 1) + " en route : " + taskId);
                    }
                }
            }.runTaskLater(TestJava.plugin, i * 20); // 1 seconde de délai entre chaque
        }
    }
    
    /**
     * Vérification de completion d'équipe
     */
    private static void checkTeamCompletion(Villager[] team, Location[] destinations) {
        boolean allCompleted = true;
        
        for (Villager villager : team) {
            if (VillagerMovementManager.isMoving(villager)) {
                allCompleted = false;
                break;
            }
        }
        
        if (allCompleted) {
            System.out.println("🎉 Toute l'équipe est arrivée !");
            System.out.println("📊 Rapport final :");
            System.out.println(VillagerMovementManager.getDebugInfo());
        }
    }
    
    /**
     * Gestion d'échec d'un membre d'équipe
     */
    private static void handleTeamMemberFailure(Villager villager, int memberNumber) {
        System.out.println("🔧 Gestion échec membre " + memberNumber);
        
        // Libérer le villageois des conflits potentiels
        VillagerMovementManager.forceReleaseVillager(villager, 
            "Échec membre équipe " + memberNumber);
        
        // En mission réelle, on pourrait :
        // - Réassigner à une tâche différente
        // - Le renvoyer à la base
        // - L'exclure temporairement de l'équipe
    }
    
    /**
     * Test de stress : Beaucoup de mouvements simultanés
     */
    public static void stressTestMovements(Villager[] villagers, Location centralLocation) {
        System.out.println("=== Test de Stress - Mouvements Massifs ===");
        System.out.println("🔥 Démarrage de " + villagers.length + " mouvements simultanés");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < villagers.length; i++) {
            final int index = i;
            final Villager villager = villagers[i];
            
            UUID taskId = VillagerMovementManager.moveVillager(villager, centralLocation)
                .withName("StressTest_" + index)
                .withSuccessDistance(5.0)
                .withTimeout(120)
                .onSuccess(() -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    System.out.println("✅ StressTest_" + index + " terminé en " + elapsed + "ms");
                })
                .onFailure(() -> {
                    System.out.println("❌ StressTest_" + index + " échoué");
                })
                .start();
            
            if (taskId == null) {
                System.out.println("❌ StressTest_" + index + " rejeté par validation");
            }
        }
        
        System.out.println("📊 État après démarrage du stress test :");
        System.out.println(VillagerMovementManager.getDebugInfo());
    }
    
    /**
     * Démonstration d'interaction joueur pendant mouvement
     */
    public static void demonstratePlayerInteraction(Villager villager, Location destination, Player player) {
        System.out.println("=== Démonstration Interaction Joueur ===");
        
        UUID taskId = VillagerMovementManager.moveVillager(villager, destination)
            .withName("InteractionDemo")
            .withTimeout(60)
            .onSuccess(() -> {
                System.out.println("✅ Mouvement terminé sans interruption");
            })
            .onFailure(() -> {
                System.out.println("❌ Mouvement interrompu ou échoué");
            })
            .start();
        
        if (taskId != null) {
            System.out.println("🎯 Mouvement démarré : " + taskId);
            System.out.println("👆 Le joueur peut maintenant interagir avec le villageois");
            System.out.println("🔄 Le listener annulera automatiquement le mouvement pour permettre l'interaction");
        }
    }
}
