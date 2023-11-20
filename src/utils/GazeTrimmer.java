package utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.Iterator;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.Pair;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Class for modifying gaze files according to x-plane output.
 */
public class GazeTrimmer {

	/**
	 * Trims gaze csv files into windows associated with different parts the simulation. Timestamps are the start times of each window.
	 * Windows end at the start of the following timestamp. Make sure to include an ending timestamp for the last window to trimmed, otherwise 
	 * the window will not be created. If you are unsure of the end, use 'Pair&lt;"End", LocalDateTime.now()&gt;' assuming all other timestamps occurred before
	 * calling the timeGazeFile method.
	 * @param inputFile		CSV file to be trimmed or cut into windows.
	 * @param outputFolder	Directory to save new csv files.
	 * @param timestamps		List of labels and timestamps of important parts of the simulation.
	 * @return					whether trim was successful.
	 */
	public static boolean trimGazeFile(String inputFile, String outputFolder, List<Pair<String, LocalDateTime>> timestamps) {

		if (timestamps.size() == 0) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "No window timestamps were provided\n");
		}
		LocalDateTime gazeStartTime = null;
		DateTimeFormatter gpTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd kk:mm:ss.SSS");	// time format in gazepoint data
		String fileName = FileNameUtils.getBaseName(inputFile);

		// we are using iterator in case a linked list is passed in for timestamps. Review your data structures if you don't understand the reason.
		Iterator<Pair<String, LocalDateTime>> tsIterator = timestamps.iterator();
		// we are using LinkedLists because insertion is faster and we will never need to index this data structure
		List<String[]> data = new LinkedList<>();

		try(
			FileReader fileReader = new FileReader(inputFile);
			CSVReader csvReader = new CSVReader(fileReader);
		){
			String[] headers = csvReader.readNext();
			String[] nextLine = null;
			
			int timestampIndex = -1;
			for(int i = 0; i < headers.length; i++)
			{
				if (headers[i].contains("TIME("))
				{
					String timeHeader = headers[i];
					gazeStartTime = LocalDateTime.parse(timeHeader.substring(timeHeader.indexOf("(")+1, timeHeader.indexOf(")")), gpTimeFormat);
					timestampIndex = i;
					break;
				}
			}

			// check there is a timestamp index
			if (timestampIndex == -1) {
				SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "No time column in file" + inputFile +"\n");
				return false;
			}

			double end;
			double timeRecord;
			Pair<String, LocalDateTime> nextTimestamp = tsIterator.next();
			// throw away start data from before the start of the first timestamp
			while((nextLine = csvReader.readNext()) != null) {
				end = Duration.between(gazeStartTime, nextTimestamp.right).toSeconds();
				timeRecord = Double.valueOf(nextLine[timestampIndex]);
				if (timeRecord >= end) {
					break;
				}
			}

			while(tsIterator.hasNext()) {
				String windowName = nextTimestamp.left;
				nextTimestamp = tsIterator.next();
				end = Duration.between(gazeStartTime, nextTimestamp.right).toSeconds();
				while((nextLine = csvReader.readNext()) != null) {
					timeRecord = Double.valueOf(nextLine[timestampIndex]);
					if (timeRecord < end) {
						data.add(nextLine);
						continue;
					}

					// write data to file 
					String outputFile = outputFolder+ "/" + fileName + "_" + windowName + ".csv";
					CsvUtils.writeCsvFile(outputFile, headers, data);
					data = new LinkedList<>(); 	// clear data list
					data.add(nextLine);
					// go to the next time window
					break;
				}
			}

		} catch(FileNotFoundException e) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Unable to open file\n" + e.toString());
			return false;
		} catch(IOException e) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Unable to read file\n" + e.toString());
			return false;
		} catch (CsvValidationException e) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Unable to read csv file\n" + e.toString());
			return false;
		} catch (DateTimeParseException e) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Time format cannot be parsed\n" + e.toString());
			return false;
		} catch (Exception e) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Unexpected error\n" + e.toString());
			return false;
		}

		return true;
	}

	/**
	 * Used for testing GazeTrimmer class.
	 * @param args none
	 */
	public static void main(String[] args) {
		SystemLogger.createSystemLog("C:/Users/ashkj/Documents/GitHub/xplane12-pilot-scoring/src/utils");
		String inputFile = "data/Kayla _fixations.csv";
		String outputFolder = "";
		List<Pair<String, LocalDateTime>> times = new LinkedList<>();
		DateTimeFormatter gpTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd kk:mm:ss.SSS");
		Pair<String, LocalDateTime> start = new Pair<>("start", LocalDateTime.parse("2022/12/08 13:09:00.024", gpTimeFormat));
		Pair<String, LocalDateTime> middle = new Pair<>("middle", LocalDateTime.parse("2022/12/08 13:09:30.024", gpTimeFormat));
		Pair<String, LocalDateTime> end = new Pair<>("end", LocalDateTime.parse("2022/12/08 13:10:10.024", gpTimeFormat));
		times.add(start);
		times.add(middle);
		times.add(end);
		trimGazeFile(inputFile, outputFolder, times);
	}

}