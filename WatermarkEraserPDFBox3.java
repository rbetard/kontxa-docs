import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFContentStreamEditor;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WatermarkEraserPDFBox3 {

    public static void removeWatermark(File inputPdf, File outputPdf, String targetHexOrText) throws IOException {
        // Chargement du document avec l'API standard PDFBox 3.x
        try (PDDocument document = Loader.loadPDF(inputPdf)) {
            
            // Uniformisation de la cible en hexadécimal majuscule
            String targetHex = targetHexOrText.matches("^[0-9A-Fa-f]+$") 
                    ? targetHexOrText.toUpperCase() 
                    : stringToHex(targetHexOrText);

            // Itération moderne sur les pages (PDFBox 3)
            for (PDPage page : document.getPages()) {

                // Éditeur de flux de contenu (basé sur PDFStreamEngine)
                PDFContentStreamEditor editor = new PDFContentStreamEditor(document, page) {
                    @Override
                    protected void write(Operator operator, List<COSBase> operands) throws IOException {
                        String opName = operator.getName();

                        // 1. Cas de l'opérateur 'Tj' (Chaîne de texte simple)
                        if ("Tj".equals(opName) && !operands.isEmpty()) {
                            if (operands.get(0) instanceof COSString) {
                                COSString cosString = (COSString) operands.get(0);
                                String currentHex = cosString.getHexString().toUpperCase();

                                if (currentHex.contains(targetHex)) {
                                    // Effacement du contenu en vidant les octets
                                    cosString.setValue(new byte[0]);
                                }
                            }
                        }
                        // 2. Cas de l'opérateur 'TJ' (Tableau de chaînes espacées)
                        else if ("TJ".equals(opName) && !operands.isEmpty()) {
                            if (operands.get(0) instanceof COSArray) {
                                COSArray cosArray = (COSArray) operands.get(0);
                                for (int j = 0; j < cosArray.size(); j++) {
                                    COSBase element = cosArray.get(j);
                                    if (element instanceof COSString) {
                                        COSString cosString = (COSString) element;
                                        String currentHex = cosString.getHexString().toUpperCase();
                                        
                                        if (currentHex.contains(targetHex)) {
                                            cosString.setValue(new byte[0]);
                                        }
                                    }
                                }
                            }
                        }

                        super.write(operator, operands);
                    }
                };

                editor.processPage(page);
            }

            // Enregistrement du fichier nettoyé (PDFBox 3 compresse par défaut)
            document.save(outputPdf);
            System.out.println("Filigrane supprimé avec succès. Fichier généré : " + outputPdf.getAbsolutePath());
        }
    }

    // Utilitaire de conversion texte vers Hexadécimal
    private static String stringToHex(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(String.format("%02X", (int) c));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        try {
            File input = new File("input_decompressed.pdf");
            File output = new File("output_sans_watermark.pdf");
            
            // Texte en clair du filigrane ou sa valeur Hex exacte trouvée précédemment
            String watermarkText = "CONFIDENTIEL"; 

            removeWatermark(input, output, watermarkText);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}