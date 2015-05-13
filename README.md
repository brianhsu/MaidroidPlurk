MaidroidPlurk 
===================

[中文](#user-content-maidroidplurk-女僕噗浪-app)

Introduction
--------------

MaidroidPlurk is a Plurk app for Android system, with a very cute maid called Lin chan.

This application is a free (open source) software, you may use / modify / redistribute it as your wish. You could find source code of this application on author's GitHub page. 

But if you like this application, you may donate to the author by using the donation button in [MENU KEY] -> About -> Donate tab.

Installation
------------

You may goto [Play Store to download latest verson of MaidroidPlurk][0].

Screenshots
-----------

 ![Screenshot](http://i.imgur.com/m3lUbeJl.png)
 ![Screenshot](http://i.imgur.com/2FOPEYNl.png)
 ![Screenshot](http://i.imgur.com/YkP1orll.png)
 ![Screenshot](http://i.imgur.com/SNf5lLTl.png)

Features
-------------

 - Read your timeline.
 - Read other user's public timeline.
 - Read other user's profile, including send friendship request and add as a
   fan.
 - Post / Reply
 - Mute / Favoriate / Replurk buttons
 - Switch to diffrent post categories.
 - Use custom emoticon.
 - Mark all as read.
 - Pull-down refresh.
 - Support bulit-in album app / Flickr / Picasa / Dropbox when upload photos.
 - Share to MaidroidPlurk directly in other apps.

Not Implemented Features
--------------------------

 - Modify your profile page.
 - Add new custom emoticon.

Build
------------

 0. Prepare Android API level 19 developing tools, including support / google play service repositories.
 1. Download and install [SBT][1] build system.
 2. Clone source code from GitHub.

    ```console
    git clone https://github.com/brianhsu/MaidroidPlurk.git
    ```

 3. Enter SBT build system command line interactive shell.

    ```console
    brianhsu@DellHome ~/WorkRoom/MaidroidPlurk $ sbt
    [info] Loading project definition from /home/brianhsu/WorkRoom/MaidroidPlurk/project
    [info] Set current project to MaidroidPlurk (in build file:/home/brianhsu/WorkRoom/MaidroidPlurk/)
    > 
    ```

 4. Use ``android:run`` inside SBT shell to compile and run on Android device / emulator.


License
-------------

This application is licensed under GPLv2 or any later version.

Copyright (C) 2014-2015 BrianHsu

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.


MaidroidPlurk 女僕噗浪 APP
============================

簡介
--------------

MaidroidPlurk 是一款以可愛為主的 Android 噗浪軟體，由可愛的女僕小鈴幫您服務！

MaidroidPlurk 是自由（開放源碼）軟體，您可以自由使用，也可以在作者的 GitHub 上找到本 APP 的原始碼並自由散佈及修改，但若您喜歡本程式，也可以到「MENU 鍵－＞關於－＞捐款」內捐款贊助作者。

安裝
------------

您可以到 [Play 上店上下載安裝最新版的 MaidroidPlurk][0] 來使用。

螢幕截圖
-----------

 ![Screenshot](http://i.imgur.com/0WUBOKgl.png)
 ![Screenshot](http://i.imgur.com/Ru9ZvsKl.png)
 ![Screenshot](http://i.imgur.com/sDa8uGRl.png)
 ![Screenshot](http://i.imgur.com/MLZFjZMl.png)


有的功能
---------------

 - 讀自己的河道
 - 看別人的河道
 - 看別人的個人資料（申請好友，加入粉絲）
 - 發噗 / 回噗
 - 消音 / 喜愛 / 轉噗
 - 切換各種不同的噗
 - 使用自訂表情符號
 - 全部標為已讀
 - 下拉更新
 - 選擇上傳圖片時支援相簿內的 Flickr / Picasa / Dropbox 相片
 - 在其他 APP 中直接分享至 MaidroidPlurk

沒有的功能
-----------

 - 修改個人檔案
 - 新增自訂表情符號

編譯建置
------------

 0. 準備 Android API level 19 的開發環境，包括 support / google play service 的 repositories.
 1. 下載 [SBT][0] 自動建製工具
 2. 從 GitHub 下載原始碼

    ```console
    git clone https://github.com/brianhsu/MaidroidPlurk.git
    ```

 3. 進入 SBT 命令列模式

    ```console
    brianhsu@DellHome ~/WorkRoom/MaidroidPlurk $ sbt
    [info] Loading project definition from /home/brianhsu/WorkRoom/MaidroidPlurk/project
    [info] Set current project to MaidroidPlurk (in build file:/home/brianhsu/WorkRoom/MaidroidPlurk/)
    > 
    ```

 4. 在 SBT 命令列模式中執行 ``android:run`` 指令，編譯並安裝至 Android 裝置、模擬器上。



[0]: https://play.google.com/store/apps/details?id=idv.brianhsu.maidroid.plurk
[1]: http://www.scala-sbt.org/ 
