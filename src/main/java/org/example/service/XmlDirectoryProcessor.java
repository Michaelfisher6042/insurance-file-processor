package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@Service
@Slf4j
@RequiredArgsConstructor
public class XmlDirectoryProcessor {

    @Value("${app.input-dir:input}")
    private String inputDir;




    private final FileProcessorService fileProcessorService;
    public void processFiles() {
        // Resolve input directory: if relative, resolve against current working directory
        try {
            Path in = Paths.get(inputDir);
            log.info("Working directory: {}", in);
            if (!Files.exists(in)) {
                Files.createDirectories(in);
            }

            int processedCount = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(in, "*.xml")) {
                for (Path path : stream) {
                    processedCount++;
                    fileProcessorService.processSingleFile(path);
                }
            }
            log.info("Files found in directory: {}", processedCount);
        } catch (IOException e) {
            log.error("I/O error during file processing setup: {}", e.getMessage(), e);
        }
    }
}
