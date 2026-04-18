package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.cian.refactor.Refactored;
import java.util.Optional;

public class RenameStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "rename";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Rename requires: old_name, new_name, start_line, scope");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 4) {
            return createErrorResult("Rename requires: old_name, new_name, start_line, scope");
        }
        
        String oldName = (String) params[0];
        String newName = (String) params[1];
        int startLine = (int) params[2];
        String scope = (String) params[3];
        
        if (oldName == null || oldName.isEmpty()) {
            return createErrorResult("Old name is required");
        }
        
        if (newName == null || newName.isEmpty()) {
            return createErrorResult("New name is required");
        }
        
        if (oldName.equals(newName)) {
            return createErrorResult("New name must be different from old name");
        }
        
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            
            int renameCount = 0;
            
            switch (scope.toLowerCase()) {
                case "variable":
                    renameCount = renameLocalVariable(cu, oldName, newName, startLine);
                    break;
                case "method":
                    renameCount = renameMethod(cu, oldName, newName);
                    break;
                case "class":
                    renameCount = renameClass(cu, oldName, newName);
                    break;
                case "field":
                    renameCount = renameField(cu, oldName, newName);
                    break;
                case "all":
                    renameCount = renameAll(cu, oldName, newName, startLine);
                    break;
                default:
                    return createErrorResult("Unknown scope: " + scope + ". Use: variable, method, class, field, or all");
            }
            
            if (renameCount == 0) {
                return createErrorResult("No occurrences of '" + oldName + "' found");
            }
            
            Refactored result = createSuccessResult(cu.toString());
            result.error = "Renamed " + renameCount + " occurrence(s)";
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Rename failed: " + e.getMessage());
        }
    }
    
    private int renameLocalVariable(CompilationUnit cu, String oldName, String newName, int cursorLine) {
        int count = 0;
        
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (!method.getRange().isPresent()) continue;
            int methodStart = method.getRange().get().begin.line;
            int methodEnd = method.getRange().get().end.line;
            
            if (cursorLine >= methodStart && cursorLine <= methodEnd) {
                for (var stmt : method.getBody().get().getStatements()) {
                    for (var var : stmt.findAll(VariableDeclarator.class)) {
                        if (var.getNameAsString().equals(oldName)) {
                            var.setName(newName);
                            count++;
                        }
                    }
                    for (var nameExpr : stmt.findAll(NameExpr.class)) {
                        if (nameExpr.getNameAsString().equals(oldName)) {
                            nameExpr.setName(newName);
                            count++;
                        }
                    }
                }
            }
        }
        
        return count;
    }
    
    private int renameAll(CompilationUnit cu, String oldName, String newName, int cursorLine) {
        int count = 0;
        
        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (cls.getNameAsString().equals(oldName)) {
                cls.setName(newName);
                count++;
            }
        }
        
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (method.getNameAsString().equals(oldName)) {
                method.setName(newName);
                count++;
            }
            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                if (call.getNameAsString().equals(oldName)) {
                    call.setName(newName);
                    count++;
                }
            }
            for (var var : method.findAll(VariableDeclarator.class)) {
                if (var.getNameAsString().equals(oldName)) {
                    var.setName(newName);
                    count++;
                }
            }
            for (NameExpr nameExpr : method.findAll(NameExpr.class)) {
                if (nameExpr.getNameAsString().equals(oldName)) {
                    nameExpr.setName(newName);
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private int renameMethod(CompilationUnit cu, String oldName, String newName) {
        int count = 0;
        
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (method.getNameAsString().equals(oldName)) {
                method.setName(newName);
                count++;
            }
        }
        
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (call.getNameAsString().equals(oldName)) {
                call.setName(newName);
                count++;
            }
        }
        
        return count;
    }
    
    private int renameClass(CompilationUnit cu, String oldName, String newName) {
        int count = 0;
        
        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (cls.getNameAsString().equals(oldName)) {
                cls.setName(newName);
                count++;
            }
        }
        
        return count;
    }
    
    private int renameField(CompilationUnit cu, String oldName, String newName) {
        int count = 0;
        
        for (var field : cu.findAll(com.github.javaparser.ast.body.FieldDeclaration.class)) {
            for (var var : field.getVariables()) {
                if (var.getNameAsString().equals(oldName)) {
                    var.setName(newName);
                    count++;
                }
            }
        }
        
        for (NameExpr nameExpr : cu.findAll(NameExpr.class)) {
            if (nameExpr.getNameAsString().equals(oldName)) {
                nameExpr.setName(newName);
                count++;
            }
        }
        
        return count;
    }
}