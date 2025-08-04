package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import TestJava.testjava.classes.CustomEntity;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.EmpireModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.EmpireRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class PlayerService {
    public void addEmpireIfNotOwnsOne(@Nonnull Player e) {
        EmpireModel empire = EmpireRepository.get(e.getPlayer().getName());

        if (empire != null) {
            return;
        }

        empire = new EmpireModel();
        empire.setEmpireName("Empire de " + e.getPlayer().getName());
        empire.setEnemyName("");
        empire.setIsInWar(false);
        empire.setId(e.getPlayer().getName());
        empire.setTotalAttackerCount(0);
        empire.setTotalDefenderCount(0);
        EmpireRepository.update(empire);

        e.getPlayer().getInventory().addItem(new ItemStack(Material.BELL));
        e.getPlayer().getInventory().addItem(new ItemStack(Material.WHITE_BED));
        e.getPlayer().getInventory().addItem(new ItemStack(Material.CAKE));

        e.getPlayer().sendMessage(ChatColor.GREEN + "Bienvenue sur iazgame.ovh!");
    }

    public Collection<CustomEntity> getAllDelegators() {
        Collection<CustomEntity> entities = CustomName.getAll();
        entities.removeIf(entity -> !entity.getEntity().getCustomName().contains("Délégateur"));
        return entities;
    }

    public Collection<CustomEntity> getAllBandits() {
        Collection<CustomEntity> entities = CustomName.getAll();
        entities.removeIf(entity -> !entity.getEntity().getCustomName().contains("Bandit"));
        return entities;
    }

    public void killAllDelegators() {
        this.getAllDelegators().forEach(entity -> entity.getEntity().remove());
    }

    public void cancelDelegatorTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getEntity() instanceof Llama)) {
            return;
        }

        if (e.getEntity() instanceof Player) {
            return;
        }

        // Si le délégateur veut target autre chose qu'un joueur
        e.setCancelled(true);
    }

    public void testIfDelegatorDamagePlayer(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LlamaSpit)) {
            return;
        }

        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Llama base = (Llama) ((LlamaSpit) e.getDamager()).getShooter();

        // Si le délégateur veut faire des dégâts à un joueur
        e.setCancelled(true);
        Player damaged = (Player) e.getEntity();

        String name = CustomName.extractVillageName(base.getCustomName());
        InventoryHolder damager = (InventoryHolder) base;
        ItemStack letter = TestJava.inventoryService.get(damager.getInventory(), Material.WRITTEN_BOOK);
        if (letter == null && name.equals(damaged.getName())) {
            e.getDamager().remove();
            damaged.sendMessage(ChatColor.GOLD + "Ce délégateur n'a pas de lettre");
            base.setTarget(null);
            e.setCancelled(true);
            return;
        }
        if (letter != null) {
            damaged.getInventory().addItem(letter.clone());
            letter.setAmount(0);
            damaged.sendMessage(ChatColor.GOLD + "Vous avez une nouvelle lettre");
            base.setTarget(null);
            e.setCancelled(true);
            return;
        }
    }

    public void testIfPlayerDamageDelegator(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player)) {
            return;
        }

        if (!(e.getEntity() instanceof Llama)) {
            return;
        }

        if (!e.getEntity().isCustomNameVisible()) {
            return;
        }

        // Si un joueur frappe un délégateur
        Player damager = (Player) e.getDamager();
        InventoryHolder damaged = (InventoryHolder) e.getEntity();
                    String name = CustomName.extractVillageName(e.getEntity().getCustomName());
        ItemStack book = null;
        if (damager.getInventory().getItemInMainHand().getType() == Material.WRITTEN_BOOK) {
            book = damager.getInventory().getItemInMainHand();
        }

        Player back = Bukkit.getPlayer(name);
        if (back == null) {
            damager.sendMessage(ChatColor.RED + name + " n'est pas connecté");
            return;
        }

        Animals delegator = (Animals) e.getEntity();
        if (book != null) {
            damaged.getInventory().addItem(book.clone());
            book.setAmount(0);
            damager.sendMessage(ChatColor.GREEN + "Vous avez envoyé une lettre");
        } else if (name.equals(damager.getName())) {
            damager.sendMessage(ChatColor.RED + "Votre délégateur est en mission");
            return;
        }

        delegator.damage(0D, back);
        delegator.setVelocity(new Vector());
        delegator.setTarget(back);
    }

    public void testIfPlayerDamageVillager(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            return;
        }
        Player player;
        if (e.getDamager() instanceof Projectile) {
            if (!(((Projectile) e.getDamager()).getShooter() instanceof Player)) {
                return;
            }
            player = (Player) ((Projectile) e.getDamager()).getShooter();
        } else {
            player = (Player) e.getDamager();
        }

        if (!(e.getEntity() instanceof Villager)) {
            return;
        }

        // Un joueur attaque un villageois
        e.setCancelled(true);
        e.setDamage(0D);
        player.sendMessage(ChatColor.RED + "Vous ne pouvez pas attaquer les villageois");
    }

    public void resetAllWars() {
        Collection<EmpireModel> empires = EmpireRepository.getAll();
        for (EmpireModel empire : empires) {
            if (empire.getIsInWar()) {
                empire.setIsInWar(false);
                empire.setEnemyName("");
                EmpireRepository.update(empire);
            }
        }
    }

    @Nullable
    public Player getNearestPlayerWhereNot(LivingEntity from, String ignoredPlayer) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        Player returned = null;
        double oldDist = 9999F;
        for (Player player : players) {
            if (player.getName().equals(ignoredPlayer)) {
                continue;
            }
            double nDist = player.getLocation().distance(from.getLocation());
            if (nDist < oldDist) {
                returned = player;
                oldDist = nDist;
            }
        }
        if (returned != null)
            System.out.println(returned.getName());
        return returned;
    }

    public void killAllBandits() {
        this.getAllBandits().forEach(entity -> entity.getEntity().remove());
    }

    public void testIfPlayerHaveVillageToTeleport(PlayerRespawnEvent e) {
        Optional<VillageModel> v = VillageRepository.getForPlayer(e.getPlayer().getName())
                .stream().findFirst();
        if (v.isEmpty()) {
            return;
        }
        VillageModel village = v.get();
        e.getPlayer().teleport(VillageRepository.getBellLocation(village));
    }

    public void testIfEntityDamageSameVillage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity damagee = e.getEntity();

        // Vérifier si les deux entités sont des LivingEntity et ont un nom personnalisé
        if (damager instanceof LivingEntity livingDamager && damagee instanceof LivingEntity livingDamagee) {

            if (livingDamager.isCustomNameVisible() && livingDamagee.isCustomNameVisible()) {
                            String damagerVillage = CustomName.extractVillageName(Objects.requireNonNull(livingDamager.getCustomName()));
            String damageeVillage = CustomName.extractVillageName(Objects.requireNonNull(livingDamagee.getCustomName()));

                // Si les deux entités sont du même village, annuler les dégâts
                if (damagerVillage.equals(damageeVillage)) {
                    e.setCancelled(true);
                    e.setDamage(0);
                }
            }
        }
    }

    public void testIfEntityDamageArmorStand(EntityDamageByEntityEvent e) {
        if(e.getEntity() instanceof ArmorStand) {
            e.setCancelled(true);
            e.setDamage(0D);
        }
    }
}
