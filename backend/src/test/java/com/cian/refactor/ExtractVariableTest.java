package com.cian.refactor;

public class ExtractVariableTest {
    public static void main(String[] args) {
        RefactoringEngine engine = new RefactoringEngine();
        Request request = new Request();
        
        // Test case: extract a complex expression to a variable
        String source = """
            public class Test {
                public void process() {
                    String name = "admin";
                    if (name.toLowerCase().equals("admin")) {
                        System.out.println("Welcome");
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
    }
}
