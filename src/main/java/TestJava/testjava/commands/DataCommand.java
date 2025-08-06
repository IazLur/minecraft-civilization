package TestJava.testjava.commands;

import TestJava.testjava.helpers.Colorize;
import TestJava.testjava.models.VillagerModel;
import TestJava.testjava.models.VillageModel;
import TestJava.testjava.repositories.HistoryRepository;
import TestJava.testjava.repositories.VillagerRepository;
import TestJava.testjava.repositories.VillageRepository;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Commande pour consulter l'historique des villages et villageois
 * Usage: /data village {villageName} ou /data villager
 */
public class DataCommand implements CommandExecutor {
    
    private static final int LINES_PER_PAGE = 12; // Nombre de lignes par page dans un livre
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par un joueur.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /data village <nom> ou /data villager");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "village":
                return handleVillageCommand(player, args);
            case "villager":
                return handleVillagerCommand(player, args);
            default:
                player.sendMessage(ChatColor.RED + "Sous-commande inconnue. Usage: /data village <nom> ou /data villager");
                return true;
        }
    }
    
    /**
     * Gère la commande /data village {villageName}
     */
    private boolean handleVillageCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /data village <nom>");
            return true;
        }
        
        String villageName = args[1];
        
        // Vérifier que le village existe
        VillageModel village = VillageRepository.get(villageName);
        if (village == null) {
            player.sendMessage(ChatColor.RED + "Village '" + villageName + "' introuvable.");
            return true;
        }
        
        // Récupérer l'historique
        List<String> historyList = HistoryRepository.getVillageHistoryList(villageName);
        
        if (historyList.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucun historique trouvé pour le village " + Colorize.name(villageName) + ".");
            return true;
        }
        
        // Créer le livre avec l'historique
        ItemStack book = createHistoryBook("Historique de " + villageName, historyList);
        
        // Donner le livre au joueur
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(book);
            player.sendMessage(ChatColor.GREEN + "📖 Livre d'historique de " + Colorize.name(villageName) + " ajouté à votre inventaire !");
        } else {
            player.sendMessage(ChatColor.RED + "Votre inventaire est plein !");
        }
        
        return true;
    }
    
    /**
     * Gère la commande /data villager (villageois le plus proche)
     */
    private boolean handleVillagerCommand(Player player, String[] args) {
        // Trouver le villageois le plus proche
        Villager nearestVillager = findNearestVillager(player);
        
        if (nearestVillager == null) {
            player.sendMessage(ChatColor.RED + "Aucun villageois trouvé à proximité.");
            return true;
        }
        
        // Récupérer le modèle du villageois
        VillagerModel villagerModel = VillagerRepository.find(nearestVillager.getUniqueId());
        if (villagerModel == null) {
            player.sendMessage(ChatColor.RED + "Données du villageois introuvables.");
            return true;
        }
        
        // Récupérer l'historique
        List<String> historyList = HistoryRepository.getVillagerHistoryList(nearestVillager.getUniqueId());
        
        if (historyList.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aucun historique trouvé pour ce villageois.");
            return true;
        }
        
        // Nom du villageois pour le titre
        String villagerName = "Villageois Inconnu";
        if (nearestVillager.getCustomName() != null) {
            String customName = nearestVillager.getCustomName();
            String cleanName = ChatColor.stripColor(customName);
            
            // Extraire le nom après le village : {X} [VillageName] Prénom Nom
            int bracketEnd = cleanName.indexOf(']');
            if (bracketEnd != -1 && bracketEnd + 2 < cleanName.length()) {
                villagerName = cleanName.substring(bracketEnd + 2);
            }
        }
        
        // Créer le livre avec l'historique
        ItemStack book = createHistoryBook("Historique de " + villagerName, historyList);
        
        // Donner le livre au joueur
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(book);
            player.sendMessage(ChatColor.GREEN + "📖 Livre d'historique de " + Colorize.name(villagerName) + " ajouté à votre inventaire !");
        } else {
            player.sendMessage(ChatColor.RED + "Votre inventaire est plein !");
        }
        
        return true;
    }
    
    /**
     * Trouve le villageois le plus proche du joueur
     */
    private Villager findNearestVillager(Player player) {
        Villager nearestVillager = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : player.getNearbyEntities(50, 50, 50)) { // Rayon de 50 blocs
            if (entity instanceof Villager) {
                double distance = player.getLocation().distance(entity.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestVillager = (Villager) entity;
                }
            }
        }
        
        return nearestVillager;
    }
    
    /**
     * Crée un livre écrit avec l'historique
     */
    private ItemStack createHistoryBook(String title, List<String> historyList) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        if (meta != null) {
            meta.setTitle(title);
            meta.setAuthor("Système de Civilisation");
            
            // Diviser l'historique en pages
            List<String> pages = new ArrayList<>();
            StringBuilder currentPage = new StringBuilder();
            int linesOnCurrentPage = 0;
            
            for (String entry : historyList) {
                // Calculer le nombre de lignes que prendra cette entrée
                int entryLines = calculateLines(entry);
                
                // Si l'ajout de cette entrée dépasse la limite, créer une nouvelle page
                if (linesOnCurrentPage + entryLines > LINES_PER_PAGE && currentPage.length() > 0) {
                    pages.add(currentPage.toString());
                    currentPage = new StringBuilder();
                    linesOnCurrentPage = 0;
                }
                
                // Ajouter l'entrée à la page actuelle
                if (currentPage.length() > 0) {
                    currentPage.append("\n\n");
                    linesOnCurrentPage += 2;
                }
                currentPage.append(entry);
                linesOnCurrentPage += entryLines;
            }
            
            // Ajouter la dernière page si elle n'est pas vide
            if (currentPage.length() > 0) {
                pages.add(currentPage.toString());
            }
            
            // Si aucune page n'a été créée, ajouter une page par défaut
            if (pages.isEmpty()) {
                pages.add("Aucun historique disponible.");
            }
            
            meta.setPages(pages);
            book.setItemMeta(meta);
        }
        
        return book;
    }
    
    /**
     * Calcule approximativement le nombre de lignes qu'une entrée prendra dans un livre
     */
    private int calculateLines(String text) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        
        // Approximation : environ 19 caractères par ligne dans un livre Minecraft
        int charactersPerLine = 19;
        int lines = (text.length() + charactersPerLine - 1) / charactersPerLine; // Division arrondie vers le haut
        
        // Compter aussi les retours à la ligne explicites
        long explicitNewlines = text.chars().filter(ch -> ch == '\n').count();
        
        return Math.max(1, lines + (int) explicitNewlines);
    }
}