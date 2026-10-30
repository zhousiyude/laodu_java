package com.laodu.p334;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description
 * @Author zhou
 * @Date 2026/10/23
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Person implements Comparable<Person>
{
    @Override
    public int compareTo(Person otherPerson) {
        int result= this.name.compareTo(otherPerson.name);
        if(result != 0) {
            return result;
        }
        result = otherPerson.age - this.age;
        return result;
    }
    String name;
    Integer age;
}
