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
import csv

from glob import glob

headers: list[str] = None
count: int = 0

def compile_source():
   print("Compiling...")
   jar_files: list[str] = ["-cp", "./libs/common-lang3.jar:./libs/opencsv-5.7.0.jar:./libs/weka.jar"]
   src_files: list[str] = glob("src/**/*.java")
   print(f"Found Java source files: {src_files}")  # Debug print
   command = ["javac"] + jar_files + src_files
   print(f"Running command: {' '.join(command)}")  # Debug print
   result = subprocess.run(command, capture_output=True, text=True)
   print(f"Compilation stdout: {result.stdout}")  # Debug print
   print(f"Compilation stderr: {result.stderr}")  # Debug print


def run_single(output_dir: str, pilot_dir: str):
   regex: str = r"datarefs.csv"
   java_program = [
      "java",
      "-cp",
      "./libs/common-lang3.jar:./libs/opencsv-5.7.0.jar:./libs/weka.jar:./src",
      "scoring/ScoreRunner"
   ]
   data_files: list[str] = []
   txt_file = glob(f"{pilot_dir}/*_xplane.txt")
   
   if txt_file:
      data_files = txt_file
      data_files += glob(f"{pilot_dir}/*.csv")
      
      # First run the Java program to get the parsed data files
      java_program += [output_dir] + data_files
      result = subprocess.run(java_program, capture_output=True, text=True)
      
      # Now process the detailed data files
      pid = os.path.basename(pilot_dir)
      detailed_output = os.path.join(output_dir, f"{pid}_detailed_scores.csv")
      
      # Process each phase's data file and calculate scores
      phases = ['stepdown', 'finalapproach', 'roundout', 'landing']
      all_scores = []
      
      for phase in phases:
         phase_file = os.path.join(output_dir, pid, f"{pid}_flight_data_{phase}.csv")
         if os.path.exists(phase_file):
               scores = process_phase_data(phase_file, phase)
               all_scores.extend(scores)
      
      # Write all scores to a single CSV
      if all_scores:
         write_detailed_scores(detailed_output, all_scores)

def process_phase_data(file_path: str, phase: str) -> list[dict]:
   scores = []
   
   with open(file_path, 'r') as f:
      csvreader = csv.DictReader(f)
      for row in csvreader:
         score_components = calculate_row_scores(row, phase)
         scores.append({
               'Phase': phase,
               'Time': row.get('missn,_time', ''),
               'Airspeed_Score': score_components['airspeed'],
               'Localizer_Score': score_components['localizer'],
               'Glideslope_Score': score_components['glideslope'],
               'Bank_Score': score_components['bank'],
               'VSI_Score': score_components['vsi'],
               'Total_Row_Score': calculate_total_score(score_components),
               **row  # Include original data
         })
   return scores

def calculate_row_scores(row: dict, phase: str) -> dict:
   scores = {}
   
   # Airspeed scoring (target 90 knots)
   try:
      airspeed = float(row.get('_Vind,_kias', 0))
      speed_diff = abs(airspeed - 90)
      scores['airspeed'] = max(0, 1 - (speed_diff / 10))
   except ValueError:
      scores['airspeed'] = 0
      
   # Localizer scoring
   try:
      hdef = float(row.get('pilN1,h-def', 0))
      scores['localizer'] = max(0, 1 - (abs(hdef) / 2.5))
   except ValueError:
      scores['localizer'] = 0
      
   # Glideslope scoring (if in approach phase)
   try:
      vdef = float(row.get('pilN1,v-def', 0))
      scores['glideslope'] = max(0, 1 - (abs(vdef) / 2.5)) if phase in ['finalapproach', 'stepdown'] else 1.0
   except ValueError:
      scores['glideslope'] = 0
      
   # Bank angle scoring
   try:
      bank = float(row.get('_roll,__deg', 0))
      scores['bank'] = max(0, 1 - (abs(bank) / 15))
   except ValueError:
      scores['bank'] = 0
      
   # Vertical speed scoring
   try:
      vsi = float(row.get('__VVI,__fpm', 0))
      scores['vsi'] = 1.0 if vsi > -1000 else 0.0
   except ValueError:
      scores['vsi'] = 0
      
   return scores

