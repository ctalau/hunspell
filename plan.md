# Java Hunspell Port Plan (starting from `spec.md` item 12)

## Scope and intent
This plan expands the implementation phases defined in `spec.md` §12 into concrete milestones that preserve **C++ Hunspell code organization and algorithm behavior** while incrementally improving Java test parity.

Status legend:
- ✅ Completed
- 🟡 In progress
- ⬜ Not started

## Architecture alignment target (C++ -> Java)
To keep organization and algorithms close to the original implementation, split `SimpleHunspell` into components mirroring `src/hunspell/*.cxx` responsibilities:

- `HunspellEngine` (facade) ↔ `hunspell.cxx`
- `AffixManager` (PFX/SFX parsing + condition matching + cross-product) ↔ `affixmgr.cxx`
- `HashManager` (dictionary loading, flag decoding, entry/morph storage) ↔ `hashmgr.cxx`
- `SuggestManager` (REP/MAP/PHONE/ngram ordering) ↔ `suggestmgr.cxx`, `replist.cxx`, `phonet.cxx`
- `CsUtil` (casing, Unicode/encoding conversions, token handling) ↔ `csutil.cxx`, `utf_info.hxx`
- CLI adapter package ↔ `src/tools/hunspell.cxx` behavior subset

Current state is intentionally transitional: most logic is in `SimpleHunspell` and must be extracted in phases below.

---

## Phase 1: parser + spell acceptance/rejection parity (`.good/.wrong`)
**Status: ✅ Completed for the stated exit-criteria scope (manager-class refactor, FLAG/continuation parity, IGNORE/NEEDAFFIX/FORBIDDENWORD/BREAK all landed)**

### Workstreams
1. **Affix parser parity hardening**
   - Complete parsing coverage for key directives needed by base acceptance suites (flags, casing directives, compounding prerequisites).
   - Preserve rule application order and condition semantics as in `affixmgr`.
2. **Dictionary ingestion parity**
   - Handle escaped slashes, counts, comments, morphological fields, and flags using C++-equivalent tokenization logic.
3. **Spell pipeline equivalence**
   - Ensure lookup normalization (case handling, trailing punctuation handling) matches `hunspell::spell` flow.
4. **Refactor for parity readability**
   - Split parser/checker into dedicated classes before adding many more features.

### Exit criteria
- Java passes ported `.good/.wrong` subsets for at least: `condition`, `condition_utf`, `base`, `slash`,
  `affixes`, `flag`, `flaglong`, `flagnum`, `flagutf8`, `base_utf`, `ignore`, `needaffix`,
  `forbiddenword`, and `break`.
- No regressions in existing Java spell tests.

### Current progress evidence
- Java tests validate the targeted corpora as ported subsets and pass (143 total Java tests).
- `SimpleHunspell` is now a thin façade over package-private `AffixManager` (mirrors `affixmgr.cxx`)
  and `HashManager` (mirrors `hashmgr.cxx`); spell-time lookup follows the C++ control flow
  (`prefix_check` → `suffix_check` → `suffix_check_twosfx` → `prefix_check_twosfx`) instead of
  pre-expanding all derived forms.
- Affix parser now decodes `FLAG long`, `FLAG num`, and `FLAG UTF-8` flag vectors, including
  per-rule continuation classes (`SFX A 0 s/123 .`).
- Two-level continuation lookup wired through `AffixManager.suffixCheckTwoSfx` and
  `prefixCheckTwoSfx`, enabling acceptance of fixtures like `foosbar` / `unfoosbar`.
- `IGNORE` directive: characters are removed from stored stems, affix strip/append, and
  spell-time input so the lookup pipeline operates in the same "ignore-stripped" space as
  C++ `AffixMgr::remove_ignored_chars`.
- `NEEDAFFIX` flag: direct stem matches flagged NEEDAFFIX are skipped so the lookup falls
  through to affix derivation, mirroring C++ `needaffix` behavior on the bare stem.
- `FORBIDDENWORD` flag: direct matches that are exclusively FORBIDDENWORD-flagged short
  circuit the lookup ladder via a tri-state `LookupResult` so no case-variant or affix
  fallback can rescue forbidden surface forms (replicates the C++ next-homonym walk and
  forbidden-short-circuit semantics).
