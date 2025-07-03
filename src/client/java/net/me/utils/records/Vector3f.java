package net.me.utils.records;

@SuppressWarnings("unused")
public record Vector3f(float x, float y, float z) {

    public Vector3f add(Vector3f other) {
        return new Vector3f(x + other.x, y + other.y, z + other.z);
    }

    public Vector3f add(float dx, float dy, float dz) {
        return new Vector3f(x + dx, y + dy, z + dz);
    }

    public float distanceTo(Vector3f other) {
        return (float) Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    public Vector3f multiply(float factor) {
        return new Vector3f(x * factor, y * factor, z * factor);
    }

    public Vector3f normalize() {
        float length = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        return new Vector3f(x / length, y / length, z / length);
    }

}
