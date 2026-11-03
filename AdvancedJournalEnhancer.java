import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class AdvancedJournalEnhancer {

    public static void adaptiveThreshold(String inputPath, String outputPath, int windowSize, int cValue) {
        try {
            BufferedImage original = ImageIO.read(new File(inputPath));
            int width = original.getWidth();
            int height = original.getHeight();
            
            // Convertir en niveaux de gris au préalable
            int[][] grayMatrix = new int[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color c = new Color(original.getRGB(x, y));
                    grayMatrix[x][y] = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                }
            }

            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
            int halfWindow = windowSize / 2;

            // Parcours avec calcul de la moyenne locale
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int sum = 0;
                    int count = 0;

                    // Calcul de la moyenne dans le voisinage local (fenêtre)
                    for (int wy = Math.max(0, y - halfWindow); wy <= Math.min(height - 1, y + halfWindow); wy++) {
                        for (int wx = Math.max(0, x - halfWindow); wx <= Math.min(width - 1, x + halfWindow); wx++) {
                            sum += grayMatrix[wx][wy];
                            count++;
                        }
                    }

                    int threshold = (sum / count) - cValue;

                    // Application du seuil local
                    if (grayMatrix[x][y] < threshold) {
                        result.setRGB(x, y, Color.BLACK.getRGB());
                    } else {
                        result.setRGB(x, y, Color.WHITE.getRGB());
                    }
                }
            }

            ImageIO.write(result, "png", new File(outputPath));
            System.out.println("Traitement adaptatif terminé avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // windowSize = 15 (taille de la zone), cValue = 10 (constante de contraste)
        adaptiveThreshold("journal_vieilli.jpg", "journal_adapte.png", 15, 10);
    }
}