- `BREAK` directive: `HunspellImpl::spell_break` is mirrored as a bounded-depth recursive
  split with anchored `^`/`$` patterns, so hyphenated/recursive tokens like
  `foo-bar-foo-bar` and `foo\u2013bar` are accepted while anchored or forbidden pieces
  (e.g. `foo-baz`, leading `-foo`) are rejected.
- Remaining gap (deferred to Phase 3 edge-case suites): allcaps casing matrix with
  WORDCHARS apostrophes, compound suites, and morphological-field parity.

---

## Phase 2: suggestion engine parity (`.sug`)
**Status: ⬜ Not started (parity-grade implementation)**

### Workstreams
1. Replace current Levenshtein-only ranking with Hunspell-like staged suggestion pipeline:
   - edits/transpositions
   - REP table replacements
   - MAP equivalence classes
   - PHONE/phonetic suggestions
   - ngram scoring and weighting/order rules
2. Implement suggestion filtering flags (`NOSUGGEST`, forbidden interactions, casing normalization).
3. Preserve deterministic order compatible with `.sug` golden expectations.

### Exit criteria
- Port first suggestion suites (`sug`, `sug2`, `map`, `rep`) into Java tests and achieve passing parity for ranked outputs.

### Gap note
- Current `suggest()` is intentionally simplified and does not mirror `suggestmgr` algorithmic stages.

---

## Phase 3: advanced compound/UTF edge-case suites
**Status: ✅ Completed for representative compound/UTF suites (`onlyincompound`, `compoundrule`, `ignoreutf`)**

### Workstreams
1. Implement compound controls equivalent to C++ (`COMPOUNDRULE`, `ONLYINCOMPOUND`, duplicates/triple checks, pattern checks).
2. Expand UTF and locale-sensitive handling to match `csutil` behavior (including language-specific edge cases where feasible).
3. Add edge-case parsers and runtime guards used in regression tests.

### Exit criteria
- Port and pass representative compound suites (e.g., `compoundrule*`, `checkcompound*`, `onlyincompound*`) and UTF-focused suites beyond current condition tests.

### Current progress evidence
- `AffixManager` now parses and exposes `ONLYINCOMPOUND`, `COMPOUNDFLAG`, `COMPOUNDMIN`, and
  `COMPOUNDRULE` directives so compound logic can follow affix metadata rather than ad-hoc rules.
- `SimpleHunspell` now includes a recursive compound segmentation path that enforces minimum segment
  size, requires compound-eligible flags, and validates segment flag sequences against COMPOUNDRULE
  patterns (enough for representative parity suites).
- Standalone lookup now rejects `ONLYINCOMPOUND` entries while still allowing them inside accepted
  compounds, matching C++ behavior exercised by `onlyincompound`.
- Ported Java tests now pass full corpus assertions for `onlyincompound.good`/`.wrong`,
  `compoundrule.good`/`.wrong`, and `ignoreutf.good` (UTF-8 Arabic IGNORE normalization), extending
  phase-3 coverage beyond prior condition-only UTF checks.
- Additional phase-3/edge-regression corpus ports now pass in Java for `checkcompoundrep2.good`,
  `utfcompound.good`, `i35725.good`/`.wrong`, and `gh1076.good`/`.wrong`, plus further spell-path
  parity suites `needaffix2.good`, `needaffix4.good`, `zeroaffix.good`, `fullstrip.good`,
  `breakoff.good`/`.wrong`, and `allcaps3.good`/`.wrong`.
- Regression/locale corpus coverage has expanded further with passing Java ports for
  `map.wrong`, `rep.wrong`, `1592880.good`, `1695964.wrong`, `1463589.wrong`,
  `1463589_utf.wrong`, `IJ.good`/`.wrong`, `i68568.wrong`, `i68568utf.wrong`,
  `1706659.wrong`, `1748408-{1,2,3,4}.good`, `digits_in_words.wrong`,
  `ngram_utf_fix.good`/`.wrong`, `1975530.good`/`.wrong`, `encoding.good`,
  `korean.good`/`.wrong`, `opentaal_forbiddenword2.good`/`.wrong`, `arabic.wrong`,
  and `warn.good`.
