package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;

public abstract class AbstractRefactoringStrategy implements RefactoringStrategy {
    
    protected ClassOrInterfaceDeclaration parseClass(String source) {
        CompilationUnit cu = StaticJavaParser.parse(source);
        return cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
    }
    
    protected Refactored createErrorResult(String errorMessage) {
        Refactored result = new Refactored();
        result.error = errorMessage;
        return result;
    }
    
    protected Refactored createSuccessResult(String newSource) {
        Refactored result = new Refactored();
        result.new_source = newSource;
        return result;
    }
    
    protected MethodDeclaration findMethodContainingLine(ClassOrInterfaceDeclaration cls, int line) {
        for (MethodDeclaration method : cls.getMethods()) {
            if (method.getBegin().isPresent() && method.getEnd().isPresent()) {
                int methodStart = method.getBegin().get().line;
                int methodEnd = method.getEnd().get().line;
                if (line >= methodStart && line <= methodEnd) {
                    return method;
                }
            }
        }
        return null;
    }
    
    protected boolean hasMethod(ClassOrInterfaceDeclaration cls, String methodName) {
        return cls.getMethods().stream()
            .anyMatch(m -> m.getNameAsString().equals(methodName));
    }
    
    protected String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
