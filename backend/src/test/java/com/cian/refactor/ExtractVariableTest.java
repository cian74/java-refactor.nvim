package com.cian.refactor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExtractVariableTest {
    @Test
    void testExtractVariable() {
        RefactoringEngine engine = new RefactoringEngine();
        Request request = new Request();
        
        String source = """
            public class Test {
                public void stringOp() {
                    if (name.toLowerCase().equals("Bob")) {
                        System.out.println("Name is bob");
                    }
                }
            }
            """;
        
        request.source = source;
        request.highlighted = "name.toLowerCase()";
        request.var_name = "lowerName";
        request.start_line = 3;
        
        String result = engine.applyRefactor("extract_variable", request);
        System.out.println("Result: " + result);
        
        // Should contain the variable declaration
        assertTrue(result.contains("lowerName"));
        // Should NOT contain the original expression in the if statement
        assertFalse(result.contains("name.toLowerCase().equals"));
    }
}
