package com.example.filewatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AbstractFileListFilter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FileIntegrationConfig {

    final FileProcessingParametersConfig config;

    public FileIntegrationConfig(FileProcessingParametersConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        for (String name: List.of(config.getUploadDir(), config.getDestinationBase(), config.getRejectedDir())) {
            File folder = new File(name);
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }
    /**
     * File Reading Message Source for polling files from the watch folder.
     */
    public FileReadingMessageSource fileReadingMessageSource() {
        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setDirectory(new File(config.getUploadDir()));
        source.setFilter(new ZipFileFilter());
        return source;
    }

    /**
     * Spring Integration Flow for file processing.
     */
    @Bean
    public IntegrationFlow fileProcessingFlow() {
        return IntegrationFlow.from(fileReadingMessageSource(), spec -> spec.poller(p -> p.fixedDelay(1000)))
                // Handle logic in ServiceActivator
                .handle(message -> handleZipFile((File) message.getPayload()))
                .get();
    }


    private Optional<FileProcessingParameters> findIncomingData(File file) {
        for (FileProcessingParameters fileProcessingParameters : config.getParameters()) {
            if (fileProcessingParameters.getPattern().matcher(file.getName()).matches()) {
                return Optional.of(fileProcessingParameters);
            }
        }
        return Optional.empty();
    }
    /**
     * ServiceActivator to process the detected file pair (filenameA.complete and filenameA.zip).
     */
    private void handleZipFile(File zipFile) {
        Optional<FileProcessingParameters> incomingData = findIncomingData(zipFile);
        log.info("Detected complete candidate : {}", zipFile.getName());
        incomingData.ifPresentOrElse(data -> {
            if (isValidZip(zipFile, data)) {
                processValidZip(zipFile, data);
            } else {
                processInvalidZip(zipFile);
            }
        }, () -> {
            log.warn("No incoming data found for ZIP file '{}'", zipFile.getName());
            processInvalidZip(zipFile);
        }
        );
    }

    private void processValidZip(File zipFile, FileProcessingParameters data) {
        log.info("ZIP file '{}' is valid. Extracting contents...", zipFile.getName());
        try {
            String destinationPath = config.getDestinationBase() + "/" + data.getDestination();
            extractZip(zipFile, destinationPath);
            log.info("Extraction completed for '{}'. Cleaning up...", zipFile.getName());
            cleanupFiles(zipFile);
        } catch (IOException e) {
            log.error("Error extracting ZIP file '{}'", zipFile.getName(), e);
        }
    }

    private void processInvalidZip(File zipFile) {
        log.error("ZIP file '{}' is invalid. Moving to bad files...", zipFile.getName());
        try {
            moveToBadFiles(zipFile);
        } catch (IOException e) {
            log.error("Error moving bad ZIP file '{}'", zipFile.getName(), e);
        }
    }

    /**
     * Validates the integrity of a ZIP file using java.util.zip.ZipFile.
     */
    private boolean isValidZip(File file, FileProcessingParameters fileProcessingParameters) {
        try (ZipFile zipFile = new ZipFile(file)) {
            int fileCount = Collections.list(zipFile.entries()).size();
            log.info("Validating file {}, expected zip entries {}, actual zip entries {}", file.getName(), fileProcessingParameters.getFileCount(), fileCount);
            return fileProcessingParameters.getFileCount() == fileCount; // If valid, it will open successfully
        } catch (IOException e) {
            log.warn("ZIP file validation failed: {}", file.getName());
            return false;
        }
    }

    /**
     * Extracts ZIP file contents to the specified directory.
     */
    private void extractZip(File zipFile, String outputDir) throws IOException {
        Map<String, String> DEFAULT_ENV = Collections.emptyMap();
        try (FileSystem zipFs = FileSystems.newFileSystem(zipFile.toPath(), DEFAULT_ENV)) {
            Path root = zipFs.getPath("/");
            Files.walk(root).forEach(source -> {
                try {
                    Path destination = Paths.get(outputDir, root.relativize(source).toString());
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.error("Error extracting file '{}' to '{}'", source, outputDir, e);
                }
            });
        }
    }

    /**
     * Cleans up the processed files.
     */
    private void cleanupFiles(File... files) {
        for (File file: files) {
            log.info("Cleanup: file deleted={}", file);
            file.delete();
        }
    }

    /**
     * Moves invalid ZIP files to the "bad files" directory.
     */
    private void moveToBadFiles(File zipFile) throws IOException {
        Files.move(zipFile.toPath(), Paths.get(config.getRejectedDir(), zipFile.getName()),
                StandardCopyOption.REPLACE_EXISTING);
        zipFile.delete(); // Always delete the .complete file
    }

    /**
     * Custom File Filter to detect .complete files with their paired .zip in the same folder.
     */
    private class ZipFileFilter extends AbstractFileListFilter<File> {
        @Override
        public boolean accept(File file) {
            if( System.currentTimeMillis() - file.lastModified() > config.getStaleFileWaitMs()) {
                log.warn("Stale file detected: {}", file.getName());
                return true;
            }
            return findIncomingData(file).map(fileProcessingParameters -> {
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(file);
                } catch (IOException e) {
                    // the file is not valid ZIP
                }
                return zipFile != null && zipFile.size() > 0 ;                
            }).orElse(false);
        }
    }
}
