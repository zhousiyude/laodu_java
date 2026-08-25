package com.laodu.p43;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/25
 */
public class CharClass {
    //在char中不能使用' '来表示空字符 只能使用'\u0000',\ u后面是一个四位十六进制数  表示一个字符的unicode码值
    public static void main(String[] args) {
        //char有如下几种定义方式
        char a='中';// 可以复制一个字符形字面量
        char b=97;// 可以使用整数型字面量
        char c = '\u0000'; //可以使用unicode码值 代表一个空格字符
        System.out.println(c);
    }
}
