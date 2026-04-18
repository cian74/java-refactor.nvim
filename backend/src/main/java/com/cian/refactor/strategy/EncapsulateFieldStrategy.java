package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.cian.refactor.Refactored;

public class EncapsulateFieldStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "encapsulate_field";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Encapsulate field requires: field_name");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 1) {
            return createErrorResult("Encapsulate field requires: field_name");
        }
        
        String fieldName = (String) params[0];
        
        if (fieldName == null || fieldName.isEmpty()) {
            return createErrorResult("Field name is required");
        }
        
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            ClassOrInterfaceDeclaration cls = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
            
            FieldDeclaration targetField = null;
            String fieldType = null;
            
            for (FieldDeclaration field : cls.getFields()) {
                for (VariableDeclarator var : field.getVariables()) {
                    if (var.getNameAsString().equals(fieldName)) {
                        targetField = field;
                        fieldType = var.getTypeAsString();
                        break;
                    }
                }
                if (targetField != null) break;
            }
            
            if (targetField == null) {
                return createErrorResult("Field '" + fieldName + "' not found");
            }
            
            if (!targetField.isPrivate()) {
                targetField.addModifier(Modifier.Keyword.PRIVATE);
            }
            
            String capitalizedName = capitalize(fieldName);
            
            String getterName = "get" + capitalizedName;
            if (!hasMethod(cls, getterName)) {
                MethodDeclaration getter = cls.addMethod(getterName, Modifier.Keyword.PUBLIC);
                getter.setType(fieldType);
                getter.createBody().addStatement("return " + fieldName + ";");
            }
            
            String setterName = "set" + capitalizedName;
            if (!hasMethod(cls, setterName)) {
                MethodDeclaration setter = cls.addMethod(setterName, Modifier.Keyword.PUBLIC);
                setter.addParameter(fieldType, fieldName);
                setter.createBody().addStatement("this." + fieldName + " = " + fieldName + ";");
            }
            
            Refactored result = createSuccessResult(cu.toString());
            result.error = "Encapsulated field '" + fieldName + "' - made private with getter/setter";
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Encapsulate field failed: " + e.getMessage());
        }
    }
}