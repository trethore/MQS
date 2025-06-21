package net.me.utils;

import oshi.util.tuples.Pair;

public class IntPair {
    private final Pair<Integer, Integer> pair;

    public IntPair(int x, int y) {
        this.pair = new Pair<>(x, y);
    }

    public int getX() {
        return pair.getA();
    }

    public int getY() {
        return pair.getB();
    }
}
