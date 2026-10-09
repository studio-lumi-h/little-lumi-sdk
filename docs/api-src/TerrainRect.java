package com.group_finity.mascot.lumi.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * A rectangular terrain piece in desktop logical coordinates, like {@link MascotState#anchor()}.
 * The top edge is a floor; other edges follow the engine's window-border behaviour.
 * This does not provide solid-body collision, slope walking or automatic jumping.
 * Keep {@code id} unchanged when moving a piece so a character's floor reference follows it.
 * Coordinates are edges, not width/height; negative desktop positions are allowed.
 */
public record TerrainRect(String id, int left, int top, int right, int bottom) {
    /** Maximum number of pieces in one terrain registration. */
    public static final int MAX_PIECES = 2048;

    public TerrainRect {
        requireId(id);
        if (right <= left || bottom <= top
                || (long) right - left > Integer.MAX_VALUE
                || (long) bottom - top > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("terrain rectangle must have positive, int-sized dimensions");
        }
    }

    static void requireId(String id) {
        if (id == null || id.isBlank() || id.length() > 128) {
            throw new IllegalArgumentException("terrain id must contain 1..128 characters and not be blank");
        }
    }

    static List<TerrainRect> snapshot(List<TerrainRect> rects) {
        Objects.requireNonNull(rects, "rects");
        List<TerrainRect> copy = List.copyOf(rects);
        if (copy.size() > MAX_PIECES) {
            throw new IllegalArgumentException("too many terrain pieces (maximum " + MAX_PIECES + ")");
        }
        var ids = new HashSet<String>();
        for (TerrainRect rect : copy) {
            if (!ids.add(rect.id())) {
                throw new IllegalArgumentException("duplicate terrain piece: " + rect.id());
            }
        }
        return copy;
    }
}
