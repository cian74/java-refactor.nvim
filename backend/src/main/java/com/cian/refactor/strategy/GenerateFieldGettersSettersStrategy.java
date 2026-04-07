package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;
import java.util.List;

public class GenerateFieldGettersSettersStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "generate_field_getters_setters";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Generate field getters/setters requires selected_fields parameter");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 1) {
            return createErrorResult("Generate field getters/setters requires selected_fields");
        }
        
        List<String> selectedFields = (List<String>) params[0];
        
        try {
            ClassOrInterfaceDeclaration cls = parseClass(source);
            
            for (FieldDeclaration field : cls.getFields()) {
                if (field.isPrivate()) {
                    String name = field.getVariable(0).getNameAsString();
                    String type = field.getVariable(0).getTypeAsString();
                    
                    if (selectedFields != null && selectedFields.contains(name)) {
                        String capitalizedName = capitalize(name);
                        
                        String getterName = "get" + capitalizedName;
                        if (!hasMethod(cls, getterName)) {
                            MethodDeclaration getter = cls.addMethod(getterName, Modifier.Keyword.PUBLIC);
                            getter.setType(type);
                            getter.createBody().addStatement("return " + name + ";");
                        }
                        
                        String setterName = "set" + capitalizedName;
                        if (!hasMethod(cls, setterName)) {
                            MethodDeclaration setter = cls.addMethod(setterName, Modifier.Keyword.PUBLIC);
                            setter.addParameter(type, name);
                            setter.createBody().addStatement("this." + name + " = " + name + ";");
                        }
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
