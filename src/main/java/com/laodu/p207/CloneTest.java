package com.laodu.p207;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/22
 */
public class CloneTest {
    public static void main(String[] args) {
        //浅克隆的问题 object.clone这个方法只会复制值
        //也就是说对于基本数据类型复制的是值 引用数据类型复制的是地址
//        TotalPrice totalPrice = new TotalPrice(1000);
//        Order order = new Order(totalPrice);
        //假如说order的totalPrice属性地址是0x1000
        //那么order2的totalPrice属性地址也是0x1000
        //也就是说order和order2的totalPrice属性地址是相同的 指向同一个对象
        //所以如果修改了order的totalPrice属性值，order2的totalPrice属性值也会改变
//        Order order2 = (Order) order.clone();
//
//        System.out.println("两个对象的totalPrice属性地址是否相同："+(order.getTotalPrice().hashCode()==order2.getTotalPrice().hashCode()));
//        System.out.println("order的totalPrice对象地址哈希码为："+order.getTotalPrice().hashCode());
//        System.out.println("order2的totalPrice对象地址哈希码为："+order2.getTotalPrice().hashCode());
//
//
//        System.out.println("order totalPrice:"+order.getTotalPrice().getPrice());
//        System.out.println("order2 totalPrice:"+order2.getTotalPrice().getPrice());
//        order2.getTotalPrice().setPrice(2000);
//        //当修改order2的totalPrice属性值时，order的totalPrice属性值也会改变
//        System.out.println("将order2的totalPrice属性值修改为2000 观察order的totalPrice属性值是否改变");
//        System.out.println("order totalPrice:"+order.getTotalPrice().getPrice());
//        System.out.println("order2 totalPrice:"+order2.getTotalPrice().getPrice());

//在这之前是p205的浅克隆代码
//============================
//演示深克隆 详情请见Order类
        TotalPrice totalPrice = new TotalPrice(1000);
        Order order = new Order(totalPrice);
        //这里的clone方法已经被改造改为了深克隆
        Order order2 = (Order) order.clone();

        System.out.println("两个对象的totalPrice属性地址是否相同："+(order.getTotalPrice()==order2.getTotalPrice()));
        System.out.println("order的totalPrice对象地址哈希码为："+order.getTotalPrice().hashCode());
        System.out.println("order2的totalPrice对象地址哈希码为："+order2.getTotalPrice().hashCode());


        System.out.println("order totalPrice:"+order.getTotalPrice().getPrice());
        System.out.println("order2 totalPrice:"+order2.getTotalPrice().getPrice());
        order2.getTotalPrice().setPrice(2000);
        //当修改order2的totalPrice属性值时，order的totalPrice属性值也会改变
        System.out.println("将order2的totalPrice属性值修改为2000 观察order的totalPrice属性值是否改变");
        System.out.println("order totalPrice:"+order.getTotalPrice().getPrice());
        System.out.println("order2 totalPrice:"+order2.getTotalPrice().getPrice());

    }
}
