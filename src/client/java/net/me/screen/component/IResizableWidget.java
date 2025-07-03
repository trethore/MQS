package net.me.screen.component;

@SuppressWarnings("unused")
public interface IResizableWidget {
    void setPos(int x, int y);

    void setSize(int width, int height);

    default void dimensions(int x, int y, int width, int height) {
        setPos(x, y);
        setSize(width, height);
    }
}
