package uppa.group2.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    private FileUtils() {}

    /**
     * Lit un fichier en bytes.
     */
    public static byte[] readFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Sauvegarde des bytes dans un fichier.
     * Si le fichier existe déjà, ajoute un suffixe numéroté.
     */
    public static File saveFile(String directory, String fileName, byte[] data) throws IOException {
        Path dir = Paths.get(directory);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path target = dir.resolve(fileName);

        // Eviter l'écrasement
        if (Files.exists(target)) {
            String baseName = fileName;
            String ext = "";
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx > 0) {
                baseName = fileName.substring(0, dotIdx);
                ext = fileName.substring(dotIdx);
            }
            int counter = 1;
            while (Files.exists(target)) {
                target = dir.resolve(baseName + "_" + counter + ext);
                counter++;
            }
        }

        Files.write(target, data);
        return target.toFile();
    }

    /**
     * Retourne le répertoire de téléchargement par défaut.
     */
    public static String getDefaultDownloadDir() {
        return System.getProperty("user.home") + File.separator + "P2PChat_Downloads";
    }
}
