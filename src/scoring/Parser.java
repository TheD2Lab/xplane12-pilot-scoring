package scoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Tools to parse flight data files to prepare for scoring process. 
 */
public class Parser {

	/**
	 * Minimum altitude MSL for the approach in feet.
	 */
	private static int minimumsAltitude = 572;
	/**
	 * Distance of the initial approach fix (JIPOX) from the runway in nautical miles.
	 */
	private static double initialAppFixDME = 22.2;
	/**
	 * Distance of the final approach fix from the runway in nautical miles.
	 */
	private static double intersectionDME = 6.3;
	/**
	 * Formatter for parsing system time.
	 */
	private static DateTimeFormatter sysTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss");
	
	/**
	 * Parses out only the useful/needed data into a different csv file.
	 * @param filePath file to be parsed
	 * @param outputFolderPath directory to save output files
	 * @param name name of the participant
	 * @return String new csv file path
	 */
	public static String parseData(String filePath, String outputFolderPath, String name) {
		String refactoredFilePath = outputFolderPath + "//" + name + "_Refactored_Data.csv";
		List<String[]> selectedColumns = new ArrayList<>();
		List<String> columnNames = Arrays.asList(
				"missn,_time", // Mission Time starting from 0 in seconds
				"_Vind,_kias", // Airspeed Indicator in knots
				"engn1,__rpm", // Engine RPM Setting
				"alpha,__deg", // Angle of Attach in degrees
				"_roll,__deg", // Roll (bank) angle in degrees
				"_land,groll", // Landing distance "ground roll" in feet
				"pitch,__deg", // Airplane Pitch in degrees
				"__VVI,__fpm", // Vertical Speed Indicator in feet per minute
				"p-alt,ftMSL", // Altitude MSL in feet
				"terrn,ftMSL", // Peak of terrain in feet
				"hding,__mag", // Magnetic heading in degrees
				"__lat,__deg", // Latitude in degrees
				"__lon,__deg", // Longitude in degrees
				"pilN1,dme-d", // DME distance in nautical miles
				"pilN1,h-def", // Localizer deflection in dots
				"pilN1,v-def"  // Glideslope deflection in dots
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
	 * Changes a txt file into a csv file.
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
	 * Parses out the phases of the flight and creates a ScoreCalculations object. Data entries prior to the initial approach
	 * fix (JIPOX) are discarded. Data entries between JIPOX and before final approach fix are included in the stepdown phase. Data
	 * entries at or after the final approach fix and above the minimum approach altitude are included in the final approach phase. Data entries at
	 * or below the minimum approach altitude and with non-positive ground roll value are included in the Roundout phase. Data entries with
	 * positive ground roll values are included in the landing phase. These phases/categories do not overlap.
	 * @param filePath path to flight data csv file.
	 * @param name Participant name or identifier.
	 * @return pilot success score as a ScoreCalculation object.
	 */
	public static ScoreCalculation parseFlightData(String filePath, String name) {

		ScoreCalculation score;

		FlightData data;
		// Stepdown portion
		List<Double> altStepdown = new LinkedList<>();
		List<Double> dmeStepdown = new LinkedList<>();
		List<Double> speedStepdown = new LinkedList<>();
		List<Double> hDefStepdown = new LinkedList<>();
		List<Double> rollBankStepdown = new LinkedList<>();
		List<Double> verticalSpeedStepdown = new LinkedList<>();
		// Final approach portion
		List<Double> vDefFinalApproach = new LinkedList<>();
		List<Double> speedFinalApproach = new LinkedList<>();
		List<Double> hDefFinalApproach = new LinkedList<>();
		List<Double> verticalSpeedFinalApp = new LinkedList<>();
		List<Double> rollBankFinalApp = new LinkedList<>();
		// Roundout portion
		List<Double> altRoundout = new LinkedList<>();
		List<Double> hDefRoundout = new LinkedList<>();
		List<Double> rollBankRoundout = new LinkedList<>();
		List<Double> verticalSpeedRoundout = new LinkedList<>();
		// Landing portion
		List<Double> altLanding = new LinkedList<>();
		List<Double> hDefLanding = new LinkedList<>();
		String timeApproachStr = "";
		String timeTotalStr = "";
		double timeApproach = 0;
		double timeLanding = 0;
		double timeTotal = 0;

		// count number of entries for each portion of flight
		int numStepdown = 0;
		int numFinalApproach = 0;
		int numRoundout = 0;
		int numLanding = 0;

		// timestamps
		LocalDateTime beginFlightTimestamp = null;
   	LocalDateTime beginApproachTimestamp = null;
   	LocalDateTime beginRoundOutTimestamp = null;
   	LocalDateTime beginLandingTimestamp = null;
   	LocalDateTime endFlightTimestamp = null;
		String endFlightTimeString = null;

		// the indexes for the X-Plane data we use for scoring and statistics
		int sysTimesIndex = -1;
		int altitudeIndex = -1;
		int dmeIndex = -1;
		int hdefIndex = -1;
		int vdefIndex = -1;
		int speedIndex = -1;
		int timeIndex = -1;
		int groundRollIndex = -1;
		int verticalSpeedIndex = -1;
		int rollBankAngleIndex = -1;
		int engineRPMIndex = -1;
		int magHeadingIndex = -1;

		// Note: try-with-resources automatically closes files
		try (
			FileReader fileReader = new FileReader(filePath);
			CSVReader csvReader = new CSVReader(fileReader);
		){

			String[] headers = csvReader.readNext();
			for (int i = 0; i < headers.length; i++) {

				switch (headers[i]) {
					case "sys_time":
						sysTimesIndex = i;
						break;
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
					case "engn1,__rpm":
						engineRPMIndex = i;
						break;
					case "pilN1,v-def":
						vdefIndex = i;
						break;
					case "missn,_time":
						timeIndex = i;
						break;
					case "_land,groll":
						groundRollIndex = i;
						break;
					case "__VVI,__fpm":
						verticalSpeedIndex = i;
						break;
					case "_roll,__deg":
						rollBankAngleIndex = i;
						break;
					case "hding,__mag":
						magHeadingIndex = i;
						break;
				}
			}
			String[] row;
			
			while ((row = csvReader.readNext()) != null) 
			{
				// Do not start scoring until after participant reaches Initial Approach Fix - JIPOX
				if (Double.valueOf(row[dmeIndex]) > initialAppFixDME) {
					if (sysTimesIndex != -1 && beginFlightTimestamp == null) {
						beginFlightTimestamp = LocalDateTime.parse(row[sysTimesIndex], sysTimeFormat);
					}
					continue;
				
				// ILS Stepdown portion
				} else if(Double.valueOf(row[dmeIndex]) < initialAppFixDME && Double.valueOf(row[dmeIndex])>intersectionDME) {
					numStepdown++;
					altStepdown.add(Double.valueOf(row[altitudeIndex]));
					dmeStepdown.add(Double.valueOf(row[dmeIndex]));
					speedStepdown.add(Double.valueOf(row[speedIndex]));
					hDefStepdown.add(Double.valueOf(row[hdefIndex]));
					rollBankStepdown.add(Double.valueOf(row[rollBankAngleIndex]));
					verticalSpeedStepdown.add(Double.valueOf(row[verticalSpeedIndex]));
					if (sysTimesIndex != -1 && beginApproachTimestamp == null) {
						beginApproachTimestamp = LocalDateTime.parse(row[sysTimesIndex], sysTimeFormat);
					} 

				// ILS Final Approach portion
				} else if(Double.valueOf(row[altitudeIndex])>minimumsAltitude) {
					numFinalApproach++;
					vDefFinalApproach.add(Double.valueOf(row[vdefIndex]));
					speedFinalApproach.add(Double.valueOf(row[speedIndex]));
					verticalSpeedFinalApp.add(Double.valueOf(row[verticalSpeedIndex]));
					hDefFinalApproach.add(Double.valueOf(row[hdefIndex]));
					timeApproachStr = row[timeIndex];
					rollBankFinalApp.add(Double.valueOf(row[rollBankAngleIndex]));

				// Roundout portion: From minimums, descent to the runway portion
				} else if(!(Double.valueOf(row[groundRollIndex])>0)){
					numRoundout++;
					hDefRoundout.add(Double.valueOf(row[hdefIndex]));
					altRoundout.add(Double.valueOf(row[altitudeIndex]));
					rollBankRoundout.add(Double.valueOf(row[rollBankAngleIndex]));
					verticalSpeedRoundout.add(Double.valueOf(row[verticalSpeedIndex]));
					if (sysTimesIndex != -1 && beginRoundOutTimestamp == null) {
						beginRoundOutTimestamp = LocalDateTime.parse(row[sysTimesIndex], sysTimeFormat);
					} 
					
				// Wheels touch the ground portion
				} else {
					numLanding++;
					altLanding.add(Double.valueOf(row[altitudeIndex]));
					hDefLanding.add(Double.valueOf(row[hdefIndex]));
					if (sysTimesIndex != -1 && beginLandingTimestamp == null) {
						beginLandingTimestamp = LocalDateTime.parse(row[sysTimesIndex], sysTimeFormat);
					} 
				}

				if (Double.valueOf(row[speedIndex]) > 0) {
					timeTotalStr = row[timeIndex];
					if (sysTimesIndex != -1)
						endFlightTimeString = row[sysTimesIndex];
				}
			}
			if (endFlightTimeString != null) {
				endFlightTimestamp = LocalDateTime.parse(endFlightTimeString, sysTimeFormat);
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
		if (sysTimesIndex == -1) {
			data = new FlightData(
				altStepdown,
				dmeStepdown,
				speedStepdown,
				hDefStepdown,
				vDefFinalApproach,
				speedFinalApproach,
				hDefFinalApproach,
				altRoundout,
				hDefRoundout,
				altLanding,
				hDefLanding,
				verticalSpeedFinalApp,
				verticalSpeedStepdown,
				verticalSpeedRoundout,
				rollBankStepdown,
				rollBankFinalApp,
				rollBankRoundout,
				timeApproach,
				timeLanding,
				timeTotal
			);
		} else {
			data = new FlightData(
				beginFlightTimestamp,
				beginApproachTimestamp,
				beginRoundOutTimestamp,
				beginLandingTimestamp,
				endFlightTimestamp,
				altStepdown,
				dmeStepdown,
				speedStepdown,
				hDefStepdown,
				vDefFinalApproach,
				speedFinalApproach,
				hDefFinalApproach,
				altRoundout,
				hDefRoundout,
				altLanding,
				hDefLanding,
				verticalSpeedFinalApp,
				verticalSpeedStepdown,
				verticalSpeedRoundout,
				rollBankStepdown,
				rollBankFinalApp,
				rollBankRoundout,
				timeApproach,
				timeLanding,
				timeTotal
			);
		}

		// Instantiate new score object
		score = new ScoreCalculation(
			name,
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
	 * Returns the minimum altitude MSL to complete the approach or go around in feet.
	 * @return minimum altitude
	 */
	public static int getMinimumsAltitude() {
		return minimumsAltitude;
	}
}
