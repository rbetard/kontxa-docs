import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class GraphicZoneDetector {

    public static class Region {
        int x, y, width, height;
        public Region(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    public static void detectGraphics(String inputPath, String outputPath) {
        try {
            BufferedImage original = ImageIO.read(new File(inputPath));
            int width = original.getWidth();
            int height = original.getHeight();

            // 1. Convertir en binaire (Noir = 0, Blanc = 1) avec le seuil adapté
            int[][] binary = new int[width][height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color c = new Color(original.getRGB(x, y));
                    int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                    binary[x][y] = (gray < 80) ? 0 : 1; // 0 pour l'encre/noir, 1 pour le fond
                }
            }

            // 2. Grille de marquage pour éviter de revisiter les pixels
            boolean[][] visited = new boolean[width][height];
            List<Region> graphicRegions = new ArrayList<>();

            // 3. Parcourir l'image pour trouver les "blobs" (composantes connexes)
            for (int y = 0; y < height; y += 5) { // Échantillonnage pour aller plus vite
                for (int x = 0; x < width; x += 5) {
                    if (binary[x][y] == 0 && !visited[x][y]) {
                        // Lancer une recherche de voisinage (Flood Fill simplifié) pour délimiter le bloc
                        Region region = floodFillBlob(binary, visited, x, y, width, height);

                        // Filtrer pour ne garder que les objets de grande taille (photos/dessins)
                        // Une ligne de texte est fine, une photo a une surface importante et un ratio équilibré
                        if (region.width > 80 && region.height > 80) {
                            // S'assurer que ce n'est pas toute la page
                            if (region.width < width * 0.9 && region.height < height * 0.9) {
                                graphicRegions.add(region);
                            }
                        }
                    }
                }
            }

            // 4. Dessiner des rectangles bleus autour des photos/dessins détectés
            BufferedImage annotated = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = annotated.createGraphics();
            g2d.drawImage(original, 0, 0, null);
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new java.awt.BasicStroke(3));

            System.out.println("--- Zones de dessins/photos détectées (" + graphicRegions.size() + ") ---");
            for (int i = 0; i < graphicRegions.size(); i++) {
                Region r = graphicRegions.get(i);
                System.out.println("Photo/Dessin " + (i+1) + " -> X: " + r.x + ", Y: " + r.y + 
                                   ", Largeur: " + r.width + ", Hauteur: " + r.height);
                g2d.drawRect(r.x, r.y, r.width, r.height);
            }
            g2d.dispose();

            // 5. Sauvegarder le résultat
            ImageIO.write(annotated, "png", new File(outputPath));
            System.out.println("Image des zones graphiques enregistrée : " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode utilitaire de Flood Fill pour regrouper les pixels d'un même bloc
    private static Region floodFillBlob(int[][] binary, boolean[][] visited, int startX, int startY, int imgWidth, int imgHeight) {
        int minX = startX, maxX = startX;
        int minY = startY, maxY = startY;

        List<int[]> queue = new ArrayList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        int index = 0;
        // Limiter la taille de la file pour éviter les surcharges sur les grands fonds noirs
        while (index < queue.size() && queue.size() < 50000) {
            int[] p = queue.get(index++);
            int cx = p[0];
            int cy = p[1];

            minX = Math.min(minX, cx);
            maxX = Math.max(maxX, cx);
            minY = Math.min(minY, cy);
            maxY = Math.max(maxY, cy);

            // Vérifier les 4 directions voisines (avec un pas pour accélérer)
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i] * 5;
                int ny = cy + dy[i] * 5;
                if (nx >= 0 && nx < imgWidth && ny >= 0 && ny < imgHeight) {
                    if (binary[nx][ny] == 0 && !visited[nx][ny]) {
                        visited[nx][ny] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }

        return new Region(minX, minY, (maxX - minX), (maxY - minY));
    }

    public static void main(String[] args) {
        detectGraphics("journal_vieilli.jpg", "journal_photos_detectees.png");
    }
}