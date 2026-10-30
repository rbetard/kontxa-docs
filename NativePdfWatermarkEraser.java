import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class NativePdfWatermarkEraser {

    /**
     * Classe utilitaire de référence pour éditer à la volée les flux d'une page PDF (compatible PDFBox 3.x)
     */
    public static abstract class PdfContentStreamEditor extends PDFStreamEngine {
        protected final PDDocument document;
        protected final PDPage page;
        private ContentStreamWriter contentStreamWriter;

        public PdfContentStreamEditor(PDDocument document, PDPage page) {
            this.document = document;
            this.page = page;
        }

        public void processPage() throws IOException {
            PDStream newContents = new PDStream(document);
            try (var outputStream = newContents.createOutputStream(COSName.FLATE_DECODE)) {
                contentStreamWriter = new ContentStreamWriter(outputStream);
                super.process(page);
            }
            page.setContents(newContents);
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            write(contentStreamWriter, operator, operands);
        }

        protected void write(ContentStreamWriter writer, Operator operator, List<COSBase> operands) throws IOException {
            writer.write(operator, operands);
        }
    }

    public static void removeWatermark(File inputPdf, File outputPdf, String targetTextOrHex) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputPdf)) {
            
            // Uniformisation de la cible en hexadécimal majuscule (pour cibler l'hexa trouvé dans le PDF)
            String targetHex = targetTextOrHex.matches("^[0-9A-Fa-f]+$") 
                    ? targetTextOrHex.toUpperCase() 
                    : stringToHex(targetTextOrHex);

            // Parcours de toutes les pages du document (API PDFBox 3)
            for (PDPage page : document.getPages()) {
                
                PdfContentStreamEditor editor = new PdfContentStreamEditor(document, page) {
                    @Override
                    protected void write(ContentStreamWriter writer, Operator operator, List<COSBase> operands) throws IOException {
                        String opName = operator.getName();

                        // 1. Traitement de l'opérateur 'Tj' (Texte simple)
                        if ("Tj".equals(opName) && !operands.isEmpty() && operands.get(0) instanceof COSString) {
                            COSString cosString = (COSString) operands.get(0);
                            if (cosString.getHexString().toUpperCase().contains(targetHex)) {
                                cosString.setValue(new byte[0]); // Efface le contenu textuel
                            }
                        }
                        // 2. Traitement de l'opérateur 'TJ' (Tableau de texte / glyphes espacés)
                        else if ("TJ".equals(opName) && !operands.isEmpty() && operands.get(0) instanceof COSArray) {
                            COSArray cosArray = (COSArray) operands.get(0);
                            for (int i = 0; i < cosArray.size(); i++) {
                                COSBase element = cosArray.get(i);
                                if (element instanceof COSString) {
                                    COSString cosString = (COSString) element;
                                    if (cosString.getHexString().toUpperCase().contains(targetHex)) {
                                        cosString.setValue(new byte[0]);
                                    }
                                }
                            }
                        }

                        // Réécriture des instructions (modifiées ou intactes) dans le nouveau flux
                        super.write(writer, operator, operands);
                    }
                };

                editor.processPage();
            }

            // Enregistrement final du document nettoyé
            document.save(outputPdf);
            System.out.println("Filigrane supprimé avec succès dans : " + outputPdf.getAbsolutePath());
        }
    }

    private static String stringToHex(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(String.format("%02X", (int) c));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        try {
            File input = new File("input.pdf");
            File output = new File("output_clean.pdf");
            
            // Indiquez soit le texte brut (ex: "CONFIDENTIEL"), 
            // soit directement la chaîne hexadécimale trouvée dans le flux
            removeWatermark(input, output, "CONFIDENTIEL");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}