package com.cian.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.gson.Gson;

public class RefactoringEngine {
	private static final Gson gson = new Gson();
	Refactored result = new Refactored();

	public String applyRefactor(String command, String source){
		switch (command) {
			case("generate_getters_setters"):
				result = generateGettersSetters(source);
				break;
		}
		return gson.toJson(result);
	}

	private Refactored generateGettersSetters(String source) throws RuntimeException {
		Refactored result = new Refactored();
		try {
			CompilationUnit cu = StaticJavaParser.parse(source);

			//have to add throw because ClassOrInterfaceDeclaration is an Optional type
			ClassOrInterfaceDeclaration bufferClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();

			for(FieldDeclaration field : bufferClass.getFields()){
				if(field.isPrivate()){
					String name = field.getVariable(0).getNameAsString();
					String type = field.getVariable(0).getTypeAsString();
					
					MethodDeclaration getter = bufferClass.addMethod("get" + name, Modifier.Keyword.PUBLIC);
					getter.setType(type);
					getter.createBody().addStatement("return " + name + ";");
				}

			}
			result.new_source = cu.toString();


		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return result;
	}


}
