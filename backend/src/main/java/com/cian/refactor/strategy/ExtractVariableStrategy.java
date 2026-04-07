package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.cian.refactor.Refactored;

public class ExtractVariableStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "extract_variable";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Extract variable requires additional parameters");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 3) {
            return createErrorResult("Extract variable requires: highlighted, var_name, start_line");
        }
        
        String highlighted = (String) params[0];
        String varName = (String) params[1];
        if (params[2] == null) {
            return createErrorResult("start_line is required for extract_variable");
        }
        int startLine = (int) params[2];
        
        if (highlighted == null || highlighted.isEmpty()) {
            return createErrorResult("No expression selected to extract");
        }
        
        if (varName == null || varName.isEmpty()) {
            return createErrorResult("No variable name provided");
        }
        
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            ClassOrInterfaceDeclaration cls = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
            
            MethodDeclaration containingMethod = findMethodContainingLine(cls, startLine);
            
            if (containingMethod == null || !containingMethod.getBody().isPresent()) {
                return createErrorResult("Could not find method containing the expression");
            }
            
            BlockStmt methodBody = containingMethod.getBody().get();
            boolean found = false;
            
            for (int i = 0; i < methodBody.getStatements().size(); i++) {
                Statement stmt = methodBody.getStatements().get(i);
                String stmtStr = stmt.toString();
                
                if (stmtStr.contains(highlighted)) {
                    found = true;
                    
                    String varType = inferType(highlighted);
                    String modifiedStmtStr = stmtStr.replace(highlighted, varName);
                    
                    String varDeclaration = varType + " " + varName + " = " + highlighted + ";";
                    
                    Statement modifiedStatement = StaticJavaParser.parseStatement(modifiedStmtStr);
                    Statement varStmt = StaticJavaParser.parseStatement(varDeclaration);
                    
                    methodBody.addStatement(i, varStmt);
                    methodBody.getStatements().set(i + 1, modifiedStatement);
                    
                    break;
                }
            }
            
            if (!found) {
                return createErrorResult("Could not find the selected expression in the code");
            }
            
            return createSuccessResult(cu.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Extract variable failed: " + e.getMessage());
        }
    }
    
    private String inferType(String expression) {
        expression = expression.trim();
        
        if (expression.startsWith("\"") && expression.endsWith("\"")) return "String";
        if (expression.equals("true") || expression.equals("false")) return "boolean";
        if (expression.startsWith("'") && expression.endsWith("'")) return "char";
        
        if (expression.contains("(") && expression.contains(")")) {
            if (expression.contains(".length()")) return "int";
            if (expression.contains(".toString()")) return "String";
            if (expression.contains(".equals(")) return "boolean";
            if (expression.contains(".hashCode()")) return "int";
        }
        
        if (expression.contains("+") || expression.contains("-") || expression.contains("*") 
                || expression.contains("/") || expression.contains("%")) {
            if (expression.contains(".")) return "double";
            return "int";
        }
        
        if (expression.contains("==") || expression.contains("!=") || expression.contains(">") 
                || expression.contains("<")) {
            return "boolean";
        }
        
        return "var";
    }
}
