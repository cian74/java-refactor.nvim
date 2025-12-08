package com.cian.refactor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Runner {
	private static final Gson gson = new GsonBuilder()
		.disableHtmlEscaping()
		.create();

	public static void main( String[] args )
	{
		RefactoringEngine engine = new RefactoringEngine();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while(true){
			try {
				String line = reader.readLine();

				if(line == null)break;

				Request request = gson.fromJson(line, Request.class);

				String response = engine.applyRefactor(request.command, request);

				String escaped = response.replace("\n", "\\n").replace("\r", "\\r");

				//Sends response out to be applied by frontend
				System.out.println(escaped);
				System.out.flush();


			} catch (Exception e) {
				System.err.println("Error:" + e.getMessage());
			}
		}
	}
}
