# C++ vs Java implementation gap analysis (2026-04-11)

This document summarizes the highest-impact parity gaps between the C++ Hunspell implementation (`src/hunspell/*.cxx`) and the Java port (`java/src/main/java/org/hunspell/*`).

## 1) Suggestion engine is still non-parity

- Java `suggest()` is explicitly a Levenshtein sort over dictionary stems and does **not** implement Hunspell's staged `SuggestMgr` flow (`REP`, `MAP`, `PHONE`, n-gram fallback, capitalization transforms, and time-limit behavior).
- Java `suffixSuggest()` currently filters stems by `startsWith` and lexical sort, while C++ delegates to suffix-rule-based generation/suggestion logic.

**Impact:** Ranked suggestions and suggestion recall can diverge materially from C++ for many real dictionaries even when spell acceptance/rejection matches.

## 2) Spell info bits are not surfaced in Java `check()`

- C++ `spell()` supports info bits (`SPELL_COMPOUND`, `SPELL_FORBIDDEN`) and root output.
- Java `check()` returns `SpellResult(correct, false, false, rootOrNull)` and does not currently propagate compound/forbidden info.

**Impact:** API consumers cannot distinguish accepted-compound vs normal words, and cannot detect forbidden-word cases through `check()` parity semantics.

## 3) Affix directive coverage remains partial vs C++ parser breadth

- C++ `AffixMgr` parses many directives, including `CHECKCOMPOUNDDUP`, `CHECKCOMPOUNDREP`, `CHECKCOMPOUNDTRIPLE`, `CHECKCOMPOUNDCASE`, `NOSUGGEST`, and `NONGRAMSUGGEST` among others.
- Java `AffixManager.parseBody()` currently handles a focused subset (`WORDCHARS`, `COMPLEXPREFIXES`, `IGNORE`, `FORBIDDENWORD`, `NEEDAFFIX`, compound core flags/rules, `AF`, `ICONV`, `BREAK`, `PFX/SFX`).

**Impact:** Some compound and suggestion constraints that are data-driven in C++ are either absent or approximated in Java.

## 4) Morphology/runtime mutation API surface is still narrower than C++

- C++ exposes `stem(vector<morph>)` and runtime `add_with_flags(word, flags, desc)`.
- Java exposes `stem(String)` and runtime mutation via `add`, `addWithAffix`, `remove` only.

**Impact:** Certain C++ workflows are unavailable in Java without extra compatibility wrappers.

## 5) Test parity is broad but still subset-oriented in many fixtures

- `tests.md` marks all C++ fixtures checked, but most entries are explicitly called out as "ported subset" and some scenarios remain intentionally disabled/non-deterministic in Java (`timelimit` stress tests).

**Impact:** Checklist completion does not yet imply full oracle-equivalent line-by-line parity for every fixture path.

## Recommended implementation order

1. **Phase 2 suggestion parity port** (highest product impact): port C++ `HunspellImpl::suggest` + `SuggestMgr` control flow and ranking.
2. **Propagate spell info bits to `SpellResult`** from Java spell pipeline.
3. **Expand affix directives** to cover missing compound/suggestion controls in parser + runtime checks.
4. **Close API deltas** (`addWithFlags`, morphology-based stemming overload) once behavior is parity-safe.
5. **Replace subset-only corpus checks** with fuller fixture assertions where deterministic.
