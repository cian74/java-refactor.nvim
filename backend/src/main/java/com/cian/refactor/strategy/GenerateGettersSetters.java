package com.cian.refactor.strategy;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;
import java.util.List;

public class GenerateGettersSetters extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "generate_getters_setters";
    }
    
    @Override
    public Refactored apply(String source) {
        return apply(source, (Object[]) null);
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        try {
            ClassOrInterfaceDeclaration cls = parseClass(source);
            
            for (FieldDeclaration field : cls.getFields()) {
                if (field.isPrivate()) {
                    String fieldName = field.getVariable(0).getNameAsString();
                    String fieldType = field.getVariable(0).getTypeAsString();
                    String capitalizedName = capitalize(fieldName);
                    
                    // Generate getter
                    String getterName = "get" + capitalizedName;
                    if (!hasMethod(cls, getterName)) {
                        MethodDeclaration getter = cls.addMethod(getterName, Modifier.Keyword.PUBLIC);
                        getter.setType(fieldType);
                        getter.createBody().addStatement("return " + fieldName + ";");
                    }
                    
                    // Generate setter
                    String setterName = "set" + capitalizedName;
                    if (!hasMethod(cls, setterName)) {
                        MethodDeclaration setter = cls.addMethod(setterName, Modifier.Keyword.PUBLIC);
                        setter.addParameter(fieldType, fieldName);
                        setter.createBody().addStatement("this." + fieldName + " = " + fieldName + ";");
                    }
                }
            }
            
            return createSuccessResult(cls.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Failed to generate getters/setters: " + e.getMessage());
        }
    }
}
