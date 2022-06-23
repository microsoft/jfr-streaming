package com.microsoft.jfr.generation;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.*;

public class JfcToJavaTest {

    @Test
    void jfcToJavaMap() throws IOException {

        String jfcPath = findResourcesPath() + File.separator + "sampleJfcFile.jfc";
        String targetPath = findTargetPath();
        String className = "ThreadAllocationConfig";
        JfcToJava.generateJavaMap(jfcPath, targetPath, className);

        String expectedGeneratedFilePath = targetPath + File.separator + "ThreadAllocationConfig" + ".java";
        assertTrue(new File(expectedGeneratedFilePath).exists());

        String generatedFileContent = findFileContent(expectedGeneratedFilePath);
        assertTrue(generatedFileContent.contains("put(\"jdk.ThreadAllocationStatistics#enabled\",\"true\")"));
        assertTrue(generatedFileContent.contains("\"jdk.ThreadAllocationStatistics#period\",\"everyChunk\""));
   }

    private static String findResourcesPath() {
        Path targetDirectory = Paths.get("src" + File.separator + "test" + File.separator + "resources");
        return targetDirectory.toFile().getAbsolutePath();
    }

    private static String findTargetPath() {
        Path targetDirectory = Paths.get("target");
        return targetDirectory.toFile().getAbsolutePath();
    }

    private static String findFileContent(String filePath) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            return lines.collect(Collectors.joining(System.lineSeparator()));
        }
    }

}
