# flashbot

## 来源
*   flashbot最早的想法来源于"闪刷机器人"（可淘宝搜索）,这类硬件在淘宝售价多在1000以上。用于给安卓手机快速刷入apk，常用于app推广以及预装。
*   对于批量安装而已，这种8口，16口的设备更具效率，但如果是零散的共享就太大材小用也不方便。
*   市面上有部分软件带有分享功能，例如快牙，豌豆荚等，但通常都是需要两部手机都装有快牙才能进行共享，如果目标机器上没装这个软件怎么共享过去呢，这是很典型的鸡生蛋，蛋生鸡的问题。

## 解决方案
*   在开发的时候，我们一般通过adb install进行apk安装。而android系统本身基于linux，那我们能不能通过android手机来为另一台手机进行adb install呢？
*   otg允许我们的android手机充当PC一样的host，从而可以连接其他设备(如键盘，鼠标，优盘)，当然也可以是连接其他android手机，目前大多数手机都支持该技术。
*   方案

	以下需要用到两台android手机，一台为宿主机H，一台为目标机D，要求宿主机H支持OTG。
   *  不完美的方案
       *  在android 4.0以后的系统，系统已经携带了adb命令，那么能直接通过该命令`adb install`么？ 
       *  实地使用otg线连接两台手机后，然后在命令行里执行`adb devices`,发现结果里并没有目标机器，究其原因实际上是`adb devices`在底层的代码里实际上扫描底层的设备句柄，而普通的用户没有权限访问这些内容，除非你以root身份运行(要求设备进行root)。
       *  将宿主机进行root，可以运行`adb install`将宿主机H的apk安装到目标机D上。
       *  目前在移动/联通营业厅里的部分刷机厂商就是使用这个方案的，他们一般手持一个支持otg的android pad，这个设备都是已经root过的，采用上述方案给你的手机来安装相应运营商的助手或其他然间。
       *  缺点: 要求**root**，这个是个硬伤，root对普通人既复杂也有安全隐患。
   *  	完美方案
   		*    android的api实际上提供了底层设备的api，如果是通过相应api进行调用，系统是允许的，最多会自动弹出一个授权框，确认一下即可。
   		*    那么目标就很清楚了，用android支持的api重写adb，这样就可以运行`adb install`而没有权限问题。部分的底层工作可以在github中已有实现，参见[cgutman的adblib](https://github.com/cgutman/AdbLib/tree/master/src/com/cgutman/adblib)。
   		*    对上述的代码进行修改，在cgutman的adblib中，数据都是通过Socket进行交互的，将其抽象为AdbChannel，并将原来的Socket实现修改为TcpChannel，并编写UsbChannel(其内部使用UsbDeviceConnection，UsbEndpoint，UsbInterface等标准api)。
   		*    然后参照adb的数据格式，实现`adb push`和`adb install`命令。
   		*    最后套个app外壳，整个工作就完成了。
   		*    工作流程:
     		*    目标机D上打开adb选项(国内用户用过电脑助手的基本都会，请自行搜索)
     		*    宿主机H和目标机D通过OTG线连接
     		*    目标机D上可能会弹出提示，大意为是否信任宿主机H等等，确认。(这个过程同电脑连接android手机一模一样)。
     		*    宿主机H可以选择自己机子上
     		  *    已安装的apk(apk安装后如果不做特意清理的话，原始的apk事实上也是保留在系统目录的某个地方)，
     		  *    sdcard上的其他apk文件
     		  
     		  安装到目标机D上了(实际上就是调用改写过`adb push`和`adb install`)，大功告成。
      * [视频效果在此](http://v.youku.com/v_show/id_XNjg3MzAxOTQ4.html?from=s1.8-1-1.2)
      * 优点: 仅要求支持OTG,不需要root。一根5块钱的otg线就可以解决商贩零散刷机或偶尔共享的需求，物超所值。
