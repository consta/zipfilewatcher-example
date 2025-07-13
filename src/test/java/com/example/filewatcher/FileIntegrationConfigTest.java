package com.example.filewatcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class FileIntegrationConfigTest {

    @Autowired
    FileIntegrationConfig integrationConfig;

    List<TestFile> files = List.of(
            new TestFile("StaleFile.txt", false),
            new TestFile("FileA-2025-07-13.zip", true),
            new TestFile("FileB-20250713.zip", true),
            new TestFile("FileC-20250713.zip", false)
    );

    @PostConstruct
    public void setupTest() throws IOException {
        cleanFolder(integrationConfig.config.getUploadDir());
        cleanFolder(integrationConfig.config.getDestinationBase());
        cleanFolder(integrationConfig.config.getRejectedDir());
    }

    @Test
    public void testFileProcessingFlow() throws Exception {
        // given - copy all files in parallel for they appear in random order
        CountDownLatch latch = new CountDownLatch(files.size());
        for (String filename : files.stream().map(TestFile::getName).collect(Collectors.toSet())) {
            Thread thread = new Thread(slowCopy("build/resources/test/" + filename,
                    integrationConfig.config.getUploadDir() + "/" + filename,
                    latch,
                    100));
            thread.start();
        }
        latch.await();

        //when - let the service to do the job
        Thread.sleep(5000);

        // then - check the files are processed as expected
        assertTrue(new File("build/files_out/FileA").exists());
        assertTrue(new File("build/files_out/FileB").exists());
        assertTrue(new File("build/files_bad/StaleFile.txt").exists());
        assertTrue(new File("build/files_bad/FileC-20250713.zip").exists());
        assertFalse(new File("build/files_in/FileA-2025-07-13.zip").exists());
        assertFalse(new File("build/files_in/ileB-20250713.zip").exists());
        assertTrue(new File("build/files_in").listFiles().length == 0);
    }

    private void cleanFolder(String folder) throws IOException {
        for (File file: Paths.get(folder).toFile().listFiles()) {
            if (file.isDirectory()) {
                cleanFolder(folder + "/" + file.getName());
                file.delete();
            }
            file.delete();
        }
    }

    private Runnable slowCopy(String source, String destination, CountDownLatch latch, long delayMs) {
        return () -> {
            try (var inputStream = Files.newInputStream(Paths.get(source));
                 var outputStream = Files.newOutputStream(Paths.get(destination))) {
                byte[] buffer = new byte[512];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    Thread.sleep(delayMs);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            latch.countDown();
            log.info("finished copying {} -> {}", source, destination);
        };
    }
}

@Data
@AllArgsConstructor
class TestFile {
    private String name;
    private boolean isValid;
}