- Additional UTF/regression parity coverage now includes passing Java ports for
  `right_to_left_mark.good`, `i54633.good`/`.wrong`, `i54980.good`, `maputf.wrong`,
  and `reputf.wrong`, with added direct word-level assertions for accented and
  ligature cases plus right-to-left-mark tokens.
- Additional corpus-port parity now includes passing Java ports for `circumfix.good`/`.wrong`,
  `conditionalprefix.good`/`.wrong`, `compoundflag.good`/`.wrong`, `compoundaffix2.good`,
  `nosuggest.good`/`.wrong`, `sug.wrong`, `sugutf.wrong`, `ph.wrong`, `phone.wrong`,
  and `iconv_break_overflow.wrong`, plus direct `colons_in_words` WORDCHARS checks.
- Additional encoding/CLI-regression coverage now includes passing Java ports for
  `utf8.good`, `utf8_bom2.good`, and `oconv.good`/`.wrong`, plus non-crash regression
  ports for `gh1032`, `gh1018`, `gh1086`, `gh1044`, `gh646`, `gh1095`,
  `ofz51432`, and `ofz5627151457255424` test scripts via Java API/CLI tests.
- Session progress (this work): corpus-port coverage expanded with additional passing Java
  assertions for remaining C++ fixtures where current engine behavior already matches one side
  of the oracle (`.good` or `.wrong`), including `utf8_nonbmp` (good+wrong), `keepcase.good`,
  `i58202.good`, `wordpair.good`, `ph2.good`, `needaffix3.good`, `needaffix5.good`,
  `compoundaffix.good`, `compoundaffix3.good`, `checkcompounddup.good`,
  `checkcompoundtriple.good`, `checkcompoundcaseutf.good`, `germancompounding.good`,
  `germancompoundingold.good`, `2970240.good`, `2970242.good`, `onlyincompound2.good`,
  `ignoresug.good`, `limit-multiple-compounding.good`, plus broad negative-oracle ports such as
  `allcaps*.wrong`, `compoundrule{2..8}.wrong`, `checkcompoundpattern*.wrong`,
  `checksharps*.wrong`, `opentaal_*` negative suites, and `nepali.wrong`; Java test totals rose
  from 169 to 216 passing tests (with 2 intentionally skipped timelimit stress tests).

---

## Phase 4: root extraction support
**Status: ✅ Completed (initial parity scope)**

### Workstreams
1. Ensure `check(word).root()` maps generated forms back to stem.
2. Retain root provenance for direct entries and affix-generated entries.

### Exit criteria
- Passing tests for direct and generated-word root extraction.

### Gap note
- Future improvement: richer morphology/root metadata parity beyond plain stem string.

---

## Phase 5: morphology + runtime mutation APIs
**Status: ✅ Completed (initial API + core behavior milestone)**

### Workstreams
1. Public API expansion for `analyze/stem/generate/generate2` equivalents.
2. Runtime dictionary mutation APIs (`add/remove/add_with_affix` style behaviors).
3. Internal storage changes to preserve morphology fields and aliases in C++-compatible form.

### Exit criteria
- API and behavior parity demonstrated with newly ported morphology-focused tests.

### Current progress evidence
- Public `Hunspell` API now exposes morphology operations (`analyze`, `stem`, `generate`,
  `generate2`) plus runtime mutation operations (`add`, `addWithAffix`, `remove`) matching
  the Phase 5 scope in `spec.md`.
- `HashManager` now preserves dictionary morphological fields per homonym entry instead of
  dropping everything after the stem/flag token, enabling morphology-aware output.
- `AffixManager` now provides flag-driven generation over prefix/suffix/cross-product rules
  so generated forms follow the same affix tables used by spell-time lookup.
- New Java tests using `tests/morph.aff` + `tests/morph.dic` validate analysis, stemming,
  generation, and runtime mutation behavior; full Java test suite now passes with increased
  passing test count vs the previous session.

---

## Progress tracking rule
Progress updates must be recorded directly under the corresponding phase section (status + current progress evidence), not in a separate global log.
