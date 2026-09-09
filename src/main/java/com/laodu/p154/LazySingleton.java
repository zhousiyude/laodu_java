package com.laodu.p154;

/**
 * @Description
 * @Author zhou
 * @Date 2026/8/31
 */
public class LazySingleton {
    private static LazySingleton instance;//3.设置instance为static
    private LazySingleton(){}//1.不允许外部随意new对象 控制到getInstance方法中获取到对象
    public static LazySingleton getInstance(){
        if (instance == null){
            instance = new LazySingleton();
        }
        return instance;
    }//2.由于空参构造方法私有化，所以外部无法通过new关键字创建对象,必须要用static,外部能调用static方法,又因为static只能访问static,所以instance必须是static的
}
