# C++ vs Java implementation gap analysis (re-analysis: 2026-04-18)

This pass focuses on **algorithmic fidelity gaps** where the Java port still uses
simplified or hardcoded behavior instead of the C++ Hunspell control flow.

## High-impact hardcoded / non-parity behavior

## 1) Suggestion pipeline is still heuristic, not `SuggestMgr`

**Where:** `SimpleHunspell.suggest`, `suggestionAlphabet`, `edits`, `distance`, `suffixSuggest`.

- Java currently generates only one-edit candidates (delete/transpose/replace/insert)
  from an alphabet built from dictionary stems, then ranks by Levenshtein distance +
  lexical tie-break.
- C++ uses staged suggestion logic (`SuggestMgr`) with REP/MAP/PHONE, capitalization
  transforms, n-gram scoring, and time-limit gating.
- `suffixSuggest` is currently a `startsWith` + sort helper, not an affix/suggestion parity path.

**Why this is hardcoded:** ranking and recall are determined by a Java-specific heuristic,
not by C++ algorithm stages.

## 2) Compound validation is partially modeled with a reduced rule set

**Where:** `SimpleHunspell.compoundCheck*`, `sequenceMatchesRules`, `sequenceMatchesBeginEnd`.

- Java implements a recursive splitter with `COMPOUNDMIN`, `COMPOUNDRULE`, plus select checks
  (`CHECKCOMPOUNDDUP/TRIPLE/CASE`) and a begin/end fallback.
- C++ compound acceptance includes broader directive interactions and more nuanced boundary/path
  checks in `AffixMgr::compound_check`.

**Why this is hardcoded:** the Java branch encodes a narrowed acceptance model that matches many
fixtures but does not yet port all data-driven C++ checks.

## 3) Case handling is simplified and locale-agnostic

**Where:** `SimpleHunspell.isTitleCase`, `isAllUpper`, `capitalize`, `resolveInfo` case ladder.

- Java uses basic Unicode char-case checks and `Locale.ROOT` lower/capitalize fallback.
- C++ relies on `csutil` language-aware and dictionary-driven casing behavior.

**Why this is hardcoded:** casing outcomes are based on generic Java transformations rather than
Hunspell's language/encoding-sensitive casing pipeline.

## 4) BREAK recursion logic is reduced vs C++ branching

**Where:** `SimpleHunspell.spellBreak`, `resolveStemForBreak`.

- Java mirrors core recursion but uses simplified anchor handling and direct early exits.
- C++ break handling has deeper integration with spell state/info propagation and branch behavior.

**Why this is hardcoded:** behavior is approximated in Java helper methods instead of porting the
full C++ branch structure and info-bit flow.

## 5) Affix parser coverage is selective (many directives still absent)

**Where:** `AffixManager.parseBody`.

- Java parses core directives used by many current fixtures.
- Numerous C++ directives and interactions remain unported (notably suggestion- and
  compound-related controls beyond the currently handled subset).

**Why this is hardcoded:** current runtime behavior depends on a selected directive subset rather
than the full C++ parser/feature matrix.

## 6) API behavior contains compatibility stubs

**Where:** `SimpleHunspell.BuilderImpl.key`, `strictAffixParsing`, `info`.

- `key(...)` and `strictAffixParsing(...)` are currently no-op pass-throughs.
- `info()` currently returns hardcoded `("java-port-dev", 0)` metadata portions.

**Why this is hardcoded:** these are placeholders rather than parity implementations.

## 7) Morph generation path is simplified and brute-force

**Where:** `SimpleHunspell.generate2`.

- Java scans all dictionary entries and matches morphology by string containment.
- C++ generation/stemming flows operate through richer morph/flag pathways.

**Why this is hardcoded:** algorithm uses global scan + direct token matching instead of the C++
model-driven generation path.

---

## Priority closure order (fidelity-first)

1. Port C++ `suggest`/`SuggestMgr` staged flow and retire Levenshtein-only ranking.
2. Expand parser/runtime directive coverage for remaining compound and suggestion controls.
3. Port `csutil`-equivalent casing path and remove Locale-root fallback assumptions.
4. Align BREAK and spell-info propagation with C++ branching semantics.
5. Replace API stubs (`key`, `strictAffixParsing`, metadata placeholders) with real behavior.
6. Rework `generate2` toward C++ morphology/flag-driven generation.
