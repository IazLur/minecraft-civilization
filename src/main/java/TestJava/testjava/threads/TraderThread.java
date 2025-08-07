package TestJava.testjava.threads;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TraderThread implements Runnable {

    Material[] buys = new Material[]{
            Material.DIAMOND,
            Material.GOLD_BLOCK,
            Material.IRON_BLOCK,
            Material.TNT,
            Material.COPPER_BLOCK,
    };

    // Système de tracking des marchands actifs
    private static final Map<UUID, Long> activeTraders = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> traderLlamas = new ConcurrentHashMap<>();
    private static final long TRADER_LIFETIME = 5 * 60 * 1000; // 5 minutes en millisecondes

    @Override
    public void run() {
        // D'abord, nettoyer les marchands expirés
        cleanupExpiredTraders();
        
        Random rand = new Random();
        Collection<VillageModel> villages = VillageRepository.getAll();

        for (VillageModel village : villages) {
            if (rand.nextInt(45) > 1) {
                continue;
            }
            Bukkit.getServer().broadcastMessage(ChatColor.GRAY + "Un marchand spécial est apparu à " +
                    Colorize.name(village.getId()) + " pour 5 minutes");
            List<MerchantRecipe> trader = new ArrayList<>();
            WanderingTrader wander = TestJava.world.spawn(VillageRepository.getBellLocation(village), WanderingTrader.class);
            TraderLlama llama1 = TestJava.world.spawn(VillageRepository.getBellLocation(village), TraderLlama.class);
            TraderLlama llama2 = TestJava.world.spawn(VillageRepository.getBellLocation(village), TraderLlama.class);
            llama1.setLeashHolder(wander);
            llama2.setLeashHolder(wander);
            
            // Configuration du marchand
            MerchantRecipe trade = new MerchantRecipe(new ItemStack(buys[rand.nextInt(buys.length)]), 1);
            ItemStack ing = new ItemStack(Material.EMERALD);
            ing.setAmount(rand.nextInt(5) + 4);
            trade.addIngredient(ing);
            trade.setMaxUses(6);
            trade.setDemand(6);
            trade.setIgnoreDiscounts(true);
            trade.setExperienceReward(false);
            trader.add(trade);
            wander.setRecipes(trader);
            wander.setDespawnDelay(20 * 60 * 5);
            wander.getTrackedPlayers().removeAll(wander.getTrackedPlayers());
            wander.getTrackedPlayers().add(Bukkit.getPlayer(village.getPlayerName()));
            
            // Enregistrer dans le système de tracking
            long currentTime = System.currentTimeMillis();
            activeTraders.put(wander.getUniqueId(), currentTime);
            
            Set<UUID> llamaSet = new HashSet<>();
            llamaSet.add(llama1.getUniqueId());
            llamaSet.add(llama2.getUniqueId());
            traderLlamas.put(wander.getUniqueId(), llamaSet);
            
            // Programmer la suppression automatique après 5 minutes + marge de sécurité
            Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
                removeTraderGroup(wander.getUniqueId());
            }, 20 * 60 * 5 + 20 * 10); // 5 minutes + 10 secondes de marge
        }
    }

    /**
     * Nettoie les marchands expirés basé sur le temps et les entités mortes/supprimées
     */
    private static void cleanupExpiredTraders() {
        long currentTime = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, Long> entry : activeTraders.entrySet()) {
            UUID traderId = entry.getKey();
            long creationTime = entry.getValue();

            // Vérifier si le marchand a expiré (+ 30 secondes de marge)
            if (currentTime - creationTime > TRADER_LIFETIME + 30000) {
                toRemove.add(traderId);
                continue;
            }

            // Vérifier si l'entité existe encore
            Entity trader = Bukkit.getEntity(traderId);
            if (trader == null || trader.isDead()) {
                toRemove.add(traderId);
            }
        }

        // Supprimer les marchands expirés
        for (UUID traderId : toRemove) {
            removeTraderGroup(traderId);
        }

        if (!toRemove.isEmpty()) {
            Bukkit.getLogger().info("[TraderThread] Nettoyage automatique : " + toRemove.size() + " groupes de marchands supprimés");
        }
    }

    /**
     * Supprime un groupe de marchand (trader + ses lamas) et les retire du tracking
     */
    private static void removeTraderGroup(UUID traderId) {
        try {
            // Supprimer le marchand
            Entity trader = Bukkit.getEntity(traderId);
            if (trader != null && !trader.isDead()) {
                trader.remove();
            }

            // Supprimer les lamas associés
            Set<UUID> llamas = traderLlamas.get(traderId);
            if (llamas != null) {
                for (UUID llamaId : llamas) {
                    Entity llama = Bukkit.getEntity(llamaId);
                    if (llama != null && !llama.isDead()) {
                        llama.remove();
                    }
                }
            }

            // Retirer du tracking
            activeTraders.remove(traderId);
            traderLlamas.remove(traderId);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[TraderThread] Erreur lors de la suppression du groupe marchand " + traderId + ": " + e.getMessage());
            // Nettoyer quand même le tracking même en cas d'erreur
            activeTraders.remove(traderId);
            traderLlamas.remove(traderId);
        }
    }

    /**
     * Méthode publique pour nettoyer tous les marchands actifs (utilisée au redémarrage)
     */
    public static void cleanupAllActiveTraders() {
        List<UUID> allTraders = new ArrayList<>(activeTraders.keySet());
        for (UUID traderId : allTraders) {
            removeTraderGroup(traderId);
        }
        Bukkit.getLogger().info("[TraderThread] Nettoyage complet : tous les marchands actifs supprimés");
    }

    /**
     * Retourne le nombre de marchands actuellement actifs
     */
    public static int getActiveTraderCount() {
        return activeTraders.size();
    }
}
