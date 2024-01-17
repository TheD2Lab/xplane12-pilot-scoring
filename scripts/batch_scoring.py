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
   print("Compiling...")
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
   data_files: list[str] = glob(f"{pilot_dir}/*.csv")
   flight_data_index: int = -1

   for i, file in enumerate(data_files):
      if re.search(regex, file):
         flight_data_index = i
         break

   # no datarefs file, find xplane.txt file
   if flight_data_index == -1:
      txt_file = glob(f"{pilot_dir}/*xplane.txt")  # check for xplane.txt file

      if txt_file:   # xplane.txt file found, so txt_files is not empty
         data_files += txt_file
         flight_data_index = len(data_files) - 1
      else:          # no flight data found
         print(f"No flight data found in {pilot_dir}")
         return
   
   # put flight data file in front of list
   data_files[0], data_files[flight_data_index] = data_files[flight_data_index], data_files[0]
   java_program += [output_dir] + data_files
   subprocess.run(java_program)

def run_multiple(output_dir: str, data_dir: str):
   pilot_dirs = glob(data_dir + "*" + os.path.sep) # get the child directories
   for p_dir in pilot_dirs:
      if "Questionnaires" in p_dir:
         continue
      run_single(output_dir, p_dir)

   print()

"""
Input Arguments:
   [-compile, -c] : compiles java program (only needs to be done once after code changes)
   [-input, -i]   : next argument is the data input directory
   [-output, -o]  : next argument is the output directory
Example Terminal Command:
   python3 scrips/batch_processing.py -c -i "Users/me/ILS Official Study Data" -o "Users/me/analysis"
Note:
   1. input argument flags can be in any order, only directory following -i/-o flag is enforced
   2. set up a launch setting in your IDE so you don't need to re-type the terminal command, or use up arrow in terminal
"""
if __name__ == "__main__":
   print("Working Directory: ", os.getcwd())

   num_args = len(sys.argv)
   if num_args == 1:
      print("No command line arguments were provided. Scoring not executed")
      exit()

   input_dir = None
   output_dir = None

   i = 1
   while i < len(sys.argv):
      if sys.argv[i] in ['-compile', '-c']:
         compile_source()
         i += 1
      elif sys.argv[i] in ['-output', '-o']:
         output_dir = sys.argv[i+1]
         i += 2
      elif sys.argv[i] in ['-input', '-i']:
         input_dir = sys.argv[i+1]
         i += 2

   if not (input_dir and output_dir):
      print("No input nor output directories were provided\n\t(ex. -i my_input_dir -o my_output_dir)")
   elif input_dir == None:
      print("No output directory was provided\n\t(ex: -i my_input_dir).")
   elif output_dir == None:
      print("No output directory was provided\n\t(ex: -o my_output_dir).")

   run_multiple(output_dir, input_dir)