# Season field files

One JSON file per FTC season's field, read by the Unity replay (and later by Java, for vision
and obstacles). The format, `pvi-harness-field` v1, is documented in `Harness/README.md`
under "Season field files".

| File | Field | Season | Built from |
|---|---|---|---|
| `biobuzz-2026.json` | BIOBUZZ | 2026-2027 | `sources/BIOBUZZ_Competition_Manual_TU06.pdf` (Section 9 ARENA, Figure 10-2) and the FTC SDK's BIOBUZZ AprilTag library |
| `disabled/decode-2025.json` | DECODE | 2025-2026 | **Switched off** (the user, 2026-10-24): not offered in Unity's field picker. The FTC SDK's DECODE AprilTag library (goal tags); other parts were never added. Move it back up into `fields/` to switch it on again. |

## `sources/`

Copies of the official documents the field files were built from, kept here so they stay
available after the original download is gone.

- `BIOBUZZ_Competition_Manual_TU06.pdf`: FIRST Tech Challenge BIOBUZZ Competition Manual,
  version **TU06** (Team Update 06), saved 2026-10-24. FIRST publishes updated versions during
  the season. When a newer one comes out, add it next to this one (keep the old one, since the
  field file names this version), and check the field file's numbers against it.

Still wanted:
- **Official BIOBUZZ field CAD.** Parts marked `"approximate"` in `biobuzz-2026.json` are worked
  out from the manual's figures and can be a few inches off. Put the CAD (or its drawings) in
  `sources/` and update those numbers.
- **The DECODE Competition Manual**, only if DECODE is switched back on, to add its other parts.

These documents belong to FIRST and are kept here for the team's own use.
