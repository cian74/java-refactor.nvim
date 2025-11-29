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
			case("extract_method"):
				result = extractMethod(source);
		}
		return gson.toJson(result);
	}

	private Refactored extractMethod(String source) throws RuntimeException {
		Refactored result = new Refactored(); 
		try {
			
		} catch (Exception e) {
			System.err.println(e.getStackTrace());
		}

		return result;
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

					String capitalisedName = capitalise(name);
					
					MethodDeclaration getter = bufferClass.addMethod("get" + capitalisedName, Modifier.Keyword.PUBLIC);
					getter.setType(type);
					getter.createBody().addStatement("return " + name + ";");

					MethodDeclaration setter = bufferClass.addMethod("set" + capitalisedName, Modifier.Keyword.PUBLIC);
					setter.addParameter(type, name);
					setter.createBody().addStatement("this." + name + " = " + name + ";");

				}

			}
			result.new_source = cu.toString();


		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return result;
	}

	//capitalise method for correct method declaration
	public String capitalise(String methodName){
		if(methodName == null || methodName.isEmpty()) return methodName;
		return methodName.substring(0,1).toUpperCase() + methodName.substring(1);
	}

}
