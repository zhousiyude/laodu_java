package com.laodu.p334;

import java.util.Arrays;

/**
 * @Description
 * @Author zhou
 * @Date 2026/10/23
 */
public class TestPerson {
    public static void main(String[] args) {

        Person[] persons={new Person("Alice", 30), new Person("Bob", 25), new Person("Charlie", 35),new Person("Charlie", 20), new Person("David", 30), new Person("David", 10)};
        Arrays.sort(persons);
        System.out.println(Arrays.toString(persons));
    }
}
