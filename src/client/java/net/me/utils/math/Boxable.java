package net.me.utils.math;

@SuppressWarnings("unused")
public interface Boxable<T> extends Cloneable {
    T clone();

    T translate(double dx, double dy, double dz);

    T mirror();

    T add(double ox1, double oy1, double oz1, double ox2, double oy2, double oz2);

    T add(T other);
}