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
    private int needAffixFlag = -1;
    private final List<String> breakTable = new ArrayList<>();
    private boolean breakTableExplicit;
    private final Map<Integer, List<AffixRule>> prefixes = new HashMap<>();
    private final Map<Integer, List<AffixRule>> suffixes = new HashMap<>();
    private final List<AffixRule> allPrefixes = new ArrayList<>();
    private final List<AffixRule> allSuffixes = new ArrayList<>();
    private final java.util.Set<Integer> contClassFlags = new java.util.HashSet<>();
    private boolean haveContClass;
    private final List<RepEntry> repTable = new ArrayList<>();
    private final List<MapEntry> mapTable = new ArrayList<>();
    private String tryChars = "";
    private String keyString = "qwertyuiop|asdfghjkl|zxcvbnm";
    private int maxNgramSugs = 4;
    private int maxSugs = 15;
    private int maxCpdSugs = 3;
    private int nosuggestFlag = -1;

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

    List<String> breakTable() {
        if (!breakTableExplicit && breakTable.isEmpty()) {
            // Hunspell's default BREAK table when none is declared: `-`, `^-`, `-$`.
            return List.of("-", "^-", "-$");
        }
        return Collections.unmodifiableList(breakTable);
    }

    List<RepEntry> repTable() {
        return Collections.unmodifiableList(repTable);
    }

    List<MapEntry> mapTable() {
        return Collections.unmodifiableList(mapTable);
    }

    String tryChars() {
        return tryChars;
    }

    String keyString() {
        return keyString;
    }

    int maxNgramSugs() {
        return maxNgramSugs;
    }

    int maxSugs() {
        return maxSugs;
    }

    int maxCpdSugs() {
        return maxCpdSugs;
    }

    int nosuggestFlag() {
        return nosuggestFlag;
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
            String line = rawLine.strip();
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
            String line = lines.get(i).strip();
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
            if ("TRY".equals(parts[0]) && parts.length >= 2) {
                tryChars = line.substring("TRY".length()).strip();
                continue;
            }
            if ("KEY".equals(parts[0]) && parts.length >= 2) {
                keyString = line.substring("KEY".length()).strip();
                continue;
            }
            if ("NOSUGGEST".equals(parts[0]) && parts.length >= 2) {
                nosuggestFlag = Flags.decodeSingle(parts[1], flagMode);
                continue;
            }
            if ("MAXNGRAMSUGS".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                maxNgramSugs = Integer.parseInt(parts[1]);
                continue;
            }
            if ("MAXCPDSUGS".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                maxCpdSugs = Integer.parseInt(parts[1]);
                continue;
            }
            if ("REP".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                int count = Integer.parseInt(parts[1]);
                int read = 0;
                while (read < count && i + 1 < lines.size()) {
                    i++;
                    String entry = lines.get(i).strip();
                    if (entry.isEmpty() || entry.startsWith("#")) {
                        continue;
                    }
                    String[] tok = entry.split("\\s+");
                    if (!"REP".equals(tok[0]) || tok.length < 3) {
                        read++;
                        continue;
                    }
                    parseRepEntry(tok[1], tok[2]);
                    read++;
                }
                continue;
            }
            if ("MAP".equals(parts[0]) && parts.length >= 2 && parts[1].matches("\\d+")) {
                int count = Integer.parseInt(parts[1]);
                int read = 0;
                while (read < count && i + 1 < lines.size()) {
                    i++;
                    String entry = lines.get(i).strip();
                    if (entry.isEmpty() || entry.startsWith("#")) {
                        continue;
                    }
                    String[] tok = entry.split("\\s+", 2);
                    if (!"MAP".equals(tok[0]) || tok.length < 2) {
                        read++;
                        continue;
                    }
                    parseMapEntry(tok[1].strip());
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
                    String entry = lines.get(i).strip();
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
                    String ruleLine = lines.get(i).strip();
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
                        contFlags = Flags.decode(contPart, flagMode);
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

    /**
     * Mirrors {@code HashMgr::parse_reptable}: {@code ^} prefix sets type=1,
     * {@code $} suffix sets type+=2, and any literal underscore in pattern
     * or replacement becomes a space. Duplicate patterns coalesce so their
     * type-specific replacement slots co-exist on the same {@link RepEntry}.
     */
    private void parseRepEntry(String patternRaw, String replacementRaw) {
        String pattern = patternRaw;
        String replacement = replacementRaw;
        int type = 0;
        if (pattern.startsWith("^")) {
            type = 1;
            pattern = pattern.substring(1);
        }
        if (pattern.endsWith("$")) {
            type += 2;
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        pattern = pattern.replace('_', ' ');
        replacement = replacement.replace('_', ' ');
        RepEntry existing = null;
        for (RepEntry candidate : repTable) {
            if (candidate.pattern().equals(pattern)) {
                existing = candidate;
                break;
            }
        }
        if (existing == null) {
            existing = new RepEntry(pattern);
            repTable.add(existing);
        }
        existing.setReplacement(type, replacement);
    }

    /**
     * Mirrors {@code HashMgr::parse_maptable}. A MAP line lists an
     * equivalence class of characters, optionally with multi-character
     * groups wrapped in parentheses (e.g. {@code MAP ß(ss)}).
     */
    private void parseMapEntry(String body) {
        List<String> units = new ArrayList<>();
        for (int j = 0; j < body.length(); ) {
            char ch = body.charAt(j);
            if (ch == '(') {
                int end = body.indexOf(')', j + 1);
                if (end < 0) {
                    break;
                }
                units.add(body.substring(j + 1, end));
                j = end + 1;
            } else {
                int cp = body.codePointAt(j);
                units.add(new String(Character.toChars(cp)));
                j += Character.charCount(cp);
            }
        }
        if (!units.isEmpty()) {
            mapTable.add(new MapEntry(units));
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
        for (AffixRule rule : allPrefixes) {
            HashManager.Entry hit = checkPrefixRule(rule, word, hashManager);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private HashManager.Entry checkPrefixRule(AffixRule rule, String word, HashManager hashManager) {
        String candidate = rule.stripFrom(word);
        if (candidate == null) {
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
