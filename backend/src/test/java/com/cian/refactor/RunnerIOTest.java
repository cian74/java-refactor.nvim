package com.cian.refactor;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RunnerIOTest {

    @Test
    void testRunnerIO() throws Exception {

        // Start Runner as a subprocess
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                "com.cian.refactor.Runner"
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Send JSON to Runner (simulating frontend)
        String sampleRequest = "{\"command\":\"generate_getters_setters\",\"source\":\"public class A { private int x; }\"}\n";

        OutputStream stdin = process.getOutputStream();
        stdin.write(sampleRequest.getBytes(StandardCharsets.UTF_8));
        stdin.flush();

        // Close input so Runner exits
        stdin.close();

        // Read output from Runner
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        process.waitFor();

        String result = output.toString();
        assertFalse(result.isEmpty(), "Runner returned empty output");

        // The escaped output should contain a getter/setter
        assertTrue(result.contains("getX") || result.contains("getX ()"),
                "Output should contain getter for x");

        assertTrue(result.contains("\\n"), "Newlines should be escaped by Runner");
    }
}

