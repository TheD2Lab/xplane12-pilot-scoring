# xplane12-pilot-scoring

Scores pilot performance on a simulated ILS approach from X-Plane 12 flight logs,
and trims synchronised eye-tracking recordings into the same flight phases.

For each pilot the program reads a raw X-Plane log, splits the flight into four
phases using charted fixes from the approach plate, scores each phase against
FAA tolerances, and writes a per-pilot CSV of scores, times and flight
statistics. If gaze files are supplied it also cuts them at the same phase
boundaries, so gaze and flight data line up for later analysis.

Built for the D2 Lab at California State University, Long Beach.

---

## What it produces

One `<pilot>_score.csv` per pilot, containing:

| Group | Columns |
|---|---|
| Overall | `Total_Time`, `Overall_Score`, `Overall_Time` |
| Phase groups | `Approach_Score/Time`, `Landing_Score/Time` |
| Segments | `Stepdown_`, `FinalApproach_`, `Roundout_`, `Landing_Segment_Score/Time` |
| Airspeed | `MIN/MAX/AVG_ILS_Airspeed`, `Percent_Proper_Airspeed` |
| Vertical speed | `MIN/MAX/AVG_ILS_VSI` |
| Guidance | `AVG_ILS_ABS_Glideslope_Deflection`, `AVG_ILS_ABS_Localizer_Deflection` |
| Attitude | `AVG/MAX_ILS_ABS_Bank_Angle` |

Plus, in `<pilot>_trim/`, the reformatted flight data and any gaze files cut
into `stepdown`, `finalapproach`, `roundout` and `landing` windows.

## Scoring tolerances

Scores are penalties against FAA criteria, averaged per data point:

- **Localizer and glideslope**: no more than 3/4-scale deflection
  (`0.75 x 2.5 = 1.875` dots), per FAA-S-ACS-8C IR.VI.B.S12. Full-scale
  deflection is 2.5 dots, which is where X-Plane's `h-def` / `v-def` saturate.
- **Airspeed**: 90 knots target (`TARGET_SPEED`).
- **Runway heading**: 344 degrees (`TARGET_HEADING`), runway 34R.
- **Descent rate**: steeper than -1000 fpm counts as unstable.

## Requirements

- **JDK 17 or newer** (`java`, `javac` on your PATH)
- **Python 3.9+** if you use `scripts/batch_scoring.py`
- Bundled jars in `libs/`: OpenCSV, Apache Commons Lang3, Weka

---

## Setup

Clone the repository and compile the Java sources:

```bash
git clone https://github.com/TheD2Lab/xplane12-pilot-scoring.git
cd xplane12-pilot-scoring

javac -cp "libs/common-lang3.jar:libs/opencsv-5.7.0.jar:libs/weka.jar" \
      -d bin $(find src -name "*.java")
```

This writes class files to `bin/`. Recompile after any change to `src/`.

On Windows, swap the `:` classpath separators for `;`.

---

## Running

There are two entry points. They expect **different input layouts**, so pick the
one that matches how your data is arranged.

### Option 1: `run_all.sh` — flat layout, segment-aware scorer

Use this for data grouped by file type. It calls `ScoreRunnerUpdated`, which
produces the four-segment scores and four gaze trim windows.

Expected layout:

```
data/
  xplane_data/
    p1_xplane.txt          raw X-Plane log
    p1_datarefs.csv        timestamps and DME
  gazepoint_data/
    p1_all_gaze.csv        gaze recording (optional)
```

Run:

```bash
./run_all.sh
```

It scans `data/xplane_data/*_xplane.txt`, derives the pilot ID from the filename
prefix, requires a matching `_datarefs.csv`, skips any pilot missing an input,
and writes everything to `output/`.

> **Before first use**, edit the two paths at the top of `run_all.sh`:
> `PROJECT_ROOT` and `JAVA` are hardcoded to one developer's machine and will
> not resolve on yours.

### Option 2: `scripts/batch_scoring.py` — per-pilot folders

Use this for data grouped by pilot. It calls the original `ScoreRunner`.

Expected layout:

```
data/
  pilot1/
    pilot1_datarefs.csv
    pilot1_all_gaze.csv    optional
    pilot1_fixations.csv   optional
  pilot2/
    ...
```

Run:

```bash
python3 scripts/batch_scoring.py -c -i data/ -o output/
```

| Flag | Meaning |
|---|---|
| `-c`, `-compile` | compile the Java sources first (only needed after code changes) |
| `-i`, `-input` | directory holding the per-pilot folders |
| `-o`, `-output` | directory to write results into |

Both input and output are required. The script also writes a summary across all
pilots when it finishes.

Run it from the repository root; it uses relative paths.

### Option 3: a single pilot, directly

```bash
java -cp "bin:libs/common-lang3.jar:libs/opencsv-5.7.0.jar:libs/weka.jar" \
     scoring.scoringUpdated.ScoreRunnerUpdated \
     output/ \
     data/xplane_data/p1_xplane.txt \
     data/xplane_data/p1_datarefs.csv \
     data/gazepoint_data/p1_all_gaze.csv
```

Arguments, in order: output directory, X-Plane log, timestamps CSV, then any
number of gaze files to trim.

---

## Output layout

```
output/
  p1/
    p1_score.csv
    p1_trim/
      p1_Reformatted_Data.csv
      p1_Refactored_Data.csv
      p1_flight_data_stepdown_segment.csv
      p1_flight_data_finalapproach_segment.csv
      p1_flight_data_roundout_segment.csv
      p1_flight_data_landing_segment.csv
      <gaze files, cut per segment>
```

---

## Source layout

```
src/scoring/
  ScoreRunner.java              original entry point
  ScoreCalculation.java         original scorer
  Parser.java                   X-Plane log to flight data
  FlightData.java               per-flight container
  FlightDataPoint.java          one logged row
  Fix.java, DataIndex.java      charted fixes, column indices

src/scoring/scoringUpdated/
  ScoreRunnerUpdated.java       segment-aware entry point
  ScoreCalculationUpdated.java  segment-aware scorer
  ParserUpdated.java            adds per-phase timestamps
  FlightDataUpdated.java

src/utils/                      CSV helpers, gaze trimming, logging
scripts/batch_scoring.py        batch driver for the per-pilot layout
run_all.sh                      batch driver for the flat layout
tests/                          see tests/README.md
```

The `scoringUpdated` package is the newer path and is what the segment-level
analysis uses. The original package is kept for reproducing earlier results.

---

## Adapting to a different airport or aircraft

- **Airport**: change the field elevation and the minimums to match the approach
  plate, and update the charted fixes.
- **Aircraft**: approach speeds differ, so update the speeds used in
  `SpeedILSCalcPenalty`.
- **Weighting**: every phase is currently weighted equally. Change
  `MAX_PTS_PER_DATA_POINT_ILS`, `_ROUNDOUT` and `_LANDING` to reweight.
