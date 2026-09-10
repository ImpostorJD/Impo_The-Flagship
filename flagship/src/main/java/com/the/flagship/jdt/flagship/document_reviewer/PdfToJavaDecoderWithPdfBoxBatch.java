package com.the.flagship.jdt.flagship.document_reviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.the.flagship.jdt.flagship.document_reviewer.utilities.PdfFileWithLibraryUtil;

@Component 
@ConditionalOnProperty(name = "app.runner", havingValue = "pdf-box") //<-- added this for profiling
public class PdfToJavaDecoderWithPdfBoxBatch implements CommandLineRunner {


    PdfFileWithLibraryUtil pdfExporterService;
    public static final Logger LOG = (Logger)LoggerFactory.getLogger(PdfToJavaDecoderWithPdfBoxBatch.class);
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("DEBUG: run() was called, args.length = " + args.length);
        if (args.length == 0) {
            System.out.println("DEBUG: no args received, exiting");
            return;
        }
        System.out.println("DEBUG: filePath = " + args[0]);

        String filePath = args[0];
        String text = this.pdfExporterService.extractText(filePath);
        System.out.println("DEBUG: extracted text length = " + text.length());
        System.out.print(text);
    }

     public PdfToJavaDecoderWithPdfBoxBatch(PdfFileWithLibraryUtil pdfExporterUtil) {
        LOG.info("Autowired PdfExporterUtil");
        this.pdfExporterService = pdfExporterUtil;
    }

}
