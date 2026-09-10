package com.the.flagship.jdt.flagship.document_reviewer;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.file-read.enabled", havingValue = "true") //<-- checks if app.file-read.enabled is configured true. otherwise dont execute
public class CommandLineSample implements CommandLineRunner {

     private static final Logger LOG = (Logger) LoggerFactory
      .getLogger(CommandLineSample.class);
        @Override
        public void run(String... args) throws Exception {
            
            for (String arg : args) {
                LOG.info(arg);
                System.out.println(arg);
            }
            String myPath = "C:\\Users\\johnd\\Desktop\\Git Projects\\Impo_The-Flagship\\flagship\\README.MD";
            LOG.info("Running FILE READ START");
            LOG.info("CONTENT");
            try (BufferedReader reader = Files.newBufferedReader(Path.of(myPath))) {
                reader.lines().forEach((a)->LOG.info(a));
            }
            LOG.info("FIN");
        }

}
