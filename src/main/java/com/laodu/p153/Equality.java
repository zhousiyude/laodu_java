package com.laodu.p153;

import com.laodu.p150.User;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/31
 */
public class Equality {
    public static void main(String[] args) {
        int a=10;
        int b=20;
        System.out.println(a==b);//判断a中存储的值10和b中存储的值20是否相等
        User user1 = new User();
        User user2 = new User();
        System.out.println(user1==user2);//判断user1中存储的地址0x1231和user2中存储的地址0x1232是否相等，即是否指向同一个对象
    }
}
