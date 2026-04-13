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
        int y = extractedMethod2();
        int z = squaringMethod();
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

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    private int extractedMethod2() {
        return 1 + (x + 7);
    }

    public String toString() {
        return "Test{" + "name2=" + String.valueOf(name2) + ", " + "major=" + String.valueOf(major) + ", " + "age=" + String.valueOf(age) + "}";
    }

    private int squaringMethod() {
        return (x + y) * (x + y);
    }
}

