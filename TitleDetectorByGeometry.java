import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TitleDetectorByGeometry {

    // Classe utilitaire pour stocker les métadonnées d'une ligne
    static class LineInfo {
        String text;
        int height;

        public LineInfo(String text, int height) {
            this.text = text;
            this.height = height;
        }
    }

    public static void main(String[] args) {
        File imageFile = new File("chemin/vers/votre_image.png");
        Path hocrPath = Path.of("resultat.hocr");

        ITesseract tesseract = new Tesseract();
        tesseract.setLanguage("fra");
        tesseract.setVariable("hocr", "T"); // Active la sortie hOCR

        try {
            // 1. Exécuter l'OCR et sauvegarder le hOCR
            String hocrOutput = tesseract.doOCR(imageFile);
            Files.writeString(hocrPath, hocrOutput, StandardCharsets.UTF_8);

            // 2. Parser le fichier hOCR avec Jsoup
            Document doc = Jsoup.parse(hocrPath.toFile(), "UTF-8");
            Elements lines = doc.select(".ocr_line");

            List<LineInfo> lineList = new ArrayList<>();
            double totalHeight = 0;

            // 3. Extraire le texte et calculer la hauteur de chaque ligne via le 'bbox'
            for (Element line : lines) {
                String titleAttr = line.attr("title"); // Ex: "bbox 50 100 300 130; baseline 0 -2"
                
                if (titleAttr.contains("bbox")) {
                    String[] parts = titleAttr.split(";");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("bbox")) {
                            String[] coords = part.substring(5).trim().split("\\s+");
                            if (coords.length == 4) {
                                int y0 = Integer.parseInt(coords[1]);
                                int y1 = Integer.parseInt(coords[3]);
                                int height = y1 - y0; // Hauteur de la ligne en pixels
                                String text = line.text().trim();

                                if (!text.isEmpty()) {
                                    lineList.add(new LineInfo(text, height));
                                    totalHeight += height;
                                }
                            }
                        }
                    }
                }
            }

            if (lineList.isEmpty()) {
                System.out.println("Aucun texte détecté.");
                return;
            }

            // 4. Calculer la hauteur moyenne des lignes du document
            double avgHeight = totalHeight / lineList.size();
            System.out.println("Hauteur de ligne moyenne (corps de texte) : " + String.format("%.2f", avgHeight) + " px\n");

            System.out.println("=== Titres potentiels détectés par taille ===");
            // Seuil : Une ligne est un titre si sa hauteur dépasse 1.3 fois la hauteur moyenne
            double threshold = avgHeight * 1.3;

            for (LineInfo info : lineList) {
                if (info.height > threshold) {
                    System.out.println("[Titre - " + info.height + "px] " + info.text);
                }
            }

        } catch (TesseractException e) {
            System.err.println("Erreur OCR : " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur Fichier / Jsoup : " + e.getMessage());
        }
    }
}