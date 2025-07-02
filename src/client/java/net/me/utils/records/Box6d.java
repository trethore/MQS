package net.me.utils.records;

@SuppressWarnings("unused")
public record Box6d(
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2
) implements Boxable<Box6d>, Cloneable {

    @Override
    public Box6d clone() {
        try {
            return (Box6d) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Box6d translate(double dx, double dy, double dz) {
        return new Box6d(
                x1 + dx, y1 + dy, z1 + dz,
                x2 + dx, y2 + dy, z2 + dz
        );
    }

    @Override
    public Box6d mirror() {
        return new Box6d(
                -x2, -y2, -z2,
                -x1, -y1, -z1
        );
    }

    @Override
    public Box6d add(
            double ox1, double oy1, double oz1,
            double ox2, double oy2, double oz2
    ) {
        return new Box6d(
                x1 + ox1, y1 + oy1, z1 + oz1,
                x2 + ox2, y2 + oy2, z2 + oz2
        );
    }

    @Override
    public Box6d add(Box6d other) {
        return add(
                other.x1(), other.y1(), other.z1(),
                other.x2(), other.y2(), other.z2()
        );
    }
}
