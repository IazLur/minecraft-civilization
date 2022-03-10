package edouard.testjava;

import edouard.testjava.commands.DelegationCommand;
import edouard.testjava.commands.RenameCommand;
import edouard.testjava.models.DelegationModel;
import edouard.testjava.models.EmpireModel;
import edouard.testjava.models.VillageModel;
import edouard.testjava.models.VillagerModel;
import edouard.testjava.services.*;
import edouard.testjava.threads.VillagerEatThread;
import edouard.testjava.threads.VillagerSpawnThread;
import io.jsondb.JsonDBTemplate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

public final class TestJava extends JavaPlugin implements Listener {

    String path = TestJava.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    String rawPath = URLDecoder.decode(path, "UTF-8");
    String decodedPath = rawPath.substring(1, rawPath.lastIndexOf("/"));
    String jsonLocation = decodedPath + "/plugins";
    String baseScanPackage = "edouard.testjava.model";
    public static final String worldName = "nouveaumonde2";
    public static Plugin plugin;

    public static JsonDBTemplate database;
    public static List<ScheduledExecutorService> threads = new ArrayList<>();
    public static BlockProtectionService blockProtectionService;
    public static VillageService villageService;
    public static PlayerService playerService;
    public static ItemService itemService;
    public static InventoryService inventoryService;
    public static EntityService entityService;
    public static World world;

    public TestJava() throws UnsupportedEncodingException {
    }

    @Override
    public void onEnable() {
        // Init
        Bukkit.getPluginManager().registerEvents(this, this);
        TestJava.plugin = this;
        getLogger().log(Level.INFO, "Loading plugin v3.0");
        TestJava.world = Bukkit.getWorld(TestJava.worldName);

        // Registering commands
        getCommand("rename").setExecutor(new RenameCommand());
        getCommand("delegation").setExecutor(new DelegationCommand());

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

        // Registering services
        TestJava.blockProtectionService = new BlockProtectionService();
        TestJava.villageService = new VillageService();
        TestJava.itemService = new ItemService();
        TestJava.playerService = new PlayerService();
        TestJava.inventoryService = new InventoryService();
        TestJava.entityService = new EntityService();

        playerService.killAllDelegators();
        playerService.resetAllWars();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerSpawnThread(), 0, 20);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new VillagerEatThread(), 0, 20 * 5);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent e) {
        TestJava.playerService.cancelDelegatorTarget(e);
        TestJava.entityService.testIfSkeletonDamageSameVillage(e);
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
        TestJava.playerService.testIfDelegatorDamagePlayer(e);
        TestJava.playerService.testIfPlayerDamageDelegator(e);
        TestJava.playerService.testIfPlayerDamageVillager(e);
    }

    @EventHandler
    public void test(BlockGrowEvent e) {

    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        TestJava.itemService.reduceSpawnChanceIfSapling(e);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        TestJava.entityService.testSpawnIfVillager(e);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        TestJava.entityService.testDeathIfVillager(e);
        TestJava.entityService.testDeathIfSkeleton(e);
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
        TestJava.playerService.addEmpireIfNotOwnsOne(e.getPlayer());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
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
            return;
        }
        TestJava.blockProtectionService.protectRestOfTheWorld(e);
    }

}
