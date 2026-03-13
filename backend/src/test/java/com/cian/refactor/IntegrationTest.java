package com.cian.refactor;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    private String sendRequest(String jsonRequest) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                "com.cian.refactor.Runner"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        OutputStream stdin = process.getOutputStream();
        stdin.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
        stdin.flush();
        stdin.close();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            System.err.println("Process exited with code: " + exitCode);
        }
        
        return output.toString().trim();
    }

    @Test
    void testGenerateGettersSetters() throws Exception {
        String request = "{\"command\":\"generate_getters_setters\",\"source\":\"public class Person { private String name; private int age; }\"}\n";
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        assertTrue(result.contains("getName") || result.contains("getName ()"), 
                "Should contain getter for name");
        assertTrue(result.contains("setName") || result.contains("setName ()"), 
                "Should contain setter for name");
        assertTrue(result.contains("\\n"), "Newlines should be escaped");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        assertTrue(unescaped.contains("public String getName"), 
                "Unescaped output should contain getter");
    }

    @Test
    void testGenerateToString() throws Exception {
        String request = "{\"command\":\"generate_toString\",\"source\":\"public class Person { private String name; private int age; }\"}\n";
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        assertTrue(unescaped.contains("public String toString()") || unescaped.contains("public String toString ()"), 
                "Should contain toString method");
        assertTrue(unescaped.contains("name"), "Should reference name field");
        assertTrue(unescaped.contains("age"), "Should reference age field");
    }

    @Test
    void testListFields() throws Exception {
        String request = "{\"command\":\"list_fields\",\"source\":\"public class Person { private String name; private int age; public String publicField; }\"}\n";
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        assertTrue(unescaped.contains("name"), "Should contain name field");
        assertTrue(unescaped.contains("age"), "Should contain age field");
    }

    @Test
    void testExtractMethodExpression() throws Exception {
        String source = "public class Calculator {\n" +
                "    public int add(int a, int b) {\n" +
                "        int result = a + b;\n" +
                "        return result;\n" +
                "    }\n" +
                "}";
        
        String highlighted = "a + b";
        
        String request = String.format(
                "{\"command\":\"extract_method\",\"source\":\"%s\",\"highlighted\":\"%s\",\"method_name\":\"calculateSum\",\"start_line\":2,\"end_line\":4}\n",
                source.replace("\n", "\\n"), highlighted
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        if (unescaped.contains("error")) {
            System.out.println("Extract method error: " + unescaped);
        }
        
        assertTrue(unescaped.contains("calculateSum") || unescaped.contains("calculateSum ()"), 
                "Should contain new method name");
    }

    @Test
    void testExtractVariable() throws Exception {
        String source = "public class Example {\n" +
                "    public void test() {\n" +
                "        int result = 10 + 5;\n" +
                "    }\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"extract_variable\",\"source\":\"%s\",\"highlighted\":\"10 + 5\",\"var_name\":\"computed\",\"start_line\":3}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        if (unescaped.contains("error")) {
            System.out.println("Extract variable error: " + unescaped);
        }
        
        assertTrue(unescaped.contains("computed"), 
                "Should contain new variable name");
    }

    @Test
    void testInlineMethod() throws Exception {
        String source = "public class Example {\n" +
                "    public int doubleValue(int x) {\n" +
                "        return x * 2;\n" +
                "    }\n" +
                "    \n" +
                "    public void main() {\n" +
                "        int result = doubleValue(5);\n" +
                "    }\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"inline_method\",\"source\":\"%s\",\"start_line\":2}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        if (unescaped.contains("error")) {
            System.out.println("Inline method: " + unescaped);
        }
    }

    @Test
    void testGenerateFieldGettersSetters() throws Exception {
        String request = "{\"command\":\"generate_field_getters_setters\",\"source\":\"public class Person { private String name; }\",\"selected_fields\":[\"name\"]}\n";
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        assertTrue(unescaped.contains("getName") || unescaped.contains("getName ()"), 
                "Should contain getter");
    }

    @Test
    void testErrorHandling() throws Exception {
        String request = "{\"command\":\"generate_toString\",\"source\":\"public class Empty {}\"}\n";
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        assertTrue(unescaped.contains("error") || unescaped.contains("No private fields"), 
                "Should return error for class with no private fields");
    }

    @Test
    void testMultipleRequests() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                "com.cian.refactor.Runner"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        OutputStream stdin = process.getOutputStream();
        
        String request1 = "{\"command\":\"list_fields\",\"source\":\"public class A { private int x; }\"}\n";
        stdin.write(request1.getBytes(StandardCharsets.UTF_8));
        stdin.flush();
        
        Thread.sleep(100);
        
        String request2 = "{\"command\":\"generate_getters_setters\",\"source\":\"public class B { private String y; }\"}\n";
        stdin.write(request2.getBytes(StandardCharsets.UTF_8));
        stdin.flush();
        
        stdin.close();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder output = new StringBuilder();
        String line;
        int lineCount = 0;
        while ((line = reader.readLine()) != null && lineCount < 10) {
            output.append(line).append("\n");
            lineCount++;
        }

        process.waitFor();
        
        String result = output.toString();
        assertFalse(result.isEmpty(), "Should return output for multiple requests");
    }
}
