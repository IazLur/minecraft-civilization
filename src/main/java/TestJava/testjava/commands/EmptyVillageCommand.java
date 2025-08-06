package TestJava.testjava.commands;

import TestJava.testjava.TestJava;
import TestJava.testjava.helpers.CustomName;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.repositories.VillageRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.services.SocialClassService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EmptyVillageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être exécutée que par un joueur");
            return true;
        }

        Player player = (Player) sender;

        // Vérification que le joueur possède un village là où il se trouve
        VillageModel village = VillageRepository.getCurrentVillageConstructibleIfOwn(player);
        
        if (village == null) {
            player.sendMessage(ChatColor.RED + "Vous devez être dans votre propre village pour utiliser cette commande");
            return true;
        }

        // Confirmation de sécurité
        player.sendMessage(ChatColor.GOLD + "=== Vidage du Village " + village.getId() + " ===");
        player.sendMessage(ChatColor.YELLOW + "⚠️ Cette opération va tuer TOUS les villageois de votre village !");
        player.sendMessage(ChatColor.GRAY + "Population actuelle : " + village.getPopulation());
        
        // Compte les villageois réels dans le monde
        List<Villager> villagersInWorld = new ArrayList<>();
        List<VillagerModel> villagersInDB = new ArrayList<>();
        
        // Trouve tous les villageois du village dans le monde
        if (TestJava.world != null) {
            for (Entity entity : TestJava.world.getEntities()) {
                if (entity instanceof Villager bukkit_villager && entity.getCustomName() != null) {
                    try {
                        String villageFromName = CustomName.extractVillageName(bukkit_villager.getCustomName());
                        if (village.getId().equals(villageFromName)) {
                            villagersInWorld.add(bukkit_villager);
                        }
                    } catch (Exception e) {
                        // Ignore les villageois avec des noms mal formés
                        TestJava.plugin.getLogger().warning("Nom villageois mal formé ignoré: " + bukkit_villager.getCustomName());
                    }
                }
            }
        }
        
        // Trouve tous les VillagerModel dans la base de données
        for (VillagerModel villagerModel : VillagerRepository.getAll()) {
            if (village.getId().equals(villagerModel.getVillageName())) {
                villagersInDB.add(villagerModel);
            }
        }
        
        player.sendMessage(ChatColor.WHITE + "Villageois dans le monde : " + ChatColor.YELLOW + villagersInWorld.size());
        player.sendMessage(ChatColor.WHITE + "Villageois en base : " + ChatColor.YELLOW + villagersInDB.size());
        
        // Demande de confirmation (simulation - en vrai on exécute directement)
        int worldKilled = 0;
        int dbRemoved = 0;
        
        try {
            // Tue tous les villageois dans le monde
            for (Villager villager : villagersInWorld) {
                villager.setHealth(0.0);
                worldKilled++;
            }
            
            // Supprime tous les VillagerModel de la base de données
            for (VillagerModel villagerModel : villagersInDB) {
                VillagerRepository.remove(villagerModel.getId());
                dbRemoved++;
            }
            
            // CORRECTION BUG: Supprimer aussi l'armée, garnison et gardes
            int militaryKilled = removeMilitaryEntities(village);
            
            // Met à jour la population du village à 0 et réinitialise les forces militaires
            village.setPopulation(0);
            village.setGroundArmy(0);
            village.setGarrison(0);
            VillageRepository.update(village);
            
            // Message de succès
            player.sendMessage(ChatColor.GREEN + "✅ Village vidé avec succès !");
            player.sendMessage(ChatColor.WHITE + "- " + worldKilled + " villageois tués dans le monde");
            player.sendMessage(ChatColor.WHITE + "- " + dbRemoved + " entrées supprimées de la base");
            player.sendMessage(ChatColor.WHITE + "- " + militaryKilled + " unités militaires supprimées");
            player.sendMessage(ChatColor.WHITE + "- Population mise à jour : " + ChatColor.YELLOW + "0");
            
            // Broadcast global
            Bukkit.getServer().broadcastMessage(
                ChatColor.DARK_RED + "💀 " + ChatColor.YELLOW + player.getName() + 
                ChatColor.GRAY + " a vidé le village " + ChatColor.GOLD + village.getId() + 
                ChatColor.GRAY + " (" + (worldKilled + dbRemoved) + " villageois)"
            );
            
            // Log pour les administrateurs
            Bukkit.getLogger().info("[EmptyVillage] " + player.getName() + " a vidé le village " + 
                                   village.getId() + " - " + worldKilled + " world + " + dbRemoved + " DB");
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Erreur lors du vidage du village : " + e.getMessage());
            Bukkit.getLogger().severe("[EmptyVillage] Erreur : " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Supprime toutes les entités militaires (armée, garnison, gardes) d'un village
     */
    private int removeMilitaryEntities(VillageModel village) {
        int removed = 0;
        
        if (TestJava.world == null) {
            return 0;
        }
        
        try {
            // Parcourir toutes les entités du monde
            for (Entity entity : TestJava.world.getEntities()) {
                // Vérifier si l'entité est une unité militaire avec un nom personnalisé
                if ((entity instanceof org.bukkit.entity.Skeleton || 
                     entity instanceof org.bukkit.entity.Pillager) && 
                    entity.getCustomName() != null) {
                    
                    // Extraire le nom du village depuis le nom personnalisé
                    String entityVillageName = CustomName.extractVillageName(entity.getCustomName());
                    
                    // Si l'entité appartient au village à vider
                    if (village.getId().equals(entityVillageName)) {
                        entity.remove();
                        removed++;
                        
                        Bukkit.getLogger().info("[EmptyVillage] Suppression entité militaire: " + 
                                               entity.getType() + " - " + entity.getCustomName());
                    }
                }
            }
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("[EmptyVillage] Erreur suppression entités militaires: " + e.getMessage());
        }
        
        return removed;
    }
}