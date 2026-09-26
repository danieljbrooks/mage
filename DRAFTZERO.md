# DraftZero's fork of WillWroble/mage

**This fork is for testing, initially.** It holds the XMage changes
[DraftZero](https://github.com/danieljbrooks/draft-zero) needs to train format-wide Limited agents
with [MageZero](https://github.com/WillWroble/MageZero), while they are tried out.

**We use Will's standard releases wherever possible.** Anything generic goes upstream to
`WillWroble/mage` as a small, opt-in change, and only if Will wants it. What stays here is
limited-specific or still experimental. The default branch is Will's, unchanged.

## Branches

| Branch | Base | What it is |
|---|---|---|
| `exp1-fdn-generalist` | `2f35d9f7` (v0.1) | The exact engine DraftZero experiment #1 ran on (`5a32441c`). Needed to play its published checkpoints. |
| `v0.2-generalist` | `cb7e9c6f` (v0.2.0) | The same changes rebased onto Will's v0.2.0 |

The changes on both branches, all opt-in (without them, behaviour is upstream's):
- **A set-wide action vocabulary:** `MZ_ACTION_VOCAB` / `-Dmz.actionVocab`. Every card and
  ability in a set gets its own policy slot, instead of hashing into 128. Proposed upstream as
  [WillWroble/MageZero#7](https://github.com/WillWroble/MageZero/issues/7).
- **Deck pools:** `deck_pool` / `deck_pool_mode` in the game config. Each game draws its decks
  from a list, reproducibly from the game seed.
- **One `GAME_SUMMARY` JSON line per game:** decks, winner, turns and the cards each player drew.
  DraftZero uses these for its per-card and per-deck statistics.

## Building

`build-generalist-bundle.sh` takes Will's release bundle for the matching version and rebuilds
only the three jars this branch changes: `mage-player-ai`, `mage-player-ai-rl` and
`mage-magezero`. Every other file stays as Will shipped it.

```bash
./build-generalist-bundle.sh <unzipped magezero-xmage release> <out dir> [JAVA_HOME]
```

Built this way from `cb7e9c6f` unmodified, all 46 classes in those three jars match the
v0.2.0-alpha release's bytecode.
