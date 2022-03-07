package edouard.testjava.services;

import edouard.testjava.Config;
import edouard.testjava.TestJava;
import edouard.testjava.classes.CustomEntity;
import edouard.testjava.helpers.CustomName;
import edouard.testjava.models.EmpireModel;
import edouard.testjava.repositories.EmpireRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.Collection;

public class PlayerService {
    public void addEmpireIfNotOwnsOne(@Nonnull Player e) {
        EmpireModel empire = EmpireRepository.get(e.getPlayer().getDisplayName());

        if (empire != null) {
            return;
        }

        empire = new EmpireModel();
        empire.setEmpireName("Empire de " + e.getPlayer().getDisplayName());
        empire.setEnemyName("");
        empire.setIsInWar(false);
        empire.setId(e.getPlayer().getDisplayName());
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

        String name = CustomName.squareBrackets(base.getCustomName(), 0);
        InventoryHolder damager = (InventoryHolder) base;
        ItemStack letter = TestJava.inventoryService.get(damager.getInventory(), Material.WRITTEN_BOOK);
        if (letter == null && name.equals(damaged.getDisplayName())) {
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
        String name = CustomName.squareBrackets(e.getEntity().getCustomName(), 0);
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
        } else if (name.equals(damager.getDisplayName())) {
            damager.sendMessage(ChatColor.RED + "Votre délégateur est en mission");
            return;
        }

        delegator.damage(0D, back);
        delegator.setVelocity(new Vector());
        delegator.setTarget(back);
    }

    public void testIfPlayerDamageVillager(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof Player)) {
            return;
        }
        Player player;
        if(e.getDamager() instanceof Projectile) {
            if(!(((Projectile) e.getDamager()).getShooter() instanceof Player)) {
                return;
            }
            player = (Player) ((Projectile) e.getDamager()).getShooter();
        } else {
            player = (Player) e.getDamager();
        }

        if(!(e.getEntity() instanceof Villager)) {
            return;
        }

        // Un joueur attaque un villageois
        e.setCancelled(true);
        e.setDamage(0D);
        player.sendMessage(ChatColor.RED + "Vous ne pouvez pas attaquer les villageois");
    }
}
