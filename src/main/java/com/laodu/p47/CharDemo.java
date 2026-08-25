package com.laodu.p47;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/25
 */
public class CharDemo {
    public static void main(String[] args) {
//char short byte 可以赋值没有超过范围的整数型字面量
        char c1 = 'a'+1;
        System.out.println(c1);//这里输出的是b 因为c1变量类型是char
        //但是
        System.out.println('a'+1);//这里输出的是98 因为该表达式的结果最终类型位int
    }
}
