
package org.example.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entities.RequestDetailsEntity;
import org.example.repository.RequestDetailsRepository;
import org.example.entities.XmlRootRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class FileProcessorService {
    private final RequestDetailsRepository requestDetailsRepository;
    private final RequestDetailsService requestDetailsService;
    private final BackupService backupService;

    protected XmlMapper xmlMapper = XmlMapper.builder().addModule(new JavaTimeModule()).build();


    @Transactional
    public void processSingleFile(Path path) {
        try {
            if (isFilesAreUnreadable(path)) return;

            try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
                XmlRootRequest root = xmlMapper.readValue(is, XmlRootRequest.class);
                if (root == null || root.getRequestDetails() == null) {
                    log.warn("Skipping file, no requestDetails: {}", path);
                    backupService.tryMoveToBackup(path);
                    return;
                }
                RequestDetailsEntity requestDetails = requestDetailsService.getRequestDetailsEntity(root);
                requestDetailsRepository.save(requestDetails);
                log.info("Saved data from file: {}", path.getFileName());
                backupService.tryMoveToBackup(path);
            }
        } catch (AccessDeniedException ade) {
            log.warn("Access denied when reading file (will retry later): {}", path);
        } catch (Exception ex) {
            log.error("Failed to process {} : {}", path, ex.getMessage(), ex);
            backupService.tryMoveToBackup(path);
        }
    }


    private static boolean isFilesAreUnreadable(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.warn("Skipping non-regular or missing file: {}", path);
            return true;
        }
        if (!Files.isReadable(path)) {
            log.warn("Skipping unreadable file (may be locked by another process): {}", path);
            return true;
        }
        return false;
    }


}
