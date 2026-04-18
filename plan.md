# Java Hunspell Port Plan (from `spec.md` §12+)

Status legend: ✅ completed · 🟡 in progress · ⬜ not started

## Completed milestones (succinct)

- ✅ Core architecture split landed: `SimpleHunspell` façade + `AffixManager` + `HashManager`.
- ✅ Baseline spell path parity work landed for major lookup branches (direct, affix, break,
  case fallback, forbidden/needaffix/onlyincompound interactions).
- ✅ Broad corpus-port coverage exists in Java tests and currently remains green.
- ✅ Many compound and UTF fixtures now have passing Java ports.
- ✅ REP suggestion stage ported from `SuggestMgr::replchars` with matching anchor
  semantics (`^` → ini, `$` → fin, combined → isol), `_`→space translation, and the
  "space in candidate" rewrite branch. REP-stage candidates now precede edit-stage
  fallbacks to match C++ stage ordering.

## Current parity position

- 🟡 Spell acceptance parity is strong on ported fixture paths.
- 🟡 Suggestion parity remains the largest algorithmic gap.
- 🟡 Some parser/runtime areas still rely on reduced or placeholder behavior.

## Detailed planned work (fidelity-first)

## Phase A — Suggestion parity port (highest priority)

1. Port `HunspellImpl::suggest` stage ordering exactly:
   - edit suggestions ⬜
   - REP table stage ✅ (parse_reptable + replchars + testsug + checkword, incl. `^`/`$`
     anchors, `_`→space translation, and the "space in candidate" rewrite branch)
   - MAP class stage ⬜
   - PHONE stage ⬜
   - ngram fallback + weighting/order ⬜
2. Port suggestion filtering controls fully:
   - `NOSUGGEST` ✅ (honored by the REP-stage `checkSuggestWord` filter),
     `NONGRAMSUGGEST` ⬜, forbidden interactions 🟡, case-sensitive gating ⬜.
3. Remove Java-only ranking heuristics once C++ ordering is in place. 🟡
4. Add deterministic parity assertions for `sug`, `sug2`, `map`, `rep` ✅, `phone`,
   `ph`, `sugutf`.

**Exit criteria:** Java suggestion order matches C++ oracle on the ported fixtures.

## Phase B — Compound algorithm closure

1. Reconcile Java compound recursion with full `AffixMgr::compound_check` branching.
2. Port remaining compound directives/interactions not yet modeled.
3. Expand parity tests for `checkcompound*`, `compoundrule*`, and OpenTaal compound suites to
   reduce subset-only assertions.

**Exit criteria:** Compound acceptance/rejection follows C++ decision points, not fixture-shaped shortcuts.

## Phase C — Casing / csutil alignment

1. Replace simplified case ladder helpers with C++-style `csutil` behavior.
2. Port language-sensitive casing edge paths used by IJ/Turkish/German and related fixtures.
3. Validate casing parity with corpus + targeted word-level checks.

**Exit criteria:** No known casing outcomes depend on Java `Locale.ROOT` simplifications.

## Phase D — BREAK + info-bit flow parity

1. Align BREAK recursion and anchored-pattern handling with C++ branch semantics.
2. Verify `SPELL_COMPOUND` / `SPELL_FORBIDDEN` propagation in all break/case/compound branches.

**Exit criteria:** Java `check()` metadata and break behavior are branch-parity with C++.

## Phase E — Parser breadth + API parity cleanup

1. Port remaining affix directives required by `spec.md` scope and C++ behavior.
2. Replace API placeholders:
   - implement meaningful `key(...)` handling or document parity-limited scope,
   - implement `strictAffixParsing(...)`,
   - replace hardcoded info metadata.
3. Rework morphology generation path to avoid brute-force scans.

**Exit criteria:** no known placeholder/hardcoded API behavior in v1-parity surface.

## Session policy

- Never introduce Java-only special cases just to satisfy a fixture.
- For every failing parity test, trace to corresponding C++ control flow and port that logic.
- End each session with no regressions and net parity progress.
