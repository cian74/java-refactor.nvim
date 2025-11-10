package com.cian.refactor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.Gson;

public class Runner {
	private static final Gson gson = new Gson();

	public static void main( String[] args )
	{
		RefactoringEngine engine = new RefactoringEngine();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		String source = "public class Foo {	String bar; String foo; }";

		while(true){
			try {
				String line = reader.readLine();

				if(line == null)break;

				Request request = gson.fromJson(line, Request.class);

				String response = engine.applyRefactor(request.command, request.source);

				//Sends response out to be applied by frontend
				System.out.println(response);
				System.out.flush();


			} catch (Exception e) {

			}
		}
	}
}
