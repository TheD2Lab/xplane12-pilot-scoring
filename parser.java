package xplane12_data_parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

public class parser {

	// For ILS 34R KSEA. Change for different airport
	private static int minimumsAltitude = 572;
	private static double initialAppFixDME = 22.2;
	private static double intersectionDME = 6.3;
	
	/**
	 * parses out only the useful/needed data into a different csv file
	 * @param filePath file to be parsed
	 * @param outputFolderPath directory to save output files
	 * @param name name of the participant
	 * @return String new csv file path
	 */
	public static String parseData(String filePath, String outputFolderPath, String name) {
		String refactoredFilePath = outputFolderPath + "//" + name + "_Refactored_Data.csv";
		List<String[]> selectedColumns = new ArrayList<>();
		List<String> columnNames = Arrays.asList(
				"missn,_time",
				"_Vind,_kias",
				"hpath,_true",
				"Vtrue,_ktgs",
				"_roll,__deg",
				"_land,groll",
				"pitch,__deg",
				"__VVI,__fpm",
				"p-alt,ftMSL",
				"terrn,ftMSL",
				"hding,__mag",
				"__mag,_comp",
				"__lat,__deg",
				"__lon,__deg",
				"pilN1,dme-d",
				"pilN1,h-def",
				"pilN1,v-def"
				);
		int[] columnIndex = new int[columnNames.size()];
		try(
			FileWriter outputFileWriter = new FileWriter(new File (refactoredFilePath));
			CSVWriter outputCSVWriter = new CSVWriter(outputFileWriter);
			FileReader fileReader = new FileReader(filePath);
			CSVReader csvReader = new CSVReader(fileReader);
		){
			String[] headers = csvReader.readNext();
			for (int i = 0; i < columnNames.size(); i++) {
				columnIndex[i] = Arrays.asList(headers).indexOf(columnNames.get(i));
				if (columnIndex[i] == -1) 
				{
					throw new IOException("Column not found: " + columnNames.get(i));
				}
			}
			String[] row;
			while ((row = csvReader.readNext()) != null) {
				String[] selectedRow = new String[columnIndex.length];
				for (int i = 0; i < columnIndex.length; i++) 
				{
					selectedRow[i] = row[columnIndex[i]];
				}
				selectedColumns.add(selectedRow);
			}
			outputCSVWriter.writeNext(columnNames.toArray(new String[0])); // write the header row
			outputCSVWriter.writeAll(selectedColumns); // write the selected columns
		} 
		catch (IOException e) {
			System.out.println(e);
		}
		catch (CsvValidationException e) {
			System.out.println(e);
		}

		return refactoredFilePath;
	}

	/**
	 * changes a txt file into a csv file
	 * @param filePath txt file to be converted
	 * @param outputFolderPath directory to save csv file
	 * @param name name of participant
	 * @return String the csv file path
	 */
	public static String txtToCSV(String filePath, String outputFolderPath, String name)
	{
		String csvFilePath = outputFolderPath + "//" + name + "_Reformatted_Data.csv";

		try 
		{
			FileReader fileReader = new FileReader(filePath);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			FileWriter outputFileWriter = new FileWriter(new File (csvFilePath));
			CSVWriter outputCSVWriter = new CSVWriter(outputFileWriter);
			String line = bufferedReader.readLine();
			
			while ((line = bufferedReader.readLine()) != null) 
			{
				String[] fields = line.split("\\|");
				//removes all the spaces from each element
				for (int i = 0; i < fields.length; i++) 
				{
				    fields[i] = fields[i].replaceAll("\\s+", "");
				}
				outputCSVWriter.writeNext(fields);
			}
			outputCSVWriter.close();
			bufferedReader.close();
			fileReader.close();
		} 
		catch (IOException e) 
		{
			System.out.println("\nUnable to convert txt file to csv file.");
			e.printStackTrace();
		}
		return csvFilePath;
	}

