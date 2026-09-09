package com.laodu.p39;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/24
 */
public class DemoClass {
    public static void main(String[] args) {

//        在多种变量类型混合运算的时候，各自自动提升为范围最大的类型进行计算，最终结果也是该类型。byte、short、char 参与运算时，会无条件先自动提升为 int
//        例如
int a=1;
short b=2;
long c= 3L;
long d=a+b+c;//这里应当用long接收最大的类型  a  b 都将被提升为int


//        当为字面量相加减的时候编译时会自动优化计算结果
        int g=10/2;//编译时会自动优化计算结果,在字节码当中,这里就是相当于 int g=5;
        //编译期常量优化不仅适用于字面量，也适用于 final 修饰的编译期常量变量；非常量变量参与运算时，编译器不会在编译期计算结果。
        int f =8;
        int h=f/4;//这里编译器不会进行优化 f没有被final修饰
String abc="abc"+"hello";//在编译时会自动优化计算结果,在字节码当中,这里就是相当于 String abc="abchello"; 在字符常量池当中.这句代码只会有一个字符常量即"abchello"
    }
}
