package com.laodu.p68;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/26
 */
public class ExtendsClass {
    public static void main(String[] args) {
        byte b=127;
        byte b=b+1;//这里编译器会报错因为b+1的计算结果是int类型应当强制类型转换
        //但是
        byte b+=1;//这里这个符号会直接进行强转也就是byte b=(byte)(b+1);最后结果依旧是byte
        System.out.println();
    }
}
