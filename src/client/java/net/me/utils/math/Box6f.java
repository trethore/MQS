package net.me.utils.math;

@SuppressWarnings("unused")
public record Box6f(
        float x1, float y1, float z1,
        float x2, float y2, float z2
) implements Boxable<Box6f>, Cloneable {

    @Override
    public Box6f clone() {
        try {
            return (Box6f) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Box6f translate(double dx, double dy, double dz) {
        return new Box6f(
                (float) (x1 + dx), (float) (y1 + dy), (float) (z1 + dz),
                (float) (x2 + dx), (float) (y2 + dy), (float) (z2 + dz)
        );
    }

    @Override
    public Box6f mirror() {
        return new Box6f(
                -x2, -y2, -z2,
                -x1, -y1, -z1
        );
    }

    @Override
    public Box6f add(double ox1, double oy1, double oz1, double ox2, double oy2, double oz2) {
        return new Box6f(
                (float) (x1 + ox1), (float) (y1 + oy1), (float) (z1 + oz1),
                (float) (x2 + ox2), (float) (y2 + oy2), (float) (z2 + oz2)
        );
    }

    @Override
    public Box6f add(Box6f other) {
        return add(
                other.x1(), other.y1(), other.z1(),
                other.x2(), other.y2(), other.z2()
        );
    }
}