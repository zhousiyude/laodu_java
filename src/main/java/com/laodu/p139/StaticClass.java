package com.laodu.p139;


/**
 * @Description
 * @Author zhou
 * @Date 2026/8/30
 */
/*静态方法局部变量表里面没有this变量
实例方法里面局部变量表有this变量
实例方法/变量被调用需要引用.实例方法/变量
        当在本类中,this.实例变量/方法,可以省略this .
        在实例方法中 可以通过引用.静态方法/变量去访问他们 ,但是实际上还是类名.静态方法/变量的方式去调用,null.静态方法也没事
静态方法/变量被调用需要类名.静态方法/变量
        当在本类中,类名.静态变量/方法的时候,可以省略类名.
        在静态方法中因为没有this 所以不能调用实例方法/变量


*/
public class StaticClass {

    public static void main(String[] args) {
        //想要调用实例方法/变量只能通过引用.实例方法/变量去访问,但是static没有隐含的this 所以不能调用实例方法/变量
        //当调用实例方法/变量的时候如果该方法/变量是在本类中 那么可以省略this.



        OtherClass.otherClassMethod();//调用静态方法/变量的时候是类名.静态方法/变量
        method();//当调用静态方法/变量的时候 如果该方法/变量是在本类中 那么可以省略类名.


        User user = new User();
        user.instanceMethod();
        user.staticMethod();//当调用静态方法/变量的时候 也可以引用.静态方法/变量调用 但是实际上还是通过类名.静态方法/变量的方式去调用,已经和对象无关了
        //当user=null的时候也可以成功访问staticMethod
        user = null;
        user.staticMethod();//这里不会报空指针异常只会在调用实例方法/变量的时候才会空指针异常


    }



    static void method(){

    }
}
class OtherClass{
    static void otherClassMethod(){

    }

}
