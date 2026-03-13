package com.cian.refactor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;

public class Runner {

    private static final Gson gson = new GsonBuilder().create();

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                Request request = gson.fromJson(line, Request.class);
                
                RefactoringEngine engine = new RefactoringEngine();
                String jsonResult = engine.applyRefactor(request.command, request);
                
                System.out.println(jsonResult);
                System.out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
