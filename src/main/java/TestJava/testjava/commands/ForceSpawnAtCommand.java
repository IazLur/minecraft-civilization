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
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

public class ForceSpawnAtCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Vérification des permissions : seulement console ou admins
        if (!(sender instanceof ConsoleCommandSender) && 
            (sender instanceof Player && !((Player) sender).isOp())) {
            sender.sendMessage(ChatColor.RED + "❌ Cette commande est réservée aux administrateurs et à la console");
            return true;
        }

        // Vérification des arguments
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /forcespawnat <villageName>");
            sender.sendMessage(ChatColor.YELLOW + "Fait apparaître un villageois dans le village spécifié");
            return true;
        }

        String villageName = args[0];
        
        // Vérification que le village existe
        VillageModel village = VillageRepository.get(villageName);
        if (village == null) {
            sender.sendMessage(ChatColor.RED + "❌ Le village '" + villageName + "' n'existe pas");
            return true;
        }

        // Vérification du monde
        if (TestJava.world == null) {
            sender.sendMessage(ChatColor.RED + "❌ Le monde n'est pas chargé");
            return true;
        }

        try {
            // Obtient la location de la cloche du village
            Location spawnLocation = VillageRepository.getBellLocation(village);
            spawnLocation.setY(spawnLocation.getY() + 1); // Spawn 1 bloc au-dessus
            
            // Génère un nom personnalisé
            String customName = CustomName.generate();
            
            // Crée le villageois dans le monde
            Villager newVillager = TestJava.world.spawn(spawnLocation, Villager.class);
            newVillager.setCustomNameVisible(true);
            newVillager.setCustomName(ChatColor.BLUE + "[" + village.getId() + "] " + ChatColor.WHITE + customName);
            
            // Crée l'entrée dans la base de données
            VillagerModel villagerModel = new VillagerModel();
            villagerModel.setId(newVillager.getUniqueId());
            villagerModel.setVillageName(village.getId());
            villagerModel.setFood(1); // Commence avec 1 point de nourriture
            villagerModel.setSocialClass(0); // Classe 0 par défaut (Misérable)
            
            // Sauvegarde le villageois
            VillagerRepository.update(villagerModel);
            
            // Met à jour la population du village
            village.setPopulation(village.getPopulation() + 1);
            VillageRepository.update(village);
            
            // Initialise la classe sociale (évaluation basée sur la nourriture)
            SocialClassService.evaluateAndUpdateSocialClass(villagerModel);
            SocialClassService.updateVillagerDisplayName(villagerModel);
            
            // Messages de succès
            sender.sendMessage(ChatColor.GREEN + "✅ Villageois créé avec succès !");
            sender.sendMessage(ChatColor.WHITE + "Village : " + ChatColor.GOLD + village.getId());
            sender.sendMessage(ChatColor.WHITE + "Nom : " + ChatColor.AQUA + customName);
            sender.sendMessage(ChatColor.WHITE + "UUID : " + ChatColor.GRAY + newVillager.getUniqueId());
            sender.sendMessage(ChatColor.WHITE + "Position : " + ChatColor.YELLOW + 
                             "X:" + spawnLocation.getBlockX() + 
                             " Y:" + spawnLocation.getBlockY() + 
                             " Z:" + spawnLocation.getBlockZ());
            sender.sendMessage(ChatColor.WHITE + "Nouvelle population : " + ChatColor.YELLOW + village.getPopulation());
            sender.sendMessage(ChatColor.WHITE + "Classe sociale : " + villagerModel.getSocialClassEnum().getColoredTag());
            
            // Broadcast global
            Bukkit.getServer().broadcastMessage(
                ChatColor.GREEN + "🆕 " + ChatColor.AQUA + customName + 
                ChatColor.GRAY + " est apparu par magie à " + ChatColor.GOLD + village.getId() + 
                ChatColor.GRAY + " (spawn forcé)"
            );
            
            // Log administrateur
            String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
            Bukkit.getLogger().info("[ForceSpawnAt] " + senderName + " a fait spawn " + customName + 
                                   " (" + newVillager.getUniqueId() + ") dans " + village.getId());
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "❌ Erreur lors de la création du villageois : " + e.getMessage());
            Bukkit.getLogger().severe("[ForceSpawnAt] Erreur lors du spawn : " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}