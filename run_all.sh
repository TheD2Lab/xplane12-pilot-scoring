#!/usr/bin/env bash
# Batch scoring script for X-Plane and gaze data.
# Scans data/xplane_data for files named "*_xplane.txt" (for example,
# "Nick_xplane.txt"), then looks for matching "${PID}_datarefs.csv" in
# data/xplane_data and "${PID}_all_gaze.csv" in data/gazepoint_data.
# For each pilot with all required inputs, it runs the Java scoring
# pipeline (ScoreRunnerUpdated) and writes all generated outputs into
# the "output" directory under the project root.

set -euo pipefail
# -e: exit immediately on any command failure
# -u: treat use of unset variables as an error
# -o pipefail: fail a pipeline if any command in it fails

# Project and data directories
PROJECT_ROOT="/Users/jonathanpena/Documents/xplane12-pilot-scoring"
OUTPUT_ROOT="$PROJECT_ROOT/output"
XPLANE_DIR="$PROJECT_ROOT/data/xplane_data"
GAZE_DIR="$PROJECT_ROOT/data/gazepoint_data"

# Java executable and classpath for the scoring pipeline
JAVA=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java
CLASSPATH="bin:$PROJECT_ROOT/libs/opencsv-5.7.0.jar:$PROJECT_ROOT/libs/common-lang3.jar:$PROJECT_ROOT/libs/weka.jar"

# Ensure output directory exists
mkdir -p "$OUTPUT_ROOT"

# Iterate over all X-Plane text files matching *_xplane.txt
for XFILE in "$XPLANE_DIR"/*_xplane.txt; do
    # If the glob matches nothing, skip the loop body
    [ -e "$XFILE" ] || continue

    BASENAME=$(basename "$XFILE")   # e.g. Nick_xplane.txt
    PID="${BASENAME%%_*}"           # Extract pilot ID before the first underscore, e.g. "Nick"

    # Derive associated datarefs and gaze CSV paths for this pilot
    DATAREFS="$XPLANE_DIR/${PID}_datarefs.csv"
    GAZE="$GAZE_DIR/${PID}_all_gaze.csv"

    echo "=== Processing $PID ==="

    # Require matching datarefs file
    if [[ ! -f "$DATAREFS" ]]; then
        echo "  Skipping $PID: missing datarefs CSV: $DATAREFS"
        continue
    fi

    # Require matching gaze file
    if [[ ! -f "$GAZE" ]]; then
        echo "  Skipping $PID: missing gaze CSV: $GAZE"
        continue
    fi

    # Run the updated Java scoring pipeline for this pilot
    $JAVA -cp "$CLASSPATH" \
        scoring.scoringUpdated.ScoreRunnerUpdated \
        "$OUTPUT_ROOT" \
        "$XFILE" \
        "$DATAREFS" \
        "$GAZE"

    echo "  Done $PID"
done

echo "All done."
