package com.the.flagship.jdt.flagship.document_reviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.the.flagship.jdt.flagship.document_reviewer.utilities.PdfExporterUtil;

@Component
@ConditionalOnProperty(name = "app.runner", havingValue = "pdf-java") //<-- added this for profiling
public class PdfToJavaReadableDecoderBatch implements CommandLineRunner {

    private PdfExporterUtil pdfExporterService;
    private static final Logger LOG = (Logger) LoggerFactory
      .getLogger(PdfToJavaReadableDecoderBatch.class);

    public PdfToJavaReadableDecoderBatch(PdfExporterUtil pdfExporterUtil) {
        LOG.info("Autowired PdfExporterUtil");
        this.pdfExporterService = pdfExporterUtil;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DEBUG: run() was called, args.length = " + args.length);
        if (args.length == 0) {
            System.out.println("DEBUG: no args received, exiting");
            return;
        }
        System.out.println("DEBUG: filePath = " + args[0]);

        String filePath = args[0];
        String text = pdfExporterService.extractText(filePath);
        System.out.println("DEBUG: extracted text length = " + text.length());
        System.out.print(text);
    }

}
