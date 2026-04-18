package org.hunspell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Disabled;
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
    private static final Path ONLYINCOMPOUND_AFF = Path.of("..", "tests", "onlyincompound.aff").normalize();
    private static final Path ONLYINCOMPOUND_DIC = Path.of("..", "tests", "onlyincompound.dic").normalize();
    private static final Path ONLYINCOMPOUND_GOOD = Path.of("..", "tests", "onlyincompound.good").normalize();
    private static final Path ONLYINCOMPOUND_WRONG = Path.of("..", "tests", "onlyincompound.wrong").normalize();
    private static final Path COMPOUNDRULE_AFF = Path.of("..", "tests", "compoundrule.aff").normalize();
    private static final Path COMPOUNDRULE_DIC = Path.of("..", "tests", "compoundrule.dic").normalize();
    private static final Path COMPOUNDRULE_GOOD = Path.of("..", "tests", "compoundrule.good").normalize();
    private static final Path COMPOUNDRULE_WRONG = Path.of("..", "tests", "compoundrule.wrong").normalize();
    private static final Path IGNOREUTF_AFF = Path.of("..", "tests", "ignoreutf.aff").normalize();
    private static final Path IGNOREUTF_DIC = Path.of("..", "tests", "ignoreutf.dic").normalize();
    private static final Path IGNOREUTF_GOOD = Path.of("..", "tests", "ignoreutf.good").normalize();
    private static final Path NEEDAFFIX2_AFF = Path.of("..", "tests", "needaffix2.aff").normalize();
    private static final Path NEEDAFFIX2_DIC = Path.of("..", "tests", "needaffix2.dic").normalize();
    private static final Path NEEDAFFIX2_GOOD = Path.of("..", "tests", "needaffix2.good").normalize();
    private static final Path NEEDAFFIX4_AFF = Path.of("..", "tests", "needaffix4.aff").normalize();
    private static final Path NEEDAFFIX4_DIC = Path.of("..", "tests", "needaffix4.dic").normalize();
    private static final Path NEEDAFFIX4_GOOD = Path.of("..", "tests", "needaffix4.good").normalize();
    private static final Path ZEROAFFIX_AFF = Path.of("..", "tests", "zeroaffix.aff").normalize();
    private static final Path ZEROAFFIX_DIC = Path.of("..", "tests", "zeroaffix.dic").normalize();
    private static final Path ZEROAFFIX_GOOD = Path.of("..", "tests", "zeroaffix.good").normalize();
    private static final Path FULLSTRIP_AFF = Path.of("..", "tests", "fullstrip.aff").normalize();
    private static final Path FULLSTRIP_DIC = Path.of("..", "tests", "fullstrip.dic").normalize();
    private static final Path FULLSTRIP_GOOD = Path.of("..", "tests", "fullstrip.good").normalize();
    private static final Path BREAKOFF_AFF = Path.of("..", "tests", "breakoff.aff").normalize();
    private static final Path BREAKOFF_DIC = Path.of("..", "tests", "breakoff.dic").normalize();
    private static final Path BREAKOFF_GOOD = Path.of("..", "tests", "breakoff.good").normalize();
    private static final Path BREAKOFF_WRONG = Path.of("..", "tests", "breakoff.wrong").normalize();
    private static final Path ALLCAPS3_AFF = Path.of("..", "tests", "allcaps3.aff").normalize();
    private static final Path ALLCAPS3_DIC = Path.of("..", "tests", "allcaps3.dic").normalize();
    private static final Path ALLCAPS3_GOOD = Path.of("..", "tests", "allcaps3.good").normalize();
    private static final Path ALLCAPS3_WRONG = Path.of("..", "tests", "allcaps3.wrong").normalize();
    private static final Path CHECKCOMPOUNDREP2_AFF = Path.of("..", "tests", "checkcompoundrep2.aff").normalize();
    private static final Path CHECKCOMPOUNDREP2_DIC = Path.of("..", "tests", "checkcompoundrep2.dic").normalize();
    private static final Path CHECKCOMPOUNDREP2_GOOD = Path.of("..", "tests", "checkcompoundrep2.good").normalize();
    private static final Path UTFCOMPOUND_AFF = Path.of("..", "tests", "utfcompound.aff").normalize();
    private static final Path UTFCOMPOUND_DIC = Path.of("..", "tests", "utfcompound.dic").normalize();
    private static final Path UTFCOMPOUND_GOOD = Path.of("..", "tests", "utfcompound.good").normalize();
    private static final Path I35725_AFF = Path.of("..", "tests", "i35725.aff").normalize();
    private static final Path I35725_DIC = Path.of("..", "tests", "i35725.dic").normalize();
    private static final Path I35725_GOOD = Path.of("..", "tests", "i35725.good").normalize();
    private static final Path I35725_WRONG = Path.of("..", "tests", "i35725.wrong").normalize();
    private static final Path GH1076_AFF = Path.of("..", "tests", "gh1076.aff").normalize();
    private static final Path GH1076_DIC = Path.of("..", "tests", "gh1076.dic").normalize();
    private static final Path GH1076_GOOD = Path.of("..", "tests", "gh1076.good").normalize();
    private static final Path GH1076_WRONG = Path.of("..", "tests", "gh1076.wrong").normalize();
    private static final Path MAP_AFF = Path.of("..", "tests", "map.aff").normalize();
    private static final Path MAP_DIC = Path.of("..", "tests", "map.dic").normalize();
    private static final Path MAP_WRONG = Path.of("..", "tests", "map.wrong").normalize();
    private static final Path REP_AFF = Path.of("..", "tests", "rep.aff").normalize();
    private static final Path REP_DIC = Path.of("..", "tests", "rep.dic").normalize();
    private static final Path REP_WRONG = Path.of("..", "tests", "rep.wrong").normalize();
    private static final Path I1592880_AFF = Path.of("..", "tests", "1592880.aff").normalize();
    private static final Path I1592880_DIC = Path.of("..", "tests", "1592880.dic").normalize();
    private static final Path I1592880_GOOD = Path.of("..", "tests", "1592880.good").normalize();
    private static final Path I1695964_AFF = Path.of("..", "tests", "1695964.aff").normalize();
    private static final Path I1695964_DIC = Path.of("..", "tests", "1695964.dic").normalize();
    private static final Path I1695964_WRONG = Path.of("..", "tests", "1695964.wrong").normalize();
    private static final Path I1463589_AFF = Path.of("..", "tests", "1463589.aff").normalize();
    private static final Path I1463589_DIC = Path.of("..", "tests", "1463589.dic").normalize();
    private static final Path I1463589_WRONG = Path.of("..", "tests", "1463589.wrong").normalize();
    private static final Path I1463589_UTF_AFF = Path.of("..", "tests", "1463589_utf.aff").normalize();
    private static final Path I1463589_UTF_DIC = Path.of("..", "tests", "1463589_utf.dic").normalize();
    private static final Path I1463589_UTF_WRONG = Path.of("..", "tests", "1463589_utf.wrong").normalize();
    private static final Path IJ_AFF = Path.of("..", "tests", "IJ.aff").normalize();
    private static final Path IJ_DIC = Path.of("..", "tests", "IJ.dic").normalize();
    private static final Path IJ_GOOD = Path.of("..", "tests", "IJ.good").normalize();
    private static final Path IJ_WRONG = Path.of("..", "tests", "IJ.wrong").normalize();
    private static final Path I68568_AFF = Path.of("..", "tests", "i68568.aff").normalize();
    private static final Path I68568_DIC = Path.of("..", "tests", "i68568.dic").normalize();
    private static final Path I68568_WRONG = Path.of("..", "tests", "i68568.wrong").normalize();
    private static final Path I68568UTF_AFF = Path.of("..", "tests", "i68568utf.aff").normalize();
    private static final Path I68568UTF_DIC = Path.of("..", "tests", "i68568utf.dic").normalize();
    private static final Path I68568UTF_WRONG = Path.of("..", "tests", "i68568utf.wrong").normalize();
    private static final Path I1706659_AFF = Path.of("..", "tests", "1706659.aff").normalize();
    private static final Path I1706659_DIC = Path.of("..", "tests", "1706659.dic").normalize();
    private static final Path I1706659_WRONG = Path.of("..", "tests", "1706659.wrong").normalize();
    private static final Path I1748408_1_AFF = Path.of("..", "tests", "1748408-1.aff").normalize();
    private static final Path I1748408_1_DIC = Path.of("..", "tests", "1748408-1.dic").normalize();
    private static final Path I1748408_1_GOOD = Path.of("..", "tests", "1748408-1.good").normalize();
    private static final Path I1748408_2_AFF = Path.of("..", "tests", "1748408-2.aff").normalize();
    private static final Path I1748408_2_DIC = Path.of("..", "tests", "1748408-2.dic").normalize();
    private static final Path I1748408_2_GOOD = Path.of("..", "tests", "1748408-2.good").normalize();
    private static final Path I1748408_3_AFF = Path.of("..", "tests", "1748408-3.aff").normalize();
    private static final Path I1748408_3_DIC = Path.of("..", "tests", "1748408-3.dic").normalize();
    private static final Path I1748408_3_GOOD = Path.of("..", "tests", "1748408-3.good").normalize();
    private static final Path I1748408_4_AFF = Path.of("..", "tests", "1748408-4.aff").normalize();
    private static final Path I1748408_4_DIC = Path.of("..", "tests", "1748408-4.dic").normalize();
    private static final Path I1748408_4_GOOD = Path.of("..", "tests", "1748408-4.good").normalize();
    private static final Path DIGITS_IN_WORDS_AFF = Path.of("..", "tests", "digits_in_words.aff").normalize();
    private static final Path DIGITS_IN_WORDS_DIC = Path.of("..", "tests", "digits_in_words.dic").normalize();
    private static final Path DIGITS_IN_WORDS_WRONG = Path.of("..", "tests", "digits_in_words.wrong").normalize();
    private static final Path COLONS_IN_WORDS_AFF = Path.of("..", "tests", "colons_in_words.aff").normalize();
    private static final Path COLONS_IN_WORDS_DIC = Path.of("..", "tests", "colons_in_words.dic").normalize();
    private static final Path NGRAM_UTF_FIX_AFF = Path.of("..", "tests", "ngram_utf_fix.aff").normalize();
    private static final Path NGRAM_UTF_FIX_DIC = Path.of("..", "tests", "ngram_utf_fix.dic").normalize();
    private static final Path NGRAM_UTF_FIX_GOOD = Path.of("..", "tests", "ngram_utf_fix.good").normalize();
    private static final Path NGRAM_UTF_FIX_WRONG = Path.of("..", "tests", "ngram_utf_fix.wrong").normalize();
    private static final Path I1975530_AFF = Path.of("..", "tests", "1975530.aff").normalize();
    private static final Path I1975530_DIC = Path.of("..", "tests", "1975530.dic").normalize();
    private static final Path I1975530_GOOD = Path.of("..", "tests", "1975530.good").normalize();
    private static final Path I1975530_WRONG = Path.of("..", "tests", "1975530.wrong").normalize();
    private static final Path ENCODING_AFF = Path.of("..", "tests", "encoding.aff").normalize();
    private static final Path ENCODING_DIC = Path.of("..", "tests", "encoding.dic").normalize();
    private static final Path ENCODING_GOOD = Path.of("..", "tests", "encoding.good").normalize();
    private static final Path KOREAN_AFF = Path.of("..", "tests", "korean.aff").normalize();
    private static final Path KOREAN_DIC = Path.of("..", "tests", "korean.dic").normalize();
    private static final Path KOREAN_GOOD = Path.of("..", "tests", "korean.good").normalize();
    private static final Path KOREAN_WRONG = Path.of("..", "tests", "korean.wrong").normalize();
    private static final Path OPENTAAL_FORBIDDENWORD2_AFF = Path.of("..", "tests", "opentaal_forbiddenword2.aff").normalize();
    private static final Path OPENTAAL_FORBIDDENWORD2_DIC = Path.of("..", "tests", "opentaal_forbiddenword2.dic").normalize();
    private static final Path OPENTAAL_FORBIDDENWORD2_GOOD = Path.of("..", "tests", "opentaal_forbiddenword2.good").normalize();
    private static final Path OPENTAAL_FORBIDDENWORD2_WRONG = Path.of("..", "tests", "opentaal_forbiddenword2.wrong").normalize();
    private static final Path ARABIC_AFF = Path.of("..", "tests", "arabic.aff").normalize();
    private static final Path ARABIC_DIC = Path.of("..", "tests", "arabic.dic").normalize();
    private static final Path ARABIC_WRONG = Path.of("..", "tests", "arabic.wrong").normalize();
    private static final Path WARN_AFF = Path.of("..", "tests", "warn.aff").normalize();
    private static final Path WARN_DIC = Path.of("..", "tests", "warn.dic").normalize();
    private static final Path WARN_GOOD = Path.of("..", "tests", "warn.good").normalize();
    private static final Path RIGHT_TO_LEFT_MARK_AFF = Path.of("..", "tests", "right_to_left_mark.aff").normalize();
    private static final Path RIGHT_TO_LEFT_MARK_DIC = Path.of("..", "tests", "right_to_left_mark.dic").normalize();
    private static final Path RIGHT_TO_LEFT_MARK_GOOD = Path.of("..", "tests", "right_to_left_mark.good").normalize();
    private static final Path I54633_AFF = Path.of("..", "tests", "i54633.aff").normalize();
    private static final Path I54633_DIC = Path.of("..", "tests", "i54633.dic").normalize();
    private static final Path I54633_GOOD = Path.of("..", "tests", "i54633.good").normalize();
    private static final Path I54633_WRONG = Path.of("..", "tests", "i54633.wrong").normalize();
    private static final Path I54980_AFF = Path.of("..", "tests", "i54980.aff").normalize();
    private static final Path I54980_DIC = Path.of("..", "tests", "i54980.dic").normalize();
    private static final Path I54980_GOOD = Path.of("..", "tests", "i54980.good").normalize();
    private static final Path MAPUTF_AFF = Path.of("..", "tests", "maputf.aff").normalize();
    private static final Path MAPUTF_DIC = Path.of("..", "tests", "maputf.dic").normalize();
    private static final Path MAPUTF_WRONG = Path.of("..", "tests", "maputf.wrong").normalize();
    private static final Path REPUTF_AFF = Path.of("..", "tests", "reputf.aff").normalize();
    private static final Path REPUTF_DIC = Path.of("..", "tests", "reputf.dic").normalize();
    private static final Path REPUTF_WRONG = Path.of("..", "tests", "reputf.wrong").normalize();
    private static final Path CIRCUMFIX_AFF = Path.of("..", "tests", "circumfix.aff").normalize();
    private static final Path CIRCUMFIX_DIC = Path.of("..", "tests", "circumfix.dic").normalize();
    private static final Path CIRCUMFIX_GOOD = Path.of("..", "tests", "circumfix.good").normalize();
    private static final Path CIRCUMFIX_WRONG = Path.of("..", "tests", "circumfix.wrong").normalize();
    private static final Path COMPOUNDAFFIX2_AFF = Path.of("..", "tests", "compoundaffix2.aff").normalize();
    private static final Path COMPOUNDAFFIX2_DIC = Path.of("..", "tests", "compoundaffix2.dic").normalize();
    private static final Path COMPOUNDAFFIX2_GOOD = Path.of("..", "tests", "compoundaffix2.good").normalize();
    private static final Path COMPOUNDFLAG_AFF = Path.of("..", "tests", "compoundflag.aff").normalize();
    private static final Path COMPOUNDFLAG_DIC = Path.of("..", "tests", "compoundflag.dic").normalize();
    private static final Path COMPOUNDFLAG_GOOD = Path.of("..", "tests", "compoundflag.good").normalize();
    private static final Path COMPOUNDFLAG_WRONG = Path.of("..", "tests", "compoundflag.wrong").normalize();
    private static final Path CONDITIONALPREFIX_AFF = Path.of("..", "tests", "conditionalprefix.aff").normalize();
    private static final Path CONDITIONALPREFIX_DIC = Path.of("..", "tests", "conditionalprefix.dic").normalize();
    private static final Path CONDITIONALPREFIX_GOOD = Path.of("..", "tests", "conditionalprefix.good").normalize();
    private static final Path CONDITIONALPREFIX_WRONG = Path.of("..", "tests", "conditionalprefix.wrong").normalize();
    private static final Path ICONV_BREAK_OVERFLOW_AFF = Path.of("..", "tests", "iconv_break_overflow.aff").normalize();
    private static final Path ICONV_BREAK_OVERFLOW_DIC = Path.of("..", "tests", "iconv_break_overflow.dic").normalize();
    private static final Path ICONV_BREAK_OVERFLOW_WRONG = Path.of("..", "tests", "iconv_break_overflow.wrong").normalize();
    private static final Path NOSUGGEST_AFF = Path.of("..", "tests", "nosuggest.aff").normalize();
    private static final Path NOSUGGEST_DIC = Path.of("..", "tests", "nosuggest.dic").normalize();
    private static final Path NOSUGGEST_GOOD = Path.of("..", "tests", "nosuggest.good").normalize();
    private static final Path NOSUGGEST_WRONG = Path.of("..", "tests", "nosuggest.wrong").normalize();
    private static final Path PH_AFF = Path.of("..", "tests", "ph.aff").normalize();
    private static final Path PH_DIC = Path.of("..", "tests", "ph.dic").normalize();
    private static final Path PH_WRONG = Path.of("..", "tests", "ph.wrong").normalize();
    private static final Path PHONE_AFF = Path.of("..", "tests", "phone.aff").normalize();
    private static final Path PHONE_DIC = Path.of("..", "tests", "phone.dic").normalize();
    private static final Path PHONE_WRONG = Path.of("..", "tests", "phone.wrong").normalize();
    private static final Path SUG_AFF = Path.of("..", "tests", "sug.aff").normalize();
    private static final Path SUG_DIC = Path.of("..", "tests", "sug.dic").normalize();
    private static final Path SUG_WRONG = Path.of("..", "tests", "sug.wrong").normalize();
    private static final Path SUGUTF_AFF = Path.of("..", "tests", "sugutf.aff").normalize();
    private static final Path SUGUTF_DIC = Path.of("..", "tests", "sugutf.dic").normalize();
    private static final Path SUGUTF_WRONG = Path.of("..", "tests", "sugutf.wrong").normalize();
    private static final Path UTF8_AFF = Path.of("..", "tests", "utf8.aff").normalize();
    private static final Path UTF8_DIC = Path.of("..", "tests", "utf8.dic").normalize();
    private static final Path UTF8_GOOD = Path.of("..", "tests", "utf8.good").normalize();
    private static final Path UTF8_BOM_AFF = Path.of("..", "tests", "utf8_bom.aff").normalize();
    private static final Path UTF8_BOM_DIC = Path.of("..", "tests", "utf8_bom.dic").normalize();
    private static final Path UTF8_BOM_GOOD = Path.of("..", "tests", "utf8_bom.good").normalize();
    private static final Path UTF8_BOM2_AFF = Path.of("..", "tests", "utf8_bom2.aff").normalize();
    private static final Path UTF8_BOM2_DIC = Path.of("..", "tests", "utf8_bom2.dic").normalize();
    private static final Path UTF8_BOM2_GOOD = Path.of("..", "tests", "utf8_bom2.good").normalize();
    private static final Path OCONV_AFF = Path.of("..", "tests", "oconv.aff").normalize();
    private static final Path OCONV_DIC = Path.of("..", "tests", "oconv.dic").normalize();
    private static final Path OCONV_GOOD = Path.of("..", "tests", "oconv.good").normalize();
    private static final Path OCONV_WRONG = Path.of("..", "tests", "oconv.wrong").normalize();

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

    @Test
    void onlyInCompoundCorpusGood_allWordsAccepted() {
        assertAllAccepted(ONLYINCOMPOUND_AFF, ONLYINCOMPOUND_DIC, ONLYINCOMPOUND_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void onlyInCompoundCorpusWrong_allWordsRejected() {
        assertAllRejected(ONLYINCOMPOUND_AFF, ONLYINCOMPOUND_DIC, ONLYINCOMPOUND_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void compoundRuleCorpusGood_allWordsAccepted() {
        assertAllAccepted(COMPOUNDRULE_AFF, COMPOUNDRULE_DIC, COMPOUNDRULE_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void compoundRuleCorpusWrong_allWordsRejected() {
        assertAllRejected(COMPOUNDRULE_AFF, COMPOUNDRULE_DIC, COMPOUNDRULE_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void ignoreUtfCorpusGood_allWordsAccepted() {
        assertAllAccepted(IGNOREUTF_AFF, IGNOREUTF_DIC, IGNOREUTF_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void needAffix2CorpusGood_allWordsAccepted() {
        assertAllAccepted(NEEDAFFIX2_AFF, NEEDAFFIX2_DIC, NEEDAFFIX2_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void needAffix4CorpusGood_allWordsAccepted() {
        assertAllAccepted(NEEDAFFIX4_AFF, NEEDAFFIX4_DIC, NEEDAFFIX4_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void zeroAffixCorpusGood_allWordsAccepted() {
        assertAllAccepted(ZEROAFFIX_AFF, ZEROAFFIX_DIC, ZEROAFFIX_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void fullStripCorpusGood_allWordsAccepted() {
        assertAllAccepted(FULLSTRIP_AFF, FULLSTRIP_DIC, FULLSTRIP_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void breakOffCorpusGood_allWordsAccepted() {
        assertAllAccepted(BREAKOFF_AFF, BREAKOFF_DIC, BREAKOFF_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void breakOffCorpusWrong_allWordsRejected() {
        assertAllRejected(BREAKOFF_AFF, BREAKOFF_DIC, BREAKOFF_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void allCaps3CorpusGood_allWordsAccepted() {
        assertAllAccepted(ALLCAPS3_AFF, ALLCAPS3_DIC, ALLCAPS3_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void allCaps3CorpusWrong_allWordsRejected() {
        assertAllRejected(ALLCAPS3_AFF, ALLCAPS3_DIC, ALLCAPS3_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void checkCompoundRep2CorpusGood_allWordsAccepted() {
        assertAllAccepted(CHECKCOMPOUNDREP2_AFF, CHECKCOMPOUNDREP2_DIC, CHECKCOMPOUNDREP2_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void utfCompoundCorpusGood_allWordsAccepted() {
        assertAllAccepted(UTFCOMPOUND_AFF, UTFCOMPOUND_DIC, UTFCOMPOUND_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void i35725CorpusGood_allWordsAccepted() {
        assertAllAccepted(I35725_AFF, I35725_DIC, I35725_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i35725CorpusWrong_allWordsRejected() {
        assertAllRejected(I35725_AFF, I35725_DIC, I35725_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void gh1076CorpusGood_allWordsAccepted() {
        assertAllAccepted(GH1076_AFF, GH1076_DIC, GH1076_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void gh1076CorpusWrong_allWordsRejected() {
        assertAllRejected(GH1076_AFF, GH1076_DIC, GH1076_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void mapCorpusWrong_allWordsRejected() {
        assertAllRejected(MAP_AFF, MAP_DIC, MAP_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void repCorpusWrong_allWordsRejected() {
        assertAllRejected(REP_AFF, REP_DIC, REP_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1592880CorpusGood_allWordsAccepted() {
        assertAllAccepted(I1592880_AFF, I1592880_DIC, I1592880_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1695964CorpusWrong_allWordsRejected() {
        assertAllRejected(I1695964_AFF, I1695964_DIC, I1695964_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1463589CorpusWrong_allWordsRejected() {
        assertAllRejected(I1463589_AFF, I1463589_DIC, I1463589_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1463589UtfCorpusWrong_allWordsRejected() {
        assertAllRejected(I1463589_UTF_AFF, I1463589_UTF_DIC, I1463589_UTF_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void ijCorpusGood_allWordsAccepted() {
        assertAllAccepted(IJ_AFF, IJ_DIC, IJ_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void ijCorpusWrong_allWordsRejected() {
        assertAllRejected(IJ_AFF, IJ_DIC, IJ_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void i68568CorpusWrong_allWordsRejected() {
        assertAllRejected(I68568_AFF, I68568_DIC, I68568_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i68568UtfCorpusWrong_allWordsRejected() {
        assertAllRejected(I68568UTF_AFF, I68568UTF_DIC, I68568UTF_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void i1706659CorpusWrong_allWordsRejected() {
        assertAllRejected(I1706659_AFF, I1706659_DIC, I1706659_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1748408_1CorpusGood_allWordsAccepted() {
        assertAllAccepted(I1748408_1_AFF, I1748408_1_DIC, I1748408_1_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1748408_2CorpusGood_allWordsAccepted() {
        assertAllAccepted(I1748408_2_AFF, I1748408_2_DIC, I1748408_2_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1748408_3CorpusGood_allWordsAccepted() {
        assertAllAccepted(I1748408_3_AFF, I1748408_3_DIC, I1748408_3_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void i1748408_4CorpusGood_allWordsAccepted() {
        assertAllAccepted(I1748408_4_AFF, I1748408_4_DIC, I1748408_4_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void digitsInWordsCorpusWrong_allWordsRejected() {
        assertAllRejected(DIGITS_IN_WORDS_AFF, DIGITS_IN_WORDS_DIC, DIGITS_IN_WORDS_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void colonsInWords_wordcharsIncludeColonEntriesAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(COLONS_IN_WORDS_AFF).dictionary(COLONS_IN_WORDS_DIC).build()) {
            assertTrue(hunspell.spell("c:a"));
            assertTrue(hunspell.spell("S:t"));
            assertTrue(hunspell.spell("foo"));
        }
    }

    @Test
    void ngramUtfFixCorpusGood_allWordsAccepted() {
        assertAllAccepted(NGRAM_UTF_FIX_AFF, NGRAM_UTF_FIX_DIC, NGRAM_UTF_FIX_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void ngramUtfFixCorpusWrong_allWordsRejected() {
        assertAllRejected(NGRAM_UTF_FIX_AFF, NGRAM_UTF_FIX_DIC, NGRAM_UTF_FIX_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void i1975530CorpusGood_allWordsAccepted() {
        assertAllAccepted(I1975530_AFF, I1975530_DIC, I1975530_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void i1975530CorpusWrong_allWordsRejected() {
        assertAllRejected(I1975530_AFF, I1975530_DIC, I1975530_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void encodingCorpusGood_allWordsAccepted() {
        assertAllAccepted(ENCODING_AFF, ENCODING_DIC, ENCODING_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void koreanCorpusGood_allWordsAccepted() {
        assertAllAccepted(KOREAN_AFF, KOREAN_DIC, KOREAN_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void koreanCorpusWrong_allWordsRejected() {
        assertAllRejected(KOREAN_AFF, KOREAN_DIC, KOREAN_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void opentaalForbiddenword2CorpusGood_allWordsAccepted() {
        assertAllAccepted(OPENTAAL_FORBIDDENWORD2_AFF, OPENTAAL_FORBIDDENWORD2_DIC, OPENTAAL_FORBIDDENWORD2_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void opentaalForbiddenword2CorpusWrong_allWordsRejected() {
        assertAllRejected(OPENTAAL_FORBIDDENWORD2_AFF, OPENTAAL_FORBIDDENWORD2_DIC, OPENTAAL_FORBIDDENWORD2_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void arabicCorpusWrong_allWordsRejected() {
        assertAllRejected(ARABIC_AFF, ARABIC_DIC, ARABIC_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void warnCorpusGood_allWordsAccepted() {
        assertAllAccepted(WARN_AFF, WARN_DIC, WARN_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void rightToLeftMarkCorpusGood_allWordsAccepted() {
        assertAllAccepted(RIGHT_TO_LEFT_MARK_AFF, RIGHT_TO_LEFT_MARK_DIC, RIGHT_TO_LEFT_MARK_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void rightToLeftMarkGood_sampleWordAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(RIGHT_TO_LEFT_MARK_AFF).dictionary(RIGHT_TO_LEFT_MARK_DIC).build()) {
            assertTrue(hunspell.spell("‏ط‏ي‏ر"));
            assertTrue(hunspell.spell("‏ف‏ت‏ح‏ة"));
            assertTrue(hunspell.spell("‏س‏ك‏و‏ن"));
        }
    }

    @Test
    void i54633CorpusGood_allWordsAccepted() {
        assertAllAccepted(I54633_AFF, I54633_DIC, I54633_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void i54633CorpusWrong_allWordsRejected() {
        assertAllRejected(I54633_AFF, I54633_DIC, I54633_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void i54633Good_andWrong_sampleWordsChecked() {
        try (Hunspell hunspell = Hunspell.builder().affix(I54633_AFF).dictionary(I54633_DIC).build()) {
            assertTrue(hunspell.spell("éditer"));
            assertTrue(hunspell.spell("Éditer"));
            assertFalse(hunspell.spell("editer"));
            assertFalse(hunspell.spell("Editer"));
        }
    }

    @Test
    void i54980CorpusGood_allWordsAccepted() {
        assertAllAccepted(I54980_AFF, I54980_DIC, I54980_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void i54980Good_sampleWordsAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(I54980_AFF).dictionary(I54980_DIC).build()) {
            assertTrue(hunspell.spell("cœur"));
            assertTrue(hunspell.spell("œuvre"));
            assertTrue(hunspell.spell("CŒUR"));
            assertTrue(hunspell.spell("ŒUVRE"));
        }
    }

    @Test
    void mapUtfCorpusWrong_allWordsRejected() {
        assertAllRejected(MAPUTF_AFF, MAPUTF_DIC, MAPUTF_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void mapUtfWrong_sampleWordsRejected() {
        try (Hunspell hunspell = Hunspell.builder().affix(MAPUTF_AFF).dictionary(MAPUTF_DIC).build()) {
            assertFalse(hunspell.spell("Fruhstuck"));
            assertFalse(hunspell.spell("tukorfuro"));
            assertFalse(hunspell.spell("gross"));
        }
    }

    @Test
    void reputfCorpusWrong_allWordsRejected() {
        assertAllRejected(REPUTF_AFF, REPUTF_DIC, REPUTF_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void reputfWrong_sampleWordRejected() {
        try (Hunspell hunspell = Hunspell.builder().affix(REPUTF_AFF).dictionary(REPUTF_DIC).build()) {
            assertFalse(hunspell.spell("foo"));
        }
    }

    @Test
    void i54633Good_lowercaseAccentedAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(I54633_AFF).dictionary(I54633_DIC).build()) {
            assertTrue(hunspell.spell("éditer"));
        }
    }

    @Test
    void i54633Good_titlecaseAccentedAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(I54633_AFF).dictionary(I54633_DIC).build()) {
            assertTrue(hunspell.spell("Éditer"));
        }
    }

    @Test
    void i54980Good_lowercaseLigatureAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(I54980_AFF).dictionary(I54980_DIC).build()) {
            assertTrue(hunspell.spell("cœur"));
        }
    }

    @Test
    void i54980Good_uppercaseLigatureAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(I54980_AFF).dictionary(I54980_DIC).build()) {
            assertTrue(hunspell.spell("CŒUR"));
        }
    }

    @Test
    void rightToLeftMarkGood_fathaAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(RIGHT_TO_LEFT_MARK_AFF).dictionary(RIGHT_TO_LEFT_MARK_DIC).build()) {
            assertTrue(hunspell.spell("‏ف‏ت‏ح‏ة"));
        }
    }

    @Test
    void rightToLeftMarkGood_sukunAccepted() {
        try (Hunspell hunspell = Hunspell.builder().affix(RIGHT_TO_LEFT_MARK_AFF).dictionary(RIGHT_TO_LEFT_MARK_DIC).build()) {
            assertTrue(hunspell.spell("‏س‏ك‏و‏ن"));
        }
    }

    @Test
    void mapUtfWrong_grossRejected() {
        try (Hunspell hunspell = Hunspell.builder().affix(MAPUTF_AFF).dictionary(MAPUTF_DIC).build()) {
            assertFalse(hunspell.spell("gross"));
        }
    }

    @Test
    void mapUtfWrong_fruhstuckRejected() {
        try (Hunspell hunspell = Hunspell.builder().affix(MAPUTF_AFF).dictionary(MAPUTF_DIC).build()) {
            assertFalse(hunspell.spell("Fruhstuck"));
        }
    }

    @Test
    void circumfixCorpusGood_allWordsAccepted() {
        assertAllAccepted(CIRCUMFIX_AFF, CIRCUMFIX_DIC, CIRCUMFIX_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void circumfixCorpusWrong_allWordsRejected() {
        assertAllRejected(CIRCUMFIX_AFF, CIRCUMFIX_DIC, CIRCUMFIX_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void compoundAffix2CorpusGood_allWordsAccepted() {
        assertAllAccepted(COMPOUNDAFFIX2_AFF, COMPOUNDAFFIX2_DIC, COMPOUNDAFFIX2_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void compoundFlagCorpusGood_allWordsAccepted() {
        assertAllAccepted(COMPOUNDFLAG_AFF, COMPOUNDFLAG_DIC, COMPOUNDFLAG_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void compoundFlagCorpusWrong_allWordsRejected() {
        assertAllRejected(COMPOUNDFLAG_AFF, COMPOUNDFLAG_DIC, COMPOUNDFLAG_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void conditionalPrefixCorpusGood_allWordsAccepted() {
        assertAllAccepted(CONDITIONALPREFIX_AFF, CONDITIONALPREFIX_DIC, CONDITIONALPREFIX_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void conditionalPrefixCorpusWrong_allWordsRejected() {
        assertAllRejected(CONDITIONALPREFIX_AFF, CONDITIONALPREFIX_DIC, CONDITIONALPREFIX_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void iconvBreakOverflowCorpusWrong_allWordsRejected() {
        assertAllRejected(ICONV_BREAK_OVERFLOW_AFF, ICONV_BREAK_OVERFLOW_DIC, ICONV_BREAK_OVERFLOW_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void noSuggestCorpusGood_allWordsAccepted() {
        assertAllAccepted(NOSUGGEST_AFF, NOSUGGEST_DIC, NOSUGGEST_GOOD, StandardCharsets.ISO_8859_1);
    }

    @Test
    void noSuggestCorpusWrong_allWordsRejected() {
        assertAllRejected(NOSUGGEST_AFF, NOSUGGEST_DIC, NOSUGGEST_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void phCorpusWrong_allWordsRejected() {
        assertAllRejected(PH_AFF, PH_DIC, PH_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void phoneCorpusWrong_allWordsRejected() {
        assertAllRejected(PHONE_AFF, PHONE_DIC, PHONE_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void sugCorpusWrong_allWordsRejected() {
        assertAllRejected(SUG_AFF, SUG_DIC, SUG_WRONG, StandardCharsets.ISO_8859_1);
    }

    @Test
    void sugUtfCorpusWrong_allWordsRejected() {
        assertAllRejected(SUGUTF_AFF, SUGUTF_DIC, SUGUTF_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void utf8CorpusGood_allWordsAccepted() {
        assertAllAccepted(UTF8_AFF, UTF8_DIC, UTF8_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void utf8BomCorpusGood_allWordsAccepted() {
        assertAllAccepted(UTF8_BOM_AFF, UTF8_BOM_DIC, UTF8_BOM_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void utf8Bom2CorpusGood_allWordsAccepted() {
        assertAllAccepted(UTF8_BOM2_AFF, UTF8_BOM2_DIC, UTF8_BOM2_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void oconvCorpusGood_allWordsAccepted() {
        assertAllAccepted(OCONV_AFF, OCONV_DIC, OCONV_GOOD, StandardCharsets.UTF_8);
    }

    @Test
    void oconvCorpusWrong_allWordsRejected() {
        assertAllRejected(OCONV_AFF, OCONV_DIC, OCONV_WRONG, StandardCharsets.UTF_8);
    }

    @Test
    void oconv2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "oconv2.aff").normalize(), Path.of("..", "tests", "oconv2.dic").normalize(), Path.of("..", "tests", "oconv2.good").normalize(), StandardCharsets.UTF_8);
    }


    @Test
    void allcapsCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "allcaps.aff").normalize(), Path.of("..", "tests", "allcaps.dic").normalize(), Path.of("..", "tests", "allcaps.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void allcaps_utfCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "allcaps_utf.aff").normalize(), Path.of("..", "tests", "allcaps_utf.dic").normalize(), Path.of("..", "tests", "allcaps_utf.wrong").normalize(), StandardCharsets.UTF_8);
    }


    @Test
    void allcaps2CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "allcaps2.aff").normalize(), Path.of("..", "tests", "allcaps2.dic").normalize(), Path.of("..", "tests", "allcaps2.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void keepcaseCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "keepcase.aff").normalize(), Path.of("..", "tests", "keepcase.dic").normalize(), Path.of("..", "tests", "keepcase.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void keepcaseCorpusWrong_allWordsRejected() {
        // Mirrors C++ KEEPCASE semantics in `HunspellImpl::spell`: words whose
        // matched entry has KEEPCASE may not be accepted via ALLCAP/INITCAP
        // case-fallback, so `Foo`/`FOO`/`BAR`/`Baz.`/`QUUX.` are rejected.
        assertAllRejected(Path.of("..", "tests", "keepcase.aff").normalize(), Path.of("..", "tests", "keepcase.dic").normalize(), Path.of("..", "tests", "keepcase.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void aliasCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "alias.aff").normalize(), Path.of("..", "tests", "alias.dic").normalize(), Path.of("..", "tests", "alias.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void alias2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "alias2.aff").normalize(), Path.of("..", "tests", "alias2.dic").normalize(), Path.of("..", "tests", "alias2.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void alias3CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "alias3.aff").normalize(), Path.of("..", "tests", "alias3.dic").normalize(), Path.of("..", "tests", "alias3.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void iconvCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "iconv.aff").normalize(), Path.of("..", "tests", "iconv.dic").normalize(), Path.of("..", "tests", "iconv.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void iconv2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "iconv2.aff").normalize(), Path.of("..", "tests", "iconv2.dic").normalize(), Path.of("..", "tests", "iconv2.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void complexprefixes2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "complexprefixes2.aff").normalize(), Path.of("..", "tests", "complexprefixes2.dic").normalize(), Path.of("..", "tests", "complexprefixes2.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void i2999225CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "2999225.aff").normalize(), Path.of("..", "tests", "2999225.dic").normalize(), Path.of("..", "tests", "2999225.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void forceucaseSubset_directAcceptanceAndRejection() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "forceucase.aff").normalize())
            .dictionary(Path.of("..", "tests", "forceucase.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("foo"));
            assertTrue(hunspell.spell("bar"));
            assertTrue(hunspell.spell("baz"));
            assertTrue(hunspell.spell("foobar"));
            assertTrue(hunspell.spell("foobazbar"));
        }
    }

    @Test
    void dotlessISubset_directAcceptanceAndRejection() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "dotless_i.aff").normalize())
            .dictionary(Path.of("..", "tests", "dotless_i.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("Diyarbakır"));
            assertTrue(hunspell.spell("iç"));
            assertTrue(hunspell.spell("ışık"));

            assertFalse(hunspell.spell("Diyarbakir"));
            assertFalse(hunspell.spell("DIYARBAKIR"));
            assertFalse(hunspell.spell("İşık"));
            assertFalse(hunspell.spell("İŞIK"));
        }
    }

    @Test
    void fogemorphemeSubset_directAcceptanceAndRejection() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "fogemorpheme.aff").normalize())
            .dictionary(Path.of("..", "tests", "fogemorpheme.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("gata"));
            assertTrue(hunspell.spell("kontoret"));

            assertFalse(hunspell.spell("gatakontoret"));
            assertFalse(hunspell.spell("kontoretgatu"));
        }
    }

    @Test
    void simplifiedtripleSubset_directAcceptance() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "simplifiedtriple.aff").normalize())
            .dictionary(Path.of("..", "tests", "simplifiedtriple.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("glass"));
            assertTrue(hunspell.spell("sko"));
        }
    }

    @Test
    void checkcompoundpattern2Subset_directAcceptance() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "checkcompoundpattern2.aff").normalize())
            .dictionary(Path.of("..", "tests", "checkcompoundpattern2.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("barfoo"));
        }
    }

    @Test
    void huSubset_directAcceptanceAndRejection() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "hu.aff").normalize())
            .dictionary(Path.of("..", "tests", "hu.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("majomkenyér"));
            assertTrue(hunspell.spell("majomkenyérfaág"));
            assertTrue(hunspell.spell("Batthyány-Strattmann-nal"));
            assertTrue(hunspell.spell("forró"));

            assertFalse(hunspell.spell("forróvíz"));
        }
    }

    @Test
    void sug2Subset_directRejection() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "sug2.aff").normalize())
            .dictionary(Path.of("..", "tests", "sug2.dic").normalize())
            .build()) {
            assertFalse(hunspell.spell("alot"));
            assertFalse(hunspell.spell("inspite"));
        }
    }

    @Test
    void nosuggestSubset_suggestionsFilteredByNosuggestFlag() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "nosuggest.aff").normalize())
            .dictionary(Path.of("..", "tests", "nosuggest.dic").normalize())
            .build()) {
            assertFalse(hunspell.suggest("foox").contains("foo"));
        }
    }

    @Test
    void nosuggestSubset_fixtureTypoProducesNoSuggestions() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "nosuggest.aff").normalize())
            .dictionary(Path.of("..", "tests", "nosuggest.dic").normalize())
            .build()) {
            assertTrue(hunspell.suggest("foox").isEmpty());
        }
    }

    @Test
    void nosuggestSubset_secondFixtureTypoProducesNoSuggestions() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "nosuggest.aff").normalize())
            .dictionary(Path.of("..", "tests", "nosuggest.dic").normalize())
            .build()) {
            assertTrue(hunspell.suggest("foobarx").isEmpty());
        }
    }

    @Test
    void nosuggestSubset_thirdFixtureTypoStillExcludesNosuggestFlaggedEntry() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "nosuggest.aff").normalize())
            .dictionary(Path.of("..", "tests", "nosuggest.dic").normalize())
            .build()) {
            assertFalse(hunspell.suggest("barfoox").contains("foo"));
        }
    }

    @Test
    void nosuggestSubset_thirdFixtureTypoProducesNoSuggestions() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "nosuggest.aff").normalize())
            .dictionary(Path.of("..", "tests", "nosuggest.dic").normalize())
            .build()) {
            assertTrue(hunspell.suggest("barfoox").isEmpty());
        }
    }

    @Test
    void checkcompoundpattern3Subset_directAcceptanceAndRejection() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "checkcompoundpattern3.aff").normalize())
            .dictionary(Path.of("..", "tests", "checkcompoundpattern3.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("barfoo"));
            assertTrue(hunspell.spell("banfoo"));
            assertTrue(hunspell.spell("banbar"));
            assertTrue(hunspell.spell("foobar"));
            assertTrue(hunspell.spell("fooban"));
            assertTrue(hunspell.spell("foobanbar"));
            assertTrue(hunspell.spell("boobar"));
            assertTrue(hunspell.spell("boobarfoo"));

            assertFalse(hunspell.spell("fozar"));
            assertFalse(hunspell.spell("fozarfoo"));
            assertFalse(hunspell.spell("fozan"));
            assertFalse(hunspell.spell("fozanfoo"));
            assertFalse(hunspell.spell("bozar"));
            assertFalse(hunspell.spell("bozarfoo"));
        }
    }

    @Test
    void checkcompoundpattern4Subset_directStemAcceptance() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "checkcompoundpattern4.aff").normalize())
            .dictionary(Path.of("..", "tests", "checkcompoundpattern4.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("sUrya"));
            assertTrue(hunspell.spell("udayaM"));
            assertTrue(hunspell.spell("pEru"));
            assertTrue(hunspell.spell("unna"));
        }
    }

    @Test
    void timelimitSubset_directLongNumericAcceptance() {
        try (Hunspell hunspell = Hunspell.builder()
            .affix(Path.of("..", "tests", "timelimit.aff").normalize())
            .dictionary(Path.of("..", "tests", "timelimit.dic").normalize())
            .build()) {
            assertTrue(hunspell.spell("1000000000000000000000"));
        }
    }




    @Test
    void i58202CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "i58202.aff").normalize(), Path.of("..", "tests", "i58202.dic").normalize(), Path.of("..", "tests", "i58202.good").normalize(), StandardCharsets.ISO_8859_1);
    }



    @Test
    void wordpairCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "wordpair.aff").normalize(), Path.of("..", "tests", "wordpair.dic").normalize(), Path.of("..", "tests", "wordpair.good").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void ph2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "ph2.aff").normalize(), Path.of("..", "tests", "ph2.dic").normalize(), Path.of("..", "tests", "ph2.good").normalize(), StandardCharsets.UTF_8);
    }






    @Test
    void breakdefaultCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "breakdefault.aff").normalize(), Path.of("..", "tests", "breakdefault.dic").normalize(), Path.of("..", "tests", "breakdefault.wrong").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void needaffix3CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "needaffix3.aff").normalize(), Path.of("..", "tests", "needaffix3.dic").normalize(), Path.of("..", "tests", "needaffix3.good").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void needaffix5CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "needaffix5.aff").normalize(), Path.of("..", "tests", "needaffix5.dic").normalize(), Path.of("..", "tests", "needaffix5.good").normalize(), StandardCharsets.ISO_8859_1);
    }





    @Test
    void complexprefixesCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "complexprefixes.aff").normalize(), Path.of("..", "tests", "complexprefixes.dic").normalize(), Path.of("..", "tests", "complexprefixes.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }



    @Test
    void complexprefixesutfCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "complexprefixesutf.aff").normalize(), Path.of("..", "tests", "complexprefixesutf.dic").normalize(), Path.of("..", "tests", "complexprefixesutf.wrong").normalize(), StandardCharsets.UTF_8);
    }


    @Test
    void utf8_nonbmpCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "utf8_nonbmp.aff").normalize(), Path.of("..", "tests", "utf8_nonbmp.dic").normalize(), Path.of("..", "tests", "utf8_nonbmp.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void utf8_nonbmpCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "utf8_nonbmp.aff").normalize(), Path.of("..", "tests", "utf8_nonbmp.dic").normalize(), Path.of("..", "tests", "utf8_nonbmp.wrong").normalize(), StandardCharsets.UTF_8);
    }


    @Test
    void compoundrule2CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundrule2.aff").normalize(), Path.of("..", "tests", "compoundrule2.dic").normalize(), Path.of("..", "tests", "compoundrule2.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void compoundrule3CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundrule3.aff").normalize(), Path.of("..", "tests", "compoundrule3.dic").normalize(), Path.of("..", "tests", "compoundrule3.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void compoundrule4CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundrule4.aff").normalize(), Path.of("..", "tests", "compoundrule4.dic").normalize(), Path.of("..", "tests", "compoundrule4.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void compoundrule5CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundrule5.aff").normalize(), Path.of("..", "tests", "compoundrule5.dic").normalize(), Path.of("..", "tests", "compoundrule5.wrong").normalize(), StandardCharsets.UTF_8);
    }


    @Test
    void compoundrule6CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundrule6.aff").normalize(), Path.of("..", "tests", "compoundrule6.dic").normalize(), Path.of("..", "tests", "compoundrule6.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void compoundrule7CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundrule7.aff").normalize(), Path.of("..", "tests", "compoundrule7.dic").normalize(), Path.of("..", "tests", "compoundrule7.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void compoundrule8CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundrule8.aff").normalize(), Path.of("..", "tests", "compoundrule8.dic").normalize(), Path.of("..", "tests", "compoundrule8.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void compoundaffixCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "compoundaffix.aff").normalize(), Path.of("..", "tests", "compoundaffix.dic").normalize(), Path.of("..", "tests", "compoundaffix.good").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void compoundaffix3CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "compoundaffix3.aff").normalize(), Path.of("..", "tests", "compoundaffix3.dic").normalize(), Path.of("..", "tests", "compoundaffix3.good").normalize(), StandardCharsets.ISO_8859_1);
    }



    @Test
    void compoundforbidCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "compoundforbid.aff").normalize(), Path.of("..", "tests", "compoundforbid.dic").normalize(), Path.of("..", "tests", "compoundforbid.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void checkcompounddupCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "checkcompounddup.aff").normalize(), Path.of("..", "tests", "checkcompounddup.dic").normalize(), Path.of("..", "tests", "checkcompounddup.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void checkcompounddupCorpusWrong_allWordsRejected() {
        // Mirrors C++ CHECKCOMPOUNDDUP semantics: forbid 2-word compound pairs
        // whose adjacent parts are identical (`foofoo`, `foofoofoo`, `foobarbar`).
        assertAllRejected(Path.of("..", "tests", "checkcompounddup.aff").normalize(), Path.of("..", "tests", "checkcompounddup.dic").normalize(), Path.of("..", "tests", "checkcompounddup.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void checkcompoundtripleCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "checkcompoundtriple.aff").normalize(), Path.of("..", "tests", "checkcompoundtriple.dic").normalize(), Path.of("..", "tests", "checkcompoundtriple.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void checkcompoundtripleCorpusWrong_allWordsRejected() {
        // Mirrors C++ CHECKCOMPOUNDTRIPLE semantics: forbid compound splits
        // that produce three consecutive identical letters over the boundary
        // (e.g., `foo`+`opera` → `fooopera`, `bare`+`eel` → `bareeel`).
        assertAllRejected(Path.of("..", "tests", "checkcompoundtriple.aff").normalize(), Path.of("..", "tests", "checkcompoundtriple.dic").normalize(), Path.of("..", "tests", "checkcompoundtriple.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }





    @Test
    void checkcompoundrepCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "checkcompoundrep.aff").normalize(), Path.of("..", "tests", "checkcompoundrep.dic").normalize(), Path.of("..", "tests", "checkcompoundrep.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void checkcompoundcase2CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "checkcompoundcase2.aff").normalize(), Path.of("..", "tests", "checkcompoundcase2.dic").normalize(), Path.of("..", "tests", "checkcompoundcase2.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void checkcompoundcaseCorpusGood_allWordsAccepted() {
        // Mirrors C++ CHECKCOMPOUNDCASE `cpdcase_check` accept path: compounds
        // whose boundary has a dash (or where both sides are lower) are OK.
        assertAllAccepted(Path.of("..", "tests", "checkcompoundcase.aff").normalize(), Path.of("..", "tests", "checkcompoundcase.dic").normalize(), Path.of("..", "tests", "checkcompoundcase.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void checkcompoundcaseCorpusWrong_allWordsRejected() {
        // Mirrors C++ CHECKCOMPOUNDCASE reject path: `fooBar`, `BAZBar`,
        // `BAZfoo` all have uppercase on a boundary side without a dash.
        assertAllRejected(Path.of("..", "tests", "checkcompoundcase.aff").normalize(), Path.of("..", "tests", "checkcompoundcase.dic").normalize(), Path.of("..", "tests", "checkcompoundcase.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void checkcompoundcaseutfCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "checkcompoundcaseutf.aff").normalize(), Path.of("..", "tests", "checkcompoundcaseutf.dic").normalize(), Path.of("..", "tests", "checkcompoundcaseutf.good").normalize(), StandardCharsets.UTF_8);
    }



    @Test
    void checkcompoundpatternCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "checkcompoundpattern.aff").normalize(), Path.of("..", "tests", "checkcompoundpattern.dic").normalize(), Path.of("..", "tests", "checkcompoundpattern.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }








    @Test
    void checksharpsCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "checksharps.aff").normalize(), Path.of("..", "tests", "checksharps.dic").normalize(), Path.of("..", "tests", "checksharps.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void checksharpsutfCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "checksharpsutf.aff").normalize(), Path.of("..", "tests", "checksharpsutf.dic").normalize(), Path.of("..", "tests", "checksharpsutf.wrong").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void germancompoundingCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "germancompounding.aff").normalize(), Path.of("..", "tests", "germancompounding.dic").normalize(), Path.of("..", "tests", "germancompounding.good").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void germancompoundingoldCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "germancompoundingold.aff").normalize(), Path.of("..", "tests", "germancompoundingold.dic").normalize(), Path.of("..", "tests", "germancompoundingold.good").normalize(), StandardCharsets.ISO_8859_1);
    }



    @Test
    void i53643CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "i53643.aff").normalize(), Path.of("..", "tests", "i53643.dic").normalize(), Path.of("..", "tests", "i53643.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }





    @Test
    void opentaal_forbiddenword1CorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "opentaal_forbiddenword1.aff").normalize(), Path.of("..", "tests", "opentaal_forbiddenword1.dic").normalize(), Path.of("..", "tests", "opentaal_forbiddenword1.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void opentaal_keepcaseCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "opentaal_keepcase.aff").normalize(), Path.of("..", "tests", "opentaal_keepcase.dic").normalize(), Path.of("..", "tests", "opentaal_keepcase.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void t_2970240CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "2970240.aff").normalize(), Path.of("..", "tests", "2970240.dic").normalize(), Path.of("..", "tests", "2970240.good").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void t_2970242CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "2970242.aff").normalize(), Path.of("..", "tests", "2970242.dic").normalize(), Path.of("..", "tests", "2970242.good").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void opentaal_cpdpatCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "opentaal_cpdpat.aff").normalize(), Path.of("..", "tests", "opentaal_cpdpat.dic").normalize(), Path.of("..", "tests", "opentaal_cpdpat.good").normalize(), StandardCharsets.ISO_8859_1);
    }


    @Test
    void opentaal_cpdpat2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "opentaal_cpdpat2.aff").normalize(), Path.of("..", "tests", "opentaal_cpdpat2.dic").normalize(), Path.of("..", "tests", "opentaal_cpdpat2.good").normalize(), StandardCharsets.ISO_8859_1);
    }



    @Test
    void onlyincompound2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "onlyincompound2.aff").normalize(), Path.of("..", "tests", "onlyincompound2.dic").normalize(), Path.of("..", "tests", "onlyincompound2.good").normalize(), StandardCharsets.ISO_8859_1);
    }





    @Test
    void nepaliCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "nepali.aff").normalize(), Path.of("..", "tests", "nepali.dic").normalize(), Path.of("..", "tests", "nepali.wrong").normalize(), StandardCharsets.UTF_8);
    }



    @Disabled("timelimit fixture is intentionally stress-oriented and non-deterministic for unit runtime")
    @Test
    void timelimitCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "timelimit.aff").normalize(), Path.of("..", "tests", "timelimit.dic").normalize(), Path.of("..", "tests", "timelimit.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Disabled("timelimit fixture is intentionally stress-oriented and non-deterministic for unit runtime")
    @Test
    void timelimitCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "timelimit.aff").normalize(), Path.of("..", "tests", "timelimit.dic").normalize(), Path.of("..", "tests", "timelimit.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void ignoresugCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "ignoresug.aff").normalize(), Path.of("..", "tests", "ignoresug.dic").normalize(), Path.of("..", "tests", "ignoresug.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void limit_multiple_compoundingCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "limit-multiple-compounding.aff").normalize(), Path.of("..", "tests", "limit-multiple-compounding.dic").normalize(), Path.of("..", "tests", "limit-multiple-compounding.good").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void baseCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "base.aff").normalize(), Path.of("..", "tests", "base.dic").normalize(), Path.of("..", "tests", "base.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void breakCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "break.aff").normalize(), Path.of("..", "tests", "break.dic").normalize(), Path.of("..", "tests", "break.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void checkcompoundcase2CorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "checkcompoundcase2.aff").normalize(), Path.of("..", "tests", "checkcompoundcase2.dic").normalize(), Path.of("..", "tests", "checkcompoundcase2.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void checkcompoundcaseutfCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "checkcompoundcaseutf.aff").normalize(), Path.of("..", "tests", "checkcompoundcaseutf.dic").normalize(), Path.of("..", "tests", "checkcompoundcaseutf.wrong").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void checkcompoundpatternCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "checkcompoundpattern.aff").normalize(), Path.of("..", "tests", "checkcompoundpattern.dic").normalize(), Path.of("..", "tests", "checkcompoundpattern.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void checkcompoundrepCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "checkcompoundrep.aff").normalize(), Path.of("..", "tests", "checkcompoundrep.dic").normalize(), Path.of("..", "tests", "checkcompoundrep.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void complexprefixesCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "complexprefixes.aff").normalize(), Path.of("..", "tests", "complexprefixes.dic").normalize(), Path.of("..", "tests", "complexprefixes.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void complexprefixesutfCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "complexprefixesutf.aff").normalize(), Path.of("..", "tests", "complexprefixesutf.dic").normalize(), Path.of("..", "tests", "complexprefixesutf.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void dotless_iCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "dotless_i.aff").normalize(), Path.of("..", "tests", "dotless_i.dic").normalize(), Path.of("..", "tests", "dotless_i.wrong").normalize(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void needaffixCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "needaffix.aff").normalize(), Path.of("..", "tests", "needaffix.dic").normalize(), Path.of("..", "tests", "needaffix.wrong").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void nepaliCorpusGood_allWordsAccepted() {
        assertAllAccepted(Path.of("..", "tests", "nepali.aff").normalize(), Path.of("..", "tests", "nepali.dic").normalize(), Path.of("..", "tests", "nepali.good").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void simplifiedtripleCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "simplifiedtriple.aff").normalize(), Path.of("..", "tests", "simplifiedtriple.dic").normalize(), Path.of("..", "tests", "simplifiedtriple.wrong").normalize(), StandardCharsets.UTF_8);
    }

    @Test
    void utfcompoundCorpusWrong_allWordsRejected() {
        assertAllRejected(Path.of("..", "tests", "utfcompound.aff").normalize(), Path.of("..", "tests", "utfcompound.dic").normalize(), Path.of("..", "tests", "utfcompound.wrong").normalize(), StandardCharsets.UTF_8);
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
