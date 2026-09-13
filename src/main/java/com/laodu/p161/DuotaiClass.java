package com.laodu.p161;

/**
 * @Description
 * @Author zhou
 * @Date 2026/9/11
 */
public class DuotaiClass {
    public static void main(String[] args) {
        Animal c =new Cat();
        c.move();
        //在编译的时候java默认看左边类型为Animal 由于Animal里面有这个move方法编译通过
        c.moajiao();//由于编译时c的类型为Animal，由于animal里面没有这个maojiao方法所以报错
        //要想使用maojiao方法c必须向下转型变为Cat类型，才可以使用特有的maojiao方法
//详情请看下面

        //instanceof 关键字 判断c中指向的堆对象是不是Cat类型 如果是那么就会返回boolean类型的true 反之则为 false
        if(c instanceof Cat){

            Cat cat=(Cat) c;
            cat.maojiao();

        }
        //instanceof新特性
        if(c instanceof Cat cat){
            cat.maojiao();
        }
    }

}
