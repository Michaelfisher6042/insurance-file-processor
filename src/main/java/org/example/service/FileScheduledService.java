package org.example.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class FileScheduledService {

    private final XmlDirectoryProcessor xmlDirectoryProcessor;


    @Scheduled(fixedDelayString = "${app.poll-interval-ms:60000}", initialDelayString = "${app.initial-delay-ms:0}")
    public void Schedule() {
        try {
            log.debug("Scheduled poll triggered");
            xmlDirectoryProcessor.processFiles();
        } catch (Exception e) {
            log.error("Error processing files: {}", e.getMessage(), e);
        }
    }
}
