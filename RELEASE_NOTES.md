# Kastle 0.1.2

Release tag: `v0.1.2`. API: `com.saggiodev:kastle-api:0.1.2`.
CLI distribution: `kastle-0.1.2.zip`.

These are the release targets; their availability must be verified after the
API and CLI Jenkins jobs complete. Publication is separate for each artifact.

## Fixes

- Terminal dialogue rewards are handled correctly. Reward definitions no longer
  appear in rooms before the conversation; completing the chosen branch places
  its reward once in the current room for collection.
- Item and character names work as command targets alongside explicit aliases.
  Matching is exact, case-insensitive and ignores surrounding whitespace.
- Game installation validates provider registrations and rejects duplicate
  identities without replacement. Installation and removal coordinate file and
  database changes through rollback/recovery, and failures return Kastle errors.
  Default-name installation confirmations no longer print `null`.
- Game loading validates definitions before changing runtime registries and
  reports accumulated contextual definition errors.
- Opening and closing links report the actual source and destination rooms.

## Compatibility

Public method signatures and DSL syntax remain unchanged. Loading is stricter:
duplicate definitions, invalid IDs or references, invalid initial rooms and
winning conditions, cyclic dialogues, empty answer lists and rewards on
nonterminal questions are rejected. Question IDs must be unique within each
character's dialogue. Correct invalid definitions before loading with this release.

`MovementManager.open` and `close` now return the destination room on success.
Code that relied on the previous, incorrect source-room result must obtain the
current room separately.

## Release process

All modules share the version in `gradle.properties`. Tagged Jenkins builds
verify the version and clean checkout before publishing API and CLI separately.
See [RELEASING.md](RELEASING.md) for verification, publication and retry procedures.
