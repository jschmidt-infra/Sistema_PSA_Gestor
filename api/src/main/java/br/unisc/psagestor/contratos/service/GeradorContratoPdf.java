package br.unisc.psagestor.contratos.service;

import br.unisc.psagestor.comum.api.FalhaApi;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/** Modelo preliminar versionado; texto definitivo depende da validação das partes. */
@Component
public class GeradorContratoPdf {
    public byte[] gerar(List<String> paragrafos) {
        try (var doc = new PDDocument();
                var bytes = new ByteArrayOutputStream()) {
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var linhas = new ArrayList<String>();
            for (String paragrafo : paragrafos) {
                StringBuilder linha = new StringBuilder();
                for (String palavra : limpar(paragrafo, font).split("\\s+")) {
                    if (font.getStringWidth(palavra) / 1000 * 11 > 490) {
                        if (!linha.isEmpty()) {
                            linhas.add(linha.toString());
                            linha.setLength(0);
                        }
                        for (int i = 0; i < palavra.length(); i++) {
                            if (font.getStringWidth(linha.toString() + palavra.charAt(i))
                                            / 1000
                                            * 11
                                    > 490) {
                                linhas.add(linha.toString());
                                linha.setLength(0);
                            }
                            linha.append(palavra.charAt(i));
                        }
                        continue;
                    }
                    if (font.getStringWidth(linha + " " + palavra) / 1000 * 11 > 490
                            && !linha.isEmpty()) {
                        linhas.add(linha.toString());
                        linha.setLength(0);
                    }
                    if (!linha.isEmpty()) linha.append(' ');
                    linha.append(palavra);
                }
                linhas.add(linha.toString());
                linhas.add("");
            }
            for (int inicio = 0; inicio < linhas.size(); inicio += 44) {
                var page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (var content = new PDPageContentStream(doc, page)) {
                    content.beginText();
                    content.setFont(font, 11);
                    content.setLeading(16);
                    content.newLineAtOffset(50, 790);
                    for (String linha :
                            linhas.subList(inicio, Math.min(inicio + 44, linhas.size()))) {
                        content.showText(linha);
                        content.newLine();
                    }
                    content.endText();
                    content.beginText();
                    content.setFont(font, 9);
                    content.newLineAtOffset(50, 35);
                    content.showText(
                            "PSA Gestor | Modelo PRELIMINAR-1 | Página " + doc.getNumberOfPages());
                    content.endText();
                }
            }
            doc.getDocumentInformation().setTitle("Contrato preliminar de PSA");
            doc.save(bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new FalhaApi(503, "PDF_INDISPONIVEL", "Não foi possível gerar o documento.");
        }
    }

    private String limpar(String texto, PDType1Font font) {
        StringBuilder out = new StringBuilder();
        texto.codePoints()
                .forEach(
                        cp -> {
                            String s = new String(Character.toChars(cp));
                            try {
                                font.encode(s);
                                out.append(s);
                            } catch (IOException | IllegalArgumentException e) {
                                out.append(' ');
                            }
                        });
        return out.toString();
    }
}