def calculate_total_score(components: dict) -> float:
   weights = {
      'airspeed': 0.3,
      'localizer': 0.3,
      'glideslope': 0.2,
      'bank': 0.1,
      'vsi': 0.1
   }
   return sum(components[k] * weights[k] for k in components)

def write_detailed_scores(output_file: str, scores: list[dict]):
   if not scores:
      return
      
   with open(output_file, 'w', newline='') as f:
      writer = csv.DictWriter(f, fieldnames=scores[0].keys())
      writer.writeheader()
      writer.writerows(scores)

def single_analysis(score_file: str, m_dict: dict, num_index: int, id_index: int) -> list[str]:
   global count  # Move global declaration to the start of the function
   rows = []
   pid = os.path.basename(os.path.dirname(score_file))
   
   # Create a CSV file for this participant's row scores
   detailed_score_file = os.path.join(os.path.dirname(score_file), f"{pid}_detailed_scores.csv")
   
   with open(score_file, 'r') as read_file:
      csvreader = csv.reader(read_file)
      global headers
      if headers is None:
         headers = next(csvreader)
      else:
         next(csvreader)
         
      values = next(csvreader)
      is_score_file = False

      for i, header in enumerate(headers):
         temp = float(values[i])
         if header == "Overall_Score":
               update_analysis(m_dict, temp, pid, "avg_oscore", "high_oscore", "low_oscore", num_index, id_index)
               is_score_file = True
         elif header == "Total_Time":
               update_analysis(m_dict, temp, pid, "avg_otime", "high_otime", "low_otime", num_index, id_index)
         elif header == "Approach_Score":
               update_analysis(m_dict, temp, pid, "avg_ascore", "high_ascore", "low_ascore", num_index, id_index)
         elif header == "Approach_Time":
               update_analysis(m_dict, temp, pid, "avg_atime", "high_atime", "low_atime", num_index, id_index)
         elif header == "Landing_Score":
               update_analysis(m_dict, temp, pid, "avg_lscore", "high_lscore", "low_lscore", num_index, id_index)
         elif header == "Landing_Time":
               update_analysis(m_dict, temp, pid, "avg_ltime", "high_ltime", "low_ltime", num_index, id_index)

      if is_score_file:
         count += 1
         return [pid] + values
         
   return None



def run_multiple(output_dir: str, data_dir: str):
   # Get the child directories (p1, p2, etc.)
   pilot_dirs = glob(os.path.join(data_dir, "p*"))  # Changed to match p1, p2, etc. pattern
   print(f"Found pilot directories: {pilot_dirs}")  # Debug print
   
   for p_dir in pilot_dirs:
      if "Questionnaires" in p_dir:  # Keep this check
         continue
      print(f"Processing pilot directory: {p_dir}")  # Debug print
      run_single(output_dir, p_dir)

