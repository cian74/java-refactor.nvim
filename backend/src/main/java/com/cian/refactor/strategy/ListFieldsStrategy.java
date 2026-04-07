package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;
import java.util.ArrayList;
import java.util.List;

public class ListFieldsStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "list_fields";
    }
    
    @Override
    public Refactored apply(String source) {
        try {
            ClassOrInterfaceDeclaration cls = parseClass(source);
            
            List<String> fieldNames = new ArrayList<>();
            for (FieldDeclaration field : cls.getFields()) {
                if (field.isPrivate()) {
                    String name = field.getVariable(0).getNameAsString();
                    fieldNames.add(name);
                }
            }
            
            Refactored result = new Refactored();
            result.fields = fieldNames;
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult(e.getMessage());
        }
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        return apply(source);
    }
}
