# Java parity tests — fixture checklist

Session policy (see `AGENTS.md` / `plan.md`):
mark fixtures as ✅ once their Java port is green; leave them ⬜ until covered.
Partial coverage (subset of acceptance/rejection, or missing `.sug`/`.good` parity) is 🟡.

## Suggestion parity (`SuggestMgr`)

### REP table stage (`SuggestMgr::replchars`)

- ✅ `rep.aff` / `rep.dic` — core REP substitution + anchor semantics. Covered by
  `HunspellPortedCorpusTest`:
  - `repSuggest_patternReplacementProducesPhormToForm` (med slot: `ph`→`f`)
  - `repSuggest_patternReplacementProducesFantomToPhantom` (ini slot: `f`→`ph`)
  - `repSuggest_finAnchorRewritesVacashunAsVacation` (`shun$`→`tion`)
  - `repSuggest_isolAnchorRewritesFooAsBar` (`^foo$`→`bar`)
  - `repSuggest_spaceRewriteProducesSplitCandidate` (`alot`→`a lot`; "space in
    candidate" rewrite branch)
  - `repSuggest_apostropheToSpaceRewritesUnalunno` (`'`→` `; underscore
    translation)
  - `repSuggest_finFallbackRewritesAutosAsAutoApostropheS` (fin fallback to med
    slot when `outstrings[fin]` is empty)
  - `repSuggest_repStageSuggestionsAppearAheadOfEditFallbacks` (stage ordering:
    REP before edits)
  - `repSuggest_nosuggestFlaggedCandidatesAreExcluded` (`checkword` filter for
    `NOSUGGEST`, via `nosuggest.aff`)
- ⬜ `reputf.aff` — REP table with UTF-8 patterns (needs UTF-aware code-point
  bounds in anchor handling).
- ⬜ `checkcompoundrep.aff` — REP interaction with `CHECKCOMPOUNDREP`.

### Edit suggestion stages (not yet ported as staged ops)

- 🟡 Fallback edit candidates still run through the Java Levenshtein heuristic
  after REP. To be replaced by the remaining C++ suggestion stages
  (`SuggestMgr::mapchars`, `SuggestMgr::phonet`, ngram).

### MAP stage (`SuggestMgr::mapchars`)

- ⬜ `map.aff`, `mapu.aff` — not ported.

### PHONE stage (`SuggestMgr::phonet`)

- ⬜ `phone.aff`, `ph.aff` — not ported.

### Ngram stage

- ⬜ `ngram_utf_fix.aff`, `sug.aff`, `sug2.aff` — not ported.

## Spell acceptance parity

Broad corpus coverage already landed prior to this session; see
`HunspellPortedCorpusTest` for the full list. New gaps added here as parity
shifts.

## Compound parity

See `plan.md` Phase B for the open compound directive set. This file will be
extended with compound fixture checkmarks as those fixtures land.
