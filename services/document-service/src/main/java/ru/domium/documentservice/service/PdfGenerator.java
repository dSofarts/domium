package ru.domium.documentservice.service;

import ru.domium.documentservice.model.DocumentTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

@Component
public class PdfGenerator {

  public GeneratedPdf generate(DocumentTemplate template, Map<String, Object> data) {
    // MVP stub: a simple 1-page PDF with template metadata and a few data keys.
    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage();
      doc.addPage(page);
      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
        cs.newLineAtOffset(50, 750);
        cs.showText("Document: " + template.getName() + " (" + template.getCode() + ")");
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(50, 720);
        cs.showText("Generated at: " + Instant.now());
        cs.endText();

        int y = 690;
        for (var e : data.entrySet()) {
          cs.beginText();
          cs.setFont(PDType1Font.HELVETICA, 11);
          cs.newLineAtOffset(50, y);
          cs.showText(e.getKey() + ": " + String.valueOf(e.getValue()));
          cs.endText();
          y -= 18;
          if (y < 80) break;
        }
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      doc.save(baos);
      return new GeneratedPdf(baos.toByteArray(), "application/pdf", template.getCode() + ".pdf");
    } catch (IOException e) {
      throw new IllegalStateException("PDF generation failed: " + e.getMessage(), e);
    }
  }

  public record GeneratedPdf(byte[] bytes, String contentType, String filename) {}
}
