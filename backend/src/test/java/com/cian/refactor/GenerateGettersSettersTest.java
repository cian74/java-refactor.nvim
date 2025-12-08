package com.cian.refactor;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateGettersSettersTest {

    @Test
    void testGenerateGettersSetters() {
        String input =
                "public class Person {\n" +
                "    private String name;\n" +
                "    private int age;\n" +
                "}";

        Request req = new Request();
        req.command = "generate_getters_setters";
        req.source = input;
        req.start_line = 0;
        req.end_line = 0;

        RefactoringEngine engine = new RefactoringEngine();
        String json = engine.applyRefactor("generate_getters_setters", req);

        assertNotNull(json, "applyRefactor returned null JSON");

        Gson gson = new Gson();
        Refactored result = gson.fromJson(json, Refactored.class);

        assertNotNull(result, "Parsed Refactored is null");
        assertNotNull(result.new_source, "new_source is null");

        String output = result.new_source;

        // Both getters and setters should exist (quick substring checks)
        assertTrue(output.contains("public String getName()") || output.contains("public String getName ()"),
                "getter for name not found");
        assertTrue(output.contains("public void setName(String name)") || output.contains("public void setName (String name)"),
                "setter for name not found");

        assertTrue(output.contains("public int getAge()") || output.contains("public int getAge ()"),
                "getter for age not found");
        assertTrue(output.contains("public void setAge(int age)") || output.contains("public void setAge (int age)"),
                "setter for age not found");
    }
}

