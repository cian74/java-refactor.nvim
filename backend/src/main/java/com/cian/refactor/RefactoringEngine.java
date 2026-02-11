package com.cian.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
			case("list_fields"):
				result = listFields(request.source);
				break;
			case("generate_field_getters_setters"):
				result = generateFieldGettersSetters(request);
				break;
			case("inline_method"):
				result = inlineMethod(request);
				break;

		}
		return gson.toJson(result);
	}
	
	private Refactored inlineMethod(Request request) throws RuntimeException{
		Refactored result = new Refactored();
		try {
			CompilationUnit cu = StaticJavaParser.parse(request.source);
			ClassOrInterfaceDeclaration bufferClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();

			// find the method to inline (cursor position or method name)
			MethodDeclaration methodToInline = findMethodAtPosition(bufferClass, request.start_line);
			
			if (methodToInline == null) {
				result.error = "No method found at cursor position";
				return result;
			}

			String methodName = methodToInline.getNameAsString();
			
			List<MethodCallExpr> methodCalls = cu.findAll(MethodCallExpr.class);
			
			for (MethodCallExpr call : methodCalls) {
				if (call.getNameAsString().equals(methodName)) {
					if (methodToInline.getBody().isPresent()) {
						String methodBody = methodToInline.getBody().get().toString();
						
						if (isSimpleMethod(methodToInline)) {
							// Remove outer braces and replace variables
							String inlinedBody = processMethodBody(methodBody, methodToInline.getParameters(), call.getArguments());
							replaceMethodCall(call, inlinedBody);
						} else {
							result.error = "Complex methods cannot be inlined automatically";
							return result;
						}
					}
				}
			}

			// Remove the inlined method
			methodToInline.remove();

			result.new_source = cu.toString();

		} catch (Exception e) {
			e.printStackTrace();
			result.error = "Inline method failed: " + e.getMessage();
		}
		return result;
	}


	private Refactored listFields(String source) throws RuntimeException {
		Refactored result = new Refactored();
		try {
			CompilationUnit cu = StaticJavaParser.parse(source);	
			ClassOrInterfaceDeclaration bufferClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();

			List<String> fieldNames = new ArrayList<>();
			for(FieldDeclaration field : bufferClass.getFields()){
				if(field.isPrivate()){
					String name = field.getVariable(0).getNameAsString();
					fieldNames.add(name);
				}
			}
			result.fields = fieldNames;

		} catch (Exception e) {
			e.printStackTrace();
			result.error = e.getMessage();
		}
		return result;
	}

	private Refactored generateFieldGettersSetters(Request request) throws RuntimeException {
		Refactored result = new Refactored();
		try {
			CompilationUnit cu = StaticJavaParser.parse(request.source);

			//have to add throw because ClassOrInterfaceDeclaration is an Optional type
			ClassOrInterfaceDeclaration bufferClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();

			for(FieldDeclaration field : bufferClass.getFields()){
				if(field.isPrivate()){
					String name = field.getVariable(0).getNameAsString();
					String type = field.getVariable(0).getTypeAsString();

					if(request.selected_fields != null && request.selected_fields.contains(name)){

						String capitalisedName = capitalise(name);
						String getterName = "get" + capitalisedName;
						String setterName = "set" + capitalisedName;

						if(!hasMethod(bufferClass, getterName)){
							MethodDeclaration getter = bufferClass.addMethod("get" + capitalisedName, Modifier.Keyword.PUBLIC);
							getter.setType(type);
							getter.createBody().addStatement("return " + name + ";");
						}

						if(!hasMethod(bufferClass, setterName)){
							MethodDeclaration setter = bufferClass.addMethod("set" + capitalisedName, Modifier.Keyword.PUBLIC);
							setter.addParameter(type, name);
							setter.createBody().addStatement("this." + name + " = " + name + ";");
						}
					}
				}

			}

			result.new_source = cu.toString();

		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return result;
	}

	private Refactored extractMethod(Request request) throws RuntimeException {
		Refactored result = new Refactored(); 
		try {
			CompilationUnit cu = StaticJavaParser.parse(request.source);	
			ClassOrInterfaceDeclaration bufferClass = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();
			MethodDeclaration containingMethod = findHighlightedMethod(bufferClass, request.start_line, request.end_line);

			if(containingMethod == null){
				result.error = "Extract Method only works on code inside methods, not on field declarations";
				return result;
			}

			System.err.println("Found method: " + containingMethod.getNameAsString());
			System.err.println("Highlighted text: '" + request.highlighted + "'");

			boolean isExpression = !request.highlighted.trim().endsWith(";");

			if (isExpression) {
				MethodDeclaration newMethod = bufferClass.addMethod(request.method_name, Modifier.Keyword.PRIVATE);

				// Try to infer return type (defaulting to int for now)
				newMethod.setType("int");
				// For expressions, wrap in return statement
				newMethod.getBody().get().addStatement("return " + request.highlighted.trim() + ";");

				// Replace expression in original method with method call
				String originalBody = containingMethod.getBody().get().toString();
				String modifiedBody = originalBody.replace(request.highlighted, request.method_name + "()");

				// Re-parse the body
				containingMethod.getBody().get().getStatements().clear();
				CompilationUnit tempCu = StaticJavaParser.parse("class Temp { void temp() " + modifiedBody + " }");
				MethodDeclaration tempMethod = tempCu.findFirst(MethodDeclaration.class).get();

				for(var stmt : tempMethod.getBody().get().getStatements()) {
					containingMethod.getBody().get().addStatement(stmt);
				}
			} else {
				MethodDeclaration newMethod = bufferClass.addMethod(request.method_name, Modifier.Keyword.PRIVATE);
				newMethod.setType("void");

				// Parse as a statement
				var statement = StaticJavaParser.parseStatement(request.highlighted.trim());
				newMethod.getBody().get().addStatement(statement);

				// Replace in original
				String originalBody = containingMethod.getBody().get().toString();
				String modifiedBody = originalBody.replace(request.highlighted, request.method_name + "();");

				containingMethod.getBody().get().getStatements().clear();
				CompilationUnit tempCu = StaticJavaParser.parse("class Temp { void temp() " + modifiedBody + " }");
				MethodDeclaration tempMethod = tempCu.findFirst(MethodDeclaration.class).get();

				for(var stmt : tempMethod.getBody().get().getStatements()) {
					containingMethod.getBody().get().addStatement(stmt);
				}
			}

			result.new_source = cu.toString();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			result.error = "Extraction failed: " + e.getMessage();
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
					String getterName = "get" + capitalisedName;
					String setterName = "set" + capitalisedName;

					if(!hasMethod(bufferClass, getterName)){
						MethodDeclaration getter = bufferClass.addMethod("get" + capitalisedName, Modifier.Keyword.PUBLIC);
						getter.setType(type);
						getter.createBody().addStatement("return " + name + ";");
					}

					if(!hasMethod(bufferClass, setterName)){
						MethodDeclaration setter = bufferClass.addMethod("set" + capitalisedName, Modifier.Keyword.PUBLIC);
						setter.addParameter(type, name);
						setter.createBody().addStatement("this." + name + " = " + name + ";");
					}
				}

			}

			result.new_source = cu.toString();

		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean hasMethod(ClassOrInterfaceDeclaration cls, String methodName){
		for(MethodDeclaration method: cls.getMethods()){
			if(method.getNameAsString().equals(methodName)){
				return true;
			}
		}
		return false;
	}

	//capitalise method for correct method declaration
	public String capitalise(String methodName){
		if(methodName == null || methodName.isEmpty()) return methodName;
		return methodName.substring(0,1).toUpperCase() + methodName.substring(1);
	}

	private MethodDeclaration findMethodAtPosition(ClassOrInterfaceDeclaration cls, Integer line) {
		for (MethodDeclaration method : cls.getMethods()) {
			if (method.getBegin().isPresent()) {
				int methodLine = method.getBegin().get().line;
				if (line != null && line == methodLine) {
					return method;
				}
			}
		}
		return null;
	}

	private boolean isSimpleMethod(MethodDeclaration method) {
		// Check if method is simple enough to inline (single return statement or simple expression)
		if (!method.getBody().isPresent()) {
			return false;
		}
		
		int statementCount = method.getBody().get().getStatements().size();
		return statementCount <= 1;
	}

	private String processMethodBody(String body, List<Parameter> parameters, List<com.github.javaparser.ast.expr.Expression> arguments) {
		String processed = body;
		
		// Remove outer braces
		if (processed.startsWith("{") && processed.endsWith("}")) {
			processed = processed.substring(1, processed.length() - 1).trim();
		}
		
		// Replace return statement if present
		if (processed.startsWith("return ")) {
			processed = processed.substring("return ".length());
			if (processed.endsWith(";")) {
				processed = processed.substring(0, processed.length() - 1);
			}
		}
		
		// Replace parameters with arguments
		for (int i = 0; i < parameters.size() && i < arguments.size(); i++) {
			String paramName = parameters.get(i).getNameAsString();
			String argValue = arguments.get(i).toString();
			processed = processed.replaceAll("\\b" + paramName + "\\b", argValue);
		}
		
		return processed;
	}

	//yet to be implemented
	private void replaceMethodCall(MethodCallExpr call, String replacement) {
		// This is a simplified replacement - in a real implementation,
		// need to handle this more carefully
		// actual replacement would be done by replacing the parent statement
	}

}
