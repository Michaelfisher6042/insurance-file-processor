package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackupService {

    @Value("${app.backup-dir:backup}")
    private String backupDir;

    private void moveToBackup(Path file) throws IOException {
        Path backup = Paths.get(backupDir);
        if (!Files.exists(backup)) Files.createDirectories(backup);
        Path target = backup.resolve(file.getFileName());
        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Moved file to backup: {}", target);
    }

    public void tryMoveToBackup(Path path) {
        try {
            moveToBackup(path);
        } catch (Exception me) {
            log.error("Failed to move skipped file to backup: {}", me.getMessage(), me);
        }
    }
}
