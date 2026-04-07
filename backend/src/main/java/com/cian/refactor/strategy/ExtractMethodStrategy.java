package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;

public class ExtractMethodStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "extract_method";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Extract method requires additional parameters");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 3) {
            return createErrorResult("Extract method requires: start_line, end_line, highlighted, method_name");
        }
        
        int startLine = (int) params[0];
        int endLine = (int) params[1];
        String highlighted = (String) params[2];
        String methodName = (String) params[3];
        
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            ClassOrInterfaceDeclaration cls = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
            MethodDeclaration containingMethod = findHighlightedMethod(cls, startLine, endLine);
            
            if (containingMethod == null) {
                return createErrorResult("Extract Method only works on code inside methods, not on field declarations");
            }
            
            boolean isExpression = !highlighted.trim().endsWith(";");
            
            if (isExpression) {
                MethodDeclaration newMethod = cls.addMethod(methodName, Modifier.Keyword.PRIVATE);
                newMethod.setType("int");
                newMethod.getBody().get().addStatement("return " + highlighted.trim() + ";");
                
                String originalBody = containingMethod.getBody().get().toString();
                String modifiedBody = originalBody.replace(highlighted, methodName + "()");
                
                containingMethod.getBody().get().getStatements().clear();
                CompilationUnit tempCu = StaticJavaParser.parse("class Temp { void temp() " + modifiedBody + " }");
                MethodDeclaration tempMethod = tempCu.findFirst(MethodDeclaration.class).get();
                
                for (var stmt : tempMethod.getBody().get().getStatements()) {
                    containingMethod.getBody().get().addStatement(stmt);
                }
            } else {
                MethodDeclaration newMethod = cls.addMethod(methodName, Modifier.Keyword.PRIVATE);
                newMethod.setType("void");
                
                var statement = StaticJavaParser.parseStatement(highlighted.trim());
                newMethod.getBody().get().addStatement(statement);
                
                String originalBody = containingMethod.getBody().get().toString();
                String modifiedBody = originalBody.replace(highlighted, methodName + "();");
                
                containingMethod.getBody().get().getStatements().clear();
                CompilationUnit tempCu = StaticJavaParser.parse("class Temp { void temp() " + modifiedBody + " }");
                MethodDeclaration tempMethod = tempCu.findFirst(MethodDeclaration.class).get();
                
                for (var stmt : tempMethod.getBody().get().getStatements()) {
                    containingMethod.getBody().get().addStatement(stmt);
                }
            }
            
            return createSuccessResult(cu.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Extraction failed: " + e.getMessage());
        }
    }
    
    private MethodDeclaration findHighlightedMethod(ClassOrInterfaceDeclaration cls, int startLine, int endLine) {
        for (MethodDeclaration method : cls.getMethods()) {
            if (method.getBegin().isPresent() && method.getEnd().isPresent()) {
                int methodStart = method.getBegin().get().line;
                int methodEnd = method.getEnd().get().line;
                if (startLine >= methodStart && endLine <= methodEnd) {
                    return method;
                }
            }
        }
        return null;
    }
}
