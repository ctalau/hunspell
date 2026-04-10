package org.hunspell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class HunspellPortedCorpusTest {

    private static final Path CONDITION_AFF = Path.of("..", "tests", "condition.aff").normalize();
    private static final Path CONDITION_DIC = Path.of("..", "tests", "condition.dic").normalize();
    private static final Path CONDITION_GOOD = Path.of("..", "tests", "condition.good").normalize();
    private static final Path CONDITION_WRONG = Path.of("..", "tests", "condition.wrong").normalize();
    private static final Path CONDITION_UTF_AFF = Path.of("..", "tests", "condition_utf.aff").normalize();
    private static final Path CONDITION_UTF_DIC = Path.of("..", "tests", "condition_utf.dic").normalize();
    private static final Path CONDITION_UTF_GOOD = Path.of("..", "tests", "condition_utf.good").normalize();
    private static final Path CONDITION_UTF_WRONG = Path.of("..", "tests", "condition_utf.wrong").normalize();
    private static final Path SLASH_DIC = Path.of("..", "tests", "slash.dic").normalize();
    private static final Path SLASH_GOOD = Path.of("..", "tests", "slash.good").normalize();
    private static final Path BASE_AFF = Path.of("..", "tests", "base.aff").normalize();
    private static final Path BASE_DIC = Path.of("..", "tests", "base.dic").normalize();
    private static final Path BASE_UTF_AFF = Path.of("..", "tests", "base_utf.aff").normalize();
    private static final Path BASE_UTF_DIC = Path.of("..", "tests", "base_utf.dic").normalize();
    private static final Path AFFIXES_AFF = Path.of("..", "tests", "affixes.aff").normalize();
    private static final Path AFFIXES_DIC = Path.of("..", "tests", "affixes.dic").normalize();
    private static final Path AFFIXES_GOOD = Path.of("..", "tests", "affixes.good").normalize();
    private static final Path FLAG_AFF = Path.of("..", "tests", "flag.aff").normalize();
    private static final Path FLAG_DIC = Path.of("..", "tests", "flag.dic").normalize();
    private static final Path FLAG_GOOD = Path.of("..", "tests", "flag.good").normalize();
    private static final Path FLAGLONG_AFF = Path.of("..", "tests", "flaglong.aff").normalize();
    private static final Path FLAGLONG_DIC = Path.of("..", "tests", "flaglong.dic").normalize();
    private static final Path FLAGLONG_GOOD = Path.of("..", "tests", "flaglong.good").normalize();
    private static final Path FLAGNUM_AFF = Path.of("..", "tests", "flagnum.aff").normalize();
    private static final Path FLAGNUM_DIC = Path.of("..", "tests", "flagnum.dic").normalize();
    private static final Path FLAGNUM_GOOD = Path.of("..", "tests", "flagnum.good").normalize();
    private static final Path FLAGUTF8_AFF = Path.of("..", "tests", "flagutf8.aff").normalize();
    private static final Path FLAGUTF8_DIC = Path.of("..", "tests", "flagutf8.dic").normalize();
    private static final Path FLAGUTF8_GOOD = Path.of("..", "tests", "flagutf8.good").normalize();
    private static final Path IGNORE_AFF = Path.of("..", "tests", "ignore.aff").normalize();
    private static final Path IGNORE_DIC = Path.of("..", "tests", "ignore.dic").normalize();
    private static final Path IGNORE_GOOD = Path.of("..", "tests", "ignore.good").normalize();
    private static final Path NEEDAFFIX_AFF = Path.of("..", "tests", "needaffix.aff").normalize();
    private static final Path NEEDAFFIX_DIC = Path.of("..", "tests", "needaffix.dic").normalize();
    private static final Path FORBIDDENWORD_AFF = Path.of("..", "tests", "forbiddenword.aff").normalize();
    private static final Path FORBIDDENWORD_DIC = Path.of("..", "tests", "forbiddenword.dic").normalize();
    private static final Path FORBIDDENWORD_GOOD = Path.of("..", "tests", "forbiddenword.good").normalize();
    private static final Path FORBIDDENWORD_WRONG = Path.of("..", "tests", "forbiddenword.wrong").normalize();
    private static final Path BREAK_AFF = Path.of("..", "tests", "break.aff").normalize();
    private static final Path BREAK_DIC = Path.of("..", "tests", "break.dic").normalize();
    private static final Path BREAK_WRONG = Path.of("..", "tests", "break.wrong").normalize();
    private static final Path SUG_AFF = Path.of("..", "tests", "sug.aff").normalize();
    private static final Path SUG_DIC = Path.of("..", "tests", "sug.dic").normalize();
    private static final Path SUG2_AFF = Path.of("..", "tests", "sug2.aff").normalize();
    private static final Path SUG2_DIC = Path.of("..", "tests", "sug2.dic").normalize();
    private static final Path MAP_AFF = Path.of("..", "tests", "map.aff").normalize();
    private static final Path MAP_DIC = Path.of("..", "tests", "map.dic").normalize();
    private static final Path REP_AFF = Path.of("..", "tests", "rep.aff").normalize();
    private static final Path REP_DIC = Path.of("..", "tests", "rep.dic").normalize();

    @Test
    void conditionGood_ofosuf1_isAccepted() {
        assertConditionAccepted("ofosuf1");
    }

    @Test
    void conditionGood_pre1ofo_isAccepted() {
        assertConditionAccepted("pre1ofo");
    }

    @Test
    void conditionGood_entertaining_isAccepted() {
        assertConditionAccepted("entertaining");
    }

    @Test
    void conditionGood_wries_isAccepted() {
        assertConditionAccepted("wries");
    }

    @Test
    void conditionGood_unwry_isAccepted() {
        assertConditionAccepted("unwry");
    }

    @Test
    void conditionGood_accentedWord_isAccepted() {
        assertConditionAccepted("érach");
    }

    @Test
    void conditionWrong_ofosuf4_isRejected() {
        assertConditionRejected("ofosuf4");
    }

    @Test
    void conditionWrong_pre4ofo_isRejected() {
        assertConditionRejected("pre4ofo");
    }

    @Test
    void conditionWrong_entertainning_isRejected() {
        assertConditionRejected("entertainning");
    }

    @Test
    void conditionWrong_gninnianretne_isRejected() {
        assertConditionRejected("gninnianretne");
    }

    @Test
    void slashGood_fraction_isAccepted() {
        assertSlashAccepted("1/2");
    }

    @Test
    void slashGood_httpPrefix_isAccepted() {
        assertSlashAccepted("http://");
    }

    @Test
    void conditionUtfGood_suf1_isAccepted() {
        assertConditionUtfAccepted("óőósuf1");
    }

    @Test
    void conditionUtfGood_pre1_isAccepted() {
        assertConditionUtfAccepted("pre1óőó");
    }

    @Test
    void conditionUtfGood_suf3_isAccepted() {
        assertConditionUtfAccepted("óőósuf3");
    }

    @Test
    void conditionUtfGood_pre7_isAccepted() {
        assertConditionUtfAccepted("pre7óőó");
    }

    @Test
    void conditionUtfGood_suf16_isAccepted() {
        assertConditionUtfAccepted("óőósuf16");
    }

    @Test
    void conditionUtfWrong_suf4_isRejected() {
        assertConditionUtfRejected("óőósuf4");
    }

    @Test
    void conditionUtfWrong_pre4_isRejected() {
        assertConditionUtfRejected("pre4óőó");
    }

    @Test
    void conditionUtfWrong_suf11_isRejected() {
        assertConditionUtfRejected("óőósuf11");
    }

    @Test
    void conditionUtfWrong_pre12_isRejected() {
        assertConditionUtfRejected("pre12óőó");
    }

    @Test
    void conditionUtfWrong_pre18_isRejected() {
        assertConditionUtfRejected("pre18óőó");
    }

    @Test
    void conditionCorpusGood_allWordsAccepted() {
        assertAllAccepted(CONDITION_AFF, CONDITION_DIC, CONDITION_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void conditionCorpusWrong_allWordsRejected() {
        assertAllRejected(CONDITION_AFF, CONDITION_DIC, CONDITION_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void conditionUtfCorpusGood_allWordsAccepted() {
        assertAllAccepted(CONDITION_UTF_AFF, CONDITION_UTF_DIC, CONDITION_UTF_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void conditionUtfCorpusWrong_allWordsRejected() {
        assertAllRejected(CONDITION_UTF_AFF, CONDITION_UTF_DIC, CONDITION_UTF_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void slashCorpusGood_allWordsAccepted() {
        assertAllAccepted(null, SLASH_DIC, SLASH_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void baseGood_uncreated_isAccepted() {
        assertBaseAccepted("uncreated");
    }

    @Test
    void baseGood_implied_isAccepted() {
        assertBaseAccepted("implied");
    }

    @Test
    void baseGood_unnatural_isAccepted() {
        assertBaseAccepted("unnatural");
    }

    @Test
    void baseGood_conveyed_isAccepted() {
        assertBaseAccepted("conveyed");
    }

    @Test
    void baseGood_faqs_isAccepted() {
        assertBaseAccepted("FAQs");
    }

    @Test
    void baseGood_helloTitlecase_isAccepted() {
        assertBaseAccepted("Hello");
    }

    @Test
    void baseGood_helloUpper_isAccepted() {
        assertBaseAccepted("HELLO");
    }

    @Test
    void baseGood_hunspellUpperWithDots_isAccepted() {
        assertBaseAccepted("HUNSPELL...");
    }

    @Test
    void baseGood_textWithDot_isAccepted() {
        assertBaseAccepted("Text.");
    }

    @Test
    void baseWrong_nasaTitlecase_isRejected() {
        assertBaseRejected("Nasa");
    }

    @Test
    void baseWrong_tomorow_isRejected() {
        assertBaseRejected("tomorow");
    }

    @Test
    void baseWrong_sugesst_isRejected() {
        assertBaseRejected("sugesst");
    }

    @Test
    void affixesCorpusGood_allWordsAccepted() {
        assertAllAccepted(AFFIXES_AFF, AFFIXES_DIC, AFFIXES_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void affixesGood_reworkedCrossProductAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(AFFIXES_AFF).dictionary(AFFIXES_DIC).build()) {
            assertTrue(hunspell.spell("rework"));
            assertTrue(hunspell.spell("worked"));
            assertTrue(hunspell.spell("reworked"));
            assertTrue(hunspell.spell("tried"));
        }
    }

    @Test
    void flagCorpusGood_singleCharFlagsAccepted() {
        assertAllAccepted(FLAG_AFF, FLAG_DIC, FLAG_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void flagGood_continuationClassChainProducesNestedSuffix() {
        try (Hunspell hunspell = Hunspell.builder().affix(FLAG_AFF).dictionary(FLAG_DIC).build()) {
            assertTrue(hunspell.spell("foosbar"));
            assertTrue(hunspell.spell("foosbaz"));
            assertTrue(hunspell.spell("unfoosbar"));
        }
    }

    @Test
    void flagLongCorpusGood_twoCharFlagsAccepted() {
        assertAllAccepted(FLAGLONG_AFF, FLAGLONG_DIC, FLAGLONG_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void flagNumCorpusGood_decimalFlagsAccepted() {
        assertAllAccepted(FLAGNUM_AFF, FLAGNUM_DIC, FLAGNUM_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void flagUtf8CorpusGood_unicodeFlagsAccepted() {
        assertAllAccepted(FLAGUTF8_AFF, FLAGUTF8_DIC, FLAGUTF8_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void baseUtfGood_unicodeStemAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(BASE_UTF_AFF).dictionary(BASE_UTF_DIC).build()) {
            assertTrue(hunspell.spell("created"));
            assertTrue(hunspell.spell("uncreated"));
            assertTrue(hunspell.spell("conveyed"));
            assertTrue(hunspell.spell("FAQs"));
            assertTrue(hunspell.spell("Hello"));
            assertTrue(hunspell.spell("HELLO"));
            assertTrue(hunspell.spell("NASA"));
        }
    }

    @Test
    void baseUtfWrong_misspelledFormsRejected() {
        try (Hunspell hunspell = Hunspell.builder().affix(BASE_UTF_AFF).dictionary(BASE_UTF_DIC).build()) {
            assertFalse(hunspell.spell("loooked"));
            assertFalse(hunspell.spell("hlelo"));
            assertFalse(hunspell.spell("tomorow"));
            assertFalse(hunspell.spell("Nasa"));
        }
    }

    @Test
    void ignoreCorpusGood_allWordsAccepted() {
        assertAllAccepted(IGNORE_AFF, IGNORE_DIC, IGNORE_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void ignoreGood_strippedVowelsMatchStemAndAffix() {
        try (Hunspell hunspell = Hunspell.builder().affix(IGNORE_AFF).dictionary(IGNORE_DIC).build()) {
            // IGNORE aeiou strips vowels from both stored stems and lookup input,
            // and `re` prefix works in that stripped space (so "reexpression" and "rxprssn"
            // round-trip to the same stem).
            assertTrue(hunspell.spell("example"));
            assertTrue(hunspell.spell("expression"));
            assertTrue(hunspell.spell("xmpl"));
            assertTrue(hunspell.spell("xprssn"));
            assertTrue(hunspell.spell("reexpression"));
            assertTrue(hunspell.spell("rxprssn"));
        }
    }

    @Test
    void needAffixGood_bareStemRejectedButAffixedAccepted() {
        // `foo/YXA` carries the NEEDAFFIX flag so `foo` on its own must be rejected,
        // while the affixed derivation `foos` (via SFX A) still rounds back to the stem.
        try (Hunspell hunspell = Hunspell.builder().affix(NEEDAFFIX_AFF).dictionary(NEEDAFFIX_DIC).build()) {
            assertFalse(hunspell.spell("foo"));
            assertTrue(hunspell.spell("foos"));
            assertTrue(hunspell.spell("bar"));
        }
    }

    @Test
    void forbiddenWordCorpusGood_allWordsAccepted() {
        assertAllAccepted(FORBIDDENWORD_AFF, FORBIDDENWORD_DIC, FORBIDDENWORD_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void forbiddenWordCorpusWrong_allForbiddenFormsRejected() {
        assertAllRejected(FORBIDDENWORD_AFF, FORBIDDENWORD_DIC, FORBIDDENWORD_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void forbiddenWordGood_homonymWithoutForbiddenFlagStillAccepted() {
        // `foo/S` and `foo/YX` are homonyms; because one homonym lacks the FORBIDDENWORD
        // flag, `foo` must be accepted. This mirrors C++ Hunspell's next_homonym walk.
        try (Hunspell hunspell = Hunspell.builder().affix(FORBIDDENWORD_AFF).dictionary(FORBIDDENWORD_DIC).build()) {
            assertTrue(hunspell.spell("foo"));
            assertTrue(hunspell.spell("bar"));
            assertTrue(hunspell.spell("kg"));
            assertTrue(hunspell.spell("cm"));
        }
    }

    @Test
    void forbiddenWordWrong_forbiddenSurfaceFormBlocksCaseFallback() {
        // `Kg/X` and `KG/X` are FORBIDDENWORD-flagged, so neither case-variant may fall
        // back to the clean `kg` entry. Likewise for `Cm/X` vs. `cm`.
        try (Hunspell hunspell = Hunspell.builder().affix(FORBIDDENWORD_AFF).dictionary(FORBIDDENWORD_DIC).build()) {
            assertFalse(hunspell.spell("bars"));
            assertFalse(hunspell.spell("foos"));
            assertFalse(hunspell.spell("Kg"));
            assertFalse(hunspell.spell("KG"));
            assertFalse(hunspell.spell("Cm"));
        }
    }

    @Test
    void breakGood_recursiveDashSplittingAccepted() {
        // BREAK `-` and `–` (n-dash) split the input recursively and each piece must be
        // independently spellable; mirrors HunspellImpl::spell_break.
        try (Hunspell hunspell = Hunspell.builder().affix(BREAK_AFF).dictionary(BREAK_DIC).build()) {
            assertTrue(hunspell.spell("foo"));
            assertTrue(hunspell.spell("bar"));
            assertTrue(hunspell.spell("fox-bax"));
            assertTrue(hunspell.spell("foo-bar"));
            assertTrue(hunspell.spell("foo\u2013bar"));
            assertTrue(hunspell.spell("bar-baz"));
            assertTrue(hunspell.spell("baz-foo"));
            assertTrue(hunspell.spell("foo-bar-foo-bar"));
            assertTrue(hunspell.spell("foo-bar\u2013foo-bar"));
            assertTrue(hunspell.spell("e-mail"));
            assertTrue(hunspell.spell("e-mail-foo"));
        }
    }

    @Test
    void breakCorpusWrong_allForbiddenFormsRejected() {
        assertAllRejected(BREAK_AFF, BREAK_DIC, BREAK_WRONG, StandardCharsets.UTF_8);
    }

    // -----------------------------------------------------------------
    // Phase 2 — Suggestion engine parity tests (.sug fixture coverage).
    //
    // Each test picks a misspelling from the C++ .wrong fixture and
    // asserts the SuggestManager produces the expected top-priority
    // suggestion that the corresponding .sug golden line records.
    // -----------------------------------------------------------------

    @Test
    void sugSuggest_nasaCapchars_NASA() {
        // capchars uppercase variant
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            assertTrue(h.suggest("nasa").contains("NASA"));
        }
    }

    @Test
    void sugSuggest_ghandiSwapchar_Gandhi() {
        // swapchar: Ghandi -> Gandhi
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            assertTrue(h.suggest("Ghandi").contains("Gandhi"));
        }
    }

    @Test
    void sugSuggest_greatfulSwapchar_grateful() {
        // swapchar: greatful -> grateful
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            assertTrue(h.suggest("greatful").contains("grateful"));
        }
    }

    @Test
    void sugSuggest_vacacationDoubletwo_vacation() {
        // doubletwochars: vacacation -> vacation
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            assertTrue(h.suggest("vacacation").contains("vacation"));
        }
    }

    @Test
    void sugSuggest_alotReplchars_aLot() {
        // REP alot a_lot + dictionary word pair "a lot": best suggestion
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            List<String> sug = h.suggest("alot");
            assertTrue(sug.contains("a lot"),
                "alot -> 'a lot' expected, got " + sug);
        }
    }

    @Test
    void sugSuggest_ahevDoubleSwap_have() {
        // length-4 double-swap: ahev -> have
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            assertTrue(h.suggest("ahev").contains("have"));
        }
    }

    @Test
    void sugSuggest_hwihcDoubleSwap_which() {
        // length-5 double-swap: hwihc -> which
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            assertTrue(h.suggest("hwihc").contains("which"));
        }
    }

    @Test
    void sug2Suggest_alotWordpair_aLotFromDictionaryStem() {
        // sug2 stores "a lot" as a dictionary stem; twowords must find it.
        try (Hunspell h = Hunspell.builder().affix(SUG2_AFF).dictionary(SUG2_DIC).build()) {
            List<String> sug = h.suggest("alot");
            assertTrue(sug.contains("a lot"),
                "alot -> 'a lot' expected (dictionary word-pair), got " + sug);
        }
    }

    @Test
    void sug2Suggest_scotfreeWordpair_scotFreeDashed() {
        // sug2 stores "scot-free" as a dictionary stem; twowords dashed hit.
        try (Hunspell h = Hunspell.builder().affix(SUG2_AFF).dictionary(SUG2_DIC).build()) {
            List<String> sug = h.suggest("scotfree");
            assertTrue(sug.contains("scot-free"),
                "scotfree -> 'scot-free' expected (dashed pair), got " + sug);
        }
    }

    @Test
    void sug2Suggest_inspiteWordpair_inSpite() {
        try (Hunspell h = Hunspell.builder().affix(SUG2_AFF).dictionary(SUG2_DIC).build()) {
            List<String> sug = h.suggest("inspite");
            assertTrue(sug.contains("in spite"),
                "inspite -> 'in spite' expected, got " + sug);
        }
    }

    @Test
    void mapSuggest_fruhstuckMapchars_fruehstueck() {
        // MAP equivalence class inserts umlauts.
        try (Hunspell h = Hunspell.builder().affix(MAP_AFF).dictionary(MAP_DIC).build()) {
            List<String> sug = h.suggest("Fruhstuck");
            assertTrue(sug.contains("Frühstück"),
                "Fruhstuck -> 'Frühstück' expected, got " + sug);
        }
    }

    @Test
    void mapSuggest_grossMapchars_grosz() {
        // MAP ß(ss) equivalence.
        try (Hunspell h = Hunspell.builder().affix(MAP_AFF).dictionary(MAP_DIC).build()) {
            List<String> sug = h.suggest("gross");
            assertTrue(sug.contains("groß"),
                "gross -> 'groß' expected, got " + sug);
        }
    }

    @Test
    void repSuggest_phormReplchars_form() {
        // REP ph f anchored nowhere (middle type), phorm -> form.
        try (Hunspell h = Hunspell.builder().affix(REP_AFF).dictionary(REP_DIC).build()) {
            assertTrue(h.suggest("phorm").contains("form"));
        }
    }

    @Test
    void repSuggest_fantomReplchars_phantom() {
        // REP f ph: fantom -> phantom.
        try (Hunspell h = Hunspell.builder().affix(REP_AFF).dictionary(REP_DIC).build()) {
            assertTrue(h.suggest("fantom").contains("phantom"));
        }
    }

    @Test
    void repSuggest_vacashunReplchars_vacation() {
        // REP shun$ tion: vacashun -> vacation (end-anchored).
        try (Hunspell h = Hunspell.builder().affix(REP_AFF).dictionary(REP_DIC).build()) {
            assertTrue(h.suggest("vacashun").contains("vacation"));
        }
    }

    @Test
    void repSuggest_alotAnchoredRep_aLot() {
        // REP ^alot$ a_lot: whole-word anchored REP + space split.
        try (Hunspell h = Hunspell.builder().affix(REP_AFF).dictionary(REP_DIC).build()) {
            List<String> sug = h.suggest("alot");
            assertTrue(sug.contains("a lot"),
                "alot -> 'a lot' expected (anchored REP), got " + sug);
        }
    }

    @Test
    void repSuggest_unAlunnoReplchars_unAlunno() {
        // REP ' _: un'alunno -> un alunno.
        try (Hunspell h = Hunspell.builder().affix(REP_AFF).dictionary(REP_DIC).build()) {
            List<String> sug = h.suggest("un'alunno");
            assertTrue(sug.contains("un alunno"),
                "un'alunno -> 'un alunno' expected, got " + sug);
        }
    }

    @Test
    void repSuggest_fooAnchored_bar() {
        // REP ^foo$ bar: whole-word fallback.
        try (Hunspell h = Hunspell.builder().affix(REP_AFF).dictionary(REP_DIC).build()) {
            assertTrue(h.suggest("foo").contains("bar"));
        }
    }

    @Test
    void sugSuggest_inspiteReplchars_inSpiteAndInspire() {
        // REP inspite in_spite → dictionary word pair "in spite",
        // plus swapchar-derived "inspire" (stored verbatim).
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            List<String> sug = h.suggest("inspite");
            assertTrue(sug.contains("in spite"),
                "inspite -> 'in spite' expected, got " + sug);
            assertTrue(sug.contains("inspire"),
                "inspite -> 'inspire' expected via badchar/swap, got " + sug);
        }
    }

    @Test
    void sugSuggest_permenantBadcharOrLongswap_permanent() {
        // Length-9 longswap e↔a or badchar 'a' (via KEY neighbour) yields permanent.
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            List<String> sug = h.suggest("permenant");
            assertTrue(sug.contains("permanent"),
                "permenant -> 'permanent' expected, got " + sug);
        }
    }

    @Test
    void sugSuggest_permqnentBadcharkey_permanent() {
        // badcharkey: q is KEY-adjacent to a (via aq row), so permqnent -> permanent.
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            List<String> sug = h.suggest("permqnent");
            assertTrue(sug.contains("permanent"),
                "permqnent -> 'permanent' expected via KEY neighbour, got " + sug);
        }
    }

    @Test
    void sugSuggest_forbiddenWordPairYieldsNoSuggestions() {
        // permanent-vacation is FORBIDDENWORD in sug.dic. checkSuggestion must
        // reject it across all stages so no pair is proposed.
        try (Hunspell h = Hunspell.builder().affix(SUG_AFF).dictionary(SUG_DIC).build()) {
            List<String> sug = h.suggest("permanent-vacation");
            assertFalse(sug.contains("permanent-vacation"),
                "forbidden pair must not be suggested, got " + sug);
        }
    }

    @Test
    void repSuggest_unAlunnoDictionaryPair_keepsPair() {
        // After REP ' _ rewrite, left="un" + right="alunno" both checkWord→true,
        // and the pair is inserted via twowords BEST_SUG.
        try (Hunspell h = Hunspell.builder().affix(REP_AFF).dictionary(REP_DIC).build()) {
            List<String> sug = h.suggest("un'alunno");
            assertTrue(sug.contains("un alunno"),
                "un'alunno -> 'un alunno' expected, got " + sug);
        }
    }

    private static void assertConditionAccepted(String word) {
        try (Hunspell hunspell = Hunspell.builder().affix(CONDITION_AFF).dictionary(CONDITION_DIC).build()) {
            assertTrue(hunspell.spell(word));
        }
    }

    private static void assertConditionRejected(String word) {
        try (Hunspell hunspell = Hunspell.builder().affix(CONDITION_AFF).dictionary(CONDITION_DIC).build()) {
            assertFalse(hunspell.spell(word));
        }
    }

    private static void assertSlashAccepted(String word) {
        try (Hunspell hunspell = Hunspell.builder().dictionary(SLASH_DIC).build()) {
            assertTrue(hunspell.spell(word));
        }
    }

    private static void assertConditionUtfAccepted(String word) {
        try (Hunspell hunspell = Hunspell.builder().affix(CONDITION_UTF_AFF).dictionary(CONDITION_UTF_DIC).build()) {
            assertTrue(hunspell.spell(word));
        }
    }

    private static void assertConditionUtfRejected(String word) {
        try (Hunspell hunspell = Hunspell.builder().affix(CONDITION_UTF_AFF).dictionary(CONDITION_UTF_DIC).build()) {
            assertFalse(hunspell.spell(word));
        }
    }

    private static void assertBaseAccepted(String word) {
        try (Hunspell hunspell = Hunspell.builder().affix(BASE_AFF).dictionary(BASE_DIC).build()) {
            assertTrue(hunspell.spell(word));
        }
    }

    private static void assertBaseRejected(String word) {
        try (Hunspell hunspell = Hunspell.builder().affix(BASE_AFF).dictionary(BASE_DIC).build()) {
            assertFalse(hunspell.spell(word));
        }
    }

    private static void assertAllAccepted(Path affix, Path dictionary, Path corpus, java.nio.charset.Charset charset) {
        List<String> words = loadCorpusWords(corpus, charset);
        try (Hunspell hunspell = build(affix, dictionary)) {
            for (String word : words) {
                assertTrue(hunspell.spell(word), () -> "Expected accepted: " + word);
            }
        }
    }

    private static void assertAllRejected(Path affix, Path dictionary, Path corpus, java.nio.charset.Charset charset) {
        List<String> words = loadCorpusWords(corpus, charset);
        try (Hunspell hunspell = build(affix, dictionary)) {
            for (String word : words) {
                assertFalse(hunspell.spell(word), () -> "Expected rejected: " + word);
            }
        }
    }

    private static Hunspell build(Path affix, Path dictionary) {
        Hunspell.Builder builder = Hunspell.builder().dictionary(dictionary);
        if (affix != null) {
            builder.affix(affix);
        }
        return builder.build();
    }

    private static List<String> loadCorpusWords(Path corpus, java.nio.charset.Charset charset) {
        try {
            return Files.readAllLines(corpus, charset).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
        } catch (IOException ex) {
            throw new AssertionError("Failed to read corpus file: " + corpus, ex);
        }
    }
}