def summary_analysis(dir: str):
   print(f"Starting summary analysis in directory: {dir}")
   # Fix the directory pattern to use os.path.join
   pilot_dirs = glob(os.path.join(dir, "p*"))
   print(f"Found pilot directories for analysis: {pilot_dirs}")
   
   global headers  # Make sure we can modify the global headers variable
   if headers is None:
      headers = ["Overall_Score", "Total_Time", "Approach_Score", "Approach_Time", "Landing_Score", "Landing_Time"]

   labels = ["Measure", "Value", "PID"]
   measures = [
      "avg_oscore",
      "high_oscore",
      "low_oscore",
      "avg_otime",
      "high_otime",
      "low_otime",
      "avg_ascore",
      "high_ascore",
      "low_ascore",
      "avg_atime",
      "high_atime",
      "low_atime",
      "avg_lscore",
      "high_lscore",
      "low_lscore",
      "avg_ltime",
      "high_ltime",
      "low_ltime",
   ]

   m_dict = {
      "avg_oscore" : ['avg_overall_score', 0, ' '],
      "high_oscore" : ['high_overall_score', 0, None],
      "low_oscore" : ['low_overall_score', 100, None],
      "avg_otime" : ['avg_total_time', 0, ' '],
      "high_otime" : ['high_total_time', 0, ' '],
      "low_otime" : ['low_total_time', float('inf'), ' '],
      "avg_ascore" : ['avg_approach_score', 0, ' '],
      "high_ascore" : ['high_approach_score', 0, None],
      "low_ascore" : ['low_approach_score', 100, None],
      "avg_atime" : ['avg_approach_time', 0, ' '],
      "high_atime" : ['high_approach_time', 0, ' '],
      "low_atime" : ['low_approach_time', float('inf'), ' '],
      "avg_lscore" : ['avg_landing_score', 0, ' '],
      "high_lscore" : ['high_landing_score', 0, None],
      "low_lscore" : ['low_landing_score', 100, None],
      "avg_ltime" : ['avg_landing_time', 0, ' '],
      "high_ltime" : ['high_landing_time', 0, ' '],
      "low_ltime" : ['low_landing_time', float('inf'), ' ']
   }

   global count
   num_index = 1
   id_index = 2


   pilot_scores = []
   for p_dir in pilot_dirs:
      # Update the score file pattern to match the actual files
      score_file = glob(os.path.join(p_dir, "*_score.csv"))
      print(f"Looking for score file in {p_dir}: {score_file}")  # Debug print
      if not score_file:   # glob returned an empty array
         print(f"No score file found in {p_dir}")  # Debug print
         continue
      row = single_analysis(score_file[0], m_dict, num_index, id_index)
      if row:              # row is not empty or None
         pilot_scores.append(row)


   all_scores_file =os.path.join(dir, "all_scores.csv") 
   with open(all_scores_file, 'w') as write_file:
      csvwriter = csv.writer(write_file)
      csvwriter.writerow(["Participant"] + headers)
      csvwriter.writerows(pilot_scores)
            
   m_dict["avg_oscore"][num_index] /= count
   m_dict["avg_otime"][num_index] /= count
   m_dict["avg_ascore"][num_index] /= count
   m_dict["avg_atime"][num_index] /= count
   m_dict["avg_lscore"][num_index] /= count
   m_dict["avg_ltime"][num_index] /= count

   summary_file = os.path.join(dir, "score_analysis.csv")
   with open(summary_file, 'w') as csvfile:
      csvwriter = csv.writer(csvfile)
      csvwriter.writerow(labels)
      for measure in measures:
         csvwriter.writerow(m_dict[measure])

   print("Analysis summary written")

def single_analysis(score_file: str, m_dict: dict, num_index: int, id_index: int) -> list[str]:
   values = []
   with open(score_file, 'r') as read_file:
      csvreader = csv.reader(read_file)
      global headers
      if headers is None:
         headers = next(csvreader) # set headers
      else:
         next(csvreader)            # skip first line
      values = next(csvreader)

   pid = os.path.basename(os.path.dirname(score_file))
   is_score_file = False
   for i, header in enumerate(headers):
      temp = float(values[i])
      if header == "Overall_Score":
         update_analysis(m_dict, temp, pid, "avg_oscore", "high_oscore", "low_oscore", num_index, id_index)
         is_score_file = True       # file contains scores
      elif header == "Total_Time":
         update_analysis(m_dict, temp, pid, "avg_otime", "high_otime", "low_otime", num_index, id_index)
      
      elif header == "Approach_Score":
         update_analysis(m_dict, temp, pid, "avg_ascore", "high_ascore", "low_ascore", num_index, id_index)

      elif header == "Approach_Time":
         update_analysis(m_dict, temp, pid, "avg_atime", "high_atime", "low_atime", num_index, id_index)
      
      elif header == "Landing_Score":
         update_analysis(m_dict, temp, pid, "avg_lscore", "high_lscore", "low_lscore", num_index, id_index)

      elif header == "Landing_Time":
         update_analysis(m_dict, temp, pid, "avg_ltime", "high_ltime", "low_ltime", num_index, id_index)

   if is_score_file:
      global count
      count += 1
      return [pid] + values

def update_analysis(m_dict, value, pid, avg_str,high_str, low_str, num_index, id_index):
   m_dict[avg_str][num_index] += value

   if value > m_dict[high_str][num_index]:
      m_dict[high_str][num_index] = value
      m_dict[high_str][id_index] = pid

   if value < m_dict[low_str][num_index]:
      m_dict[low_str][num_index] = value
      m_dict[low_str][id_index] = pid 


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
   summary_analysis(output_dir)