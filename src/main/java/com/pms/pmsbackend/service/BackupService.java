package com.pms.pmsbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/**
 * Backup & restore via the Postgres command-line tools (pg_dump / psql).
 * These ship with any Postgres server install and are the standard, reliable
 * way to snapshot/restore a whole database -- reimplementing that over JPA
 * would only cover the entities we've mapped and risks missing something.
 *
 * Requires pg_dump and psql to be on the server's PATH. On Windows they're
 * usually under "...\PostgreSQL\<version>\bin" -- add that folder to PATH if
 * the backup/restore calls fail with "command not found".
 */
@Service
public class BackupService {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    public BackupService(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not parse spring.datasource.url for backup/restore: " + jdbcUrl);
        }
        this.host = matcher.group(1);
        this.port = Integer.parseInt(matcher.group(2));
        this.database = matcher.group(3);
        this.username = username;
        this.password = password;
    }

    /**
     * Runs pg_dump and returns the path to a plain-SQL backup file. Caller is
     * responsible for deleting the temp file once it's been streamed back.
     */
    public Path createBackup() throws IOException {
        Path outputFile = Files.createTempFile("pms-backup-", ".sql");
        List<String> command = List.of(
                "pg_dump",
                "-h", host,
                "-p", String.valueOf(port),
                "-U", username,
                "-d", database,
                "--no-password",
                "-f", outputFile.toString()
        );
        runProcess(command, "pg_dump");
        return outputFile;
    }

    /**
     * Restores a previously-created backup file by piping it into psql.
     * This does NOT drop existing data first -- it replays the dump's own
     * statements (which include DROP/CREATE for each object from a default
     * pg_dump), so restoring into a database with unrelated extra data can
     * leave stray rows behind. For a clean restore, restore into a fresh
     * database.
     */
    public void restoreBackup(MultipartFile file) throws IOException {
        Path uploaded = Files.createTempFile("pms-restore-", ".sql");
        try {
            file.transferTo(uploaded);
            List<String> command = List.of(
                    "psql",
                    "-h", host,
                    "-p", String.valueOf(port),
                    "-U", username,
                    "-d", database,
                    "--no-password",
                    "-f", uploaded.toString()
            );
            runProcess(command, "psql");
        } finally {
            Files.deleteIfExists(uploaded);
        }
    }

    private void runProcess(List<String> command, String toolName) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("PGPASSWORD", password);
        builder.redirectErrorStream(true);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException(
                    toolName + " could not be started. Make sure PostgreSQL's command-line tools " +
                    "(pg_dump/psql) are installed and on the system PATH.", e);
        }

        String output = new String(process.getInputStream().readAllBytes());
        boolean finished;
        try {
            finished = process.waitFor(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException(toolName + " was interrupted.", e);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(toolName + " timed out after 120 seconds.");
        }
        if (process.exitValue() != 0) {
            throw new IOException(toolName + " failed: " + output);
        }
    }
}
