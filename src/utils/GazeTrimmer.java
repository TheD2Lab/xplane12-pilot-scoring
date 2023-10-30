package utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.Pair;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

public class GazeTrimmer {


	public static void trimGaze(String inputFile, String outputFolder, List<Pair<String, Double>> timestamps) {
		
	}


	/**
	 * creates a CSV file containing all of the data points within the given start and end times based on the input file
	 * 
	 * @param inputFile		the CSV file containing the data points that will be copied
	 * @param fileName		the name of the input CSV file
	 * @param outputFolder	the path of the output location
	 * @param start			the start time
	 * @param end				the end time
	 * @return boolean		true if the program was able to successfully execute it, false otherwise
	 */
	private static boolean trimCSV(String inputFile, String fileName, String outputFolder, int start, int end) throws IOException, CsvValidationException
	{
		String outputFile = outputFolder + fileName;

		try (
			FileWriter outputFileWriter = new FileWriter(new File (outputFile));
			CSVWriter outputCSVWriter = new CSVWriter(outputFileWriter);
			FileReader fileReader = new FileReader(inputFile);
			CSVReader csvReader = new CSVReader(fileReader);
		){
			String[]nextLine = csvReader.readNext();
			outputCSVWriter.writeNext(nextLine);
			
			int timestampIndex = -1;
			for(int i = 0; i < nextLine.length; i++)
			{
				if(nextLine[i].contains("TIME("))
				{
					timestampIndex = i;
					break;
				}
			}
			
			while((nextLine = csvReader.readNext()) != null) 
			{
				if(Double.valueOf(nextLine[timestampIndex]) < start)
				{
					continue;
				}
				else if(Double.valueOf(nextLine[timestampIndex]) > end)
				{
					break;
				}
				else
				{
					outputCSVWriter.writeNext(nextLine);
				}
			}

			if((nextLine = csvReader.readNext()).equals(null))
			{
				return false;
			}
				
			systemLogger.writeToSystemLog(Level.INFO, GazeTrimmer.class.getName(), "Successfully created file " + outputFile );
		} catch(NullPointerException e) {
			return false;
		}
		catch(Exception e) {
			systemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Error with window method  " + outputFile + "\n" + e.toString());
			System.exit(0);
		}
		return true;
	}
}