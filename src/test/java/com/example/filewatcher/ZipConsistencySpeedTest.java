package com.example.filewatcher;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;



public class ZipConsistencySpeedTest {

    @Test
    @Disabled
    public void testZipConsistencySpeed() {
        File zipFile = new File("/tmp/Archive3.zip");
        boolean isValid = false;
        Long startTime = System.currentTimeMillis();
        try {
            ZipFile archive = new ZipFile(zipFile);
            isValid = true;
        } catch (IOException e1) {
            System.out.println("File seems to be corrupt: " + e1.getMessage());
        }
        System.out.println("Time spent: " + (System.currentTimeMillis() - startTime) + " ms");
        assertTrue(isValid);
    }
}
