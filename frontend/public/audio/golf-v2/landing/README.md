# Pro Golf Career — Audio v2 landing contacts (first pass)

All **7 user-supplied recordings** were present in the uploaded ZIP. This package contains **33 cropped or copied playback candidates** (24 fairway, 2 green, 1 rough, 3 bunker, 3 water). Originals are not modified and are not included in this production-ready sample set; retain your original source ZIP.

## Sound mapping

- `audio/fairway/`: 24 separate landings from the long grass recording, cropped at each transient, with short anti-click fades.
- `audio/green/green_01.wav`: original standalone ground contact.
- `audio/green/green_02_experimental.wav`: extracted last contact near **1.35s** from the chip recording; the source cuts off at ~1.44s, so this recording is *extremely short and has limited natural decay*. Audition before enabling.
- `audio/rough/rough_01.mp3`: original longer-grass contact.
- `audio/bunker/`: 3 extracted pillow impacts, provisional bunker Foley.
- `audio/water/water_01.mp3`: original splash; `water_02/03.mp3`: two separate water ripple events.

**All selections are provisional pending in-game listening.** Human/foley grass impacts may be too heavy as golf-ball impacts; use conservative gain. No complex equalization or cinematic processing has been applied. Extracted clips have only crop and very short fades. Do not infer multiple bounces from any of these samples.

## Integration scope

Use authoritative **first landing** surface when actually present. A final resting lie is not automatically the first landing surface; do not silently pretend otherwise. Fire once at the existing visual landing/contact phase; do not change backend shot outcomes, frontend trajectory timing, existing audio v1, or putt cup entry. No non-putt bounce, tree or path effects. Only sound water when backed by the resolved water result/contact, never based on a guessed location. For putting, do not add a landing sound on every ordinary putt.

## Rights / provenance

`SOURCE_MANIFEST.csv` includes source URLs, listed licenses, extracted timings and SHA-256 hashes. Pixabay sources list the Pixabay Content License. **Caution:** the two `yukon456` Freesound files are marked CC0 by their uploader, but the descriptions say they were extracted from a **PGA TOUR YouTube video**. The uploader's CC0 label does not by itself establish ownership or commercial redistribution rights. Use only provisionally for internal evaluation pending clearance or replacement.
