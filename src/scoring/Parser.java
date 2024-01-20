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
		List<FlightDataPoint> stepdownData = new LinkedList<>();
		List<FlightDataPoint> finalApproachData = new LinkedList<>();
		List<FlightDataPoint> roundoutData = new LinkedList<>();
		List<FlightDataPoint> landingData = new LinkedList<>();
		Double timeApproachStart = null;
		Double timeRoundoutStart = null;
		String timeTotalStr = null;
		double timeApproach = 0;
		double timeLanding = 0;
		double timeTotal = 0;

		// timestamps
		LocalDateTime beginFlightTimestamp = null;
   	LocalDateTime beginApproachTimestamp = null;
   	LocalDateTime beginRoundOutTimestamp = null;
   	LocalDateTime beginLandingTimestamp = null;
   	LocalDateTime endFlightTimestamp = null;
		String endFlightTimeString = null;

		// the indexes for the X-Plane data we use for scoring and statistics
		DataIndex indexes = new DataIndex();

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
						indexes.setiSysTime(i);
						break;
					case "p-alt,ftMSL":
						indexes.setiAlt(i);
						break;
					case"pilN1,dme-d":
						indexes.setiDme(i);
						break;
					case "pilN1,h-def":
						indexes.setiHdef(i);
						break;
					case "_Vind,_kias":
						indexes.setiASpeed(i);
						break;
					case "engn1,__rpm":
						indexes.setiEng(i);
						break;
					case "pilN1,v-def":
						indexes.setiVdef(i);
						break;
					case "missn,_time":
						indexes.setiMTime(i);
						break;
					case "_land,groll":
						indexes.setiGroll(i);
						break;
					case "__VVI,__fpm":
						indexes.setiVspeed(i);
						break;
					case "_roll,__deg":
						indexes.setiBank(i);
						break;
					case "hding,__mag":
						indexes.setiHead(i);
						break;
					case "__lat,__deg":
						indexes.setiLatitude(i);
						break;
					case "__lon,__deg":
						indexes.setiLongitude(i);
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
				if (Double.valueOf(row[indexes.getiDme()]) > initialAppFixDME) {
					if (indexes.getiSysTime() != -1 && beginFlightTimestamp == null) {
						beginFlightTimestamp = parseTime(row[indexes.getiSysTime()]);
					}
					continue;
				
				// ILS Stepdown portion
				} else if(Double.valueOf(row[indexes.getiDme()]) < initialAppFixDME && Double.valueOf(row[indexes.getiDme()])>intersectionDME) {
					outputStepdownCSVWriter.writeNext(row);
					stepdownData.add(listToDataPoint(row, indexes));
					if (timeApproachStart == null) {
						timeApproachStart	= Double.parseDouble(row[indexes.getiMTime()]); 
						if (indexes.getiSysTime() != -1 && beginApproachTimestamp == null) {
							beginApproachTimestamp = parseTime(row[indexes.getiSysTime()]);
						} 

					}
					

				// ILS Final Approach portion
				} else if(Double.valueOf(row[indexes.getiAlt()])>minimumsAltitude) {
					outputFinalApproachCSVWriter.writeNext(row);
					finalApproachData.add(listToDataPoint(row, indexes));

				// Roundout portion: From minimums, descent to the runway portion
				} else if(!(Double.valueOf(row[indexes.getiGroll()])>0)){
					outputRoundOutCSVWriter.writeNext(row);
					roundoutData.add(listToDataPoint(row, indexes));
					if (timeRoundoutStart == null) {
						timeRoundoutStart = Double.parseDouble(row[indexes.getiMTime()]);
						if (indexes.getiSysTime() != -1 && beginRoundOutTimestamp == null) {
							beginRoundOutTimestamp = parseTime(row[indexes.getiSysTime()]);
						}
					}
					
				// Wheels touch the ground portion
				} else {
					outputLandingCSVWriter.writeNext(row);
					landingData.add(listToDataPoint(row, indexes));

					if (indexes.getiSysTime() != -1 && beginLandingTimestamp == null) {
						beginLandingTimestamp = parseTime(row[indexes.getiSysTime()]);
					} 
				}

				if (Double.valueOf(row[indexes.getiASpeed()]) > 0) {
					timeTotalStr = row[indexes.getiMTime()];
					if (indexes.getiSysTime() != -1)
						endFlightTimeString = row[indexes.getiSysTime()];
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


		if (timeApproachStart != null) {
			if (timeRoundoutStart != null) {
				// pilot completed the approach. 
				// Time elapsed during approach = [approach end time] - [approach start time]
				timeApproach = timeRoundoutStart - timeApproachStart;
				timeLanding = timeTotal - timeRoundoutStart;
			} else {
				// pilot crashed before completing approach. 
				// Time elapsed during approach = [total flight time] - [approach start time]
				timeApproach = timeTotal - timeApproachStart;
				timeLanding = 0;
			}
		} else {
			// pilot crashed before Initial Approach Fix.
			timeApproach = 0;
			timeLanding = 0;
		}

		if (timeApproach < 0 || timeLanding < 0) {
			System.out.println(name + " time is negative");
		}

		// construct flight data object to pass to scorer
		if (indexes.getiSysTime() == -1) {
			data = new FlightData(
				stepdownData,
				finalApproachData,
				roundoutData,
				landingData,
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
				stepdownData,
				finalApproachData,
				roundoutData,
				landingData,
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
			data
		);
		
		return score;

	}

	public static FlightDataPoint listToDataPoint(String[] entry, DataIndex indexes) {
		return new FlightDataPoint(
			Double.parseDouble(entry[indexes.getiMTime()]),
			Double.parseDouble(entry[indexes.getiASpeed()]),	
			Double.parseDouble(entry[indexes.getiEng()]),
			Double.parseDouble(entry[indexes.getiBank()]),
			Double.parseDouble(entry[indexes.getiGroll()]),
			Double.parseDouble(entry[indexes.getiVspeed()]),
			Double.parseDouble(entry[indexes.getiAlt()]),
			Double.parseDouble(entry[indexes.getiHead()]),
			Double.parseDouble(entry[indexes.getiLatitude()]),
			Double.parseDouble(entry[indexes.getiLongitude()]),
			Double.parseDouble(entry[indexes.getiDme()]),
			Double.parseDouble(entry[indexes.getiHdef()]),
			Double.parseDouble(entry[indexes.getiVdef()])
		);
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
