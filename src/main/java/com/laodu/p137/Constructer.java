package com.laodu.p137;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/30
 */
public class Constructer {
    public static void main(String[] args) {
        User user = new User();
    /*构造方法的创建分为两个阶段
    一个是 new 即创建对象阶段 会在堆中开辟一片空间 赋值上默认的值
    二是 调用构造方法阶段     在这之中也分别为两个阶段一个是调用代码块 一个是调用对应的构造方法



    关于代码块,代码块执行的时候还没有到构造方法的调用时刻 此时实例对象还是默认值 代码块一般不用


    如果没有代码快
       class User{
                    private String name
                        User(){
                        //多行相同代码
                        //其他代码
                        }
                        User(String name){
                        //多行相同代码
                        //其他代码
                        }
                 }





    在有了代码块以后就可以这样
         class User{
                    private String name
                    {
                    //多行相同代码
                    }
                        User(){
                        //其他代码

                        }
                        User(String name){
                      //其他代码
                        }
                 }




    */

    }
}
