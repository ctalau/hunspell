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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Java port of the affix-handling subset of {@code AffixMgr} ({@code affixmgr.cxx}).
 *
 * <p>Responsibilities mirrored:</p>
 * <ul>
 *   <li>Parsing the affix file ({@code SET}, {@code FLAG}, {@code WORDCHARS},
 *       {@code PFX}/{@code SFX} blocks).</li>
 *   <li>Holding prefix and suffix tables keyed by their flag identifier.</li>
 *   <li>Performing lookup-time {@code prefix_check}, {@code suffix_check} and
 *       two-level continuation-class {@code suffix_check_twosfx}.</li>
 * </ul>
 *
 * <p>The recursive lookup deliberately mirrors the C++ control flow rather
 * than pre-expanding generated forms eagerly, so that continuation-class
 * chains (e.g. {@code FLAG}-style fixtures) are checked at spell-time the way
 * Hunspell does.</p>
 */
final class AffixManager {

    private FlagMode flagMode = FlagMode.CHAR;
    private String encoding = "UTF-8";
    private Charset charset = StandardCharsets.ISO_8859_1;
    private String wordChars = "";
    private String ignoreChars = "";
    private int forbiddenWordFlag = -1;
    private int noSuggestFlag = -1;
    private int needAffixFlag = -1;
    private int onlyInCompoundFlag = -1;
    private int compoundFlag = -1;
    private int compoundBeginFlag = -1;
    private int compoundEndFlag = -1;
    private int compoundMin = 3;
    private final List<int[]> compoundRules = new ArrayList<>();
    private final List<String> breakTable = new ArrayList<>();
    private final IconvTable inputConversions = new IconvTable();
    private boolean breakTableExplicit;
    private final Map<Integer, List<AffixRule>> prefixes = new HashMap<>();
    private final Map<Integer, List<AffixRule>> suffixes = new HashMap<>();
    private final List<int[]> flagAliases = new ArrayList<>();
    private final List<AffixRule> allPrefixes = new ArrayList<>();
    private final List<AffixRule> allSuffixes = new ArrayList<>();
    private final java.util.Set<Integer> contClassFlags = new java.util.HashSet<>();
    private boolean haveContClass;
    private boolean complexPrefixes;

    FlagMode flagMode() {
        return flagMode;
    }

    String encoding() {
        return encoding;
    }

    Charset charset() {
        return charset;
    }

    String wordChars() {
        return wordChars;
    }

    String ignoreChars() {
        return ignoreChars;
    }

    int forbiddenWordFlag() {
        return forbiddenWordFlag;
    }

    int needAffixFlag() {
        return needAffixFlag;
    }

    int noSuggestFlag() {
        return noSuggestFlag;
    }

    int onlyInCompoundFlag() {
        return onlyInCompoundFlag;
    }

    int compoundFlag() {
        return compoundFlag;
    }

    int compoundBeginFlag() {
        return compoundBeginFlag;
    }

    int compoundEndFlag() {
        return compoundEndFlag;
    }

    int compoundMin() {
        return compoundMin;
    }

    List<int[]> compoundRules() {
        return Collections.unmodifiableList(compoundRules);
    }

    List<String> breakTable() {
        if (!breakTableExplicit && breakTable.isEmpty()) {
            // Hunspell's default BREAK table when none is declared: `-`, `^-`, `-$`.
            return List.of("-", "^-", "-$");
        }
        return Collections.unmodifiableList(breakTable);
    }

    /** Strip IGNORE characters from a word, matching {@code AffixMgr::remove_ignored_chars}. */
    String normalizeWord(String word) {
        if (ignoreChars.isEmpty() || word == null || word.isEmpty()) {
            return word;
        }
        StringBuilder sb = new StringBuilder(word.length());
        for (int i = 0; i < word.length(); ) {
            int cp = word.codePointAt(i);
            int count = Character.charCount(cp);
            if (ignoreChars.indexOf(cp) < 0) {
                sb.appendCodePoint(cp);
            }
            i += count;
        }
        return sb.toString();
    }

