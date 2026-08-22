package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.SettingsDto;
import com.pms.pmsbackend.service.BackupService;
import com.pms.pmsbackend.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final BackupService backupService;

    @GetMapping
    public SettingsDto get() {
        return settingsService.get();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public SettingsDto update(@Valid @RequestBody SettingsDto dto) {
        return settingsService.update(dto);
    }

    // Streams a pg_dump snapshot of the whole database back as a downloadable
    // .sql file. Requires pg_dump on the server's PATH -- see BackupService.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/backup")
    public ResponseEntity<Resource> downloadBackup() throws IOException {
        Path backupFile = backupService.createBackup();
        String filename = "pms-backup-" +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")) + ".sql";

        Resource resource = new FileSystemResource(backupFile) {
            // Best-effort cleanup: delete the temp file once Spring is done
            // streaming it. If the JVM restarts mid-stream, the OS temp
            // directory will still get cleaned up eventually.
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                java.io.InputStream in = super.getInputStream();
                return new java.io.FilterInputStream(in) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        Files.deleteIfExists(backupFile);
                    }
                };
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }

    // Restores the database from a previously downloaded backup file.
    // Destructive -- only ADMIN, and the frontend makes the user confirm.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreBackup(@RequestParam("file") MultipartFile file) throws IOException {
        backupService.restoreBackup(file);
    }
}
