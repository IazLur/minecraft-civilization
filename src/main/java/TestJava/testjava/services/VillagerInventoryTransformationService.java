package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.helpers.JuridictionHelper;
import TestJava.testjava.models.ResourceModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.ResourceRepository;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/**
 * Service pour gérer les transformations automatiques d'inventaire des villageois
 * et la vente à la banque mondiale
 */
public class VillagerInventoryTransformationService {

    /**
     * Exécute le processus de transformation et vente pour tous les villageois
     * Cette méthode est appelée toutes les minutes
     */
    public static void executeInventoryTransformationCycle() {
        if (TestJava.world == null) {
            Bukkit.getLogger().warning("[InventoryTransformation] Monde non disponible");
            return;
        }

        int totalWheatTransformed = 0;
        int totalBreadTransformed = 0;
        int totalHayBlocksSold = 0;
        int villagersProcessed = 0;

        try {
            // Récupérer tous les villageois
            Collection<VillagerModel> allVillagers = VillagerRepository.getAll();

            for (VillagerModel villagerModel : allVillagers) {
                Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villagerModel.getId());
                if (entity == null || entity.isDead()) {
                    continue; // Villageois inexistant ou mort
                }

                // Traiter l'inventaire de ce villageois
                TransformationResult result = processVillagerInventory(villagerModel, entity);
                
                if (result.hasTransformations()) {
                    villagersProcessed++;
                    totalWheatTransformed += result.wheatTransformed;
                    totalBreadTransformed += result.breadTransformed;
                    totalHayBlocksSold += result.hayBlocksSold;
                }
            }

            // Log de résumé unique
            if (villagersProcessed > 0) {
                Bukkit.getLogger().info("[InventoryTransformation] ✅ Résumé: " + villagersProcessed + " villageois traités " +
                                       "(blé→pain: " + totalWheatTransformed + ", pain→foin: " + totalBreadTransformed + 
                                       ", foin vendus: " + totalHayBlocksSold + ")");
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[InventoryTransformation] Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Traite l'inventaire d'un villageois : transformations et vente
     */
    private static TransformationResult processVillagerInventory(VillagerModel villagerModel, Villager entity) {
        TransformationResult result = new TransformationResult();
        Inventory inventory = entity.getInventory();

        // 1. Transformer 3 blés en 1 pain
        result.wheatTransformed = transformWheatToBread(inventory);

        // 2. Transformer 3 pains en 1 bloc de foin
        result.breadTransformed = transformBreadToHayBlock(inventory);

        // 3. Vendre les blocs de foin à la banque mondiale
        result.hayBlocksSold = sellHayBlocksToWorldBank(villagerModel, entity, inventory);

        return result;
    }

    /**
     * Transforme 3 blés en 1 pain autant que possible
     */
    private static int transformWheatToBread(Inventory inventory) {
        int wheatCount = getItemCount(inventory, Material.WHEAT);
        int transformations = wheatCount / 3;

        if (transformations > 0) {
            // Retirer 3x transformations de blé
            removeItems(inventory, Material.WHEAT, transformations * 3);
            
            // Ajouter transformations de pain
            inventory.addItem(new ItemStack(Material.BREAD, transformations));
        }

        return transformations;
    }

    /**
     * Transforme 3 pains en 1 bloc de foin autant que possible
     */
    private static int transformBreadToHayBlock(Inventory inventory) {
        int breadCount = getItemCount(inventory, Material.BREAD);
        int transformations = breadCount / 3;

        if (transformations > 0) {
            // Retirer 3x transformations de pain
            removeItems(inventory, Material.BREAD, transformations * 3);
            
            // Ajouter transformations de bloc de foin
            inventory.addItem(new ItemStack(Material.HAY_BLOCK, transformations));
        }

        return transformations;
    }

    /**
     * Vend tous les blocs de foin à la banque mondiale
     */
    private static int sellHayBlocksToWorldBank(VillagerModel villagerModel, Villager entity, Inventory inventory) {
        int hayBlockCount = getItemCount(inventory, Material.HAY_BLOCK);
        
        if (hayBlockCount == 0) {
            return 0;
        }

        // Récupérer la ressource "HAY_BLOCK" de la banque mondiale
        ResourceModel hayResource = findResourceByName("HAY_BLOCK");
        if (hayResource == null) {
            Bukkit.getLogger().warning("[InventoryTransformation] Ressource HAY_BLOCK introuvable dans la banque mondiale");
            return 0;
        }

        // Calculer le prix de vente (même logique que MarketCommand)
        float pricePerBlock = JuridictionHelper.calculatePriceForSell(hayResource);
        float totalEarnings = pricePerBlock * hayBlockCount;
        totalEarnings = Math.round(totalEarnings * 100.0f) / 100.0f;

        // Retirer tous les blocs de foin de l'inventaire
        removeItems(inventory, Material.HAY_BLOCK, hayBlockCount);

        // Donner l'argent au villageois
        villagerModel.setRichesse(villagerModel.getRichesse() + totalEarnings);
        VillagerRepository.update(villagerModel);

        // Mettre à jour la banque mondiale
        hayResource.setQuantity(hayResource.getQuantity() + hayBlockCount);
        ResourceRepository.update(hayResource);

        // Afficher le message coloré de vente
        String villagerName = extractVillagerName(entity);
        Bukkit.getServer().broadcastMessage(
            Colorize.name(villagerName) + " a vendu " + Colorize.name(hayBlockCount + " foin") + 
            " à la " + Colorize.name("banque mondiale") + " pour " + Colorize.name(totalEarnings + "µ")
        );

        return hayBlockCount;
    }

    /**
     * Compte le nombre d'items d'un type donné dans l'inventaire
     */
    private static int getItemCount(Inventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Retire un nombre spécifique d'items d'un type donné de l'inventaire
     */
    private static void removeItems(Inventory inventory, Material material, int amountToRemove) {
        int remaining = amountToRemove;
        
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int currentAmount = item.getAmount();
                
                if (currentAmount <= remaining) {
                    // Retirer complètement cet item
                    remaining -= currentAmount;
                    inventory.remove(item);
                } else {
                    // Retirer partiellement
                    item.setAmount(currentAmount - remaining);
                    remaining = 0;
                }
            }
        }
    }

    /**
     * Extrait le nom d'affichage du villageois
     */
    private static String extractVillagerName(Villager villager) {
        if (villager.getCustomName() == null) {
            return "Un villageois";
        }
        
        String customName = villager.getCustomName();
        String cleanName = org.bukkit.ChatColor.stripColor(customName);
        
        // Format: "{classe} [Village] Prénom Nom"
        int bracketEnd = cleanName.indexOf(']');
        if (bracketEnd != -1 && bracketEnd + 2 < cleanName.length()) {
            return cleanName.substring(bracketEnd + 2).trim();
        }
        
        return cleanName;
    }

    /**
     * Trouve une ressource par son nom dans la banque mondiale
     */
    private static ResourceModel findResourceByName(String resourceName) {
        Collection<ResourceModel> allResources = ResourceRepository.getAll();
        for (ResourceModel resource : allResources) {
            if (resource.getName().equals(resourceName)) {
                return resource;
            }
        }
        return null;
    }

    /**
     * Classe pour stocker les résultats de transformation d'un villageois
     */
    private static class TransformationResult {
        int wheatTransformed = 0;
        int breadTransformed = 0;
        int hayBlocksSold = 0;

        boolean hasTransformations() {
            return wheatTransformed > 0 || breadTransformed > 0 || hayBlocksSold > 0;
        }
    }
}
