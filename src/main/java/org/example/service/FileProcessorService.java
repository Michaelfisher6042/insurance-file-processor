// java
package org.example.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.model.EventEntity;
import org.example.model.ProductEntity;
import org.example.model.RequestDetailsEntity;
import org.example.repository.RequestDetailsRepository;
import org.example.xml.EventDto;
import org.example.xml.ProductDto;
import org.example.xml.RootRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@Slf4j
public class FileProcessorService {
    private final RequestDetailsRepository requestDetailsRepository;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${app.input-dir:./input}")
    private String inputDir;

    @Value("${app.backup-dir:./backup}")
    private String backupDir;


    private final DateTimeFormatter acceptDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

    @Autowired
    public FileProcessorService(RequestDetailsRepository requestDetailsRepository) {
        this.requestDetailsRepository = requestDetailsRepository;
    }

    @Scheduled(fixedDelayString = "${app.poll-interval-ms:6000}", initialDelayString = "${app.initial-delay-ms:0}")
    public void pollFolder() {
        try {
            log.debug("Scheduled poll triggered");
            processFiles();
        } catch (Exception e) {
            log.error("Error processing files: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processFiles() {
        // Resolve input directory: if relative, resolve against current working directory
        try {
            Path in = Paths.get(inputDir);
            String workingDir = System.getProperty("user.dir");
            if (!in.isAbsolute()) {
                in = Paths.get(workingDir).resolve(in).normalize();
            }
            log.info("Working directory: {}", workingDir);
            log.info("Processing files in: {}", in.toAbsolutePath());
            if (!Files.exists(in)) {
                Files.createDirectories(in);
                log.info("Created input directory: {}", in.toAbsolutePath());
            }
            log.info("Scanning input directory: {}", in.toAbsolutePath());

            // Use Files.list to enumerate and allow case-insensitive checks if needed
            int processedCount = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(in, "*.xml")) {
                log.info("Found XML files in directory, starting processing");
                for (Path path : stream) {
                    processedCount++;
                    processSingleFile(path, true); // scheduled runs move processed files to backup
                }
            }
            log.info("Files found in directory: {}", processedCount);
            log.info("Completed processing files in input directory");
        } catch (IOException e) {
            log.error("I/O error during file processing setup: {}", e.getMessage(), e);
        }
    }

    private void processSingleFile(Path path, boolean moveToBackupAfter) {
        try {
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                log.warn("Skipping non-regular or missing file: {}", path);
                return;
            }
            if (!Files.isReadable(path)) {
                log.warn("Skipping unreadable file (may be locked by another process): {}", path);
                return;
            }

            try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
                RootRequest root = xmlMapper.readValue(is, RootRequest.class);

                if (root == null || root.getRequestDetails() == null) {
                    log.warn("Skipping file, no requestDetails: {}", path);
                    if (moveToBackupAfter) {
                        try { moveToBackup(path); } catch (Exception me) { log.error("Failed to move skipped file to backup: {}", me.getMessage(), me); }
                    }
                    return;
                }

                RequestDetailsEntity rd = new RequestDetailsEntity();
                rd.setId(root.getRequestDetails().getId());
                // parse acceptDate tolerant
                try {
                    rd.setAcceptDate(LocalDateTime.parse(root.getRequestDetails().getAcceptDate(), acceptDateFormatter));
                } catch (Exception ex) {
                    try {
                        rd.setAcceptDate(LocalDateTime.parse(root.getRequestDetails().getAcceptDate()));
                    } catch (Exception ex2) {
                        rd.setAcceptDate(null);
                    }
                }
                rd.setSourceCompany(root.getRequestDetails().getSourceCompany());

                List<EventEntity> events = new ArrayList<>();
                if (root.getEvents() != null) {
                    for (EventDto ed : root.getEvents()) {
                        EventEntity e = new EventEntity();
                        e.setId(ed.getId());
                        e.setType(ed.getType());
                        e.setInsuredId(ed.getInsuredId());
                        e.setRequestDetails(rd);

                        List<ProductEntity> products = new ArrayList<>();
                        if (ed.getProducts() != null) {
                            for (ProductDto pd : ed.getProducts()) {
                                ProductEntity p = new ProductEntity();
                                p.setId(UUID.randomUUID().toString());
                                p.setType(pd.getType());
                                try {
                                    p.setPrice(new BigDecimal(pd.getPrice()));
                                } catch (Exception ex) {
                                    p.setPrice(null);
                                }
                                try { p.setStartDate(LocalDate.parse(pd.getStartDate())); } catch (Exception ex) { p.setStartDate(null); }
                                try { p.setEndDate(LocalDate.parse(pd.getEndDate())); } catch (Exception ex) { p.setEndDate(null); }
                                p.setEvent(e);
                                products.add(p);
                            }
                        }
                        e.setProducts(products);
                        events.add(e);
                    }
                }
                rd.setEvents(events);

                requestDetailsRepository.save(rd);
                log.info("Saved data from file: {}", path.getFileName());

                if (moveToBackupAfter) {
                    try {
                        moveToBackup(path);
                    } catch (Exception me) {
                        log.error("Failed to move processed file to backup: {}", me.getMessage(), me);
                    }
                } else {
                    log.info("Startup run: not moving file to backup so it remains for verification: {}", path.getFileName());
                }
            }
        } catch (AccessDeniedException ade) {
            log.warn("Access denied when reading file (will retry later): {}", path);
        } catch (Exception ex) {
            log.error("Failed to process {} : {}", path, ex.getMessage(), ex);
            if (moveToBackupAfter) {
                try { moveToBackup(path); } catch (Exception moveEx) { log.error("Failed to move bad file: {}", moveEx.getMessage(), moveEx); }
            }
        }
    }

    private void moveToBackup(Path file) throws IOException {
        Path backup = Paths.get(backupDir);
        if (!Files.exists(backup)) Files.createDirectories(backup);
        Path target = backup.resolve(file.getFileName());
        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Moved file to backup: {}", target);
    }
}