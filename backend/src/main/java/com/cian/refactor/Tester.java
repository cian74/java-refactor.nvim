package com.cian.refactor;

public class Tester implements IMyInterfaces {

    String namer = "Bob";

    String name2;

    private String richard;

    private String major;

    private int age;

    int x = 0;

    int y = 1;

    public void calculate() {
        int x = 0;
        int y = extractedMethod2();
        int z = (x + y) * (x + y);
    }

    public void stringOp() {
        String lowerName = namer.toLowerCase();
        if (lowerName.equals("Bob")) {
            System.out.println("Name is bob");
        }
    }

    public String getNamer() {
        return namer;
    }

    public void setNamer(String name) {
        this.namer = name;
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

    public String getRichard() {
        return richard;
    }

    public void setRichard(String richard) {
        this.richard = richard;
    }
}

