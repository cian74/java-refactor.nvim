package com.cian.refactor;

public class Test {

    String name = "Bob";

    private String name2;

    private String major;

    private int age;

    int x = 0;

    int y = 1;

    public void calculate() {
        int x = 0;
        int y = 1 + (x + 7);
        int z = extractedMethod();
    }

    public void stringOp() {
        String lowerName = name.toLowerCase();
        if (lowerName.equals("Bob")) {
            System.out.println("Name is bob");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    private int extractedMethod() {
        return (x + y) * (x + y);
    }

    public String toString() {
        return "Test{" + "name=" + String.valueOf(name) + ", " + "name2=" + String.valueOf(name2) + ", " + "major=" + String.valueOf(major) + ", " + "age=" + String.valueOf(age) + "}";
    }
}

