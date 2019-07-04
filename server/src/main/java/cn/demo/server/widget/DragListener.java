package cn.demo.server.widget;

public interface DragListener {
    void overlayScrollChanged(float f);

    void drag();

    void dragTouchable();

    void open();

    void close();

    boolean cnI();

    void close(boolean z);
}
