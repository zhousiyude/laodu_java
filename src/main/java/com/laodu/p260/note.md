编译器在调用方法的时候会检测方法签名上面有没有    throws  编译时异常(必须在编译的时候处理,编译时异常=throwable的子类中 排除Error以及子类,RuntimeException以及子类)
注意throws 非编译时异常不会报编译错误
报编译时错误同时满足throws 和 编译时错误 或者   手动throw new 编译时错误

<img src="./img.png"/>
