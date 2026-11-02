import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class DocumentEnhancer {

    public static void enhanceJournalImage(String inputPath, String outputPath, int thresholdValue) {
        try {
            // 1. Charger l'image originale
            BufferedImage originalImage = ImageIO.read(new File(inputPath));
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // 2. Créer une image de sortie en niveaux de gris/RGB
            BufferedImage enhancedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

            // 3. Parcourir chaque pixel pour appliquer le seuillage
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = originalImage.getRGB(x, y);
                    Color color = new Color(rgb);

                    // Calcul de la luminance (moyenne pondérée ou simple moyenne des canaux)
                    int gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3;

                    // Application du seuil : si plus sombre que le seuil -> Noir, sinon -> Blanc
                    if (gray < thresholdValue) {
                        enhancedImage.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        enhancedImage.setRGB(x, y, Color.WHITE.getRGB());
                    }
                }
            }

            // 4. Sauvegarder l'image améliorée
            ImageIO.write(enhancedImage, "png", new File(outputPath));
            System.out.println("Image améliorée enregistrée avec succès sous : " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Exemple d'utilisation (ajustez le seuil entre 100 et 200 selon l'éclairage)
        enhanceJournalImage("journal_vieilli.jpg", "journal_nettoye.png", 150);
    }
}