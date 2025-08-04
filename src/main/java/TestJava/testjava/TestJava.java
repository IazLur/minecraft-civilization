package TestJava.testjava;

import TestJava.testjava.commands.*;
import TestJava.testjava.listeners.SocialClassJobListener;
import TestJava.testjava.listeners.SheepManagementListener;
import TestJava.testjava.listeners.JobBlockPlacementListener;
import TestJava.testjava.models.*;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.ResourceRepository;
import TestJava.testjava.services.*;
import TestJava.testjava.threads.*;
import io.jsondb.JsonDBTemplate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public final class TestJava extends JavaPlugin implements Listener {

    String path = TestJava.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    String rawPath = URLDecoder.decode(path, "UTF-8");
    String decodedPath = rawPath.substring(1, rawPath.lastIndexOf("/"));
    String jsonLocation = decodedPath + "/plugins";
    String baseScanPackage = "TestJava.testjava.model";
    public static final String worldName = "nouveaumonde2";
    public static Plugin plugin;

    public static JsonDBTemplate database;
    public static HashMap<UUID, Integer> threads = new HashMap<>();
    public static BlockProtectionService blockProtectionService;
    public static VillageService villageService;
    public static PlayerService playerService;
    public static ItemService itemService;
    public static InventoryService inventoryService;
    public static EntityService entityService;
    public static VillagerService villagerService;
    public static WarBlockService warBlockService;
    public static World world;

    public static HashMap<UUID, String> banditTargets = new HashMap<>();
    public static HashMap<UUID, VillageModel> locustTargets = new HashMap<>();

    public TestJava() throws UnsupportedEncodingException {
    }
    
    /**
     * Méthode utilitaire pour changer de monde ou réessayer de charger le monde spécifique
     */
    public static void tryLoadSpecificWorld() {
        World specificWorld = Bukkit.getWorld(worldName);
        if (specificWorld != null && !specificWorld.equals(world)) {
            world = specificWorld;
            plugin.getLogger().info("Monde spécifique '" + worldName + "' maintenant chargé et utilisé.");
        }
    }

    @Override
    public void onEnable() {
        // Init
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new SocialClassJobListener(), this);
        Bukkit.getPluginManager().registerEvents(new SheepManagementListener(), this);
        Bukkit.getPluginManager().registerEvents(new JobBlockPlacementListener(), this);
        TestJava.plugin = this;
        getLogger().log(Level.INFO, "Loading plugin v3.2");
        TestJava.world = Bukkit.getWorld(TestJava.worldName);
        
        // Vérification et fallback pour le monde
        if (TestJava.world == null) {
            getLogger().warning("Le monde '" + TestJava.worldName + "' n'existe pas ou n'est pas encore chargé.");
            TestJava.world = Bukkit.getWorlds().get(0); // Utiliser le monde principal
            getLogger().info("Utilisation du monde par défaut : " + TestJava.world.getName());
        } else {
            getLogger().info("Monde chargé avec succès : " + TestJava.world.getName());
        }

        // Registering commands
        getCommand("rename").setExecutor(new RenameCommand());
        getCommand("delegation").setExecutor(new DelegationCommand());
        getCommand("war").setExecutor(new WarCommand());
        getCommand("village").setExecutor(new VillageCommand());
        getCommand("marketprice").setExecutor(new MarketPriceCommand());
        getCommand("market").setExecutor(new MarketCommand());
        getCommand("money").setExecutor(new MoneyCommand());
        getCommand("nearest").setExecutor(new NearestCommand());
        getCommand("build").setExecutor(new BuildCommand());
        getCommand("social").setExecutor(new SocialCommand());
        getCommand("emptyvillage").setExecutor(new EmptyVillageCommand());
        getCommand("forcespawnat").setExecutor(new ForceSpawnAtCommand());
        getCommand("reactivate").setExecutor(new ReactivateCommand());
        getCommand("distance").setExecutor(new DistanceCommand());

        // Registering databases
        TestJava.database = new JsonDBTemplate(this.jsonLocation, this.baseScanPackage);
        if (!TestJava.database.collectionExists(EmpireModel.class)) {
            TestJava.database.createCollection(EmpireModel.class);
        }
        if (!TestJava.database.collectionExists(VillageModel.class)) {
            TestJava.database.createCollection(VillageModel.class);
        }
        if (!TestJava.database.collectionExists(DelegationModel.class)) {
            TestJava.database.createCollection(DelegationModel.class);
        }
        if (!TestJava.database.collectionExists(VillagerModel.class)) {
            TestJava.database.createCollection(VillagerModel.class);
        }
        if (!TestJava.database.collectionExists(EatableModel.class)) {
            TestJava.database.createCollection(EatableModel.class);
        }
        if (!TestJava.database.collectionExists(WarBlockModel.class)) {
            TestJava.database.createCollection(WarBlockModel.class);
        }
        if (!TestJava.database.collectionExists(ResourceModel.class)) {
            TestJava.database.createCollection(ResourceModel.class);
        }
        if (!TestJava.database.collectionExists(BuildingModel.class)) {
            TestJava.database.createCollection(BuildingModel.class);
        }
        if (!TestJava.database.collectionExists(SheepModel.class)) {
            TestJava.database.createCollection(SheepModel.class);
        }

        // Initialisation des ressources depuis resources.json
        ResourceInitializationService.initializeResourcesIfEmpty();
        
        // Chargement des configurations de distance pour métiers et bâtiments
        DistanceConfigService.loadAllConfigurations();
        
                // Synchronisation des villageois du monde avec la base de données
        VillagerSynchronizationService.synchronizeWorldVillagersWithDatabase();

        // Migration format tags classes sociales ([0] vers {0})
        SocialClassService.migrateSocialClassTagsToNewFormat();

        // Initialisation des classes sociales pour les villageois existants
        SocialClassService.initializeSocialClassForExistingVillagers();

        // Migration des juridictions
        for (EmpireModel empire : EmpireRepository.getAll()) {
            try {
                empire.getJuridictionCount();
            } catch (NullPointerException ex) {
                empire.setJuridictionCount(0);
                EmpireRepository.update(empire);
            }
        }

        // Registering services
        TestJava.blockProtectionService = new BlockProtectionService();
        TestJava.villageService = new VillageService();
        TestJava.itemService = new ItemService();
        TestJava.playerService = new PlayerService();
        TestJava.inventoryService = new InventoryService();
        TestJava.entityService = new EntityService();
        TestJava.villagerService = new VillagerService();
        TestJava.warBlockService = new WarBlockService();

        // Nettoyage des entités (seulement si le monde est disponible)
        if (TestJava.world != null) {
            try {
                playerService.killAllDelegators();
                playerService.killAllBandits();
                playerService.resetAllWars();
                SheepService.removeNaturalSheep();
                getLogger().info("Nettoyage des entités terminé");
            } catch (Exception e) {
                getLogger().warning("Erreur lors du nettoyage des entités : " + e.getMessage());
            }
        } else {
            getLogger().warning("Nettoyage des entités ignoré - monde non disponible");
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerSpawnThread(), 0, 20 * 60);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerEatThread(), 0, 20 * 60 * 5);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerGoEatThread(), 0, 20 * 60 * 2);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new DefenderThread(), 0, 20 * 5);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new TraderThread(), 0, 20 * 60);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new LocustThread(), 0, 20);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new DailyBuildingCostThread(), 0,  20 * 60 * 20);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SocialClassEnforcementThread(), 0, 20 * 60 * 2); // Toutes les 2 minutes
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SheepSpawnThread(), 0, 20 * 60 * 20); // Même fréquence que DailyBuildingCostThread
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SheepMovementThread(), 0, 20 * 60 * 5); // Toutes les 5 minutes
    }

    @EventHandler
    public void onPlayerCommandSend(PlayerCommandSendEvent e) {
        /* Bukkit.getServer().broadcastMessage(ChatColor.DARK_AQUA + "@" + e.getPlayer().getDisplayName() +
                ChatColor.GRAY + " a executé la commande " + Colorize.name(String.join(" ", e.getCommands()))); */
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent e) {
        TestJava.entityService.preventVillageEntityTransform(e);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent e) {
        TestJava.itemService.testIfVillagerPickupFood(e);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent e) {
        TestJava.playerService.cancelDelegatorTarget(e);
        TestJava.entityService.testIfSkeletonDamageSameVillage(e);
        TestJava.entityService.testIfPillagerDamageSameVillage(e);
        TestJava.entityService.testIfGolemDamageSameVillage(e);
        TestJava.entityService.testIfBanditTargetRight(e);
        TestJava.entityService.testIfMobTargetSkeleton(e);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        TestJava.entityService.preventFireForCustom(e);
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
        TestJava.playerService.testIfDelegatorDamagePlayer(e);
        TestJava.playerService.testIfPlayerDamageDelegator(e);
        TestJava.playerService.testIfPlayerDamageVillager(e);
        TestJava.playerService.testIfEntityDamageSameVillage(e);
        TestJava.playerService.testIfEntityDamageArmorStand(e);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        TestJava.entityService.testAnimalSpawn(event);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent e) {
        TestJava.villagerService.testIfGrowIsEatable(e);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        TestJava.itemService.reduceSpawnChanceIfSapling(e);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        TestJava.entityService.testSpawnIfVillager(e);
        TestJava.entityService.testSpawnIfGolem(e);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        TestJava.entityService.testDeathIfVillager(e);
        TestJava.entityService.testDeathIfSkeleton(e);
        TestJava.entityService.testDeathIfPillager(e);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        TestJava.warBlockService.testIfTNTExplode(e);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (TestJava.blockProtectionService.preventCultivableDestroy(e)) return;

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (TestJava.blockProtectionService.isVillageCenterTypeGettingDestroyed(e)) return;
        if (TestJava.blockProtectionService.canPlayerBreakBlock(e)) {
            // Player's territory
            TestJava.villageService.testIfBreakingBed(e);
            return;
        }
        TestJava.blockProtectionService.protectRestOfTheWorld(e);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        ResourceRepository.getAll();
        TestJava.playerService.addEmpireIfNotOwnsOne(e.getPlayer());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (TestJava.warBlockService.testIfCanPlaceTNT(e)) return;
        if (e.getBlockPlaced().getType() == Config.CONQUER_TYPE) {
            if (TestJava.villageService.canConquerVillage(e)) {
                TestJava.villageService.conquer(e);
                return;
            }
        }
        if (e.getBlockPlaced().getType() == Config.VILLAGE_CENTER_TYPE) {
            if (TestJava.blockProtectionService.canPlayerCreateVillage(e)) {
                TestJava.villageService.create(e);
                return;
            }
        }

        if (TestJava.blockProtectionService.canPlayerPlaceBlock(e)) {
            // Player's territory
            TestJava.villageService.testIfPlacingBed(e);
            TestJava.entityService.testIfPlaceDefender(e);
            TestJava.entityService.testIfPlaceAttacker(e);
            TestJava.entityService.testIfPlaceBandit(e);
            TestJava.entityService.testIfPlaceLocust(e);
            return;
        }
        TestJava.blockProtectionService.protectRestOfTheWorld(e);
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        // Prevent breeding
        if (!(event.getFather() instanceof Villager))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        TestJava.playerService.testIfPlayerHaveVillageToTeleport(e);
    }
}
