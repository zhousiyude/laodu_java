package com.laodu.p204;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/20
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
public class User implements Cloneable {
    private String name;
    private int age;
    private String address;
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
