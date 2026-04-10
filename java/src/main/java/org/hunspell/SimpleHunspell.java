package org.hunspell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Façade implementation of {@link Hunspell} that wires together the manager
 * classes mirroring the C++ Hunspell architecture:
 * <ul>
 *   <li>{@link AffixManager} ↔ {@code AffixMgr}/{@code affentry.cxx}</li>
 *   <li>{@link HashManager} ↔ {@code HashMgr}</li>
 *   <li>This class plays the role of {@code Hunspell}/{@code HunspellImpl} in
 *       {@code hunspell.cxx}, exposing the user-facing spell/check/suggest
 *       surface required by the v1 Java API.</li>
 * </ul>
 */
final class SimpleHunspell implements Hunspell {

    private final AffixManager affixManager;
    private final HashManager hashManager;
    private final int maxSuggestions;
    private final SuggestManager suggestManager;

    private SimpleHunspell(AffixManager affixManager, HashManager hashManager, int maxSuggestions) {
        this.affixManager = affixManager;
        this.hashManager = hashManager;
        this.maxSuggestions = maxSuggestions;
        this.suggestManager = new SuggestManager(affixManager, this::checkSuggestion, maxSuggestions);
    }

    /**
     * Acts as the {@code SuggestMgr::checkword} gate: a candidate is only a
     * valid suggestion if it spell-checks and does not match any entry that
     * is FORBIDDENWORD or NOSUGGEST. This mirrors how the C++ suggestion
     * pipeline filters its own output.
     */
    private boolean checkSuggestion(String candidate) {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        String normalized = affixManager.normalizeWord(candidate);
        int forbiddenFlag = affixManager.forbiddenWordFlag();
        int needAffixFlag = affixManager.needAffixFlag();
        int nosuggestFlag = affixManager.nosuggestFlag();
        List<HashManager.Entry> direct = hashManager.lookup(normalized);
        boolean sawForbidden = false;
        if (!direct.isEmpty()) {
            for (HashManager.Entry entry : direct) {
                if (forbiddenFlag >= 0 && entry.hasFlag(forbiddenFlag)) {
                    sawForbidden = true;
                    continue;
                }
                if (nosuggestFlag >= 0 && entry.hasFlag(nosuggestFlag)) {
                    continue;
                }
                if (needAffixFlag >= 0 && entry.hasFlag(needAffixFlag)) {
                    continue;
                }
                return true;
            }
            if (sawForbidden) {
                return false;
            }
        }
        HashManager.Entry hit = affixManager.affixCheck(normalized, hashManager);
        if (hit == null) {
            return false;
        }
        if (forbiddenFlag >= 0 && hit.hasFlag(forbiddenFlag)) {
            return false;
        }
        if (nosuggestFlag >= 0 && hit.hasFlag(nosuggestFlag)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean spell(String word) {
        return resolveStem(word) != null;
    }

    @Override
    public SpellResult check(String word) {
        String stem = resolveStem(word);
        boolean correct = stem != null;
        return new SpellResult(correct, false, false, correct ? stem : null);
    }

    @Override
    public List<String> suggest(String word) {
        if (word == null || word.isEmpty()) {
            return List.of();
        }
        // Short-circuit: if the word is already accepted, there's nothing to suggest.
        if (spell(word)) {
            return List.of();
        }
        return suggestManager.suggest(word);
    }

    @Override
    public List<String> suffixSuggest(String rootWord) {
        List<String> matches = new ArrayList<>();
        for (var bucket : hashManager.all()) {
            String stem = bucket.getKey();
            if (stem.startsWith(rootWord)) {
                matches.add(stem);
            }
        }
        Collections.sort(matches);
        return Collections.unmodifiableList(matches);
    }

    @Override
    public int addDictionary(Path dicPath) {
        int before = hashManager.size();
        hashManager.load(dicPath, affixManager.charset());
        return hashManager.size() - before;
    }

    @Override
    public DictionaryInfo info() {
        return new DictionaryInfo(affixManager.encoding(), "java-port-dev", 0, affixManager.wordChars());
    }

    @Override
    public void close() {
        hashManager.clear();
    }

    /**
     * Walks the same lookup ladder as {@code HunspellImpl::spell}: the caller's
     * word first, then (if still unresolved) a BREAK-driven recursive split,
     * trailing-dot normalisation, and case-variant fallbacks. FORBIDDENWORD on
     * the surface form short-circuits the ladder so a forbidden entry cannot be
     * rescued by a lowercased variant or by affix derivation.
     */
    private String resolveStem(String word) {
        if (word == null || word.isEmpty()) {
            return null;
        }
        LookupResult direct = lookupVariant(word);
        if (direct.accepted()) {
            return direct.stem();
        }
        if (direct.forbidden()) {
            return null;
        }
        if (spellBreak(word, /*depth=*/ 0)) {
            return word;
        }
        String withoutTrailingDots = stripTrailingDots(word);
        if (!withoutTrailingDots.equals(word) && !withoutTrailingDots.isEmpty()) {
            String stem = resolveStem(withoutTrailingDots);
            if (stem != null) {
                return stem;
            }
        }
        if (isTitleCase(word)) {
            String lower = word.toLowerCase(Locale.ROOT);
            LookupResult lr = lookupVariant(lower);
            if (lr.accepted()) {
                return lr.stem();
            }
        } else if (isAllUpper(word)) {
            String lower = word.toLowerCase(Locale.ROOT);
            LookupResult lr = lookupVariant(lower);
            if (lr.accepted()) {
                return lr.stem();
            }
            String capitalized = capitalize(lower);
            lr = lookupVariant(capitalized);
            if (lr.accepted()) {
                return lr.stem();
            }
        }
        return null;
    }

    /**
     * Tri-state lookup result mirroring how {@code HunspellImpl::spell}
     * distinguishes "found", "not found (fall through)", and
     * "found-but-forbidden (short circuit)".
     */
    private record LookupResult(boolean accepted, boolean forbidden, String stem) {
        static final LookupResult NONE = new LookupResult(false, false, null);
        static final LookupResult FORBIDDEN = new LookupResult(false, true, null);

        static LookupResult of(String stem) {
            return new LookupResult(true, false, stem);
        }
    }

    private LookupResult lookupVariant(String word) {
        String normalized = affixManager.normalizeWord(word);
        int forbiddenFlag = affixManager.forbiddenWordFlag();
        int needAffixFlag = affixManager.needAffixFlag();

        List<HashManager.Entry> direct = hashManager.lookup(normalized);
        if (!direct.isEmpty()) {
            boolean sawForbidden = false;
            for (HashManager.Entry entry : direct) {
                boolean entryForbidden = forbiddenFlag >= 0 && entry.hasFlag(forbiddenFlag);
                if (entryForbidden) {
                    sawForbidden = true;
                    continue;
                }
                if (needAffixFlag >= 0 && entry.hasFlag(needAffixFlag)) {
                    // Stem is marked needaffix; skip direct acceptance but still try derivations.
                    continue;
                }
                return LookupResult.of(entry.stem());
            }
            if (sawForbidden) {
                // A FORBIDDENWORD surface form is final: don't fall through to affix
                // derivations or case fallbacks. This matches how C++ Hunspell rejects
                // `bars`/`foos`/`Kg`/`Cm` even though affix or case variants exist.
                return LookupResult.FORBIDDEN;
            }
        }

        HashManager.Entry hit = affixManager.affixCheck(normalized, hashManager);
        if (hit != null) {
            if (forbiddenFlag >= 0 && hit.hasFlag(forbiddenFlag)) {
                return LookupResult.FORBIDDEN;
            }
            return LookupResult.of(hit.stem());
        }
        return LookupResult.NONE;
    }

    /**
     * Mirrors {@code HunspellImpl::spell_break}: if the word contains any
     * entry from the BREAK table, split recursively on that break point and
     * accept only when both sides are spellable. Recursion depth is bounded
     * the same way C++ Hunspell caps it (10) to avoid pathological inputs.
     */
    private boolean spellBreak(String word, int depth) {
        if (depth > 10) {
            return false;
        }
        List<String> breakTable = affixManager.breakTable();
        for (String pattern : breakTable) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            boolean anchorStart = pattern.startsWith("^");
            boolean anchorEnd = pattern.endsWith("$");
            String core = pattern;
            if (anchorStart) {
                core = core.substring(1);
            }
            if (anchorEnd) {
                core = core.substring(0, core.length() - 1);
            }
            if (core.isEmpty()) {
                continue;
            }
            if (anchorStart) {
                if (word.length() > core.length() && word.startsWith(core)) {
                    // Leading break character is not allowed as a standalone token.
                    return false;
                }
                continue;
            }
            if (anchorEnd) {
                if (word.length() > core.length() && word.endsWith(core)) {
                    return false;
                }
                continue;
            }
            int from = 1;
            int idx;
            while ((idx = word.indexOf(core, from)) >= 0) {
                if (idx + core.length() >= word.length()) {
                    break;
                }
                String left = word.substring(0, idx);
                String right = word.substring(idx + core.length());
                if (resolveStemForBreak(left, depth + 1) && resolveStemForBreak(right, depth + 1)) {
                    return true;
                }
                from = idx + 1;
            }
        }
        return false;
    }

    /**
     * Helper invoked by {@link #spellBreak} for each split half. Mirrors the
     * C++ recursive call into {@code spell} on the sub-word so forbidden/case
     * handling and further break splits apply to every piece.
     */
    private boolean resolveStemForBreak(String segment, int depth) {
        if (segment.isEmpty()) {
            return false;
        }
        LookupResult direct = lookupVariant(segment);
        if (direct.accepted()) {
            return true;
        }
        if (direct.forbidden()) {
            return false;
        }
        if (spellBreak(segment, depth)) {
            return true;
        }
        if (isTitleCase(segment)) {
            LookupResult lr = lookupVariant(segment.toLowerCase(Locale.ROOT));
            if (lr.accepted()) {
                return true;
            }
        } else if (isAllUpper(segment)) {
            String lower = segment.toLowerCase(Locale.ROOT);
            LookupResult lr = lookupVariant(lower);
            if (lr.accepted()) {
                return true;
            }
            lr = lookupVariant(capitalize(lower));
            if (lr.accepted()) {
                return true;
            }
        }
        return false;
    }

    private static String stripTrailingDots(String word) {
        int end = word.length();
        while (end > 0 && word.charAt(end - 1) == '.') {
            end--;
        }
        return end == word.length() ? word : word.substring(0, end);
    }

    private static boolean isTitleCase(String word) {
        if (word.length() < 2) {
            return false;
        }
        if (!Character.isUpperCase(word.charAt(0))) {
            return false;
        }
        for (int i = 1; i < word.length(); i++) {
            if (!Character.isLowerCase(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllUpper(String word) {
        boolean hasLetter = false;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetter = true;
                if (!Character.isUpperCase(ch)) {
                    return false;
                }
            }
        }
        return hasLetter;
    }

    private static String capitalize(String word) {
        if (word.isEmpty()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    static final class BuilderImpl implements Hunspell.Builder {
        private Path affPath;
        private Path primaryDictionary;
        private final List<Path> extraDictionaries = new ArrayList<>();
        private int maxSuggestions = 10;

        @Override
        public Hunspell.Builder affix(Path affPath) {
            this.affPath = Objects.requireNonNull(affPath, "affPath");
            return this;
        }

        @Override
        public Hunspell.Builder dictionary(Path dicPath) {
            this.primaryDictionary = Objects.requireNonNull(dicPath, "dicPath");
            return this;
        }

        @Override
        public Hunspell.Builder addDictionary(Path dicPath) {
            this.extraDictionaries.add(Objects.requireNonNull(dicPath, "dicPath"));
            return this;
        }

        @Override
        public Hunspell.Builder key(String key) {
            return this;
        }

        @Override
        public Hunspell.Builder strictAffixParsing(boolean strict) {
            return this;
        }

        @Override
        public Hunspell.Builder maxSuggestions(int max) {
            this.maxSuggestions = Math.max(1, max);
            return this;
        }

        @Override
        public Hunspell build() {
            if (primaryDictionary == null) {
                throw new HunspellStateException("Primary dictionary is required");
            }
            AffixManager affixManager = new AffixManager();
            if (affPath != null) {
                if (!Files.exists(affPath)) {
                    throw new HunspellParseException("Affix file does not exist: " + affPath);
                }
                affixManager.load(affPath);
            }
            HashManager hashManager = new HashManager(affixManager.flagMode());
            java.util.function.UnaryOperator<String> normalizer = affixManager::normalizeWord;
            hashManager.load(primaryDictionary, affixManager.charset(), normalizer);
            for (Path extraDictionary : extraDictionaries) {
                hashManager.load(extraDictionary, affixManager.charset(), normalizer);
            }
            return new SimpleHunspell(affixManager, hashManager, maxSuggestions);
        }
    }
}
