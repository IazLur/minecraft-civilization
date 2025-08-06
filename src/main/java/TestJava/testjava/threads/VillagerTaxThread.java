package TestJava.testjava.threads;

import TestJava.testjava.services.TaxService;
import org.bukkit.Bukkit;

/**
 * Thread qui s'exécute toutes les 5 minutes pour collecter les impôts des villageois
 */
public class VillagerTaxThread implements Runnable {

    @Override
    public void run() {
        try {
            // Collecter les impôts de tous les villageois ayant un métier
            TaxService.collectTaxes();
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("[VillagerTaxThread] Erreur lors de la collecte d'impôts: " + e.getMessage());
            e.printStackTrace();
        }
    }
}