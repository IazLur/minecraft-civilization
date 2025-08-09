package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire centralisé pour tous les déplacements de villageois dans le plugin.
 * 
 * Ce service centralise la logique de déplacement qui était auparavant dispersée
 * dans plusieurs threads et services, utilisant des boucles répétitives avec 
 * scheduleSyncRepeatingTask pour forcer les villageois à atteindre leur destination.
 * 
 * Fonctionnalités :
 * - Gestion centralisée des déplacements avec callbacks
 * - Timeout et retry automatiques
 * - Vérification de proximité intelligente  
 * - Nettoyage automatique des tâches
 * - API simple et cohérente
 * 
 * Usage :
 * VillagerMovementManager.moveVillager(villager, targetLocation)
 *     .onSuccess(() -> performAction())
 *     .onFailure(() -> handleFailure())
 *     .withTimeout(30)
 *     .start();
 */
public class VillagerMovementManager {
    
    // Configuration par défaut
    private static final double DEFAULT_SUCCESS_DISTANCE = 3.0;
    private static final double DEFAULT_MOVE_SPEED = 1.0;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_CHECK_INTERVAL_TICKS = 20; // 1 seconde
    private static final int DEFAULT_RETRY_INTERVAL_TICKS = 20; // Relancer pathfinding toutes les secondes
    
    // Stockage des tâches actives pour nettoyage
    private static final Map<UUID, MovementTask> activeTasks = new ConcurrentHashMap<>();
    
    // NOUVEAU: Whitelist courte pour autoriser nos propres appels moveTo à passer le listener
    // key: villager UUID, value: expiryTimeMillis
    private static final Map<UUID, Long> pathfindingWhitelist = new ConcurrentHashMap<>();
    private static final int WHITELIST_DEFAULT_GRACE_TICKS = 5; // ~250ms
    
    private static void whitelistPathfinding(Villager villager, int graceTicks) {
        if (villager == null) return;
        long expiry = System.currentTimeMillis() + Math.max(1, graceTicks) * 50L;
        pathfindingWhitelist.put(villager.getUniqueId(), expiry);
    }
    
    public static boolean isPathfindingWhitelisted(Villager villager) {
        if (villager == null) return false;
        Long expiry = pathfindingWhitelist.get(villager.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() <= expiry) {
            return true;
        }
        // Expiré -> nettoyer
        pathfindingWhitelist.remove(villager.getUniqueId());
        return false;
    }

    /**
     * Interface pour les callbacks de succès et d'échec
     */
    public interface MovementCallback {
        void execute();
    }
    
    /**
     * Interface pour les callbacks de mise à jour de position
     */
    public interface PositionCallback {
        void execute(double distance, int attempts);
    }
    
    /**
     * Classe représentant une tâche de déplacement configurée
     */
    public static class MovementBuilder {
        private final Villager villager;
        private final Location targetLocation;
        private MovementCallback onSuccess;
        private MovementCallback onFailure;
        private PositionCallback onPositionUpdate;
        private double successDistance = DEFAULT_SUCCESS_DISTANCE;
        private double moveSpeed = DEFAULT_MOVE_SPEED;
        private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        private boolean enableRetry = true;
        private String taskName = "UnnamedMovement";
        
        public MovementBuilder(Villager villager, Location targetLocation) {
            this.villager = villager;
            this.targetLocation = targetLocation.clone();
        }
        
        public MovementBuilder onSuccess(MovementCallback callback) {
            this.onSuccess = callback;
            return this;
        }
        
        public MovementBuilder onFailure(MovementCallback callback) {
            this.onFailure = callback;
            return this;
        }
        
        public MovementBuilder onPositionUpdate(PositionCallback callback) {
            this.onPositionUpdate = callback;
            return this;
        }
        
        public MovementBuilder withSuccessDistance(double distance) {
            this.successDistance = distance;
            return this;
        }
        
        public MovementBuilder withMoveSpeed(double speed) {
            this.moveSpeed = speed;
            return this;
        }
        
