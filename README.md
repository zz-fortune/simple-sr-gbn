####简述


    这是用java编写的，基于UDP实现的一个关于`GBN`协议和`SR`协议的小例子。支持数据的双向传输，但是并不支持用一个端口和几个客户端进行通信。

    整个例子的框架是模仿基于`TCP`连接的`Socket`设计的，以`SR`为例，整个实现封装成一个`UdpSr`的类，这个类提供`send`和`receive`两个方法，可以将数据发送给对方或者读取对方发送的数据。

    在代码中包含两个使用的小例子。
