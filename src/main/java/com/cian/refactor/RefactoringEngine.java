package com.cian.refactor;

import com.google.gson.Gson;

public class RefactoringEngine {
	private static final Gson gson = new Gson();
	Refactored result = new Refactored();
	String refactoredSource;

	public String applyRefactor(String command, String source){
		switch (command) {
			case("generate_getters_setters"):
				result.source = source;
				//TODO: implement the refactor logic
				//result.refactor = gson.toJson(source);
			default:
				result.source = source;
				//TODO: implement the refactor logic
				//result.refactor = gson.toJson(source);
				String json = gson.toJson(result);
				System.out.println(json);
			break;
		}
		 return "";
	}
}
