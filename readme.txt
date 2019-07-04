Google桌面 与 GoogleNow的滑动联动效果, 简称负一屏

0.实现原理
  a)桌面(Client端) bindService. GoogleNow(Server端)
  b)桌面滑动时, 向Server端传递滑动进度(0%-100%)
  c)Server端响应桌面的滑动进度,将server的window(负一屏)滑出来.
  d)SlidePanelLayout 滑动处理. 通过修改子View的translationX来实现与client端的平滑过渡.
  e)其他注意点:
    window的token跟type尤为重要,且 token/type具有关联/匹配性.
    这里token使用的是 桌面Activity的token, type 为 TYPE_DRAWN_APPLICATION/TYPE_APPLICATION

Refer:
https://github.com/patriksletmo/launcherclient
https://github.com/FabianTerhorst/DrawerOverlayService
https://github.com/lobellomatteo/Pixel-Launcher3/