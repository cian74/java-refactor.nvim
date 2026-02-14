package com.cian.refactor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import java.util.Arrays;
import java.util.List;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class RefactoringEnginePerformanceTest {
    
    private RefactoringEngine engine;
    private Request request;
    private DecimalFormat df = new DecimalFormat("#.##");
    private List<String> performanceMetrics = new ArrayList<>();
    
    @BeforeEach
    void setUp() {
        engine = new RefactoringEngine();
        request = new Request();
        performanceMetrics.clear();
    }
    
    @AfterEach
    void tearDown() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DETAILED PERFORMANCE REPORT");
        System.out.println("=".repeat(80));
        for (String metric : performanceMetrics) {
            System.out.println(metric);
        }
        System.out.println("=".repeat(80) + "\n");
    }
    
    private void recordMetric(String testName, long durationMs, String details) {
        String metric = String.format("%-40s | %8s ms | %s", 
            testName, 
            df.format(durationMs), 
            details);
        performanceMetrics.add(metric);
    }
    
    @Test
    @DisplayName("Performance test for generate_getters_setters with small class")
    void testGenerateGettersSettersPerformanceSmall() {
        String smallClass = """
            public class SmallClass {
                private String name;
                private int age;
                private boolean active;
            }
            """;
        
        request.source = smallClass;
        int fieldCount = 3;
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("generate_getters_setters", request);
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        double operationsPerSecond = fieldCount * 2.0 / (durationMs / 1000.0); // 2 methods per field
        
        recordMetric("Generate Getters/Setters (Small)", durationMs, 
            String.format("%d fields → %d methods, %.1f ops/sec", 
                fieldCount, fieldCount * 6, operationsPerSecond));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Operation should complete in less than 1 second");
    }
    
    @Test
    @DisplayName("Performance test for generate_getters_setters with large class")
    void testGenerateGettersSettersPerformanceLarge() {
        StringBuilder largeClass = new StringBuilder("public class LargeClass {\n");
        
        int fieldCount = 100;
        for (int i = 0; i < fieldCount; i++) {
            largeClass.append("    private String field").append(i).append(";\n");
        }
        
        largeClass.append("}");
        
        request.source = largeClass.toString();
        int sourceSize = largeClass.toString().length();
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("generate_getters_setters", request);
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        double operationsPerSecond = fieldCount * 2.0 / (durationMs / 1000.0);
        int resultSize = result != null ? result.length() : 0;
        
        recordMetric("Generate Getters/Setters (Large)", durationMs, 
            String.format("%d fields (%d→%d chars), %.1f ops/sec", 
                fieldCount, sourceSize, resultSize, operationsPerSecond));
        
        assertNotNull(result);
        assertTrue(durationMs < 5000, "Operation should complete in less than 5 seconds");
    }
    
    @Test
    @DisplayName("Performance test for extract_method operation")
    void testExtractMethodPerformance() {
        String classWithMethod = """
            public class TestClass {
                public void calculateSomething() {
                    int a = 5;
                    int b = 10;
                    int result = a + b;
                    System.out.println(result);
                }
            }
            """;
        
        request.source = classWithMethod;
        request.highlighted = "a + b";  // Expression without semicolon
        request.method_name = "calculateResult";
        request.start_line = 4;
        request.end_line = 4;
        
        int highlightedLength = request.highlighted.length();
        int sourceSize = classWithMethod.length();
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("extract_method", request);
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        int resultSize = result != null ? result.length() : 0;
        
        recordMetric("Extract Method (Expression)", durationMs, 
            String.format("Extracted %d chars (%d→%d chars total)", 
                highlightedLength, sourceSize, resultSize));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Operation should complete in less than 1 second");
        
        // Also test statement extraction
        String classWithStatement = """
            public class TestClass2 {
                public void process() {
                    int a = 5;
                    int b = 10;
                    System.out.println(a + b);
                    int result = a * 2;
                }
            }
            """;
        
        request.source = classWithStatement;
        request.highlighted = "System.out.println(a + b)";  // Statement (no semicolon)
        request.method_name = "printSum";
        request.start_line = 4;
        request.end_line = 4;
        
        startTime = System.nanoTime();
        result = engine.applyRefactor("extract_method", request);
        endTime = System.nanoTime();
        
        durationMs = (endTime - startTime) / 1_000_000;
        
        recordMetric("Extract Method (Statement)", durationMs, 
            String.format("Extracted %d chars (%d→%d chars total)", 
                request.highlighted.length(), classWithStatement.length(), 
                result != null ? result.length() : 0));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Operation should complete in less than 1 second");
    }
    
    @Test
    @DisplayName("Performance test for list_fields operation")
    void testListFieldsPerformance() {
        StringBuilder classWithManyFields = new StringBuilder("public class TestClass {\n");
        
        int fieldCount = 50;
        for (int i = 0; i < fieldCount; i++) {
            classWithManyFields.append("    private String field").append(i).append(";\n");
        }
        
        classWithManyFields.append("}");
        
        request.source = classWithManyFields.toString();
        int sourceSize = classWithManyFields.toString().length();
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("list_fields", request);
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        double fieldsPerSecond = fieldCount / (durationMs / 1000.0);
        int resultSize = result != null ? result.length() : 0;
        
        recordMetric("List Fields", durationMs, 
            String.format("Parsed %d fields (%d→%d chars), %.1f fields/sec", 
                fieldCount, sourceSize, resultSize, fieldsPerSecond));
        
        assertNotNull(result);
        assertTrue(durationMs < 500, "Operation should complete in less than 500ms");
    }
    
    @Test
    @DisplayName("Performance test for generate_field_getters_setters operation")
    void testGenerateFieldGettersSettersPerformance() {
        String classWithFields = """
            public class TestClass {
                private String name;
                private int age;
                private boolean active;
                private double salary;
            }
            """;
        
        request.source = classWithFields;
        List<String> selectedFields = Arrays.asList("name", "age", "active", "salary");
        request.selected_fields = selectedFields;
        
        int selectedCount = selectedFields.size();
        int sourceSize = classWithFields.length();
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("generate_field_getters_setters", request);
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        double methodsGenerated = selectedCount * 2.0;
        int resultSize = result != null ? result.length() : 0;
        
        recordMetric("Generate Selected Getters/Setters", durationMs, 
            String.format("%d selected fields → %.0f methods (%d→%d chars)", 
                selectedCount, methodsGenerated, sourceSize, resultSize));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Operation should complete in less than 1 second");
        
        // Test with partial field selection (simulating UI interaction)
        request.selected_fields = Arrays.asList("name", "salary");
        selectedCount = 2;
        
        startTime = System.nanoTime();
        result = engine.applyRefactor("generate_field_getters_setters", request);
        endTime = System.nanoTime();
        
        durationMs = (endTime - startTime) / 1_000_000;
        resultSize = result != null ? result.length() : 0;
        
        recordMetric("Generate Partial Getters/Setters", durationMs, 
            String.format("%d selected fields → %.0f methods (%d→%d chars)", 
                selectedCount, selectedCount * 2.0, sourceSize, resultSize));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Partial selection should complete in less than 1 second");
    }
    
    @Test
    @DisplayName("Performance benchmark for multiple operations")
    void testMultipleOperationsBenchmark() {
        String testClass = """
            public class BenchmarkClass {
                private String name;
                private int count;
                private boolean status;
                
                public void process() {
                    String processed = name.toUpperCase();
                    int total = count + 1;
                    System.out.println(processed + total);
                }
            }
            """;
        
        request.source = testClass;
        
        int iterations = 100;
        long[] times = new long[iterations];
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            engine.applyRefactor("generate_getters_setters", request);
            long end = System.nanoTime();
            times[i] = end - start;
            totalTime += times[i];
        }
        
        long avgTimeNs = totalTime / iterations;
        long avgTimeMs = avgTimeNs / 1_000_000;
        
        long minTime = Long.MAX_VALUE, maxTime = 0;
        for (long time : times) {
            long timeMs = time / 1_000_000;
            if (timeMs < minTime) minTime = timeMs;
            if (timeMs > maxTime) maxTime = timeMs;
        }
        
        recordMetric("Benchmark (100 iterations)", avgTimeMs, 
            String.format("avg: %sms, min: %sms, max: %sms", 
                df.format(avgTimeMs), minTime, maxTime));
        
        assertTrue(avgTimeMs < 100, "Average operation time should be less than 100ms");
    }
    
    @Test
    @DisplayName("Memory usage test for large refactoring operations")
    void testMemoryUsageForLargeOperations() {
        Runtime runtime = Runtime.getRuntime();
        
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        StringBuilder hugeClass = new StringBuilder("public class HugeClass {\n");
        int fieldCount = 500;
        for (int i = 0; i < fieldCount; i++) {
            hugeClass.append("    private String field").append(i).append(";\n");
        }
        hugeClass.append("}");
        
        request.source = hugeClass.toString();
        int sourceSize = hugeClass.toString().length();
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("generate_getters_setters", request);
        long endTime = System.nanoTime();
        
        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        long memoryUsedMb = (memoryAfter - memoryBefore) / (1024 * 1024);
        int resultSize = result != null ? result.length() : 0;
        
        recordMetric("Memory Usage (Large Operation)", durationMs, 
            String.format("%d fields, %d→%d chars, %.1f MB memory", 
                fieldCount, sourceSize, resultSize, memoryUsedMb / 1024.0));
        
        assertTrue(memoryUsedMb < 50, "Memory usage should be reasonable (< 50MB)");
    }
    
    @Test
    @DisplayName("Performance test for inline_method operation")
    void testInlineMethodPerformance() {
        String classWithMethod = """
            public class TestClass {
                public int calculateSum(int a, int b) {
                    return a + b;
                }
                
                public void process() {
                    int result = calculateSum(5, 10);
                    System.out.println(result);
                }
            }
            """;
        
        request.source = classWithMethod;
        request.start_line = 2;  // Position of calculateSum method
        
        int sourceSize = classWithMethod.length();
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("inline_method", request);
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        int resultSize = result != null ? result.length() : 0;
        
        recordMetric("Inline Method", durationMs, 
            String.format("Inlined simple method (%d→%d chars)", 
                sourceSize, resultSize));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Inline method should complete in less than 1 second");
        
        // Test with multiple method calls
        String classWithMultipleCalls = """
            public class Calculator {
                public int multiply(int x, int y) {
                    return x * y;
                }
                
                public void calculate() {
                    int a = multiply(2, 3);
                    int b = multiply(4, 5);
                    int c = multiply(a, b);
                    System.out.println(c);
                }
            }
            """;
        
        request.source = classWithMultipleCalls;
        request.start_line = 2;
        sourceSize = classWithMultipleCalls.length();
        
        startTime = System.nanoTime();
        result = engine.applyRefactor("inline_method", request);
        endTime = System.nanoTime();
        
        durationMs = (endTime - startTime) / 1_000_000;
        resultSize = result != null ? result.length() : 0;
        
        recordMetric("Inline Method (Multiple Calls)", durationMs, 
            String.format("Inlined method with 3 calls (%d→%d chars)", 
                sourceSize, resultSize));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Multiple calls inline should complete in less than 1 second");
    }
    
    @Test
    @DisplayName("Performance test for generate_toString operation")
    void testGenerateToStringPerformance() {
        String smallClass = """
            public class Person {
                private String name;
                private int age;
                private boolean active;
            }
            """;
        
        request.source = smallClass;
        
        int fieldCount = 3;
        int sourceSize = smallClass.length();
        
        long startTime = System.nanoTime();
        String result = engine.applyRefactor("generate_toString", request);
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        int resultSize = result != null ? result.length() : 0;
        
        recordMetric("Generate toString (Small)", durationMs, 
            String.format("%d fields (%d→%d chars)", 
                fieldCount, sourceSize, resultSize));
        
        assertNotNull(result);
        assertTrue(durationMs < 1000, "Generate toString should complete in less than 1 second");
        
        // Test with large class
        StringBuilder largeClass = new StringBuilder("public class LargeClass {\n");
        int largeFieldCount = 50;
        for (int i = 0; i < largeFieldCount; i++) {
            largeClass.append("    private String field").append(i).append(";\n");
        }
        largeClass.append("}");
        
        request.source = largeClass.toString();
        sourceSize = largeClass.toString().length();
        
        startTime = System.nanoTime();
        result = engine.applyRefactor("generate_toString", request);
        endTime = System.nanoTime();
        
        durationMs = (endTime - startTime) / 1_000_000;
        resultSize = result != null ? result.length() : 0;
        
        recordMetric("Generate toString (Large)", durationMs, 
            String.format("%d fields (%d→%d chars)", 
                largeFieldCount, sourceSize, resultSize));
        
        assertNotNull(result);
        assertTrue(durationMs < 2000, "Generate toString for large class should complete in less than 2 seconds");
        
        // Test duplicate toString handling
        String classWithExistingToString = """
            public class Person {
                private String name;
                
                @Override
                public String toString() {
                    return "Person{}";
                }
            }
            """;
        
        request.source = classWithExistingToString;
        
        startTime = System.nanoTime();
        result = engine.applyRefactor("generate_toString", request);
        endTime = System.nanoTime();
        
        durationMs = (endTime - startTime) / 1_000_000;
        
        recordMetric("Generate toString (Duplicate)", durationMs, 
            String.format("Detected existing toString"));
        
        assertNotNull(result);
        assertTrue(result.contains("error"), "Should return error when toString already exists");
    }
}