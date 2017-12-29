# kurento-jfinal

该项目为使用JFinal与KMS通信的Demo。目前KMS安装于自己的虚拟机中，想使用该项目，请先自行安装KMS。

其中的WebSocket是基于https协议进行通信的，如需使用，请先进行一些https代理的配置。
如果不想进行https代理的配置，请对相应页面的`js文件`中的构建WebSocket对象的协议由`wss`更改为`ws`。

注：该项目中绝大部分代码为[kurento-tutorial-java](https://github.com/Kurento/kurento-tutorial-java)
下项目中的代码，只是将其搭配的Spring Boot框架换成了JFinal框架。

----

好吧，其实JFinal也就是用到了一个路由跳转的功能。因此还是直接把这项目当做去除了Spring Boot框架后的
Kurento示例吧。

----

添加了一个屏幕共享和摄像头图像一起广播发送的示例页面，使用该示例需要先在谷歌浏览器中安装`Screen Capturing`
插件。

----