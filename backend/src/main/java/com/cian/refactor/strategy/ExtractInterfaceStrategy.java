package com.cian.refactor.strategy;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.cian.refactor.Refactored;

import java.util.ArrayList;
import java.util.List;

public class ExtractInterfaceStrategy extends AbstractRefactoringStrategy {
    
    @Override
    public String getCommandName() {
        return "extract_interface";
    }
    
    @Override
    public Refactored apply(String source) {
        return createErrorResult("Extract interface requires: interface_name, method_names");
    }
    
    @Override
    public Refactored apply(String source, Object... params) {
        if (params.length < 2) {
            return createErrorResult("Extract interface requires: interface_name, method_names");
        }
        
        String interfaceName = (String) params[0];
        String methodNamesStr = (String) params[1];
        
        if (interfaceName == null || interfaceName.isEmpty()) {
            return createErrorResult("Interface name is required");
        }
        
        if (methodNamesStr == null || methodNamesStr.isEmpty()) {
            return createErrorResult("Method names are required");
        }
        
        if (methodNamesStr.contains(",")) {
            methodNamesStr = methodNamesStr.replace(",", " ");
        }
        
        List<String> methodNames = new ArrayList<>();
        for (String name : methodNamesStr.split("\\s+")) {
            if (!name.trim().isEmpty()) {
                methodNames.add(name.trim());
            }
        }
        
        if (methodNames.isEmpty()) {
            return createErrorResult("No valid method names provided");
        }
        
        try {
            CompilationUnit cu = StaticJavaParser.parse(source);
            ClassOrInterfaceDeclaration cls = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
            
            if (cls.isInterface()) {
                return createErrorResult("Cannot extract interface from an interface");
            }
            
            String className = cls.getNameAsString();
            
            List<MethodDeclaration> methodsToExtract = new ArrayList<>();
            for (String methodName : methodNames) {
                MethodDeclaration method = findMethodByName(cls, methodName);
                if (method == null) {
                    return createErrorResult("Method '" + methodName + "' not found in class");
                }
                methodsToExtract.add(method);
            }
            
            CompilationUnit interfaceCu = StaticJavaParser.parse("public interface " + interfaceName + " {}");
            ClassOrInterfaceDeclaration newInterface = interfaceCu.findFirst(ClassOrInterfaceDeclaration.class).get();
            
            for (MethodDeclaration method : methodsToExtract) {
                var interfaceMethod = newInterface.addMethod(method.getNameAsString())
                    .setType(method.getType().clone())
                    .setModifiers(Modifier.Keyword.PUBLIC);
                
                var newParams = interfaceMethod.getParameters();
                for (var param : method.getParameters()) {
                    newParams.add(param.clone());
                }
                
                interfaceMethod.setBody(null);
            }
            
            if (!cls.getExtendedTypes().isEmpty()) {
                for (var extended : cls.getExtendedTypes()) {
                    newInterface.addExtendedType(extended.clone());
                }
                cls.getExtendedTypes().clear();
            }
            
            cls.addImplementedType(interfaceName);
            
            Refactored result = new Refactored();
            result.new_source = cu.toString();
            result.new_interface_source = interfaceCu.toString();
            result.new_interface_name = interfaceName;
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResult("Extract interface failed: " + e.getMessage());
        }
    }
    
    private MethodDeclaration findMethodByName(ClassOrInterfaceDeclaration cls, String methodName) {
        for (MethodDeclaration method : cls.getMethods()) {
            if (method.getNameAsString().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
}