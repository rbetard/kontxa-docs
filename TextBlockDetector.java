import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class TextBlockDetector {

    // Structure simple pour stocker les coordonnées d'une zone de texte
    public static class BoundingBox {
        int x, y, width, height;
        public BoundingBox(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static void detectTextBlocks(String inputPath, String outputPath) {
        try {
            // 1. Charger l'image originale
            BufferedImage original = ImageIO.read(new File(inputPath));
            int width = original.getWidth();
            int height = original.getHeight();

            // 2. Convertir en binaire (Noir et Blanc pur) - en reprenant votre logique de seuil à 80
            int[][] binary = new int[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color c = new Color(original.getRGB(x, y));
                    int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                    // Texte = Noir (0), Fond = Blanc (1)
                    binary[x][y] = (gray < 80) ? 0 : 1; 
                }
            }

            // 3. Projection Horizontale (Compter les pixels noirs par ligne)
            int[] horizontalProjection = new int[height];
            for (int y = 0; y < height; y++) {
                int blackPixels = 0;
                for (int x = 0; x < width; x++) {
                    if (binary[x][y] == 0) blackPixels++;
                }
                horizontalProjection[y] = blackPixels;
            }

            // 4. Découpage en blocs verticaux (lignes de texte) basés sur la projection
            List<BoundingBox> textBlocks = new ArrayList<>();
            boolean inBlock = false;
            int blockStartY = 0;
            
            // Seuil minimal de pixels noirs pour considérer qu'une ligne contient du texte
            int minPixelsPerLine = width / 100; 

            for (int y = 0; y < height; y++) {
                if (horizontalProjection[y] > minPixelsPerLine) {
                    if (!inBlock) {
                        inBlock = true;
                        blockStartY = y;
                    }
                } else {
                    if (inBlock) {
                        inBlock = false;
                        int blockHeight = y - blockStartY;
                        // Filtrer les tout petits bruits de poussière
                        if (blockHeight > 8) { 
                            textBlocks.add(new BoundingBox(0, blockStartY, width, blockHeight));
                        }
                    }
                }
            }

            // 5. Dessiner les rectangles détectés sur une copie de l'image pour visualisation
            BufferedImage annotatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = annotatedImage.createGraphics();
            g2d.drawImage(original, 0, 0, null);
            g2d.setColor(Color.RED);
            g2d.setStroke(new java.awt.BasicStroke(2));

            System.out.println("--- Zones de texte détectées (" + textBlocks.size() + " blocs) ---");
            for (int i = 0; i < textBlocks.size(); i++) {
                BoundingBox box = textBlocks.get(i);
                System.out.println("Bloc " + (i+1) + " -> Y: " + box.y + ", Hauteur: " + box.height);
                g2d.drawRect(box.x, box.y, box.width, box.height);
            }
            g2d.dispose();

            // 6. Sauvegarder l'image avec les boîtes de texte surlignées
            ImageIO.write(annotatedImage, "png", new File(outputPath));
            System.out.println("Image annotée enregistrée sous : " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        detectTextBlocks("journal_nettoye.png", "journal_zones_detectees.png");
    }
}