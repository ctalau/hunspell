package org.hunspell;

import java.nio.file.Path;
import java.util.List;

public interface Hunspell extends AutoCloseable {
    static Builder builder() {
        return new SimpleHunspell.BuilderImpl();
    }

    boolean spell(String word);

    SpellResult check(String word);

    List<String> suggest(String word);

    List<String> suffixSuggest(String rootWord);

    int addDictionary(Path dicPath);

    List<String> analyze(String word);

    List<String> stem(String word);

    List<String> generate(String word, String modelWord);

    List<String> generate2(String word, List<String> morphDescriptions);

    void add(String word);

    void addWithAffix(String word, String modelWord);

    void remove(String word);

    DictionaryInfo info();

    @Override
    void close();

    interface Builder {
        Builder affix(Path affPath);

        Builder dictionary(Path dicPath);

        Builder addDictionary(Path dicPath);

        Builder key(String key);

        Builder strictAffixParsing(boolean strict);

        Builder maxSuggestions(int max);

        Hunspell build();
    }
}
