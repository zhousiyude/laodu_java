package com.laodu.p284;

/**
 * @Description
 * @Author zhou
 * @Date 2026/10/7
 */
public class AutoPackageClass {
    public static void main(String[] args) {

        //自动装箱,实际底层做的是Integer integer = Integer.valueOf(1);
        Integer integer = 1;
        //自动拆箱,实际底层做的是int int_value = integer.intValue();
        int int_value = integer;
        System.out.println("int_value: " + int_value);
        //当
        Integer Integer_null = null;
//        System.out.println(Integer_null + 1);//这里会报错,Cannot invoke "java.lang.Integer.intValue()",说明底层是调用了intValue()方法
      /*  Exception in thread "main"
           java.lang.NullPointerException:
            Cannot invoke "java.lang.Integer.intValue()"
             because "Integer_null" is null
        at com.laodu.p284.AutoPackageClass.main(AutoPackageClass.java:18)
      */
        //Integer 类初始化时，IntegerCache 默认缓存 -128 到 127，共 256 个 Integer 对象。
        //当创建Integer对象时,如果范围在-128~127之间,会从缓存中取,否则会创建新的对象
        // 我们把存放256个Integer对象的空间，叫做整数型常量池。
        //只要以后程序中使用这个范围中的数据，直接从整数型常量池中获取，效率较高。
        Integer integer1 = 1;
        Integer integer2 = 1;
        System.out.println("integer1 == integer2: " + (integer1 == integer2));//true
        Integer integer3=1000;
        Integer integer4=1000;
        System.out.println("integer3 == integer4: " + (integer3 == integer4));//false
    }
}
