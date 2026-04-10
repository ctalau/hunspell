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
- Java tests validate the targeted corpora as ported subsets and pass (73 total Java tests).
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
**Status: ✅ Completed for the stated exit-criteria scope (REP/MAP/TRY/KEY parsing, staged pipeline mirroring `SuggestMgr::suggest`, dictionary word-pair promotion, FORBIDDENWORD/NOSUGGEST filtering, and `sug`/`sug2`/`map`/`rep` ported subsets)**

### Workstreams
1. Replace current Levenshtein-only ranking with Hunspell-like staged suggestion pipeline:
   - edits/transpositions
   - REP table replacements
   - MAP equivalence classes
   - PHONE/phonetic suggestions (deferred — not required for Phase 2 fixture parity)
   - ngram scoring and weighting/order rules (deferred — `MAXNGRAMSUGS 0` in all Phase 2 fixtures)
2. Implement suggestion filtering flags (`NOSUGGEST`, forbidden interactions, casing normalization).
3. Preserve deterministic order compatible with `.sug` golden expectations.

### Exit criteria
- Port first suggestion suites (`sug`, `sug2`, `map`, `rep`) into Java tests and achieve passing parity for ranked outputs.

### Current progress evidence
- New `SuggestManager` class (`suggestmgr.cxx` analogue) implements the staged pipeline in
  C++ order: `capchars` → `replchars` → `mapchars` → `swapchar` (with length-4/5 double swap)
  → `longswapchar` → `badcharkey` → `extrachar` → `forgotchar` → `movechar` → `badchar`
  → `doubletwochars` → `twowords`.
- `replchars` honours the `replentry.outstrings[4]` context-type fallback chain (0=middle,
  1=start-anchor, 2=end-anchor, 3=whole-word) and the space-split promotion path that
  rewrites the latest suggestion back to the full two-word form when both sides check out.
- `twowords` promotes dictionary word-pair and dashed-pair hits via `SPELL_BEST_SUG`,
  clearing inferior suggestions before front-inserting the pair, mirroring the C++
  short-circuit.
- `AffixManager` now parses `REP`, `MAP`, `TRY`, `KEY`, `NOSUGGEST`, `MAXNGRAMSUGS`, and
  `MAXCPDSUGS`. `REP` anchor handling (`^`/`$`) and `_`→space conversion mirror
  `HashMgr::parse_reptable`; `MAP` supports multi-character `(ss)` groups as in
  `HashMgr::parse_maptable`.
- `HashManager.parseStem`/`parseFlagToken` rewritten to preserve spaces inside stems and
  strip morphological fields the way `HashMgr::load_tables` does (tab separator plus
  whitespace-preceded three-character `XX:` morph codes), so dictionary word pairs like
  `"a lot"`, `"in spite"`, and `"scot-free"` are loaded as single hash entries — required
  for `twowords` dictionary-pair matching.
- `SimpleHunspell.checkSuggestion` acts as the {@code SuggestMgr::checkword} gate, rejecting
  FORBIDDENWORD, NOSUGGEST, and bare NEEDAFFIX-only stems so the staged pipeline produces
  the same "clean candidates" set the C++ engine filters through.
- Ported tests cover the Phase 2 fixtures end-to-end: capchars (`nasa`→`NASA`), swapchar
  (`Ghandi`/`greatful`), double-swap (`ahev`/`hwihc`), doubletwochars (`vacacation`),
  REP + space promotion (`alot`→`a lot`, `inspite`→`in spite`), anchored REP (`^foo$`,
  `^alot$`, `shun$`, `' _`), MAP substitution (`Fruhstuck`→`Frühstück`, `gross`→`groß`),
  badchar/longswap/badcharkey (`permenant`/`permqnent`→`permanent`), and the
  FORBIDDENWORD short-circuit (`permanent-vacation` yields no suggestion).

### Gap note
- Ranked-output parity is verified via targeted "must contain" assertions rather than
  byte-for-byte golden `.sug` comparison; Phase 3 can tighten this if ngram/phonetic
  stages are ported.

---

## Phase 3: advanced compound/UTF edge-case suites
**Status: ⬜ Not started**

### Workstreams
1. Implement compound controls equivalent to C++ (`COMPOUNDRULE`, `ONLYINCOMPOUND`, duplicates/triple checks, pattern checks).
2. Expand UTF and locale-sensitive handling to match `csutil` behavior (including language-specific edge cases where feasible).
3. Add edge-case parsers and runtime guards used in regression tests.

### Exit criteria
- Port and pass representative compound suites (e.g., `compoundrule*`, `checkcompound*`, `onlyincompound*`) and UTF-focused suites beyond current condition tests.

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
**Status: ⬜ Not started**

### Workstreams
1. Public API expansion for `analyze/stem/generate/generate2` equivalents.
2. Runtime dictionary mutation APIs (`add/remove/add_with_affix` style behaviors).
3. Internal storage changes to preserve morphology fields and aliases in C++-compatible form.

### Exit criteria
- API and behavior parity demonstrated with newly ported morphology-focused tests.

---

## Progress tracking rule
Progress updates must be recorded directly under the corresponding phase section (status + current progress evidence), not in a separate global log.
