package org.hunspell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class HunspellBootstrapTest {

    @Test
    void spellAcceptsExactDictionaryEntry() throws IOException {
        Path dictionary = writeDictionary("2", "hello", "world");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            assertTrue(hunspell.spell("hello"));
        }
    }

    @Test
    void spellRejectsMissingWord() throws IOException {
        Path dictionary = writeDictionary("1", "hello");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            assertFalse(hunspell.spell("unknown"));
        }
    }

    @Test
    void addDictionaryLoadsAdditionalWords() throws IOException {
        Path dictionary = writeDictionary("1", "hello");
        Path extra = writeDictionary("1", "world");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            int added = hunspell.addDictionary(extra);
            assertEquals(1, added);
            assertTrue(hunspell.spell("world"));
        }
    }

    @Test
    void suggestRespectsMaxSuggestionsAndStableOrder() throws IOException {
        Path dictionary = writeDictionary("5", "cart", "cat", "cast", "coat", "dog");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).maxSuggestions(3).build()) {
            List<String> suggestions = hunspell.suggest("caat");
            assertEquals(3, suggestions.size());
            assertIterableEquals(List.of("cart", "cast", "cat"), suggestions);
        }
    }

    @Test
    void suffixSuggestReturnsDerivedWordsForRootPrefix() throws IOException {
        Path dictionary = writeDictionary("4", "run", "runner", "running", "outrun");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            assertIterableEquals(List.of("run", "runner", "running"), hunspell.suffixSuggest("run"));
        }
    }

    @Test
    void checkExposesSpellResultShape() throws IOException {
        Path dictionary = writeDictionary("1", "hello");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            SpellResult result = hunspell.check("hello");
            assertTrue(result.correct());
            assertFalse(result.compound());
            assertFalse(result.forbidden());
            assertEquals("hello", result.root().orElseThrow());
        }
    }

    @Test
    void checkReturnsRootForGeneratedAffixWord() {
        Path affix = Path.of("..", "tests", "condition.aff").normalize();
        Path dictionary = Path.of("..", "tests", "condition.dic").normalize();

        try (Hunspell hunspell = Hunspell.builder().affix(affix).dictionary(dictionary).build()) {
            SpellResult result = hunspell.check("entertaining");
            assertTrue(result.correct());
            assertEquals("entertain", result.root().orElseThrow());
        }
    }

    @Test
    void dictionaryParsingSupportsEscapedSlashEntries() throws IOException {
        Path dictionary = writeDictionary("3", "1\\/2", "http:\\/\\/", "\\/usr\\/share\\/myspell\\/");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            assertTrue(hunspell.spell("1/2"));
            assertTrue(hunspell.spell("http://"));
            assertTrue(hunspell.spell("/usr/share/myspell/"));
        }
    }

    @Test
    void dictionaryParsingIgnoresFlagsAndMorphologicalFields() throws IOException {
        Path dictionary = writeDictionary("1", "created/U\tst:created");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            assertTrue(hunspell.spell("created"));
            assertFalse(hunspell.spell("created/U"));
        }
    }

    @Test
    void dictionaryParsingIgnoresCommentLines() throws IOException {
        Path dictionary = writeDictionary("3", "# comment", "hello", "# another");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            assertTrue(hunspell.spell("hello"));
            assertFalse(hunspell.spell("# comment"));
        }
    }

    @Test
    void affixConditionsGenerateExpectedSuffixAndPrefixForms() {
        Path affix = Path.of("..", "tests", "condition.aff").normalize();
        Path dictionary = Path.of("..", "tests", "condition.dic").normalize();

        try (Hunspell hunspell = Hunspell.builder().affix(affix).dictionary(dictionary).build()) {
            assertTrue(hunspell.spell("ofosuf1"));
            assertTrue(hunspell.spell("pre1ofo"));
            assertTrue(hunspell.spell("entertaining"));
            assertTrue(hunspell.spell("wries"));
            assertTrue(hunspell.spell("unwry"));

            assertFalse(hunspell.spell("ofosuf4"));
            assertFalse(hunspell.spell("pre4ofo"));
            assertFalse(hunspell.spell("entertainning"));
        }
    }

    @Test
    void infoReadsEncodingAndWordCharsFromAffixFile() {
        Path affix = Path.of("..", "tests", "condition.aff").normalize();
        Path dictionary = Path.of("..", "tests", "condition.dic").normalize();

        try (Hunspell hunspell = Hunspell.builder().affix(affix).dictionary(dictionary).build()) {
            DictionaryInfo info = hunspell.info();
            assertEquals("ISO8859-2", info.encoding());
            assertEquals("0123456789", info.wordCharacters());
        }
    }

    @Test
    void analyzeReturnsMorphologyForDirectAndDerivedWords() {
        Path affix = Path.of("..", "tests", "morph.aff").normalize();
        Path dictionary = Path.of("..", "tests", "morph.dic").normalize();

        try (Hunspell hunspell = Hunspell.builder().affix(affix).dictionary(dictionary).build()) {
            assertIterableEquals(List.of("po:noun", "po:verb al:drank al:drunk ts:present"), hunspell.analyze("drink"));
            assertIterableEquals(List.of("po:noun"), hunspell.analyze("drinks"));
        }
    }

    @Test
    void stemReturnsRootForAffixedWord() {
        Path affix = Path.of("..", "tests", "morph.aff").normalize();
        Path dictionary = Path.of("..", "tests", "morph.dic").normalize();

        try (Hunspell hunspell = Hunspell.builder().affix(affix).dictionary(dictionary).build()) {
            assertIterableEquals(List.of("drink"), hunspell.stem("drinks"));
        }
    }

    @Test
    void generateUsesModelFlagsToProduceInflections() {
        Path affix = Path.of("..", "tests", "morph.aff").normalize();
        Path dictionary = Path.of("..", "tests", "morph.dic").normalize();

        try (Hunspell hunspell = Hunspell.builder().affix(affix).dictionary(dictionary).build()) {
            List<String> generated = hunspell.generate("walk", "drink");
            assertTrue(generated.contains("walk"));
            assertTrue(generated.contains("walks"));
            assertTrue(generated.contains("walkable"));
        }
    }

    @Test
    void runtimeMutationSupportsAddAddWithAffixAndRemove() throws IOException {
        Path dictionary = writeDictionary("1", "hello");

        try (Hunspell hunspell = Hunspell.builder().dictionary(dictionary).build()) {
            hunspell.add("world");
            assertTrue(hunspell.spell("world"));

            hunspell.addWithAffix("copy", "hello");
            assertTrue(hunspell.spell("copy"));

            hunspell.remove("hello");
            assertFalse(hunspell.spell("hello"));
        }
    }

    private static Path writeDictionary(String... lines) throws IOException {
        Path file = Files.createTempFile("hunspell-java", ".dic");
        Files.write(file, String.join(System.lineSeparator(), lines).concat(System.lineSeparator()).getBytes());
        file.toFile().deleteOnExit();
        return file;
    }
}
