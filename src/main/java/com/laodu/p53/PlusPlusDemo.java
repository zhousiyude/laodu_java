package com.laodu.p53;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/25
 */
public class PlusPlusDemo {
    public static void main(String[] args) {

        int a=10;
        int b=20;

        int c=++a;
        int d=b++;

        System.out.println(c); // Output: 11
        System.out.println(d); // Output: 20
        //int c=++a;是先++后赋值  因为int c=(++a);  从左往右依次阅读 是++a的计算结果整体给c
        //int d=b++;是先赋值后++  因为int d=b    ++;   从左往右依次阅读 是b的值给d 然后b++


//        注意看这行代码
        int y=100;
        System.out.println(y++);//输出结果为100!!!!假如说sout第一个形参名字叫作z  实际上为z=y++    故z的值为100
    }

}
