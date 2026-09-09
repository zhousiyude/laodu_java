package com.laodu.p154;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/31
 */
public class HungrySingleton {
    private static HungrySingleton instance =new HungrySingleton();
    public static HungrySingleton getInstance(){
        return instance;
    }
    private HungrySingleton(){}
}
