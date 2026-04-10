package org.hunspell;

/**
 * Java port of {@code replentry} ({@code htypes.hxx}).
 *
 * <p>Mirrors the C++ shape of a REP-table row as parsed in
 * {@code HashMgr::parse_reptable}: a literal pattern plus up to four
 * position-dependent replacement variants indexed by the context type:</p>
 * <ul>
 *   <li>{@code outstrings[0]} – middle (unanchored)</li>
 *   <li>{@code outstrings[1]} – start-anchored ({@code ^pattern})</li>
 *   <li>{@code outstrings[2]} – end-anchored ({@code pattern$})</li>
 *   <li>{@code outstrings[3]} – whole-word ({@code ^pattern$})</li>
 * </ul>
 *
 * <p>The C++ {@code RepList::replace} fallback chain is replayed in
 * {@link SuggestManager#replchars}: if the variant slot for the detected
 * context is empty, fall back through less-specific slots.</p>
 */
final class RepEntry {

    private final String pattern;
    private final String[] outstrings = new String[4];

    RepEntry(String pattern) {
        this.pattern = pattern;
    }

    String pattern() {
        return pattern;
    }

    void setReplacement(int type, String replacement) {
        outstrings[type] = replacement;
    }

    String replacement(int type) {
        return outstrings[type];
    }
}
