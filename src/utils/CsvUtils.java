package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import exceptions.FileMergeException;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Utility methods for manipulating CSV files
 */
public class CsvUtils {

    /**
	 * Creates and writes data to a csv file
	 * @param outputFile	name of file to write to
	 * @param headers		table headers
	 * @param data			table data
	 * @return				whether the data was written to file
	 */
	public static boolean writeCsvFile(String outputFile, String[] headers, List<String[]> data) {

		try (
			FileWriter outputFileWriter = new FileWriter(new File (outputFile));
			CSVWriter outputCSVWriter = new CSVWriter(outputFileWriter);
		){
			outputCSVWriter.writeNext(headers);
			outputCSVWriter.writeAll(data);
		} catch (FileNotFoundException e) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Unable to open file" + outputFile + "\n" + e.toString());
			return false;
		} catch (IOException e) {
			SystemLogger.writeToSystemLog(Level.SEVERE, GazeTrimmer.class.getName(), "Error writing to" + outputFile + "\n" + e.toString());
			return false;
		} 
		return true;
	}

    /**
     * Combines a list of csv files with identical header into one csv file.
     * @param outputFile    Path to csv file to write to
     * @param filesToMerge  List of csv files to merge
     */
    public static void mergeCsvFile(String outputFile, List<String> filesToMerge) throws CsvValidationException, IOException, FileMergeException {
        String[] headers = null;
        try(
            FileWriter fileWriter = new FileWriter(outputFile);
            CSVWriter csvWriter = new CSVWriter(fileWriter);
        ) {        
            Iterator<String> iterFiles = filesToMerge.listIterator();
            while (iterFiles.hasNext()) {
                String currentFile = iterFiles.next();

                FileReader fileReader = new FileReader(currentFile);
                CSVReader csvReader = new CSVReader(fileReader);
                String[] line = null;
                String[] firstLine = null;
                if ((line = csvReader.readNext()) != null)
                    firstLine = line;
                    if (headers == null)
                        headers = firstLine;
                        csvWriter.writeNext(headers);

                if (!Arrays.equals (headers, firstLine)) {
                    csvReader.close();
                    throw new FileMergeException("Header mis-match between CSV files: '" +
                            outputFile + "' and '" + currentFile);
                }         
                while ((line = csvReader.readNext()) != null) {
                    csvWriter.writeNext(line);
                }
                csvReader.close();
            }
        }
	}
}
