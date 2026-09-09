package com.the.flagship.jdt.flagship.document_reviewer;
import org.springframework.boot.SpringApplication;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CommandLineSample implements CommandLineRunner {

     private static final Logger LOG = (Logger) LoggerFactory
      .getLogger(CommandLineSample.class);
    @Override
    public void run(String... args) throws Exception {
        // TODO Auto-generated method stub
        //  for (String arg : args) {
            // System.out.println(arg);
        // }
        String myPath = "C:\\Users\\johnd\\Desktop\\Git Projects\\Impo_The-Flagship\\flagship\\README.MD";
        LOG.info("Running FILE READ START");
        LOG.info("CONTENT");
        try (BufferedReader reader = Files.newBufferedReader(Path.of(myPath))) {
            reader.lines().forEach((a)->LOG.info(a));
        }
        LOG.info("FIN");
    }

}
