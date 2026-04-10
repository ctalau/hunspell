package org.hunspell;

import java.util.List;

/**
 * Java port of {@code mapentry} ({@code htypes.hxx}).
 *
 * <p>A MAP entry is an equivalence class of characters (or multi-character
 * groups wrapped in parentheses, e.g. {@code MAP ß(ss)}) that
 * {@code SuggestMgr::map_related} substitutes freely during the
 * {@code mapchars} suggestion stage.</p>
 */
final class MapEntry {

    private final List<String> units;

    MapEntry(List<String> units) {
        this.units = List.copyOf(units);
    }

    List<String> units() {
        return units;
    }
}
