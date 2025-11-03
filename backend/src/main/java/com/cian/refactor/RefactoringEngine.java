package com.cian.refactor;

import com.google.gson.Gson;

public class RefactoringEngine {
	private static final Gson gson = new Gson();
	Refactored result = new Refactored();

	public String applyRefactor(String command, String source){
		switch (command) {
			case("generate_getters_setters"):
				result.originalSource= source;
				//TODO: implement the refactor logic
				//result.refactoredSource = gson.toJson(source);
			default:
				result.originalSource= source;
				//TODO: implement the refactor logic
				//result.refactoredSource = gson.toJson(source);
				String json = gson.toJson(result);
				System.out.println(json);
			break;
		}
		 return "";
	}
}
