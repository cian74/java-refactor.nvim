package com.cian.refactor;

public class Test {

    private String name;

    private int age;

    int x = 0;

    int y = 1;

    int z = x + y * (x + y);

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
}

