Google桌面 与 GoogleNow的滑动联动效果, 简称负一屏

0.实现原理
  a)桌面(Client端) bindService. GoogleNow(Server端)
  b)桌面滑动时, 向Server端传递滑动进度(0%-100%)
  c)Server端响应桌面的滑动进度,将server的window(负一屏)滑出来.
  d)
Refer:
https://github.com/patriksletmo/launcherclient
https://github.com/FabianTerhorst/DrawerOverlayService
https://github.com/lobellomatteo/Pixel-Launcher3/