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
    void testRenameVariable() throws Exception {
        String source = "public class Example {\n" +
                "    public void test() {\n" +
                "        int oldName = 10;\n" +
                "        System.out.println(oldName);\n" +
                "    }\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"rename\",\"source\":\"%s\",\"old_name\":\"oldName\",\"new_name\":\"newName\",\"start_line\":3,\"scope\":\"variable\"}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("newName"), 
                "Should contain new variable name");
    }

    @Test
    void testRenameMethod() throws Exception {
        String source = "public class Example {\n" +
                "    public int oldMethod(int x) {\n" +
                "        return x;\n" +
                "    }\n" +
                "    \n" +
                "    public void test() {\n" +
                "        int r = oldMethod(5);\n" +
                "    }\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"rename\",\"source\":\"%s\",\"old_name\":\"oldMethod\",\"new_name\":\"newMethod\",\"start_line\":1,\"scope\":\"method\"}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("newMethod"), 
                "Should contain new method name");
    }

    @Test
    void testRenameClass() throws Exception {
        String source = "public class OldClass {\n" +
                "    public int x;\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"rename\",\"source\":\"%s\",\"old_name\":\"OldClass\",\"new_name\":\"NewClass\",\"start_line\":1,\"scope\":\"class\"}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("NewClass"), 
                "Should contain new class name");
    }

    @Test
    void testRenameAll() throws Exception {
        String source = "public class Test {\n" +
                "    public int data = 5;\n" +
                "    \n" +
                "    public void process() {\n" +
                "        int temp = data;\n" +
                "    }\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"rename\",\"source\":\"%s\",\"old_name\":\"data\",\"new_name\":\"value\",\"start_line\":3,\"scope\":\"all\"}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("value"), 
                "Should contain new name");
    }

    @Test
    void testEncapsulateFieldPublic() throws Exception {
        String source = "public class Example {\n" +
                "    String myField;\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"encapsulate_field\",\"source\":\"%s\",\"field_name\":\"myField\"}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("private"), 
                "Should make field private");
        assertTrue(unescaped.contains("getMyField"), 
                "Should contain getter");
        assertTrue(unescaped.contains("setMyField"), 
                "Should contain setter");
    }

    @Test
    void testEncapsulateFieldPrivate() throws Exception {
        String source = "public class Example {\n" +
                "    private String name;\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"encapsulate_field\",\"source\":\"%s\",\"field_name\":\"name\"}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("getName"), 
                "Should contain getter for already private field");
        assertTrue(unescaped.contains("setName"), 
                "Should contain setter for already private field");
    }

    @Test
    void testEncapsulateFieldNotFound() throws Exception {
        String source = "public class Example {\n" +
                "    String name;\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"encapsulate_field\",\"source\":\"%s\",\"field_name\":\"nonexistent\"}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        assertTrue(unescaped.contains("error") && unescaped.contains("not found"), 
                "Should return error for non-existent field");
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

    @Test
    void testPullMethod() throws Exception {
        String source = "public class Parent {}\n" +
                "\n" +
                "public class Child extends Parent {\n" +
                "    public int calculate() {\n" +
                "        return 42;\n" +
                "    }\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"pull_push\",\"source\":\"%s\",\"direction\":\"pull\",\"member_name\":\"calculate\",\"start_line\":4}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("new_source"), 
                "Should have new_source in result");
    }

    @Test
    void testPushMethod() throws Exception {
        String source = "public class Parent {\n" +
                "    public int calculate() {\n" +
                "        return 42;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "public class Child extends Parent {}";
        
        String request = String.format(
                "{\"command\":\"pull_push\",\"source\":\"%s\",\"direction\":\"push\",\"member_name\":\"calculate\",\"start_line\":2}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        
        assertTrue(unescaped.contains("new_source"), 
                "Should have new_source in result");
    }

    @Test
    void testPullPushSingleClass() throws Exception {
        String source = "public class Single {\n" +
                "    public int value;\n" +
                "}";
        
        String request = String.format(
                "{\"command\":\"pull_push\",\"source\":\"%s\",\"direction\":\"pull\",\"member_name\":\"value\",\"start_line\":2}\n",
                source.replace("\n", "\\n")
        );
        
        String result = sendRequest(request);
        
        assertFalse(result.isEmpty(), "Runner returned empty output");
        
        String unescaped = result.replace("\\n", "\n").replace("\\r", "");
        assertTrue(unescaped.contains("error") && unescaped.contains("2 classes"),
                "Should return error for single class");
    }
}
