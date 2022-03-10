1. Feature Delivery
    * install time delivery, 通过下载下来的split apk中的manifest文件进行判断。
    * conditional delivery, 因为该功能依附于install time delivery, 所以也可以通过下载下来的split apk中的manifest文件进行判断
    * on-demand delivery, 通过代码中是否调用了指定api进行判断
2. Asset Delivery
    * install-time, 通过下载时的split apk中的manifest文件判断
    * fast-follow, 通过安装app,检测指定目录进行判断
    * on-demand，通过调用指定的API进行判断