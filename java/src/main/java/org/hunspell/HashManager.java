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
        private final List<String> morphology;

        Entry(String stem, int[] flags, List<String> morphology) {
            this.stem = stem;
            this.flags = flags;
            this.morphology = morphology;
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

        List<String> morphology() {
            return morphology;
        }
    }

    private final FlagMode flagMode;
    private final List<int[]> flagAliases;
    private final Map<String, List<Entry>> entries = new HashMap<>();

    HashManager(FlagMode flagMode) {
        this(flagMode, List.of());
    }

    HashManager(FlagMode flagMode, List<int[]> flagAliases) {
        this.flagMode = flagMode;
        this.flagAliases = new ArrayList<>(flagAliases);
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
                List<String> morphFields = parseMorphFields(line);
                int[] flags = decodeFlags(flagToken);
                String normalized = normalizer.apply(stem);
                addEntry(normalized, flags, morphFields);
                loaded++;
            }
            return loaded;
        } catch (IOException ex) {
            throw new HunspellIoException("Unable to read dictionary: " + dicPath, ex);
        }
    }

    void addEntry(String stem, int[] flags) {
        addEntry(stem, flags, List.of());
    }

    void addEntry(String stem, int[] flags, List<String> morphology) {
        entries.computeIfAbsent(stem, ignored -> new ArrayList<>(1))
            .add(new Entry(stem, flags, List.copyOf(morphology)));
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

    boolean removeEntry(String stem) {
        return entries.remove(stem) != null;
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

    private static String parseStem(String line) {
        if ("/".equals(line)) {
            return "/";
        }
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
            if (current == '/' || Character.isWhitespace(current)) {
                break;
            }
            stem.append(current);
        }
        return stem.toString();
    }

    private static String parseFlagToken(String line) {
        int slashIndex = findFirstUnescapedSlash(line);
        if (slashIndex < 0 || slashIndex + 1 >= line.length()) {
            return "";
        }
        int end = slashIndex + 1;
        while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
            end++;
        }
        return line.substring(slashIndex + 1, end);
    }

    private static List<String> parseMorphFields(String line) {
        int slashIndex = findFirstUnescapedSlash(line);
        int start = -1;
        if (slashIndex >= 0) {
            start = slashIndex + 1;
            while (start < line.length() && !Character.isWhitespace(line.charAt(start))) {
                start++;
            }
        } else {
            String stem = parseStem(line);
            start = stem.length();
        }
        if (start < 0 || start >= line.length()) {
            return List.of();
        }
        String tail = line.substring(start).strip();
        if (tail.isEmpty()) {
            return List.of();
        }
        return List.of(tail.split("\\s+"));
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

    private int[] decodeFlags(String token) {
        if (!flagAliases.isEmpty() && token.matches("\\d+")) {
            int index = Integer.parseInt(token);
            if (index >= 1 && index <= flagAliases.size()) {
                return flagAliases.get(index - 1);
            }
        }
        return Flags.decode(token, flagMode);
    }
}