	/**
	 * parses out the phases of the flight into different csv files
	 * @param filePath csv file to be parsed
	 * @param outputFolderPath directory to save output files
	 * @param name name of the participant
	 */
	public static scoreCalculations parseOutSections(String filePath, String outputFolderPath, String name) {

		scoreCalculations score;
		String stepdownOutputFilePath = outputFolderPath + "//" + name + "_Stepdown_Data.csv";
		String finalApproachOutputFilePath = outputFolderPath + "//" + name + "_FinalApproach_Data.csv";
		String roundOutOutputFilePath = outputFolderPath + "//" + name + "_RoundOut_Data.csv";
		String landingOutputFilePath = outputFolderPath + "//" + name + "_Landing_Data.csv";

		FlightData data;
		List<Double> altStepdown = new LinkedList<>();
		List<Double> dmeStepdown = new LinkedList<>();
		List<Double> speedStepdown = new LinkedList<>();
		List<Double> hDefStepdown = new LinkedList<>();
		List<Double> vDefFinalApproach = new LinkedList<>();
		List<Double> speedFinalApproach = new LinkedList<>();
		List<Double> hDefFinalApproach = new LinkedList<>();
		List<Double> altRoundout = new LinkedList<>();
		List<Double> altLanding = new LinkedList<>();
		List<Double> hDefLanding = new LinkedList<>();
		String timeApproachStr = "";
		String timeTotalStr = "";
		double timeApproach = 0;
		double timeLanding = 0;
		double timeTotal = 0;

		// save file paths to score object
		int numStepdown = 0;
		int numFinalApproach = 0;
		int numRoundout = 0;
		int numLanding = 0;

		int altitudeIndex = -1;
		int dmeIndex = -1;
		int hdefIndex = -1;
		int vdefIndex = -1;
		int speedIndex = -1;
		int timeIndex = -1;
		int groundRollIndex = -1;

		// Note: try-with-resources automatically closes files
		try (
			FileWriter outputStepdownFileWriter = new FileWriter(new File(stepdownOutputFilePath));
			FileWriter outputFinalApproachFileWriter = new FileWriter(new File(finalApproachOutputFilePath));
			FileWriter outputRoundOutFileWriter = new FileWriter(new File(roundOutOutputFilePath));
			FileWriter outputLandingFileWriter = new FileWriter(new File(landingOutputFilePath));
			CSVWriter outputStepdownCSVWriter = new CSVWriter(outputStepdownFileWriter);
			CSVWriter outputFinalApproachCSVWriter = new CSVWriter(outputFinalApproachFileWriter);
			CSVWriter outputLandingCSVWriter = new CSVWriter(outputLandingFileWriter);
			CSVWriter outputRoundOutCSVWriter = new CSVWriter(outputRoundOutFileWriter);
			FileReader fileReader = new FileReader(filePath);
			CSVReader csvReader = new CSVReader(fileReader);
		){

			String[] headers = csvReader.readNext();
			for (int i = 0; i < headers.length; i++) {

				switch (headers[i]) {
					case "p-alt,ftMSL":
						altitudeIndex = i;
						break;
					case"pilN1,dme-d":
						dmeIndex = i;
						break;
					case "pilN1,h-def":
						hdefIndex = i;
						break;
					case "_Vind,_kias":
						speedIndex = i;
						break;
					case "pilN1,v-def":
						vdefIndex = i;
						break;
					case "missn,_time":
						timeIndex = i;
						break;
					case "_land,groll":
						groundRollIndex = i;
				}
			}
			outputStepdownCSVWriter.writeNext(headers);
			outputFinalApproachCSVWriter.writeNext(headers);
			outputRoundOutCSVWriter.writeNext(headers);
			outputLandingCSVWriter.writeNext(headers);
			String[] row;
			
			while ((row = csvReader.readNext()) != null) 
			{
				if (Double.valueOf(row[dmeIndex]) > initialAppFixDME) {
					continue;
					
				} else if(Double.valueOf(row[dmeIndex]) < initialAppFixDME && Double.valueOf(row[dmeIndex])>intersectionDME) {
					outputStepdownCSVWriter.writeNext(row);
					numStepdown++;
					altStepdown.add(Double.valueOf(row[altitudeIndex]));
					dmeStepdown.add(Double.valueOf(row[dmeIndex]));
					speedStepdown.add(Double.valueOf(row[speedIndex]));
					hDefStepdown.add(Double.valueOf(row[hdefIndex]));

				} else if(Double.valueOf(row[altitudeIndex])>minimumsAltitude) {
					outputFinalApproachCSVWriter.writeNext(row);
					numFinalApproach++;
					vDefFinalApproach.add(Double.valueOf(row[vdefIndex]));
					speedFinalApproach.add(Double.valueOf(row[speedIndex]));
					hDefFinalApproach.add(Double.valueOf(row[hdefIndex]));
					timeApproachStr = row[timeIndex];

				} else if(!(Double.valueOf(row[groundRollIndex])>0)){
					outputRoundOutCSVWriter.writeNext(row);
					numRoundout++;
					altRoundout.add(Double.valueOf(row[altitudeIndex]));
				} else {
					outputLandingCSVWriter.writeNext(row);
					numLanding++;
					altLanding.add(Double.valueOf(row[altitudeIndex]));
					hDefLanding.add(Double.valueOf(row[hdefIndex]));
				}
				timeTotalStr = row[timeIndex];
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println(e);
		}

		timeApproach = Double.valueOf(timeApproachStr);
		timeTotal = Double.valueOf(timeTotalStr);
		timeLanding = timeTotal - timeApproach;

		// construct flight data object to pass to scorer
		data = new FlightData(
			altStepdown,
			dmeStepdown,
			speedStepdown,
			hDefStepdown,
			vDefFinalApproach,
			speedFinalApproach,
			hDefFinalApproach,
			altRoundout,
			altLanding,
			hDefLanding,
			timeApproach,
			timeLanding,
			timeTotal
		);

		// Instantiate new score object
		score = new scoreCalculations(
			name,
			stepdownOutputFilePath,
			finalApproachOutputFilePath,
			roundOutOutputFilePath,
			landingOutputFilePath,
			numStepdown + numFinalApproach + numRoundout + numLanding, 
			numStepdown,
			numFinalApproach,
			numRoundout,
			numLanding,
			data
		);
		
		return score;

	}
	
	/**
	 * @return the minimumsAltitude
	 */
	public static int getMinimumsAltitude() {
		return minimumsAltitude;
	}
	/**
	 * @param minimumsAltitude the minimumsAltitude to set
	 */
	public static void setMinimumsAltitude(int minimumsAltitude) {
		parser.minimumsAltitude = minimumsAltitude;
	}

}
