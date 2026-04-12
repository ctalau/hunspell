package org.hunspell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

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

    private SimpleHunspell(AffixManager affixManager, HashManager hashManager, int maxSuggestions) {
        this.affixManager = affixManager;
        this.hashManager = hashManager;
        this.maxSuggestions = maxSuggestions;
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
        // Edit-stage candidate generation (delete/transpose/replace/insert),
        // then C++-style flag filtering for forbidden suggestion classes.
        String normalized = affixManager.normalizeLookupWord(word);
        Set<String> candidates = new LinkedHashSet<>();
        int noSuggestFlag = affixManager.noSuggestFlag();
        int forbiddenWordFlag = affixManager.forbiddenWordFlag();
        int onlyInCompoundFlag = affixManager.onlyInCompoundFlag();
        Set<Integer> alphabet = suggestionAlphabet();
        for (String candidate : edits(normalized, alphabet)) {
            List<HashManager.Entry> entries = hashManager.lookup(candidate);
            for (HashManager.Entry entry : entries) {
                if (!hasFlag(entry.flags(), noSuggestFlag)
                    && !hasFlag(entry.flags(), forbiddenWordFlag)
                    && !hasFlag(entry.flags(), onlyInCompoundFlag)) {
                    candidates.add(candidate);
                    break;
                }
            }
        }
        List<String> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator
            .comparingInt((String candidate) -> distance(word, candidate))
            .thenComparing(Comparator.naturalOrder()));
        if (ranked.size() > maxSuggestions) {
            ranked = ranked.subList(0, maxSuggestions);
        }
        return Collections.unmodifiableList(ranked);
    }

    private Set<Integer> suggestionAlphabet() {
        Set<Integer> alphabet = new LinkedHashSet<>();
        for (var bucket : hashManager.all()) {
            String stem = bucket.getKey();
            for (int i = 0; i < stem.length(); ) {
                int cp = stem.codePointAt(i);
                alphabet.add(cp);
                i += Character.charCount(cp);
            }
        }
        return alphabet;
    }

    private static Set<String> edits(String word, Set<Integer> alphabet) {
        Set<String> out = new LinkedHashSet<>();
        if (word == null) {
            return out;
        }
        List<Integer> cps = new ArrayList<>();
        for (int i = 0; i < word.length(); ) {
            int cp = word.codePointAt(i);
            cps.add(cp);
            i += Character.charCount(cp);
        }
        int n = cps.size();
        for (int i = 0; i <= n; i++) {
            // deletion
            if (i < n) {
                out.add(joinWithoutIndex(cps, i));
            }
            // transposition
            if (i + 1 < n) {
                out.add(joinWithSwap(cps, i, i + 1));
            }
            // replacement
            if (i < n) {
                for (int repl : alphabet) {
                    if (repl != cps.get(i)) {
                        out.add(joinWithReplace(cps, i, repl));
                    }
                }
            }
            // insertion
            for (int ins : alphabet) {
                out.add(joinWithInsert(cps, i, ins));
            }
        }
        out.remove(word);
        return out;
    }

    private static String joinWithoutIndex(List<Integer> cps, int skip) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.size(); i++) {
            if (i != skip) {
                sb.appendCodePoint(cps.get(i));
            }
        }
        return sb.toString();
    }

    private static String joinWithSwap(List<Integer> cps, int left, int right) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.size(); i++) {
            if (i == left) {
                sb.appendCodePoint(cps.get(right));
            } else if (i == right) {
                sb.appendCodePoint(cps.get(left));
            } else {
                sb.appendCodePoint(cps.get(i));
            }
        }
        return sb.toString();
    }

    private static String joinWithReplace(List<Integer> cps, int index, int replacement) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.size(); i++) {
            sb.appendCodePoint(i == index ? replacement : cps.get(i));
        }
        return sb.toString();
    }

    private static String joinWithInsert(List<Integer> cps, int index, int inserted) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.size(); i++) {
            if (i == index) {
                sb.appendCodePoint(inserted);
            }
            sb.appendCodePoint(cps.get(i));
        }
        if (index == cps.size()) {
            sb.appendCodePoint(inserted);
        }
        return sb.toString();
    }

    private static boolean hasFlag(int[] flags, int flag) {
        if (flag < 0) {
            return false;
        }
        for (int candidateFlag : flags) {
            if (candidateFlag == flag) {
                return true;
            }
        }
        return false;
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
    public List<String> analyze(String word) {
        List<HashManager.Entry> hits = lookupEntries(word);
        if (hits.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (HashManager.Entry entry : hits) {
            if (entry.morphology().isEmpty()) {
                out.add("st:" + entry.stem());
            } else {
                out.add(String.join(" ", entry.morphology()));
            }
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public List<String> stem(String word) {
        List<HashManager.Entry> hits = lookupEntries(word);
        if (hits.isEmpty()) {
            return List.of();
        }
        Set<String> stems = new LinkedHashSet<>();
        for (HashManager.Entry entry : hits) {
            stems.add(entry.stem());
        }
        return Collections.unmodifiableList(new ArrayList<>(stems));
    }

    @Override
    public List<String> generate(String word, String modelWord) {
        List<HashManager.Entry> models = lookupEntries(modelWord);
        if (models.isEmpty()) {
            return List.of();
        }
        Set<String> generated = new LinkedHashSet<>();
        for (HashManager.Entry model : models) {
            generated.addAll(affixManager.generateWords(affixManager.normalizeLookupWord(word), model.flags()));
        }
        return Collections.unmodifiableList(new ArrayList<>(generated));
    }

    @Override
    public List<String> generate2(String word, List<String> morphDescriptions) {
        if (morphDescriptions == null || morphDescriptions.isEmpty()) {
            return List.of(word);
        }
        List<HashManager.Entry> candidates = new ArrayList<>();
        for (var bucket : hashManager.all()) {
            candidates.addAll(bucket.getValue());
        }
        Set<String> generated = new LinkedHashSet<>();
        for (String desc : morphDescriptions) {
            for (HashManager.Entry candidate : candidates) {
                if (candidate.morphology().contains(desc)) {
                    generated.addAll(affixManager.generateWords(affixManager.normalizeLookupWord(word), candidate.flags()));
                }
            }
        }
        if (generated.isEmpty()) {
            generated.add(word);
        }
        return Collections.unmodifiableList(new ArrayList<>(generated));
    }

    @Override
    public void add(String word) {
        String normalized = affixManager.normalizeLookupWord(word);
        hashManager.addEntry(normalized, new int[0], List.of("st:" + normalized));
    }

    @Override
    public void addWithAffix(String word, String modelWord) {
        List<HashManager.Entry> models = lookupEntries(modelWord);
        String normalized = affixManager.normalizeLookupWord(word);
        if (models.isEmpty()) {
            hashManager.addEntry(normalized, new int[0], List.of("st:" + normalized));
            return;
        }
        HashManager.Entry model = models.get(0);
        hashManager.addEntry(normalized, model.flags(), model.morphology());
    }

    @Override
    public void remove(String word) {
        hashManager.removeEntry(affixManager.normalizeLookupWord(word));
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
        LookupResult direct = lookupVariant(word, /*inCompound=*/ false);
        if (direct.accepted()) {
            return direct.stem();
        }
        if (direct.forbidden()) {
            return null;
        }
        if (compoundCheck(word)) {
            return word;
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
            LookupResult lr = lookupVariant(lower, /*inCompound=*/ false);
            if (lr.accepted()) {
                return lr.stem();
            }
        } else if (isAllUpper(word)) {
            String lower = word.toLowerCase(Locale.ROOT);
            LookupResult lr = lookupVariant(lower, /*inCompound=*/ false);
            if (lr.accepted()) {
                return lr.stem();
            }
            String capitalized = capitalize(lower);
            lr = lookupVariant(capitalized, /*inCompound=*/ false);
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

    private LookupResult lookupVariant(String word, boolean inCompound) {
        String normalized = affixManager.normalizeLookupWord(word);
        int forbiddenFlag = affixManager.forbiddenWordFlag();
        int needAffixFlag = affixManager.needAffixFlag();
        int onlyInCompoundFlag = affixManager.onlyInCompoundFlag();

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
                if (!inCompound && onlyInCompoundFlag >= 0 && entry.hasFlag(onlyInCompoundFlag)) {
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
            if (!inCompound && onlyInCompoundFlag >= 0 && hit.hasFlag(onlyInCompoundFlag)) {
                return LookupResult.NONE;
            }
            return LookupResult.of(hit.stem());
        }
        return LookupResult.NONE;
    }

    private List<HashManager.Entry> lookupEntries(String word) {
        if (word == null || word.isEmpty()) {
            return List.of();
        }
        String normalized = affixManager.normalizeLookupWord(word);
        List<HashManager.Entry> direct = hashManager.lookup(normalized);
        if (!direct.isEmpty()) {
            return direct;
        }
        HashManager.Entry affixed = affixManager.affixCheck(normalized, hashManager);
        if (affixed != null) {
            return List.of(affixed);
        }
        return List.of();
    }

    /**
     * Minimal compound checker for COMPOUNDFLAG/COMPOUNDRULE/ONLYINCOMPOUND suites.
     * Segments are matched using the same direct+affix lookup path as spell(),
     * but each part must carry a compound-permitting flag and rule sequences
     * (when declared) must match at least one COMPOUNDRULE pattern.
     */
    private boolean compoundCheck(String word) {
        int min = affixManager.compoundMin();
        if (word.length() < min * 2) {
            return false;
        }
        return compoundCheckFrom(word, 0, min, new ArrayList<>());
    }

    private boolean compoundCheckFrom(String word, int offset, int min,
                                      List<int[]> sequence) {
        for (int split = offset + min; split <= word.length(); split++) {
            String segment = word.substring(offset, split);
            List<int[]> flagsForSegment = compoundFlagsForSegment(segment);
            if (flagsForSegment.isEmpty()) {
                continue;
            }
            for (int[] flags : flagsForSegment) {
                sequence.add(flags);
                if (split == word.length()) {
                    if (sequence.size() >= 2 && sequenceMatchesRules(sequence)) {
                        return true;
                    }
                } else if (word.length() - split >= min && compoundCheckFrom(word, split, min, sequence)) {
                    return true;
                }
                sequence.remove(sequence.size() - 1);
            }
        }
        return false;
    }

    private List<int[]> compoundFlagsForSegment(String segment) {
        String normalized = affixManager.normalizeLookupWord(segment);
        int forbiddenFlag = affixManager.forbiddenWordFlag();
        int needAffixFlag = affixManager.needAffixFlag();
        int compoundFlag = affixManager.compoundFlag();
        List<int[]> hits = new ArrayList<>();
        for (HashManager.Entry entry : hashManager.lookup(normalized)) {
            if (forbiddenFlag >= 0 && entry.hasFlag(forbiddenFlag)) {
                continue;
            }
            if (needAffixFlag >= 0 && entry.hasFlag(needAffixFlag)) {
                continue;
            }
            if (compoundFlag >= 0 && !entry.hasFlag(compoundFlag)) {
                continue;
            }
            hits.add(entry.flags());
        }
        HashManager.Entry affixed = affixManager.affixCheck(normalized, hashManager);
        if (affixed != null) {
            if ((forbiddenFlag < 0 || !affixed.hasFlag(forbiddenFlag))
                && (needAffixFlag < 0 || !affixed.hasFlag(needAffixFlag))
                && (compoundFlag < 0 || affixed.hasFlag(compoundFlag))) {
                hits.add(affixed.flags());
            }
        }
        return hits;
    }

    private boolean sequenceMatchesRules(List<int[]> sequence) {
        List<int[]> rules = affixManager.compoundRules();
        if (rules.isEmpty()) {
            return true;
        }
        for (int[] rule : rules) {
            if (rule.length != sequence.size()) {
                continue;
            }
            boolean allMatched = true;
            for (int i = 0; i < rule.length; i++) {
                if (!Flags.contains(sequence.get(i), rule[i])) {
                    allMatched = false;
                    break;
                }
            }
            if (allMatched) {
                return true;
            }
        }
        return sequenceMatchesBeginEnd(sequence);
    }

    private boolean sequenceMatchesBeginEnd(List<int[]> sequence) {
        if (sequence.size() < 2) {
            return false;
        }
        int beginFlag = affixManager.compoundBeginFlag();
        int endFlag = affixManager.compoundEndFlag();
        if (beginFlag < 0 || endFlag < 0) {
            return false;
        }
        return Flags.contains(sequence.get(0), beginFlag)
            && Flags.contains(sequence.get(sequence.size() - 1), endFlag);
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
        LookupResult direct = lookupVariant(segment, /*inCompound=*/ false);
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
            LookupResult lr = lookupVariant(segment.toLowerCase(Locale.ROOT), /*inCompound=*/ false);
            if (lr.accepted()) {
                return true;
            }
        } else if (isAllUpper(segment)) {
            String lower = segment.toLowerCase(Locale.ROOT);
            LookupResult lr = lookupVariant(lower, /*inCompound=*/ false);
            if (lr.accepted()) {
                return true;
            }
            lr = lookupVariant(capitalize(lower), /*inCompound=*/ false);
            if (lr.accepted()) {
                return true;
            }
        }
        return false;
    }

    private static int distance(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int substitutionCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + substitutionCost);
            }
        }
        return dp[left.length()][right.length()];
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
            HashManager hashManager = new HashManager(affixManager.flagMode(), affixManager.flagAliases());
            java.util.function.UnaryOperator<String> normalizer = affixManager::normalizeWord;
            hashManager.load(primaryDictionary, affixManager.charset(), normalizer);
            for (Path extraDictionary : extraDictionaries) {
                hashManager.load(extraDictionary, affixManager.charset(), normalizer);
            }
            return new SimpleHunspell(affixManager, hashManager, maxSuggestions);
        }
    }
}
