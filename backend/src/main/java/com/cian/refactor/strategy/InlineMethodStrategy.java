package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.Node;
import com.cian.refactor.Refactored;
import java.util.List;
import java.util.Optional;

public class InlineMethodStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "inline_method";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Inline method requires additional parameters");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 1) {
            return createErrorResult("Inline method requires: start_line");
        }
        
        int startLine = (int) params[0];
        
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            ClassOrInterfaceDeclaration cls = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
            
            MethodDeclaration methodToInline = findMethodAtPosition(cls, startLine);
            
            if (methodToInline == null) {
                return createErrorResult("No method found at cursor position");
            }
            
            String methodName = methodToInline.getNameAsString();
            
            List<MethodCallExpr> methodCalls = cu.findAll(MethodCallExpr.class);
            
            for (MethodCallExpr call : methodCalls) {
                if (call.getNameAsString().equals(methodName)) {
                    if (methodToInline.getBody().isPresent()) {
                        String methodBody = methodToInline.getBody().get().toString();
                        
                        if (isSimpleMethod(methodToInline)) {
                            String inlinedBody = processMethodBody(methodBody, methodToInline.getParameters(), call.getArguments());
                            replaceMethodCall(call, inlinedBody);
                        } else {
                            return createErrorResult("Complex methods cannot be inlined automatically");
                        }
                    }
                }
            }
            
            methodToInline.remove();
            
            return createSuccessResult(cu.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Inline method failed: " + e.getMessage());
        }
    }
    
    private MethodDeclaration findMethodAtPosition(ClassOrInterfaceDeclaration cls, int line) {
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
    
    private boolean isSimpleMethod(MethodDeclaration method) {
        if (!method.getBody().isPresent()) return false;
        return method.getBody().get().getStatements().size() <= 1;
    }
    
    private String processMethodBody(String body, List<Parameter> parameters, 
            List<com.github.javaparser.ast.expr.Expression> arguments) {
        String processed = body;
        
        if (processed.startsWith("{") && processed.endsWith("}")) {
            processed = processed.substring(1, processed.length() - 1).trim();
        }
        
        if (processed.startsWith("return ")) {
            processed = processed.substring("return ".length());
            if (processed.endsWith(";")) {
                processed = processed.substring(0, processed.length() - 1);
            }
        }
        
        for (int i = 0; i < parameters.size() && i < arguments.size(); i++) {
            String paramName = parameters.get(i).getNameAsString();
            String argValue = arguments.get(i).toString();
            processed = processed.replaceAll("\\b" + paramName + "\\b", argValue);
        }
        
        return processed;
    }
    
    private void replaceMethodCall(MethodCallExpr call, String replacement) {
        try {
            var replacementExpr = StaticJavaParser.parseExpression(replacement);
            
            Optional<Node> parentOpt = call.getParentNode();
            if (parentOpt.isPresent()) {
                Node parent = parentOpt.get();
                
                if (parent instanceof com.github.javaparser.ast.expr.AssignExpr) {
                    var assignExpr = (com.github.javaparser.ast.expr.AssignExpr) parent;
                    assignExpr.setValue(replacementExpr);
                } else {
                    call.replace(replacementExpr);
                }
            } else {
                call.replace(replacementExpr);
            }
        } catch (Exception e) {
            System.err.println("Failed to replace method call: " + e.getMessage());
        }
    }
}
