package com.cian.refactor;

public class Runner {
	public static void main( String[] args )
	{
		RefactoringEngine re = new RefactoringEngine();

		String source = "public class Tester { String word; }";

		re.applyRefactor("", source);

	}
}
