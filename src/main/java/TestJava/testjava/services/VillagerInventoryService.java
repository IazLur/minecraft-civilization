package TestJava.testjava.services;

import TestJava.testjava.TestJava;

import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillagerRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Service pour gérer l'inventaire des villageois et les échanges de nourriture
 */
public class VillagerInventoryService {

    public enum FeedResult {
        SELF_FED,      // Autosuffisant (mangé de son inventaire)
        BOUGHT_FOOD,   // Client (acheté auprès d'un fermier) 
        FAILED         // Échec
    }

    // Valeurs nutritionnelles et prix des aliments
    private static final Map<Material, Integer> FOOD_VALUES = new HashMap<>() {{
        put(Material.WHEAT, 1);      // +1 nourriture, 1µ
        put(Material.BREAD, 3);      // +3 nourriture, 3µ  
        put(Material.HAY_BLOCK, 9);  // +9 nourriture, 9µ
    }};

    private static final Map<Material, Float> FOOD_PRICES = new HashMap<>() {{
        put(Material.WHEAT, 1.0f);
        put(Material.BREAD, 3.0f);
        put(Material.HAY_BLOCK, 9.0f);
    }};

    // Paramètres de déplacement
    private static final int RANGE_DELAY = 20 * 10;
    private static final double MOVE_SPEED = 1.0;
    private static final double INTERACTION_DISTANCE = 3.0;

    /**
     * Tente de nourrir un villageois affamé en utilisant son inventaire ou en achetant de la nourriture
     * @param hungryVillager Le villageois affamé
     * @return FeedResult indiquant le type d'action effectuée
     */
    public static FeedResult attemptToFeedVillager(VillagerModel hungryVillager) {
        Villager entity = (Villager) TestJava.plugin.getServer().getEntity(hungryVillager.getId());
        if (entity == null) {
            return FeedResult.FAILED; // Villageois fantôme
        }

        // 1. Essayer de consommer depuis son propre inventaire
        if (consumeFromOwnInventory(hungryVillager, entity)) {
            return FeedResult.SELF_FED;
        }

        // 2. Essayer d'acheter de la nourriture auprès d'un fermier
        if (buyFoodFromFarmer(hungryVillager, entity)) {
            return FeedResult.BOUGHT_FOOD;
        }

        return FeedResult.FAILED;
    }

