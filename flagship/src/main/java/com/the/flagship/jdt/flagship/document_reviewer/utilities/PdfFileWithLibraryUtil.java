package com.the.flagship.jdt.flagship.document_reviewer.utilities;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component 
public class PdfFileWithLibraryUtil {

    public PdfFileWithLibraryUtil() {}

    @Bean 
    private PDFTextStripper stripper (){
        return new PDFTextStripper();
    }

    public String extractText(String path) throws Exception {

        Path filePath = Path.of(path);
        byte[] raw = Files.readAllBytes(filePath);
        try ( PDDocument document = Loader.loadPDF(raw)) {
            this.stripper().setSortByPosition(true);
            return this.stripper().getText(document);
        } catch (Exception e) {}

        return null;
    }
}
