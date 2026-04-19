package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.cian.refactor.Refactored;
import java.util.List;

public class PullPushStrategy extends AbstractRefactoringStrategy {
    
    private CompilationUnit currentCu;
    
    @Override
    public String getCommandName() {
        return "pull_push";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Pull/Push requires: direction, member_name, from_line");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 3) {
            return createErrorResult("Pull/Push requires: direction, member_name, from_line");
        }
        
        String direction = (String) params[0];
        String memberName = (String) params[1];
        int fromLine = (int) params[2];
        
        if (direction == null || direction.isEmpty()) {
            return createErrorResult("Direction required: 'pull' or 'push'");
        }
        
        if (memberName == null || memberName.isEmpty()) {
            return createErrorResult("Member name (method or field) is required");
        }
        
        try {
            currentCu = StaticJavaParser.parse(source);
            List<ClassOrInterfaceDeclaration> classes = currentCu.findAll(ClassOrInterfaceDeclaration.class);
            
            if (classes.size() < 2) {
                return createErrorResult("Need at least 2 classes (parent and child) for pull/push");
            }
            
            ClassOrInterfaceDeclaration sourceClass = null;
            ClassOrInterfaceDeclaration targetClass = null;
            
            for (ClassOrInterfaceDeclaration cls : classes) {
                if (cls.getRange().isPresent()) {
                    int startLine = cls.getRange().get().begin.line;
                    int endLine = cls.getRange().get().end.line;
                    
                    if (fromLine >= startLine && fromLine <= endLine) {
                        sourceClass = cls;
                    } else if (targetClass == null) {
                        targetClass = cls;
                    }
                }
            }
            
            if (sourceClass == null) {
                return createErrorResult("Could not find class at cursor position");
            }
            
            if (targetClass == null) {
                return createErrorResult("Could not find target class (need exactly 2 classes)");
            }
            
            if (targetClass.isInterface() || sourceClass.isInterface()) {
                return createErrorResult("Cannot pull/push to/from interfaces");
            }
            
            if (direction.equalsIgnoreCase("pull")) {
                return pullUp(sourceClass, targetClass, memberName);
            } else if (direction.equalsIgnoreCase("push")) {
                return pushDown(sourceClass, targetClass, memberName);
            } else {
                return createErrorResult("Invalid direction: use 'pull' or 'push'");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Pull/Push failed: " + e.getMessage());
        }
    }
    
    private Refactored pullUp(ClassOrInterfaceDeclaration fromClass, ClassOrInterfaceDeclaration toClass, String memberName) {
        MethodDeclaration method = null;
        FieldDeclaration field = null;
        
        for (MethodDeclaration m : fromClass.getMethods()) {
            if (m.getNameAsString().equals(memberName)) {
                method = m;
                break;
            }
        }
        
        if (method == null) {
            for (FieldDeclaration f : fromClass.getFields()) {
                for (VariableDeclarator v : f.getVariables()) {
                    if (v.getNameAsString().equals(memberName)) {
                        field = f;
                        break;
                    }
                }
                if (field != null) break;
            }
        }
        
        if (method == null && field == null) {
            return createErrorResult("Member '" + memberName + "' not found in source class");
        }
        
        if (method != null) {
            String methodName = method.getNameAsString();
            String returnType = method.getTypeAsString();
            
            boolean alreadyExists = toClass.getMethods().stream()
                .anyMatch(m -> m.getNameAsString().equals(methodName));
            
            if (!alreadyExists) {
                MethodDeclaration newMethod = toClass.addMethod(methodName, Modifier.Keyword.PUBLIC);
                newMethod.setType(returnType);
                if (method.getBody().isPresent()) {
                    for (var stmt : method.getBody().get().getStatements()) {
                        newMethod.getBody().get().addStatement(stmt.clone());
                    }
                }
            }
            
            method.remove();
            
            Refactored result = createSuccessResult(currentCu.toString());
            result.error = "Pulled up method '" + methodName + "' to parent class";
            return result;
        }
        
        if (field != null) {
            boolean alreadyExists = toClass.getFields().stream()
                .anyMatch(f -> f.getVariables().stream()
                    .anyMatch(v -> v.getNameAsString().equals(memberName)));
            
            if (!alreadyExists) {
                toClass.addField(field.getElementType(), memberName, Modifier.Keyword.PRIVATE);
            }
            
            field.remove();
            
            Refactored result = createSuccessResult(currentCu.toString());
            result.error = "Pulled up field '" + memberName + "' to parent class";
            return result;
        }
        
        return createErrorResult("Failed to pull up member");
    }
    
    private Refactored pushDown(ClassOrInterfaceDeclaration fromClass, ClassOrInterfaceDeclaration toClass, String memberName) {
        MethodDeclaration method = null;
        FieldDeclaration field = null;
        
        for (MethodDeclaration m : fromClass.getMethods()) {
            if (m.getNameAsString().equals(memberName)) {
                method = m;
                break;
            }
        }
        
        if (method == null) {
            for (FieldDeclaration f : fromClass.getFields()) {
                for (VariableDeclarator v : f.getVariables()) {
                    if (v.getNameAsString().equals(memberName)) {
                        field = f;
                        break;
                    }
                }
                if (field != null) break;
            }
        }
        
        if (method == null && field == null) {
            return createErrorResult("Member '" + memberName + "' not found in source class");
        }
        
        if (method != null) {
            String methodName = method.getNameAsString();
            String returnType = method.getTypeAsString();
            
            boolean alreadyExists = toClass.getMethods().stream()
                .anyMatch(m -> m.getNameAsString().equals(methodName));
            
            if (!alreadyExists) {
                MethodDeclaration newMethod = toClass.addMethod(methodName, Modifier.Keyword.PUBLIC);
                newMethod.setType(returnType);
                if (method.getBody().isPresent()) {
                    for (var stmt : method.getBody().get().getStatements()) {
                        newMethod.getBody().get().addStatement(stmt.clone());
                    }
                }
            }
            
            method.remove();
            
            Refactored result = createSuccessResult(currentCu.toString());
            result.error = "Pushed down method '" + methodName + "' to child class";
            return result;
        }
        
        if (field != null) {
            boolean alreadyExists = toClass.getFields().stream()
                .anyMatch(f -> f.getVariables().stream()
                    .anyMatch(v -> v.getNameAsString().equals(memberName)));
            
            if (!alreadyExists) {
                toClass.addField(field.getElementType(), memberName, Modifier.Keyword.PRIVATE);
            }
            
            field.remove();
            
            Refactored result = createSuccessResult(currentCu.toString());
            result.error = "Pushed down field '" + memberName + "' to child class";
            return result;
        }
        
        return createErrorResult("Failed to push down member");
    }
}
