import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

public class GenererDocx {

    record Chapitre(String titre, List<String> paragraphes) {}

    public static void main(String[] args) throws Exception {
        Path destination = Path.of("rapport.docx");
        String nomFichier = destination.getFileName().toString();

        List<Chapitre> chapitres = List.of(
            new Chapitre("Introduction", List.of(
                "Premier paragraphe de l’introduction.",
                "Deuxième paragraphe."
            )),
            new Chapitre("Méthodologie", List.of(
                "Description de la méthode.",
                "Détails complémentaires."
            )),
            new Chapitre("Conclusion", List.of(
                "Conclusion du document."
            ))
        );

        try (XWPFDocument doc = new XWPFDocument()) {
            String date = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            for (int i = 0; i < chapitres.size(); i++) {
                Chapitre chapitre = chapitres.get(i);

                // Chaque chapitre commence sur une nouvelle page.
                if (i > 0) {
                    doc.createParagraph().setPageBreak(true);
                }

                // Une section par chapitre permet d'avoir son propre en-tête.
                XWPFParagraph titre = doc.createParagraph();
                titre.setStyle("Heading1");
                titre.createRun().setText(chapitre.titre());

                for (String texte : chapitre.paragraphes()) {
                    doc.createParagraph().createRun().setText(texte);
                }

                // L'en-tête de la section doit être créé après l'ajout de la section.
                XWPFHeader header = doc.createHeader(HeaderFooterType.DEFAULT);
                XWPFParagraph hp = header.createParagraph();
                hp.setAlignment(ParagraphAlignment.CENTER);

                ajouterTexte(hp, nomFichier + "  |  " + date
                        + "  |  " + chapitre.titre()
                        + "  |  Page ");

                ajouterChamp(hp, "PAGE");
                ajouterTexte(hp, " / ");
                ajouterChamp(hp, "NUMPAGES");

                // Pour une vraie séparation de sections, utiliser un paragraphe
                // de fin de section plutôt qu'un simple saut de page.
            }

            try (OutputStream out = Files.newOutputStream(destination)) {
                doc.write(out);
            }
        }
    }

    private static void ajouterTexte(XWPFParagraph p, String texte) {
        p.createRun().setText(texte);
    }

    private static void ajouterChamp(XWPFParagraph p, String instruction) {
        XWPFRun run = p.createRun();

        run.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);

        run = p.createRun();
        run.getCTR().addNewInstrText().setStringValue(instruction);

        run = p.createRun();
        run.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);

        // Valeur affichée avant recalcul par Word/LibreOffice.
        run = p.createRun();
        run.setText("1");

        run = p.createRun();
        run.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
    }
}
