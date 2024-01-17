package scoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

public class Parser {

	// For ILS 34R KSEA. Change for different airport
	private static int minimumsAltitude = 572;
	private static double initialAppFixDME = 22.2;
	private static double intersectionDME = 6.3;
	private static DateTimeFormatter[] sysTimeFormat = {
		DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss"),
		DateTimeFormatter.ofPattern("MM/dd/yy kk:mm")
	};
	
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
			int lineNum = 0;
			while ((row = csvReader.readNext()) != null) {
				String[] selectedRow = new String[columnIndex.length];
				lineNum++;

				processRow: {	// label used to mimic for-else loop without boolean flags
					for (int i = 0; i < columnIndex.length; i++) 
					{
						if (columnIndex[i] >= row.length) {
							System.out.printf("%s: Removed data line number %d\n", name, lineNum);
							break processRow;
						}
						selectedRow[i] = row[columnIndex[i]];
					}
					selectedColumns.add(selectedRow);
				}

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
	public static ScoreCalculation parseOutSections(String filePath, String outputFolderPath, String name) {

		ScoreCalculation score;
		String basePathName = outputFolderPath + "//" + name + "_flight_data";
		String stepdownOutputFilePath = basePathName + "_stepdown.csv";
		String finalApproachOutputFilePath = basePathName + "_approach.csv";
		String roundOutOutputFilePath = basePathName + "_roundout.csv";
		String landingOutputFilePath = basePathName+ "_landing.csv";

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
		String timeApproachStartStr = null;
		String timeApproachEndStr = null;
		String timeTotalStr = null;
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
			outputStepdownCSVWriter.writeNext(headers);
			outputFinalApproachCSVWriter.writeNext(headers);
			outputRoundOutCSVWriter.writeNext(headers);
			outputLandingCSVWriter.writeNext(headers);
			String[] row;
			
			while ((row = csvReader.readNext()) != null) 
			{
				// Do not start scoring until after participant reaches Initial Approach Fix - JIPOX
				if (Double.valueOf(row[dmeIndex]) > initialAppFixDME) {
					if (sysTimesIndex != -1 && beginFlightTimestamp == null) {
						beginFlightTimestamp = parseTime(row[sysTimesIndex]);
					}
					timeApproachStartStr = row[timeIndex];
					continue;
				
				// ILS Stepdown portion
				} else if(Double.valueOf(row[dmeIndex]) < initialAppFixDME && Double.valueOf(row[dmeIndex])>intersectionDME) {
					outputStepdownCSVWriter.writeNext(row);
					numStepdown++;
					altStepdown.add(Double.valueOf(row[altitudeIndex]));
					dmeStepdown.add(Double.valueOf(row[dmeIndex]));
					speedStepdown.add(Double.valueOf(row[speedIndex]));
					hDefStepdown.add(Double.valueOf(row[hdefIndex]));
					rollBankStepdown.add(Double.valueOf(row[rollBankAngleIndex]));
					verticalSpeedStepdown.add(Double.valueOf(row[verticalSpeedIndex]));
					if (sysTimesIndex != -1 && beginApproachTimestamp == null) {
						beginApproachTimestamp = parseTime(row[sysTimesIndex]);
					} 

				// ILS Final Approach portion
				} else if(Double.valueOf(row[altitudeIndex])>minimumsAltitude) {
					outputFinalApproachCSVWriter.writeNext(row);
					numFinalApproach++;
					vDefFinalApproach.add(Double.valueOf(row[vdefIndex]));
					speedFinalApproach.add(Double.valueOf(row[speedIndex]));
					verticalSpeedFinalApp.add(Double.valueOf(row[verticalSpeedIndex]));
					hDefFinalApproach.add(Double.valueOf(row[hdefIndex]));
					timeApproachEndStr = row[timeIndex];
					rollBankFinalApp.add(Double.valueOf(row[rollBankAngleIndex]));

				// Roundout portion: From minimums, descent to the runway portion
				} else if(!(Double.valueOf(row[groundRollIndex])>0)){
					outputRoundOutCSVWriter.writeNext(row);
					numRoundout++;
					hDefRoundout.add(Double.valueOf(row[hdefIndex]));
					altRoundout.add(Double.valueOf(row[altitudeIndex]));
					rollBankRoundout.add(Double.valueOf(row[rollBankAngleIndex]));
					verticalSpeedRoundout.add(Double.valueOf(row[verticalSpeedIndex]));
					if (sysTimesIndex != -1 && beginRoundOutTimestamp == null) {
						beginRoundOutTimestamp = parseTime(row[sysTimesIndex]);
					} 
					
				// Wheels touch the ground portion
				} else {
					outputLandingCSVWriter.writeNext(row);
					numLanding++;
					altLanding.add(Double.valueOf(row[altitudeIndex]));
					hDefLanding.add(Double.valueOf(row[hdefIndex]));
					if (sysTimesIndex != -1 && beginLandingTimestamp == null) {
						beginLandingTimestamp = parseTime(row[sysTimesIndex]);
					} 
				}

				if (Double.valueOf(row[speedIndex]) > 0) {
					timeTotalStr = row[timeIndex];
					if (sysTimesIndex != -1)
						endFlightTimeString = row[sysTimesIndex];
				}
			}
			if (endFlightTimeString != null) {
				endFlightTimestamp = parseTime(endFlightTimeString);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println(e);
		}

		timeTotal = Double.valueOf(timeTotalStr);

		if (timeApproachStartStr != null) {
			 double timeApproachStart = Double.valueOf(timeApproachStartStr);
			if (timeApproachEndStr != null) {
				// pilot completed the approach. 
				// Time elapsed during approach = [approach end time] - [approach start time]
				double timeApproachEnd = Double.valueOf(timeApproachEndStr);
				timeApproach = timeApproachEnd - timeApproachStart;
				timeLanding = timeTotal - timeApproachEnd;
			} else {
				// pilot crashed before completing approach. 
				// Time elapsed during approach = [total flight time] - [approach start time]
				timeApproach = timeTotal - Double.valueOf(timeApproachStartStr);
				timeLanding = 0;
			}
		} else {
			// pilot crashed before Initial Approach Fix.
			timeApproach = 0;
			timeLanding = 0;
		}

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

	public static LocalDateTime parseTime(String timeString) {
		LocalDateTime time = null;
		for (DateTimeFormatter format : sysTimeFormat) {
			try {
				time = LocalDateTime.parse(timeString, format);
				break;
			} catch (DateTimeParseException e) {
				// keep looping
			}
		}
		return time;
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
		Parser.minimumsAltitude = minimumsAltitude;
	}

}
