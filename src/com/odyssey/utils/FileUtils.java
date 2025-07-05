package com.odyssey.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FileUtils {
    public static String readFromFile(String path) {
        try (var stream = FileUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new RuntimeException("Can't find file at " + path);
            }
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }
} 