    /**
     * Lookup-time normalization: apply ICONV table first, then IGNORE stripping.
     * Mirrors Hunspell input conversion before spell lookup.
     */
    String normalizeLookupWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        String converted = inputConversions.convert(word);
        if (converted == null) {
            converted = word;
        }
        return normalizeWord(converted);
    }

    void load(Path affPath) {
        try {
            byte[] bytes = Files.readAllBytes(affPath);
            String iso = new String(bytes, StandardCharsets.ISO_8859_1);
            // Two-pass parse: locate SET (charset) and FLAG (mode) before parsing affix bodies,
            // matching how HashMgr/AffixMgr scan headers ahead of body decoding.
            preparseHeader(iso);
            String decoded = new String(bytes, charset);
            parseBody(decoded.lines().toList());
        } catch (IOException ex) {
            throw new HunspellIoException("Unable to read affix file: " + affPath, ex);
        }
    }

    private void preparseHeader(String content) {
        for (String rawLine : content.lines().toList()) {
            String line = stripUtf8Bom(rawLine).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            if ("SET".equals(parts[0])) {
                encoding = parts[1];
                try {
                    charset = Charset.forName(parts[1]);
                } catch (IllegalArgumentException ignored) {
                    charset = StandardCharsets.ISO_8859_1;
                }
            } else if ("FLAG".equals(parts[0])) {
                flagMode = parseFlagMode(parts[1]);
            }
        }
    }

    private static FlagMode parseFlagMode(String token) {
        return switch (token) {
            case "long" -> FlagMode.LONG;
            case "num" -> FlagMode.NUM;
            case "UTF-8" -> FlagMode.UTF8;
            default -> FlagMode.CHAR;
        };
    }

    private void parseBody(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = stripUtf8Bom(lines.get(i)).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length == 0) {
                continue;
            }
            if ("WORDCHARS".equals(parts[0]) && parts.length >= 2) {
                wordChars = line.substring("WORDCHARS".length()).strip();
                continue;
            }
            if ("COMPLEXPREFIXES".equals(parts[0])) {
                complexPrefixes = true;
                continue;
            }
            if ("IGNORE".equals(parts[0]) && parts.length >= 2) {
                ignoreChars = line.substring("IGNORE".length()).strip();
                continue;
            }
            if ("FORBIDDENWORD".equals(parts[0]) && parts.length >= 2) {
                forbiddenWordFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("NEEDAFFIX".equals(parts[0]) && parts.length >= 2) {
                needAffixFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("NOSUGGEST".equals(parts[0]) && parts.length >= 2) {
                noSuggestFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("ONLYINCOMPOUND".equals(parts[0]) && parts.length >= 2) {
                onlyInCompoundFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("COMPOUNDFLAG".equals(parts[0]) && parts.length >= 2) {
                compoundFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("COMPOUNDBEGIN".equals(parts[0]) && parts.length >= 2) {
                compoundBeginFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("COMPOUNDEND".equals(parts[0]) && parts.length >= 2) {
                compoundEndFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("COMPOUNDMIN".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                compoundMin = Math.max(1, Integer.parseInt(parts[1]));
                continue;
            }
            if ("COMPOUNDRULE".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                int count = Integer.parseInt(parts[1]);
                int read = 0;
                while (read < count && i + 1 < lines.size()) {
                    i++;
                    String ruleLine = stripUtf8Bom(lines.get(i)).strip();
                    if (ruleLine.isEmpty() || ruleLine.startsWith("#")) {
                        continue;
                    }
                    String[] ruleTokens = ruleLine.split("\\s+");
                    if (ruleTokens.length >= 2 && "COMPOUNDRULE".equals(ruleTokens[0])) {
                        compoundRules.add(Flags.decode(ruleTokens[1], flagMode));
                    }
                    read++;
                }
                continue;
            }
            if ("AF".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                int count = Integer.parseInt(parts[1]);
                int read = 0;
                while (read < count && i + 1 < lines.size()) {
                    i++;
                    String aliasLine = stripUtf8Bom(lines.get(i)).strip();
                    if (aliasLine.isEmpty() || aliasLine.startsWith("#")) {
                        continue;
                    }
                    String[] aliasParts = aliasLine.split("\\s+");
                    if (aliasParts.length >= 2 && "AF".equals(aliasParts[0])) {
                        flagAliases.add(Flags.decode(aliasParts[1], flagMode));
                    }
                    read++;
                }
                continue;
            }
            if ("ICONV".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                int count = Integer.parseInt(parts[1]);
                int read = 0;
                while (read < count && i + 1 < lines.size()) {
                    i++;
                    String iconvLine = stripUtf8Bom(lines.get(i)).strip();
                    if (iconvLine.isEmpty() || iconvLine.startsWith("#")) {
                        continue;
                    }
                    String[] iconvParts = iconvLine.split("\\s+");
                    if (iconvParts.length >= 3 && "ICONV".equals(iconvParts[0])) {
                        inputConversions.add(iconvParts[1], iconvParts[2]);
                    }
                    read++;
                }
                continue;
            }
            if ("BREAK".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                breakTableExplicit = true;
                int count = Integer.parseInt(parts[1]);
                int read = 0;
                while (read < count && i + 1 < lines.size()) {
                    i++;
                    String entry = stripUtf8Bom(lines.get(i)).strip();
                    if (entry.isEmpty() || entry.startsWith("#")) {
                        continue;
                    }
                    String[] tokens = entry.split("\\s+", 2);
                    if ("BREAK".equals(tokens[0]) && tokens.length >= 2) {
                        breakTable.add(tokens[1].strip());
                    } else {
                        breakTable.add(entry);
                    }
                    read++;
                }
                continue;
            }
            if (("PFX".equals(parts[0]) || "SFX".equals(parts[0])) && parts.length >= 4
                && parts[3].matches("\\d+")) {
                boolean isPrefix = "PFX".equals(parts[0]);
                int flag = Flags.decodeSingle(parts[1], flagMode);
                boolean cross = "Y".equals(parts[2]);
                int count = Integer.parseInt(parts[3]);
                int read = 0;
                while (read < count && i + 1 < lines.size()) {
                    i++;
                    String ruleLine = stripUtf8Bom(lines.get(i)).strip();
                    if (ruleLine.isEmpty() || ruleLine.startsWith("#")) {
                        continue;
                    }
                    String[] rule = ruleLine.split("\\s+");
                    if (rule.length < 5) {
                        read++;
                        continue;
                    }
                    String strip = "0".equals(rule[2]) ? "" : rule[2];
                    String appendField = rule[3];
                    String append;
                    int[] contFlags;
                    int slash = appendField.indexOf('/');
                    if (slash >= 0) {
                        String addPart = appendField.substring(0, slash);
                        String contPart = appendField.substring(slash + 1);
                        append = "0".equals(addPart) ? "" : addPart;
                        contFlags = decodeFlags(contPart);
                    } else {
                        append = "0".equals(appendField) ? "" : appendField;
                        contFlags = new int[0];
                    }
                    // IGNORE characters are removed from affix strip/append so the rule operates
                    // in the same "ignore-stripped" space as the dictionary stems and lookup input,
                    // mirroring how C++ Hunspell applies IGNORE across the lookup pipeline.
                    strip = normalizeWord(strip);
                    append = normalizeWord(append);
                    String condition = rule[4];
                    AffixRule affixRule =
                        new AffixRule(flag, isPrefix, cross, strip, append, condition, contFlags);
                    addRule(affixRule);
                    if (contFlags.length > 0) {
                        haveContClass = true;
                        for (int cf : contFlags) {
                            contClassFlags.add(cf);
                        }
                    }
                    read++;
                }
            }
        }
    }

    private static String stripUtf8Bom(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        if (line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        if (line.startsWith("\u00EF\u00BB\u00BF")) {
            return line.substring(3);
        }
        return line;
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

    private static final class IconvTable {
        private final List<IconvEntry> entries = new ArrayList<>();

        void add(String patternIn, String replacementIn) {
            if (patternIn == null || patternIn.isEmpty() || replacementIn == null || replacementIn.isEmpty()) {
                return;
            }
            int type = 0;
            String pattern = patternIn;
            if (pattern.charAt(0) == '_') {
                pattern = pattern.substring(1);
                type = 1;
            }
            if (!pattern.isEmpty() && pattern.charAt(pattern.length() - 1) == '_') {
                pattern = pattern.substring(0, pattern.length() - 1);
                type += 2;
            }
            pattern = pattern.replace("_", " ");
            String replacement = replacementIn.replace("_", " ");

            int existing = findLongestPrefixIndex(pattern, 0);
            if (existing >= 0 && entries.get(existing).pattern.equals(pattern)) {
                entries.get(existing).out[type] = replacement;
                return;
            }

            IconvEntry newEntry = new IconvEntry(pattern);
            newEntry.out[type] = replacement;
            entries.add(newEntry);
            entries.sort((a, b) -> a.pattern.compareTo(b.pattern));
        }

        String convert(String word) {
            if (entries.isEmpty() || word.isEmpty()) {
                return null;
            }
            StringBuilder out = new StringBuilder(word.length());
            boolean changed = false;
            for (int i = 0; i < word.length(); i++) {
                int idx = findLongestPrefixIndex(word, i);
                if (idx < 0) {
                    out.append(word.charAt(i));
                    continue;
                }
                IconvEntry entry = entries.get(idx);
                String replacement = replacement(word.length() - i, entry, i == 0);
                if (replacement.isEmpty()) {
                    out.append(word.charAt(i));
                    continue;
                }
                out.append(replacement);
                if (!entry.pattern.isEmpty()) {
                    i += entry.pattern.length() - 1;
                }
                changed = true;
            }
            return changed ? out.toString() : null;
        }

        private String replacement(int remainingLen, IconvEntry entry, boolean atStart) {
            int type = atStart ? 1 : 0;
            if (remainingLen == entry.pattern.length()) {
                type = atStart ? 3 : 2;
            }
            while (type >= 0 && entry.out[type].isEmpty()) {
                type = (type == 2 && !atStart) ? 0 : type - 1;
            }
            return type >= 0 ? entry.out[type] : "";
        }

        private int findLongestPrefixIndex(String text, int start) {
            int p1 = 0;
            int p2 = entries.size() - 1;
            int ret = -1;
            while (p1 <= p2) {
                int m = (p1 + p2) >>> 1;
                String pattern = entries.get(m).pattern;
                int cmp = comparePrefix(text, start, pattern);
                if (cmp < 0) {
                    p2 = m - 1;
                } else if (cmp > 0) {
                    p1 = m + 1;
                } else {
                    ret = m;
                    p1 = m + 1;
                }
            }
            return ret;
        }

        private int comparePrefix(String text, int start, String pattern) {
            int max = Math.min(pattern.length(), text.length() - start);
            for (int i = 0; i < max; i++) {
                char wc = text.charAt(start + i);
                char pc = pattern.charAt(i);
                if (wc != pc) {
                    return wc < pc ? -1 : 1;
                }
            }
            if (pattern.length() > text.length() - start) {
                return -1;
            }
            return 0;
        }
    }

    private static final class IconvEntry {
        private final String pattern;
        private final String[] out = {"", "", "", ""};

        private IconvEntry(String pattern) {
            this.pattern = pattern;
        }
    }

    private void addRule(AffixRule rule) {
        Map<Integer, List<AffixRule>> map = rule.prefix() ? prefixes : suffixes;
        map.computeIfAbsent(rule.flag(), ignored -> new ArrayList<>()).add(rule);
        (rule.prefix() ? allPrefixes : allSuffixes).add(rule);
    }

    List<AffixRule> rulesForFlag(int flag, boolean prefix) {
        Map<Integer, List<AffixRule>> map = prefix ? prefixes : suffixes;
        List<AffixRule> rules = map.get(flag);
        return rules == null ? Collections.emptyList() : rules;
    }

    List<int[]> flagAliases() {
        return Collections.unmodifiableList(flagAliases);
    }

    List<String> generateWords(String stem, int[] flags) {
        Set<String> generated = new LinkedHashSet<>();
        generated.add(stem);
        List<AffixRule> prefixesForEntry = new ArrayList<>();
        List<AffixRule> suffixesForEntry = new ArrayList<>();
        for (int flag : flags) {
            prefixesForEntry.addAll(rulesForFlag(flag, true));
            suffixesForEntry.addAll(rulesForFlag(flag, false));
        }
        for (AffixRule p : prefixesForEntry) {
            String prefixed = p.apply(stem);
            if (prefixed != null) {
                generated.add(prefixed);
            }
        }
        for (AffixRule s : suffixesForEntry) {
            String suffixed = s.apply(stem);
            if (suffixed != null) {
                generated.add(suffixed);
            }
        }
        for (AffixRule p : prefixesForEntry) {
            if (!p.crossProduct()) {
                continue;
            }
            String prefixed = p.apply(stem);
            if (prefixed == null) {
                continue;
            }
            for (AffixRule s : suffixesForEntry) {
                if (!s.crossProduct()) {
                    continue;
                }
                String both = s.apply(prefixed);
                if (both != null) {
                    generated.add(both);
                }
            }
        }
        return new ArrayList<>(generated);
    }

    /**
     * Replicates {@code AffixMgr::affix_check} for the no-compound, no-needflag case
     * by trying prefix stripping, simple suffix stripping, and (when continuation
     * classes exist in the .aff) the two-level suffix walk.
     */
    HashManager.Entry affixCheck(String word, HashManager hashManager) {
        HashManager.Entry rv = prefixCheck(word, hashManager);
        if (rv != null) {
            return rv;
        }
        if (complexPrefixes && haveContClass) {
            rv = prefixCheckTwoPfx(word, hashManager);
            if (rv != null) {
                return rv;
            }
        }
        rv = suffixCheck(word, /*prefixRule=*/ null, /*requiredCont=*/ -1, hashManager);
        if (rv != null) {
            return rv;
        }
        if (haveContClass) {
            rv = suffixCheckTwoSfx(word, /*prefixRule=*/ null, hashManager);
            if (rv != null) {
                return rv;
            }
            rv = prefixCheckTwoSfx(word, hashManager);
            if (rv != null) {
                return rv;
            }
        }
        return null;
    }

    /** Mirrors {@code AffixMgr::prefix_check}. */
    HashManager.Entry prefixCheck(String word, HashManager hashManager) {
        return prefixCheck(word, /*requiredCont=*/ -1, hashManager);
    }

    private HashManager.Entry prefixCheck(String word, int requiredCont, HashManager hashManager) {
        for (AffixRule rule : allPrefixes) {
            HashManager.Entry hit = checkPrefixRule(rule, word, requiredCont, hashManager);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private HashManager.Entry checkPrefixRule(AffixRule rule, String word, int requiredCont,
                                              HashManager hashManager) {
        String candidate = rule.stripFrom(word);
        if (candidate == null) {
            return null;
        }
        if (requiredCont >= 0 && !Flags.contains(rule.contFlags(), requiredCont)) {
            return null;
        }
        for (HashManager.Entry entry : hashManager.lookup(candidate)) {
            if (entry.hasFlag(rule.flag())) {
                return entry;
            }
        }
        if (rule.crossProduct()) {
            HashManager.Entry hit = suffixCheck(candidate, rule, /*requiredCont=*/ -1, hashManager);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * Mirrors the complex-prefix continuation walk by stripping an outer prefix and
     * requiring the inner prefix rule to expose the outer flag in its continuation
     * class set.
     */
    private HashManager.Entry prefixCheckTwoPfx(String word, HashManager hashManager) {
        for (AffixRule outer : allPrefixes) {
            if (!contClassFlags.contains(outer.flag())) {
                continue;
            }
            String candidate = outer.stripFrom(word);
            if (candidate == null) {
                continue;
            }
            HashManager.Entry hit = prefixCheck(candidate, outer.flag(), hashManager);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * Mirrors {@code AffixMgr::suffix_check}. When {@code prefixRule} is supplied
     * we check the cross-product flag and accept the entry only if its flag set
     * (or the prefix's continuation class) carries the prefix's flag, matching
     * Hunspell's combined PFX×SFX validation.
     */
    HashManager.Entry suffixCheck(String word, AffixRule prefixRule, int requiredCont,
                                  HashManager hashManager) {
        for (AffixRule rule : allSuffixes) {
            HashManager.Entry hit =
                checkSuffixRule(rule, word, prefixRule, requiredCont, hashManager);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private HashManager.Entry checkSuffixRule(AffixRule rule, String word, AffixRule prefixRule,
                                              int requiredCont, HashManager hashManager) {
        if (prefixRule != null && !rule.crossProduct()) {
            return null;
        }
        String candidate = rule.stripFrom(word);
        if (candidate == null) {
            return null;
        }
        for (HashManager.Entry entry : hashManager.lookup(candidate)) {
            boolean rootCarriesFlag = entry.hasFlag(rule.flag());
            boolean prefixCarriesFlag = prefixRule != null
                && Flags.contains(prefixRule.contFlags(), rule.flag());
            if (!rootCarriesFlag && !prefixCarriesFlag) {
                continue;
            }
            if (prefixRule != null) {
                boolean entryHasPrefixFlag = entry.hasFlag(prefixRule.flag());
                boolean ruleHasPrefixFlag = Flags.contains(rule.contFlags(), prefixRule.flag());
                if (!entryHasPrefixFlag && !ruleHasPrefixFlag) {
                    continue;
                }
            }
            if (requiredCont >= 0 && !Flags.contains(rule.contFlags(), requiredCont)) {
                continue;
            }
            return entry;
        }
        return null;
    }

    /**
     * Mirrors {@code AffixMgr::suffix_check_twosfx}. The OUTER suffix is one
     * whose flag identifier appears in some other rule's continuation class
     * ({@code contclasses[se->getFlag()]} in C++). After stripping the outer
     * suffix we recurse, requiring the inner rule to expose the outer flag in
     * its own continuation set so that the chain {@code stem → inner → outer}
     * round-trips back to a real dictionary entry.
     */
    HashManager.Entry suffixCheckTwoSfx(String word, AffixRule prefixRule, HashManager hashManager) {
        for (AffixRule outer : allSuffixes) {
            if (!contClassFlags.contains(outer.flag())) {
                continue;
            }
            String candidate = outer.stripFrom(word);
            if (candidate == null) {
                continue;
            }
            HashManager.Entry hit = suffixCheck(candidate, prefixRule, outer.flag(), hashManager);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * Mirrors {@code AffixMgr::prefix_check_twosfx} → {@code PfxEntry::check_twosfx}.
     * Strip a prefix and then look for a two-level suffix combination on the
     * resulting word, propagating the prefix through the recursive lookup so
     * cross-product validation can succeed.
     */
    HashManager.Entry prefixCheckTwoSfx(String word, HashManager hashManager) {
        for (AffixRule prefix : allPrefixes) {
            if (!prefix.crossProduct()) {
                continue;
            }
            String candidate = prefix.stripFrom(word);
            if (candidate == null) {
                continue;
            }
            HashManager.Entry hit = suffixCheckTwoSfx(candidate, prefix, hashManager);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * Resolve which root produced a given correctly-spelled form by replaying
     * the affix lookup. Returns the stem string when found, or {@code null}.
     */
    String findRoot(String word, HashManager hashManager) {
        if (hashManager.contains(word)) {
            return word;
        }
        HashManager.Entry hit = affixCheck(word, hashManager);
        return hit == null ? null : hit.stem();
    }

    boolean hasContinuationClasses() {
        return haveContClass;
    }
}
