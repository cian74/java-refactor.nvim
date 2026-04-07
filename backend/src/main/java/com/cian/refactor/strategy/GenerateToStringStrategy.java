package com.cian.refactor.strategy;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;
import java.util.ArrayList;
import java.util.List;

public class GenerateToStringStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "generate_toString";
    }
    
    @Override
    public Refactored apply(String source) {
        try {
            ClassOrInterfaceDeclaration cls = parseClass(source);
            
            if (hasMethod(cls, "toString")) {
                return createErrorResult("toString() method already exists");
            }
            
            List<String> fieldNames = new ArrayList<>();
            
            for (FieldDeclaration field : cls.getFields()) {
                if (field.isPrivate()) {
                    String name = field.getVariable(0).getNameAsString();
                    fieldNames.add(name);
                }
            }
            
            if (fieldNames.isEmpty()) {
                return createErrorResult("No private fields found in class");
            }
            
            String className = cls.getNameAsString();
            
            MethodDeclaration toStringMethod = cls.addMethod("toString", Modifier.Keyword.PUBLIC);
            toStringMethod.setType("String");
            
            StringBuilder sb = new StringBuilder();
            sb.append("\"");
            sb.append(className);
            sb.append("{\" + ");
            
            for (int i = 0; i < fieldNames.size(); i++) {
                sb.append("\"");
                sb.append(fieldNames.get(i));
                sb.append("=\" + String.valueOf(");
                sb.append(fieldNames.get(i));
                sb.append(")");
                if (i < fieldNames.size() - 1) {
                    sb.append(" + \", \" + ");
                }
            }
            
            sb.append(" + \"}\"");
            
            toStringMethod.createBody().addStatement("return " + sb.toString() + ";");
            
            return createSuccessResult(cls.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Failed to generate toString: " + e.getMessage());
        }
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        return apply(source);
    }
}
