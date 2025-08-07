package TestJava.testjava.services;

import TestJava.testjava.TestJava;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

public class DatabaseBackupService {
    private static final String BACKUP_FOLDER = "database_backups";
    private static final int MAX_BACKUPS = 10;

    /**
     * Crée une sauvegarde de la base de données
     * @return le chemin du backup créé, ou null en cas d'erreur
     */
    public static Path createBackup() {
        try {
            // Création du dossier de backup s'il n'existe pas
            File backupDir = new File(TestJava.getInstance().getDataFolder(), BACKUP_FOLDER);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // Génération du nom du fichier de backup avec timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path backupPath = backupDir.toPath().resolve("backup_" + timestamp);

            // Copie des fichiers de la base de données
            Path dbFolder = Paths.get(TestJava.getInstance().getJsonLocation());
            copyFolder(dbFolder, backupPath);

            // Nettoyage des vieux backups
            cleanOldBackups(backupDir);

            Bukkit.getLogger().info("[Backup] Base de données sauvegardée dans " + backupPath);
            return backupPath;

        } catch (Exception e) {
            Bukkit.getLogger().severe("[Backup] Erreur lors de la création du backup: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Restaure une sauvegarde spécifique
     * @param backupPath chemin du backup à restaurer
     * @return true si la restauration a réussi
     */
    public static boolean restoreBackup(Path backupPath) {
        try {
            if (!Files.exists(backupPath)) {
                throw new IOException("Le backup n'existe pas: " + backupPath);
            }

            // Fermeture de la connexion à la base de données
            TestJava.getInstance().closeDatabase();

            // Suppression des fichiers actuels
            Path dbFolder = Paths.get(TestJava.getInstance().getJsonLocation());
            deleteFolder(dbFolder);

            // Copie des fichiers du backup
            copyFolder(backupPath, dbFolder);

            // Réinitialisation de la connexion
            TestJava.getInstance().initDatabase();

            Bukkit.getLogger().info("[Backup] Base de données restaurée depuis " + backupPath);
            return true;

        } catch (Exception e) {
            Bukkit.getLogger().severe("[Backup] Erreur lors de la restauration du backup: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private static void deleteFolder(Path folder) throws IOException {
        if (Files.exists(folder)) {
            Files.walk(folder)
                .sorted((p1, p2) -> -p1.compareTo(p2))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }

    private static void cleanOldBackups(File backupDir) {
        File[] backups = backupDir.listFiles();
        if (backups != null && backups.length > MAX_BACKUPS) {
            // Tri par date de modification (plus ancien en premier)
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            
            // Suppression des backups les plus anciens
            for (int i = 0; i < backups.length - MAX_BACKUPS; i++) {
                try {
                    deleteFolder(backups[i].toPath());
                } catch (IOException e) {
                    Bukkit.getLogger().warning("[Backup] Impossible de supprimer l'ancien backup: " + backups[i]);
                }
            }
        }
    }
}
