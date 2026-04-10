package org.hunspell;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Lightweight Java port of {@code HashMgr} ({@code hashmgr.cxx}).
 *
 * <p>The C++ class loads a {@code .dic} file, decodes the per-word flag
 * vector according to the active {@link FlagMode}, and stores entries in a
 * hash table keyed by the stem (preserving homonyms via the
 * {@code next_homonym} chain).</p>
 *
 * <p>This Java implementation keeps the same surface area: parse a dictionary
 * once with a known flag mode, then expose homonym lookups by stem.</p>
 */
final class HashManager {

    /** Single dictionary entry – the {@code hentry} equivalent. */
    static final class Entry {
        private final String stem;
        private final int[] flags;

        Entry(String stem, int[] flags) {
            this.stem = stem;
            this.flags = flags;
        }

        String stem() {
            return stem;
        }

        int[] flags() {
            return flags;
        }

        boolean hasFlag(int flag) {
            return Flags.contains(flags, flag);
        }
    }

    private final FlagMode flagMode;
    private final Map<String, List<Entry>> entries = new HashMap<>();

    HashManager(FlagMode flagMode) {
        this.flagMode = flagMode;
    }

    int load(Path dicPath, Charset charset) {
        return load(dicPath, charset, UnaryOperator.identity());
    }

    /**
     * Load a dictionary file while applying {@code normalizer} to each stem
     * before indexing. The normalizer mirrors {@code AffixMgr} IGNORE handling
     * so stored stems live in the same "ignore-stripped" space as lookup input.
     */
    int load(Path dicPath, Charset charset, UnaryOperator<String> normalizer) {
        try {
            List<String> lines = Files.readAllLines(dicPath, charset);
            int start = 0;
            if (!lines.isEmpty() && lines.get(0).strip().matches("\\d+")) {
                start = 1;
            }
            int loaded = 0;
            for (int i = start; i < lines.size(); i++) {
                String line = stripComment(lines.get(i)).strip();
                if (line.isEmpty()) {
                    continue;
                }
                String stem = parseStem(line);
                if (stem.isEmpty()) {
                    continue;
                }
                String flagToken = parseFlagToken(line);
                int[] flags = Flags.decode(flagToken, flagMode);
                String normalized = normalizer.apply(stem);
                addEntry(normalized, flags);
                loaded++;
            }
            return loaded;
        } catch (IOException ex) {
            throw new HunspellIoException("Unable to read dictionary: " + dicPath, ex);
        }
    }

    void addEntry(String stem, int[] flags) {
        entries.computeIfAbsent(stem, ignored -> new ArrayList<>(1)).add(new Entry(stem, flags));
    }

    List<Entry> lookup(String stem) {
        List<Entry> list = entries.get(stem);
        return list == null ? Collections.emptyList() : list;
    }

    boolean contains(String stem) {
        return entries.containsKey(stem);
    }

    Iterable<Map.Entry<String, List<Entry>>> all() {
        return entries.entrySet();
    }

    int size() {
        return entries.size();
    }

    void clear() {
        entries.clear();
    }

    static Charset defaultCharset() {
        return StandardCharsets.ISO_8859_1;
    }

    private static String stripComment(String line) {
        // Hunspell treats lines beginning with '#' as comments. Inline comments are not stripped
        // here because real .dic entries may contain '#' inside aliases/morph fields handled later.
        if (line.startsWith("#")) {
            return "";
        }
        return line;
    }

    /**
     * Strip morphological fields off a dictionary line, matching the C++
     * {@code HashMgr::load_tables} detection: either a tab (the legacy morph
     * separator) or a whitespace-preceded three-character code followed by
     * {@code ':'} (e.g. {@code "a lot ph:alot"}).
     */
    private static String stripMorphFields(String line) {
        int tab = line.indexOf('\t');
        int dp = -1;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ':') {
                continue;
            }
            if (i >= 4) {
                char sep = line.charAt(i - 4);
                if (sep == ' ' || sep == '\t') {
                    int end = i - 4;
                    while (end > 0 && (line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\t')) {
                        end--;
                    }
                    dp = end;
                    break;
                }
            }
        }
        int cut = line.length();
        if (tab >= 0) {
            cut = Math.min(cut, tab);
        }
        if (dp >= 0) {
            cut = Math.min(cut, dp);
        }
        return line.substring(0, cut);
    }

    private static String parseStem(String line) {
        if ("/".equals(line)) {
            return "/";
        }
        // Stem terminators mirror C++ HashMgr::load_tables: morphological
        // field separator (tab or `[space]XX:`) and unescaped slash. Spaces
        // are preserved inside the stem so dictionary word pairs like
        // "a lot", "in spite", and "scot-free" load as single hash entries,
        // which is required for `twowords` dictionary-pair matching in the
        // suggestion pipeline.
        line = stripMorphFields(line);
        StringBuilder stem = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (escaped) {
                stem.append(current);
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '/') {
                break;
            }
            stem.append(current);
        }
        int end = stem.length();
        while (end > 0 && stem.charAt(end - 1) == ' ') {
            end--;
        }
        return stem.substring(0, end);
    }

    private static String parseFlagToken(String line) {
        line = stripMorphFields(line);
        int slashIndex = findFirstUnescapedSlash(line);
        if (slashIndex < 0 || slashIndex + 1 >= line.length()) {
            return "";
        }
        int end = slashIndex + 1;
        // Flag tokens run to end-of-line once morph fields have been stripped.
        while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
            end++;
        }
        return line.substring(slashIndex + 1, end);
    }

    private static int findFirstUnescapedSlash(String text) {
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '/') {
                return i;
            }
        }
        return -1;
    }
}
