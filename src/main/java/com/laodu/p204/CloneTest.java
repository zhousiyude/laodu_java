package com.laodu.p204;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/20
 */
/*
浅克隆
clone的方法签名是protected native Object clone() throws CloneNotSupportedException;
这是一个native方法，通过jni技术,调用的是C++的实现
 clone的方法作用是将对象进行复制返回一个新对象该对象类型实际是调用者类型
clone需要实现Cloneable接口
接口分为两种一种是标记性接口(里面没有方法) 一种是普通接口
*/

/*
    问题：User 没有重写 clone() 时，CloneTest 中调用 user.clone() 会编译报错

    原因：
    1. clone() 定义在 Object 类中，修饰符是 protected
    2. User 继承了 clone()，但字节码中并没有这个方法，运行时通过虚方法表查找 Object 的实现
    3. protected 的访问检查看的是"方法声明类（Object）所在的包"，而不是"引用类型（User）所在的包"
    4. Object 在 java.lang 包，CloneTest 在 com.laodu.p204 包，不同包且 CloneTest 不是 Object 的子类
       → 不符合 protected 的访问范围，编译报错

    解决方案：
    在 User 类中重写 clone()，并将修饰符改为 public，
    此时 clone() 的声明类变为 User，CloneTest 与 User 同包，可以正常访问
*/



public class CloneTest {
    public static void main(String[] args) {
        User user = new User();
        user.setName("zhou");
        user.setAge(18);
        user.setAddress("beijing");
        User user2 = (User) user.clone();
        //user.clone()调用是一个clone操作 修饰符是protected
        System.out.println(user2.getName());
        System.out.println(user2.getAge());
        System.out.println(user2.getAddress());


    }
}
