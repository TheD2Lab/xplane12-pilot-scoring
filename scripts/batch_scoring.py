"""
Script for processing multiple pilots's data. It can also compile the source java program.

directory structure:
data/
   pilot1/
      pilot1_datarefs.csv
      [pilot1_all_gaze.csv]
      [pilot1_fixations.csv]
   pilot2/
      pilot2_datarefs.csv
      [pilot2_all_gaze.csv]
      [pilot2_fixations.csv]
   pilot3/
      ...
   ...

Note: 
   1. Square brackets denote optional parameters
   2. Working directory must be the xplane12-pilot-scoring repository. 
   3. Tested only on Mac
"""

import os.path
import re
import subprocess
import sys

from glob import glob

def compile_source():
   jar_files: list[str] = ["-cp", "./libs/common-lang3.jar:./libs/opencsv-5.7.0.jar:./libs/weka.jar"]
   src_files: list[str] = glob("src/**/*.java")
   command = ["javac"] + jar_files + src_files
   subprocess.run(command)


def run_single(output_dir: str, pilot_dir: str):
   regex: str = r"datarefs.csv"
   java_program = [
      "java",
      "-cp",
      "./libs/common-lang3.jar:./libs/opencsv-5.7.0.jar:./libs/weka.jar:./src",
      "scoring/ScoreRunner"
   ]
   csv_files: list[str] = glob(f"{pilot_dir}/*.csv")
   datarefs_index: int = -1

   for i, file in enumerate(csv_files):
      if re.search(regex, file):
         datarefs_index = i
         break

   # no flight data found
   if datarefs_index == -1:
      return
   
   # put flight data file in front of list
   csv_files[0], csv_files[datarefs_index] = csv_files[datarefs_index], csv_files[0]
   java_program += [output_dir] + csv_files
   subprocess.run(java_program)

def run_multiple(output_dir: str, data_dir: str):
   pilot_dirs = glob(data_dir + "*" + os.path.sep) # get the child directories
   for p_dir in pilot_dirs:
      if p_dir == "Questionnaires":
         continue
      run_single(output_dir, p_dir)

   print()


if __name__ == "__main__":
   print(os.getcwd())
   if len(sys.argv) > 1 and sys.argv[1] == "-compile":
      print("Compiling...")
      compile_source()

   run_multiple(
      "/Users/ashleyjones/Documents/CSULB/EyeTracking/xplane12-pilot-scoring/local_outputs/",
      "/Users/ashleyjones/Documents/CSULB/EyeTracking/Data/ILS Official Study Data/"
   )