        public MovementBuilder withTimeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }
        
        public MovementBuilder withoutRetry() {
            this.enableRetry = false;
            return this;
        }
        
        public MovementBuilder withName(String name) {
            this.taskName = name;
            return this;
        }
        
        /**
         * Démarre la tâche de déplacement
         * @return UUID de la tâche pour annulation possible
         */
        public UUID start() {
            // Validation préalable
            if (!validateMovementRequest()) {
                if (onFailure != null) {
                    onFailure.execute();
                }
                return null;
            }
            
            return startMovementTask(villager, targetLocation, onSuccess, onFailure, onPositionUpdate,
                                   successDistance, moveSpeed, timeoutSeconds, enableRetry, taskName);
        }
        
        /**
         * Valide que la demande de mouvement est viable
         * @return true si le mouvement peut être démarré, false sinon
         */
        private boolean validateMovementRequest() {
            Bukkit.getLogger().info("[VillagerMovement] DEBUG - Début validation pour tâche: " + taskName);
            
            // Vérifier que le villageois existe et est valide
            if (villager == null || villager.isDead() || !villager.isValid()) {
                Bukkit.getLogger().warning("[VillagerMovement] Tentative de déplacement d'un villageois invalide pour tâche: " + taskName);
                Bukkit.getLogger().info("[VillagerMovement] DEBUG - villager null: " + (villager == null) + ", dead: " + (villager != null && villager.isDead()) + ", valid: " + (villager != null && villager.isValid()));
                return false;
            }
            
            // Vérifier que la destination est valide
            if (targetLocation == null || targetLocation.getWorld() == null) {
                Bukkit.getLogger().warning("[VillagerMovement] Destination invalide pour tâche: " + taskName);
                Bukkit.getLogger().info("[VillagerMovement] DEBUG - targetLocation null: " + (targetLocation == null) + ", world null: " + (targetLocation != null && targetLocation.getWorld() == null));
                return false;
            }
            
            // Vérifier que le villageois et la destination sont dans le même monde
            if (villager.getWorld() != targetLocation.getWorld()) {
                Bukkit.getLogger().warning("[VillagerMovement] Villageois et destination dans des mondes différents pour tâche: " + taskName);
                Bukkit.getLogger().info("[VillagerMovement] DEBUG - Monde villageois: " + villager.getWorld().getName() + ", Monde destination: " + targetLocation.getWorld().getName());
                return false;
            }
            
            // Vérifier distance raisonnable (éviter les mouvements impossibles)
            double distance = villager.getLocation().distance(targetLocation);
            Bukkit.getLogger().info("[VillagerMovement] DEBUG - Distance calculée: " + String.format("%.2f", distance) + " blocs");
            if (distance > 1000) { // Plus de 1000 blocs = probablement une erreur
                Bukkit.getLogger().warning("[VillagerMovement] Distance excessive (" + String.format("%.1f", distance) + " blocs) pour tâche: " + taskName);
                return false;
            }
            
            Bukkit.getLogger().info("[VillagerMovement] DEBUG - Validation réussie pour tâche: " + taskName);
            return true;
        }
    }
    
    /**
     * Classe interne pour gérer une tâche de déplacement
     */
    private static class MovementTask extends BukkitRunnable {
        private final UUID taskId;
        private final Villager villager;
        private final Location targetLocation;
        private final MovementCallback onSuccess;
        private final MovementCallback onFailure;
        private final PositionCallback onPositionUpdate;
        private final double successDistance;
        private final double moveSpeed;
        private final int maxAttempts;
        private final boolean enableRetry;
        private final String taskName;
        
        private int attempts = 0;
        private int lastPathfindingTick = 0;
        
        public MovementTask(UUID taskId, Villager villager, Location targetLocation,
                           MovementCallback onSuccess, MovementCallback onFailure, PositionCallback onPositionUpdate,
                           double successDistance, double moveSpeed, int timeoutSeconds, 
                           boolean enableRetry, String taskName) {
            this.taskId = taskId;
            this.villager = villager;
            this.targetLocation = targetLocation;
            this.onSuccess = onSuccess;
            this.onFailure = onFailure;
            this.onPositionUpdate = onPositionUpdate;
            this.successDistance = successDistance;
            this.moveSpeed = moveSpeed;
            this.maxAttempts = (timeoutSeconds * 20) / DEFAULT_CHECK_INTERVAL_TICKS;
            this.enableRetry = enableRetry;
            this.taskName = taskName;
        }
        
        @Override
        public void run() {
            attempts++;
            
            // Vérifications de sécurité
            if (villager == null || villager.isDead() || !villager.isValid()) {
                completeTask(false, "Villager invalide ou mort");
                return;
            }
            
            // Timeout
            if (attempts > maxAttempts) {
                completeTask(false, "Timeout atteint après " + attempts + " tentatives");
                return;
            }
            
            // Réveiller si endormi
            if (villager.isSleeping()) {
                villager.wakeup();
            }
            
            // Calculer distance actuelle
            Location currentLocation = villager.getLocation();
            double distance = currentLocation.distance(targetLocation);
            
            // Vérifier si le villageois a atteint sa destination
            if (distance <= successDistance) {
                completeTask(true, "Destination atteinte (distance: " + String.format("%.2f", distance) + ")");
                return;
            }
            
            // Relancer le pathfinding périodiquement (par défaut chaque seconde)
            if (enableRetry && (attempts * DEFAULT_CHECK_INTERVAL_TICKS - lastPathfindingTick) >= DEFAULT_RETRY_INTERVAL_TICKS) {
                // Marquer ce pathfinding comme autorisé pour contourner le listener global
                whitelistPathfinding(villager, WHITELIST_DEFAULT_GRACE_TICKS);
                villager.getPathfinder().moveTo(targetLocation, moveSpeed);
                lastPathfindingTick = attempts * DEFAULT_CHECK_INTERVAL_TICKS;
            }
            
            // Callback de mise à jour de position si défini
            if (onPositionUpdate != null) {
                try {
                    onPositionUpdate.execute(distance, attempts);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[VillagerMovement] Erreur dans callback position pour " + taskName + ": " + e.getMessage());
                }
            }
        }
        
        private void completeTask(boolean success, String reason) {
            try {
                if (success && onSuccess != null) {
                    onSuccess.execute();
                } else if (!success && onFailure != null) {
                    onFailure.execute();
                }
                
                // Log de débogage
                Bukkit.getLogger().fine("[VillagerMovement] Tâche " + taskName + " terminée: " + 
                                       (success ? "SUCCÈS" : "ÉCHEC") + " - " + reason);
                
            } catch (Exception e) {
                Bukkit.getLogger().warning("[VillagerMovement] Erreur dans callback pour " + taskName + ": " + e.getMessage());
            } finally {
                // Nettoyage de la tâche
                activeTasks.remove(taskId);
                cancel();
            }
        }
    }
    
    /**
     * Point d'entrée principal pour créer un déplacement de villageois
     * 
     * @param villager Le villageois à déplacer
     * @param targetLocation La destination
     * @return Builder pour configurer le déplacement
     */
    public static MovementBuilder moveVillager(Villager villager, Location targetLocation) {
        if (villager == null || targetLocation == null) {
            throw new IllegalArgumentException("Villager et targetLocation ne peuvent pas être null");
        }
        return new MovementBuilder(villager, targetLocation);
    }
    
    /**
     * Démarre une tâche de déplacement avec tous les paramètres
     */
    private static UUID startMovementTask(Villager villager, Location targetLocation,
                                        MovementCallback onSuccess, MovementCallback onFailure, PositionCallback onPositionUpdate,
                                        double successDistance, double moveSpeed, int timeoutSeconds,
                                        boolean enableRetry, String taskName) {
        UUID taskId = UUID.randomUUID();
        
        // Annuler toute tâche existante pour ce villageois
        cancelMovementForVillager(villager);
        
        // Démarrage initial du pathfinding
        whitelistPathfinding(villager, WHITELIST_DEFAULT_GRACE_TICKS);
        villager.getPathfinder().moveTo(targetLocation, moveSpeed);
        
        // Créer et démarrer la tâche
        MovementTask task = new MovementTask(taskId, villager, targetLocation, onSuccess, onFailure, onPositionUpdate,
                                           successDistance, moveSpeed, timeoutSeconds, enableRetry, taskName);
        
        activeTasks.put(taskId, task);
        
        // Démarrer la tâche avec l'intervalle de vérification par défaut
        task.runTaskTimer(TestJava.plugin, DEFAULT_CHECK_INTERVAL_TICKS, DEFAULT_CHECK_INTERVAL_TICKS);
        
        Bukkit.getLogger().fine("[VillagerMovement] Démarrage tâche " + taskName + " pour villageois " + villager.getUniqueId());
        
        return taskId;
    }
    
    /**
     * Annule une tâche de déplacement spécifique
     * 
     * @param taskId UUID de la tâche à annuler
     * @return true si la tâche a été trouvée et annulée
     */
    public static boolean cancelMovement(UUID taskId) {
        MovementTask task = activeTasks.remove(taskId);
        if (task != null) {
            task.cancel();
            Bukkit.getLogger().fine("[VillagerMovement] Tâche " + taskId + " annulée");
            return true;
        }
        return false;
    }
    
    /**
     * Annule toutes les tâches de déplacement pour un villageois spécifique
     * 
     * @param villager Le villageois pour lequel annuler les déplacements
     * @return Nombre de tâches annulées
     */
    public static int cancelMovementForVillager(Villager villager) {
        if (villager == null) return 0;
        
        UUID villagerUUID = villager.getUniqueId();
        int cancelled = 0;
        
        for (Map.Entry<UUID, MovementTask> entry : activeTasks.entrySet()) {
            MovementTask task = entry.getValue();
            if (task.villager != null && villagerUUID.equals(task.villager.getUniqueId())) {
                task.cancel();
                activeTasks.remove(entry.getKey());
                cancelled++;
            }
        }
        
        if (cancelled > 0) {
            // Arrêter le pathfinding actuel
            villager.getPathfinder().stopPathfinding();
            Bukkit.getLogger().fine("[VillagerMovement] " + cancelled + " tâche(s) annulée(s) pour villageois " + villagerUUID);
        }
        
        return cancelled;
    }
    
    /**
     * Annule toutes les tâches de déplacement actives
     */
    public static void cancelAllMovements() {
        int cancelled = activeTasks.size();
        
        for (MovementTask task : activeTasks.values()) {
            task.cancel();
        }
        
        activeTasks.clear();
        
        if (cancelled > 0) {
            Bukkit.getLogger().info("[VillagerMovement] " + cancelled + " tâche(s) de déplacement annulée(s)");
        }
    }
    
    /**
     * Vérifie si un villageois est actuellement en mouvement via ce gestionnaire
     * @param villager Le villageois à vérifier
     * @return true si le villageois a un mouvement actif, false sinon
     */
    public static boolean isMoving(Villager villager) {
        if (villager == null) return false;
        
        UUID villagerUUID = villager.getUniqueId();
        
        for (MovementTask task : activeTasks.values()) {
            if (task.villager != null && villagerUUID.equals(task.villager.getUniqueId())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Retourne le nombre de tâches de déplacement actives
     * @return Nombre de déplacements en cours
     */
    public static int getActiveMovementCount() {
        return activeTasks.size();
    }
    
    /**
     * Vérifie si un villageois a une tâche de déplacement active
     */
    public static boolean hasActiveMovement(Villager villager) {
        if (villager == null) return false;
        
        UUID villagerUUID = villager.getUniqueId();
        return activeTasks.values().stream()
                .anyMatch(task -> task.villager != null && villagerUUID.equals(task.villager.getUniqueId()));
    }
    
    /**
     * Résout les conflits de pathfinding pour un villageois
     * Cette méthode est appelée par le listener pour gérer les conflits
     * 
     * @param villager Le villageois en conflit
     * @param conflictSource Source du conflit (pour debug)
     * @return true si un conflit a été résolu, false sinon
     */
    public static boolean resolvePathfindingConflict(Villager villager, String conflictSource) {
        if (villager == null) return false;
        
        // Si le pathfinding courant est whiteliste par notre gestionnaire, ne pas bloquer
        if (isPathfindingWhitelisted(villager)) {
            return false; // pas de conflit à résoudre
        }
        
        // Vérifier si ce villageois est sous notre contrôle
        if (isMoving(villager)) {
            // Arrêter le pathfinding naturel pour éviter les conflits
            villager.getPathfinder().stopPathfinding();
            
            // Log pour debug si nécessaire
            Bukkit.getLogger().fine(String.format(
                "[VillagerMovement] Conflit résolu pour %s - Source: %s", 
                villager.getUniqueId().toString().substring(0, 8), 
                conflictSource
            ));
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Méthode d'urgence pour forcer l'arrêt de tous les pathfindings
     * et remettre le pathfinding naturel sur un villageois
     * 
     * @param villager Le villageois à libérer
     * @param reason Raison de la libération (pour debug)
     */
    public static void forceReleaseVillager(Villager villager, String reason) {
        if (villager == null) return;
        
        // Annuler toutes les tâches pour ce villageois
        int cancelled = cancelMovementForVillager(villager);
        
        // S'assurer que le pathfinding est arrêté
        villager.getPathfinder().stopPathfinding();
        
        if (cancelled > 0) {
            Bukkit.getLogger().info(String.format(
                "[VillagerMovement] Villageois %s libéré forcément - Raison: %s (%d tâche(s) annulée(s))", 
                villager.getUniqueId().toString().substring(0, 8), 
                reason, 
                cancelled
            ));
        }
    }
    
    /**
     * Statistiques pour debug et monitoring
     * @return Informations sur l'état actuel du gestionnaire
     */
    public static String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== VillagerMovementManager Debug Info ===\n");
        info.append("Tâches actives: ").append(activeTasks.size()).append("\n");
        
        if (!activeTasks.isEmpty()) {
            info.append("Détail des tâches:\n");
            for (Map.Entry<UUID, MovementTask> entry : activeTasks.entrySet()) {
                MovementTask task = entry.getValue();
                info.append("  - ").append(task.taskName)
                    .append(" | Villageois: ").append(task.villager.getUniqueId().toString().substring(0, 8))
                    .append(" | Tentatives: ").append(task.attempts)
                    .append("\n");
            }
        }
        
        return info.toString();
    }
    
    /**
     * Méthodes de convenance pour les cas d'usage courants
     */
    
    /**
     * Déplacement simple vers une location
     */
    public static UUID moveToLocation(Villager villager, Location target, String taskName) {
        return moveVillager(villager, target)
                .withName(taskName)
                .start();
    }
    
    /**
     * Déplacement avec action à l'arrivée
     */
    public static UUID moveToLocationAndExecute(Villager villager, Location target, 
                                              Runnable onArrival, String taskName) {
        return moveVillager(villager, target)
                .onSuccess(onArrival::run)
                .withName(taskName)
                .start();
    }
    
    /**
     * Déplacement vers une entité avec suivi
     */
    public static UUID moveToEntity(Villager villager, Entity target, 
                                   Runnable onSuccess, Runnable onFailure, String taskName) {
        return moveVillager(villager, target.getLocation())
                .onSuccess(onSuccess::run)
                .onFailure(onFailure::run)
                .onPositionUpdate((distance, attempts) -> {
                    // Mettre à jour la destination si l'entité a bougé
                    if (target.isValid() && !target.isDead()) {
                        // Note: Cette logique peut être améliorée pour vraiment suivre l'entité
                        // Pour l'instant on garde la position initiale
                    }
                })
                .withName(taskName)
                .start();
    }
}