    /**
     * Tente de consommer de la nourriture depuis l'inventaire du villageois
     */
    private static boolean consumeFromOwnInventory(VillagerModel villager, Villager entity) {
        Inventory inventory = entity.getInventory();

        // Chercher dans l'ordre de priorité : HAY_BLOCK > BREAD > WHEAT
        for (Material foodType : new Material[]{Material.HAY_BLOCK, Material.BREAD, Material.WHEAT}) {
            if (inventory.contains(foodType)) {
                ItemStack foodItem = inventory.getItem(inventory.first(foodType));
                if (foodItem != null && foodItem.getAmount() > 0) {
                    
                    // Consommer l'item
                    foodItem.setAmount(foodItem.getAmount() - 1);
                    if (foodItem.getAmount() == 0) {
                        inventory.remove(foodItem);
                    }

                    // Ajouter la nourriture au villageois
                    int nutritionValue = FOOD_VALUES.get(foodType);
                    villager.setFood(villager.getFood() + nutritionValue);
                    VillagerRepository.update(villager);

                    // Enregistrer la consommation dans l'historique
                    HistoryService.recordFoodConsumption(villager, HistoryService.getFoodDisplayName(foodType));

                    // Évaluer la classe sociale
                    SocialClassService.evaluateAndUpdateSocialClass(villager);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Tente d'acheter de la nourriture auprès du fermier le plus proche
     * Le villageois se déplace physiquement vers le fermier avant d'acheter
     */
    private static boolean buyFoodFromFarmer(VillagerModel hungryVillager, Villager hungryEntity) {
        Villager nearestFarmer = findNearestFarmerWithFood(hungryEntity);
        if (nearestFarmer == null) {
            return false; // Aucun fermier avec nourriture trouvé
        }

        VillagerModel farmerModel = VillagerRepository.find(nearestFarmer.getUniqueId());
        if (farmerModel == null) {
            return false;
        }

        // Chercher quel type de nourriture le fermier peut vendre (priorité : HAY > BREAD > WHEAT)
        Material foodToBuy = null;
        Inventory farmerInventory = nearestFarmer.getInventory();

        for (Material foodType : new Material[]{Material.HAY_BLOCK, Material.BREAD, Material.WHEAT}) {
            if (farmerInventory.contains(foodType)) {
                ItemStack foodItem = farmerInventory.getItem(farmerInventory.first(foodType));
                if (foodItem != null && foodItem.getAmount() > 0) {
                    foodToBuy = foodType;
                    break;
                }
            }
        }

        if (foodToBuy == null) {
            return false; // Le fermier n'a plus de nourriture
        }

        float price = FOOD_PRICES.get(foodToBuy);

        // Vérifier si le villageois affamé a assez d'argent
        if (hungryVillager.getRichesse() < price) {
            return false; // Pas assez d'argent
        }

        // NOUVEAU: Déplacer le villageois vers le fermier avant la transaction
        startMovementToFarmer(hungryVillager, farmerModel, hungryEntity, nearestFarmer, foodToBuy, price);
        return true; // La transaction aura lieu après le déplacement
    }

    /**
     * Démarre le déplacement du villageois vers le fermier pour effectuer la transaction
     * NOUVEAU: Utilise VillagerMovementManager pour centraliser la logique de déplacement
     */
    private static void startMovementToFarmer(VillagerModel buyer, VillagerModel seller, 
                                            Villager buyerEntity, Villager sellerEntity, 
                                            Material foodType, float price) {
        
        Random rand = new Random();
        int delay = rand.nextInt(RANGE_DELAY);
        
        // Attendre un délai aléatoire avant de commencer le déplacement
        Bukkit.getScheduler().runTaskLater(TestJava.plugin, () -> {
            
            // Vérifications de sécurité avant de commencer
            if (buyerEntity == null || sellerEntity == null || buyerEntity.isDead() || sellerEntity.isDead()) {
                return;
            }
            
            if (!hasSpecificFood(sellerEntity, foodType)) {
                return; // Le fermier n'a plus la nourriture
            }
            
            // Utiliser le gestionnaire centralisé pour le déplacement
            VillagerMovementManager.moveVillager(buyerEntity, sellerEntity.getLocation())
                .withSuccessDistance(INTERACTION_DISTANCE)
                .withMoveSpeed(MOVE_SPEED)
                .withTimeout(30) // 30 secondes maximum
                .withName("VillagerFoodPurchase")
                .onSuccess(() -> {
                    // Transaction à l'arrivée
                    if (performFoodTransaction(buyer, seller, buyerEntity, sellerEntity, foodType, price)) {
                        // Transaction réussie
                        Bukkit.getLogger().fine("[VillagerInventory] Transaction réussie entre " + 
                                              buyer.getId() + " et " + seller.getId());
                    }
                })
                .onFailure(() -> {
                    // Échec du déplacement
                    Bukkit.getLogger().fine("[VillagerInventory] Échec du déplacement pour transaction entre " + 
                                          buyer.getId() + " et " + seller.getId());
                })
                .start();
                
        }, delay);
    }

    /**
     * Vérifie si un villageois a un type spécifique de nourriture
     */
    private static boolean hasSpecificFood(Villager villager, Material foodType) {
        Inventory inventory = villager.getInventory();
        if (!inventory.contains(foodType)) {
            return false;
        }
        
        ItemStack foodItem = inventory.getItem(inventory.first(foodType));
        return foodItem != null && foodItem.getAmount() > 0;
    }

    /**
     * Effectue la transaction d'achat de nourriture entre villageois
     */
    private static boolean performFoodTransaction(VillagerModel buyer, VillagerModel seller, 
                                                 Villager buyerEntity, Villager sellerEntity, 
                                                 Material foodType, float price) {
        
        Inventory sellerInventory = sellerEntity.getInventory();
        ItemStack foodItem = sellerInventory.getItem(sellerInventory.first(foodType));
        
        if (foodItem == null || foodItem.getAmount() == 0) {
            return false; // Plus de nourriture disponible
        }

        // Retirer l'item de l'inventaire du vendeur
        foodItem.setAmount(foodItem.getAmount() - 1);
        if (foodItem.getAmount() == 0) {
            sellerInventory.remove(foodItem);
        }

        // Transaction financière
        buyer.setRichesse(buyer.getRichesse() - price);
        seller.setRichesse(seller.getRichesse() + price);

        // Consommation immédiate par l'acheteur
        int nutritionValue = FOOD_VALUES.get(foodType);
        buyer.setFood(buyer.getFood() + nutritionValue);

        // Enregistrer l'achat dans l'historique
        HistoryService.recordPurchase(buyer, seller, HistoryService.getFoodDisplayName(foodType), price);
        
        // Enregistrer la consommation dans l'historique
        HistoryService.recordFoodConsumption(buyer, HistoryService.getFoodDisplayName(foodType));

        // Sauvegarder les changements
        VillagerRepository.update(buyer);
        VillagerRepository.update(seller);
        // Mise à jour temps réel des noms (richesse)
        SocialClassService.updateVillagerDisplayName(buyer);
        SocialClassService.updateVillagerDisplayName(seller);

        // Évaluer la classe sociale
        SocialClassService.evaluateAndUpdateSocialClass(buyer);

        return true;
    }

    /**
     * Trouve le fermier le plus proche ayant de la nourriture dans son inventaire
     */
    private static Villager findNearestFarmerWithFood(Villager hungryVillager) {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        Villager nearestFarmer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (VillagerModel villagerModel : allVillagers) {
            // Ignorer le villageois affamé lui-même
            if (villagerModel.getId().equals(hungryVillager.getUniqueId())) {
                continue;
            }

            Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villagerModel.getId());
            if (entity == null) {
                continue; // Villageois fantôme
            }

            // Vérifier si c'est un fermier
            if (entity.getProfession() != Villager.Profession.FARMER) {
                continue;
            }

            // Vérifier si le fermier a de la nourriture
            if (!hasAnyFood(entity)) {
                continue;
            }

            // Calculer la distance
            double distance = hungryVillager.getLocation().distance(entity.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestFarmer = entity;
            }
        }

        return nearestFarmer;
    }

    /**
     * Vérifie si un villageois a des items alimentaires dans son inventaire
     */
    private static boolean hasAnyFood(Villager villager) {
        Inventory inventory = villager.getInventory();
        
        for (Material foodType : FOOD_VALUES.keySet()) {
            if (inventory.contains(foodType)) {
                ItemStack item = inventory.getItem(inventory.first(foodType));
                if (item != null && item.getAmount() > 0) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Donne des items alimentaires à un fermier pour simuler sa production
     * Appelé périodiquement pour que les fermiers aient des stocks
     */
    public static void giveFoodToFarmers() {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();

        for (VillagerModel villagerModel : allVillagers) {
            Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villagerModel.getId());
            if (entity == null || entity.getProfession() != Villager.Profession.FARMER) {
                continue;
            }

            Inventory inventory = entity.getInventory();
            
            // Donner du blé aux fermiers (simulation de production)
            if (!inventory.contains(Material.WHEAT) || 
                inventory.getItem(inventory.first(Material.WHEAT)).getAmount() < 3) {
                
                inventory.addItem(new ItemStack(Material.WHEAT, 2));
            }

            // Occasionnellement donner du pain
            if (Math.random() < 0.3 && !inventory.contains(Material.BREAD)) {
                inventory.addItem(new ItemStack(Material.BREAD, 1));
            }

            // Rarement donner un bloc de foin
            if (Math.random() < 0.1 && !inventory.contains(Material.HAY_BLOCK)) {
                inventory.addItem(new ItemStack(Material.HAY_BLOCK, 1));
            }
        }
    }
    
    /**
     * Méthode de diagnostic pour identifier les villageois problématiques
     * @param villageName Le nom du village à diagnostiquer
     */
    public static void diagnoseVillageVillagers(String villageName) {
        Collection<VillagerModel> allVillagers = VillagerRepository.getAll();
        int totalVillagers = 0;
        int villagersInWorld = 0;
        int villagersWithFood = 0;
        int villagersWithoutFood = 0;
        int ghostVillagers = 0;
        
        for (VillagerModel villager : allVillagers) {
            if (!villageName.equals(villager.getVillageName())) {
                continue;
            }
            
            totalVillagers++;
            
            // Vérifier si le villageois existe dans le monde
            Villager entity = (Villager) TestJava.plugin.getServer().getEntity(villager.getId());
            if (entity == null) {
                ghostVillagers++;
                continue;
            }
            
            villagersInWorld++;
            
            // Vérifier la nourriture
            if (villager.getFood() != null && villager.getFood() > 0) {
                villagersWithFood++;
            } else {
                villagersWithoutFood++;
            }
        }
        
        Bukkit.getLogger().info("[Diagnostic] Village " + villageName + ":");
        Bukkit.getLogger().info("[Diagnostic] - Total en DB: " + totalVillagers);
        Bukkit.getLogger().info("[Diagnostic] - Dans le monde: " + villagersInWorld);
        Bukkit.getLogger().info("[Diagnostic] - Avec nourriture: " + villagersWithFood);
        Bukkit.getLogger().info("[Diagnostic] - Sans nourriture: " + villagersWithoutFood);
        Bukkit.getLogger().info("[Diagnostic] - Villageois fantômes: " + ghostVillagers);
    }
}