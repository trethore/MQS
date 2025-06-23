package net.me.utils;

/**
 * A simple, immutable record to store a 2D integer coordinate pair (x, y).
 * Replaces the custom IntPair class that depended on Oshi.
 */
public record Position(int x, int y) {
}