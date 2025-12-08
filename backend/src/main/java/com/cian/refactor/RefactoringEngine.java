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
	Request request = new Request();

	public String applyRefactor(String command, Request request){
		switch (command) {
			case("generate_getters_setters"):
				result = generateGettersSetters(request.source);
				break;
			case("extract_method"):
				result = extractMethod(request);
				break;
		}
		return gson.toJson(result);
	}

	private Refactored extractMethod(Request request) throws RuntimeException {
		//System.out.println(request.source);
		Refactored result = new Refactored(); 
		try {
			CompilationUnit cu = StaticJavaParser.parse(request.source);	

			ClassOrInterfaceDeclaration bufferClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();

			MethodDeclaration highlightedMethod= findHighlightedMethod(bufferClass, request.start_line, request.end_line);

			if(highlightedMethod == null){
				return result;
			}

			var statements = highlightedMethod.getBody().get().getStatements();
			

			System.err.println(highlightedMethod.getNameAsString());

			result.new_source = cu.toString();


		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		return result;
	}

	private MethodDeclaration findHighlightedMethod(ClassOrInterfaceDeclaration cls, int startLine, int endLine){
		for(MethodDeclaration method : cls.getMethods()){
			if(method.getBegin().isPresent() && method.getEnd().isPresent()){
				int methodStart = method.getBegin().get().line;
				int methodEnd = method.getEnd().get().line;

				System.err.println("Checking Method: " + method.getNameAsString() + " start: " + methodStart + " end: " + methodEnd);

				if (startLine >= methodStart && endLine <= methodEnd) {
					return method;
				}
			}
		}
		return null;
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
