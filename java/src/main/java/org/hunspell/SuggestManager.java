package org.hunspell;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Java port of the suggestion pipeline subset of {@code SuggestMgr}
 * ({@code suggestmgr.cxx}).
 *
 * <p>This implements the same staged pipeline the C++ Hunspell runs when
 * {@code SuggestMgr::suggest} is called, in the same order so ranked output
 * parity with the reference {@code .sug} fixtures is possible:</p>
 *
 * <ol>
 *   <li>{@code capchars}   – uppercase variant (e.g. {@code nasa -> NASA})</li>
 *   <li>{@code replchars}  – REP table substitutions with anchor awareness
 *       plus whole-suggestion dictionary word-pair promotion</li>
 *   <li>{@code mapchars}   – MAP equivalence-class enumeration</li>
 *   <li>{@code swapchar}   – adjacent swap plus the length-4/5 double swap</li>
 *   <li>{@code longswap}   – non-adjacent swap within the character distance
 *       budget used by the C++ pipeline</li>
 *   <li>{@code badcharkey} – uppercase + KEY-neighbor substitution</li>
 *   <li>{@code extrachar}  – single-character deletion</li>
 *   <li>{@code forgotchar} – single-character insertion from TRY</li>
 *   <li>{@code movechar}   – move a character up to the distance budget</li>
 *   <li>{@code badchar}    – single-character substitution from TRY</li>
 *   <li>{@code doubletwochars} – double-duplication collapse
 *       (e.g. {@code vacacation -> vacation})</li>
 *   <li>{@code twowords}   – split into two words, with dictionary word-pair
 *       and dashed-pair promotion matching the C++ {@code SPELL_BEST_SUG}
 *       short-circuit behaviour</li>
 * </ol>
 *
 * <p>REP and word-pair hits carry {@code SPELL_BEST_SUG} semantics: once
 * found, later stages that would only add worse candidates are suppressed.
 * This mirrors the C++ control flow in {@link #suggest(String) suggest}.</p>
 */
final class SuggestManager {

    /** Callback to validate a candidate in the "is this a valid suggestion?" sense. */
    @FunctionalInterface
    interface CheckWord {
        /** Return {@code true} when {@code candidate} is a clean, non-forbidden word. */
        boolean check(String candidate);
    }

    private static final int MAX_CHAR_DISTANCE = 4;

    private final AffixManager affixManager;
    private final CheckWord checkWord;
    private final int maxSug;

    SuggestManager(AffixManager affixManager, CheckWord checkWord, int maxSug) {
        this.affixManager = affixManager;
        this.checkWord = checkWord;
        this.maxSug = maxSug;
    }

    List<String> suggest(String word) {
        LinkedHashSet<String> wlst = new LinkedHashSet<>();
        if (word == null || word.isEmpty()) {
            return List.of();
        }
        int[] info = new int[1];
        boolean[] good = new boolean[1];

        // capchars
        if (wlst.size() < maxSug) {
            String upper = word.toUpperCase(Locale.ROOT);
            if (!upper.equals(word)) {
                testsug(wlst, upper, info);
            }
        }

        // replchars – REP substitutions (may set SPELL_BEST_SUG and short-circuit)
        int before = wlst.size();
        replchars(wlst, word, info);
        if (wlst.size() > before && (info[0] & SPELL_BEST_SUG) != 0) {
            good[0] = true;
            return freeze(wlst);
        }

        // mapchars – MAP equivalence enumeration
        if (wlst.size() < maxSug) {
            mapchars(wlst, word, info);
        }

        // swapchar (with length-4/5 double swap)
        if (wlst.size() < maxSug) {
            swapchar(wlst, word, info);
        }

        // longswapchar
        if (wlst.size() < maxSug) {
            longswapchar(wlst, word, info);
        }

        // badcharkey
        if (wlst.size() < maxSug) {
            badcharkey(wlst, word, info);
        }

        // extrachar
        if (wlst.size() < maxSug) {
            extrachar(wlst, word, info);
        }

        // forgotchar
        if (wlst.size() < maxSug) {
            forgotchar(wlst, word, info);
        }

        // movechar
        if (wlst.size() < maxSug) {
            movechar(wlst, word, info);
        }

        // badchar
        if (wlst.size() < maxSug) {
            badchar(wlst, word, info);
        }

        // doubletwochars
        if (wlst.size() < maxSug) {
            doubletwochars(wlst, word, info);
        }

        // twowords (dictionary word-pair promotion carries SPELL_BEST_SUG)
        twowords(wlst, word, good, info);

        return freeze(wlst);
    }

    private List<String> freeze(LinkedHashSet<String> wlst) {
        if (wlst.size() > maxSug) {
            List<String> trimmed = new ArrayList<>(maxSug);
            int n = 0;
            for (String s : wlst) {
                if (n++ >= maxSug) {
                    break;
                }
                trimmed.add(s);
            }
            return List.copyOf(trimmed);
        }
        return List.copyOf(wlst);
    }

    /** Mirrors {@code SuggestMgr::testsug}. */
    private boolean testsug(LinkedHashSet<String> wlst, String candidate, int[] info) {
        if (wlst.size() >= maxSug) {
            return false;
        }
        if (wlst.contains(candidate)) {
            return false;
        }
        if (checkWord.check(candidate)) {
            wlst.add(candidate);
            return true;
        }
        return false;
    }

    private static final int SPELL_BEST_SUG = 1 << 3;

    /**
     * Mirrors {@code SuggestMgr::replchars}, including the type fallback
     * chain (3 → 2 → 1 → 0) and the space-split promotion where a REP
     * replacement like {@code "a lot"} is validated both as a whole
     * dictionary pair and as a trailing-word fallback.
     */
    private void replchars(LinkedHashSet<String> wlst, String word, int[] info) {
        List<RepEntry> reps = affixManager.repTable();
        if (reps.isEmpty() || word.length() < 2) {
            return;
        }
        for (RepEntry entry : reps) {
            String pattern = entry.pattern();
            if (pattern.isEmpty()) {
                continue;
            }
            int r = 0;
            while ((r = word.indexOf(pattern, r)) >= 0) {
                int type = (r == 0) ? 1 : 0;
                if (r + pattern.length() == word.length()) {
                    type += 2;
                }
                while (type > 0 && entry.replacement(type) == null) {
                    type = (type == 2 && r != 0) ? 0 : type - 1;
                }
                String out = entry.replacement(type);
                if (out == null || out.isEmpty() && type == 0) {
                    // C++ treats missing type==0 the same as "no rule here".
                    if (out == null) {
                        r++;
                        continue;
                    }
                }
                if (out == null) {
                    r++;
                    continue;
                }
                String candidate = word.substring(0, r) + out + word.substring(r + pattern.length());
                int space = candidate.indexOf(' ');
                int oldSize = wlst.size();
                testsug(wlst, candidate, info);
                if (wlst.size() > oldSize) {
                    info[0] |= SPELL_BEST_SUG;
                }

                // Space-split fallback: if the candidate contains a space
                // (e.g. "a lot"), validate each left chunk as a dictionary
                // word and re-test the trailing word; when accepted, rewrite
                // the latest suggestion back to the full two-word form so
                // "alot -> a lot" replaces the interim "lot" suggestion.
                if (space >= 0) {
                    int prev = 0;
                    int sp = space;
                    while (sp >= 0) {
                        String prevChunk = candidate.substring(prev, sp);
                        if (checkWord.check(prevChunk)) {
                            int before = wlst.size();
                            String postChunk = candidate.substring(sp + 1);
                            testsug(wlst, postChunk, info);
                            if (wlst.size() > before) {
                                // Replace the last inserted suggestion with the full candidate
                                // and preserve insertion order, mirroring C++ behavior of
                                // wlst[wlst.size()-1] = candidate.
                                replaceLast(wlst, postChunk, candidate);
                            }
                        }
                        prev = sp + 1;
                        sp = candidate.indexOf(' ', prev);
                    }
                }
                r++;
            }
        }
    }

    /** Replace the most recently inserted element in a LinkedHashSet. */
    private static void replaceLast(LinkedHashSet<String> wlst, String oldVal, String newVal) {
        if (!wlst.remove(oldVal)) {
            return;
        }
        wlst.add(newVal);
    }

    /** Mirrors {@code SuggestMgr::mapchars} and {@code map_related}. */
    private void mapchars(LinkedHashSet<String> wlst, String word, int[] info) {
        List<MapEntry> maps = affixManager.mapTable();
        if (word.length() < 2 || maps.isEmpty()) {
            return;
        }
        StringBuilder candidate = new StringBuilder();
        mapRelated(word, candidate, 0, wlst, maps, 0, info);
    }

    private void mapRelated(String word, StringBuilder candidate, int wn,
                            LinkedHashSet<String> wlst, List<MapEntry> maptable,
                            int depth, int[] info) {
        if (wn == word.length()) {
            String result = candidate.toString();
            testsug(wlst, result, info);
            return;
        }
        if (depth > 0x3F00 || wlst.size() >= maxSug) {
            return;
        }
        boolean inMap = false;
        for (MapEntry map : maptable) {
            for (String unit : map.units()) {
                int len = unit.length();
                if (len > 0 && word.regionMatches(wn, unit, 0, len)) {
                    inMap = true;
                    int saved = candidate.length();
                    for (String sub : map.units()) {
                        candidate.setLength(saved);
                        candidate.append(sub);
                        mapRelated(word, candidate, wn + len, wlst, maptable, depth + 1, info);
                    }
                    candidate.setLength(saved);
                }
            }
        }
        if (!inMap) {
            int saved = candidate.length();
            candidate.append(word.charAt(wn));
            mapRelated(word, candidate, wn + 1, wlst, maptable, depth + 1, info);
            candidate.setLength(saved);
        }
    }

    /** Mirrors {@code SuggestMgr::swapchar} including the length-4/5 double swap. */
    private void swapchar(LinkedHashSet<String> wlst, String word, int[] info) {
        if (word.length() < 2) {
            return;
        }
        char[] candidate = word.toCharArray();
        for (int i = 0; i < candidate.length - 1; i++) {
            char tmp = candidate[i];
            candidate[i] = candidate[i + 1];
            candidate[i + 1] = tmp;
            testsug(wlst, new String(candidate), info);
            candidate[i + 1] = candidate[i];
            candidate[i] = tmp;
        }
        if (candidate.length == 4 || candidate.length == 5) {
            candidate[0] = word.charAt(1);
            candidate[1] = word.charAt(0);
            candidate[2] = word.charAt(2);
            candidate[candidate.length - 2] = word.charAt(candidate.length - 1);
            candidate[candidate.length - 1] = word.charAt(candidate.length - 2);
            testsug(wlst, new String(candidate), info);
            if (candidate.length == 5) {
                candidate[0] = word.charAt(0);
                candidate[1] = word.charAt(2);
                candidate[2] = word.charAt(1);
                testsug(wlst, new String(candidate), info);
            }
        }
    }

    /** Mirrors {@code SuggestMgr::longswapchar}. */
    private void longswapchar(LinkedHashSet<String> wlst, String word, int[] info) {
        char[] candidate = word.toCharArray();
        for (int p = 0; p < candidate.length; p++) {
            for (int q = 0; q < candidate.length; q++) {
                int distance = Math.abs(q - p);
                if (distance > 1 && distance <= MAX_CHAR_DISTANCE) {
                    char tmp = candidate[p];
                    candidate[p] = candidate[q];
                    candidate[q] = tmp;
                    testsug(wlst, new String(candidate), info);
                    tmp = candidate[p];
                    candidate[p] = candidate[q];
                    candidate[q] = tmp;
                }
            }
        }
    }

    /** Mirrors {@code SuggestMgr::badcharkey}. */
    private void badcharkey(LinkedHashSet<String> wlst, String word, int[] info) {
        char[] candidate = word.toCharArray();
        String key = affixManager.keyString();
        for (int i = 0; i < candidate.length; i++) {
            char tmpc = candidate[i];
            char upper = Character.toUpperCase(tmpc);
            if (upper != tmpc) {
                candidate[i] = upper;
                testsug(wlst, new String(candidate), info);
                candidate[i] = tmpc;
            }
            if (key.isEmpty()) {
                continue;
            }
            int loc = 0;
            while (loc < key.length() && key.charAt(loc) != tmpc) {
                loc++;
            }
            while (loc < key.length()) {
                if (loc > 0 && key.charAt(loc - 1) != '|') {
                    candidate[i] = key.charAt(loc - 1);
                    testsug(wlst, new String(candidate), info);
                }
                if (loc + 1 < key.length() && key.charAt(loc + 1) != '|') {
                    candidate[i] = key.charAt(loc + 1);
                    testsug(wlst, new String(candidate), info);
                }
                do {
                    loc++;
                } while (loc < key.length() && key.charAt(loc) != tmpc);
            }
            candidate[i] = tmpc;
        }
    }

    /** Mirrors {@code SuggestMgr::extrachar}. */
    private void extrachar(LinkedHashSet<String> wlst, String word, int[] info) {
        if (word.length() < 2) {
            return;
        }
        StringBuilder candidate = new StringBuilder(word);
        for (int i = 0; i < word.length(); i++) {
            int index = candidate.length() - 1 - i;
            char tmp = candidate.charAt(index);
            candidate.deleteCharAt(index);
            testsug(wlst, candidate.toString(), info);
            candidate.insert(index, tmp);
        }
    }

    /** Mirrors {@code SuggestMgr::forgotchar}. */
    private void forgotchar(LinkedHashSet<String> wlst, String word, int[] info) {
        String ctry = affixManager.tryChars();
        if (ctry.isEmpty()) {
            return;
        }
        StringBuilder candidate = new StringBuilder(word);
        for (int k = 0; k < ctry.length(); k++) {
            char ch = ctry.charAt(k);
            for (int i = 0; i <= word.length(); i++) {
                int index = candidate.length() - i;
                candidate.insert(index, ch);
                testsug(wlst, candidate.toString(), info);
                candidate.deleteCharAt(index);
            }
        }
    }

    /** Mirrors {@code SuggestMgr::movechar}. */
    private void movechar(LinkedHashSet<String> wlst, String word, int[] info) {
        if (word.length() < 2) {
            return;
        }
        char[] candidate = word.toCharArray();
        for (int p = 0; p < candidate.length; p++) {
            for (int q = p + 1; q < candidate.length && (q - p) <= MAX_CHAR_DISTANCE; q++) {
                char tmp = candidate[q];
                candidate[q] = candidate[q - 1];
                candidate[q - 1] = tmp;
                if ((q - p) < 2) {
                    continue;
                }
                testsug(wlst, new String(candidate), info);
            }
            System.arraycopy(word.toCharArray(), 0, candidate, 0, candidate.length);
        }
        for (int p = candidate.length - 1; p > 0; p--) {
            for (int q = p - 1; q >= 0 && (p - q) <= MAX_CHAR_DISTANCE; q--) {
                char tmp = candidate[q];
                candidate[q] = candidate[q + 1];
                candidate[q + 1] = tmp;
                if ((p - q) < 2) {
                    continue;
                }
                testsug(wlst, new String(candidate), info);
            }
            System.arraycopy(word.toCharArray(), 0, candidate, 0, candidate.length);
        }
    }

    /** Mirrors {@code SuggestMgr::badchar}. */
    private void badchar(LinkedHashSet<String> wlst, String word, int[] info) {
        String ctry = affixManager.tryChars();
        if (ctry.isEmpty()) {
            return;
        }
        char[] candidate = word.toCharArray();
        for (int j = 0; j < ctry.length(); j++) {
            char t = ctry.charAt(j);
            for (int i = candidate.length - 1; i >= 0; i--) {
                char tmpc = candidate[i];
                if (tmpc == t) {
                    continue;
                }
                candidate[i] = t;
                testsug(wlst, new String(candidate), info);
                candidate[i] = tmpc;
            }
        }
    }

    /** Mirrors {@code SuggestMgr::doubletwochars}. */
    private void doubletwochars(LinkedHashSet<String> wlst, String word, int[] info) {
        int wl = word.length();
        if (wl < 5) {
            return;
        }
        int state = 0;
        for (int i = 2; i < wl; i++) {
            if (word.charAt(i) == word.charAt(i - 2)) {
                state++;
                if (state == 3 || (state == 2 && i >= 4)) {
                    String candidate = word.substring(0, i - 1) + word.substring(i + 1);
                    testsug(wlst, candidate, info);
                    state = 0;
                }
            } else {
                state = 0;
            }
        }
    }

    /**
     * Mirrors {@code SuggestMgr::twowords}: at every split point insert a
     * space (and, language-permitting, a dash) and test the joined candidate
     * as a dictionary word. A successful whole-pair hit is a
     * {@code SPELL_BEST_SUG}: previously-collected inferior suggestions are
     * cleared and the pair takes the front of the list. Otherwise, if both
     * halves validate individually, add the dashed and spaced variants to
     * the tail.
     */
    private void twowords(LinkedHashSet<String> wlst, String word, boolean[] good, int[] info) {
        int wl = word.length();
        if (wl < 3) {
            return;
        }
        for (int i = 1; i < wl; i++) {
            String left = word.substring(0, i);
            String right = word.substring(i);
            String candidate = left + " " + right;
            if (checkWord.check(candidate)) {
                // Dictionary word pair: top priority, wipes inferior suggestions.
                info[0] |= SPELL_BEST_SUG;
                if (!good[0]) {
                    good[0] = true;
                    wlst.clear();
                }
                // Insert at front (C++ wlst.insert(wlst.begin(), ...)).
                LinkedHashSet<String> rebuilt = new LinkedHashSet<>();
                rebuilt.add(candidate);
                rebuilt.addAll(wlst);
                wlst.clear();
                wlst.addAll(rebuilt);
            }
            String dashed = left + "-" + right;
            if (checkWord.check(dashed)) {
                info[0] |= SPELL_BEST_SUG;
                if (!good[0]) {
                    good[0] = true;
                    wlst.clear();
                }
                LinkedHashSet<String> rebuilt = new LinkedHashSet<>();
                rebuilt.add(dashed);
                rebuilt.addAll(wlst);
                wlst.clear();
                wlst.addAll(rebuilt);
            }
            if (!good[0] && wlst.size() < maxSug) {
                boolean c1 = checkWord.check(left);
                if (c1) {
                    boolean c2 = checkWord.check(right);
                    if (c2) {
                        if (!wlst.contains(candidate)) {
                            wlst.add(candidate);
                        }
                        if (right.length() > 1 && left.length() > 1 && !wlst.contains(dashed)) {
                            wlst.add(dashed);
                        }
                    }
                }
            }
        }
    }
}
