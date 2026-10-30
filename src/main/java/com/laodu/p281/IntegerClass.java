package com.laodu.p281;

/**
 * @Description
 * @Author zhou
 * @Date 2026/10/7
 */
public class IntegerClass {
    public static void main(String[] args) {
        //装箱
        Integer integer = new Integer(1);
        Integer integer_str = new Integer("1");
        //上面方法已经过时建议采用如下方法
        Integer integer1 = Integer.valueOf(1);
        Integer integer2 = Integer.valueOf("1");

        //拆箱
        int int_value = integer.intValue();
        int int_value_str = integer_str.intValue();
        //常用方法
        int max_value = Integer.MAX_VALUE;
        int min_value = Integer.MIN_VALUE;
        //String => int "1" (常用*) => 1
        Integer.parseInt("1");//结果为 int 1

        //int => String  1=>"1"
        Integer.toString(1);//结果为 String "1"
                 //string类也有类似的int转换为string的方法
        String.valueOf(1);


        //常见字段
        System.out.println("MAX_VALUE: " + Integer.MAX_VALUE);
        System.out.println("MIN_VALUE: " + Integer.MIN_VALUE);





    }
}
