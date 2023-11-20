package utils;

import java.io.BufferedReader;
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
 * Utility methods for manipulating CSV files.
 */
public class CsvUtils {

    /**
	 * Creates csv file and writes headers and data to the file.
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
     * @throws CsvValidationException
     * @throws IOException
     * @throws FileMergeException
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

    /**
    * Compares two files and returns true if their contents are the same.
    * @param expectedFile known file.
    * @param testFile file to test.
    * @return if the file contents match.
    */
   public static boolean compareFiles(String expectedFile, String testFile) {
		String eLine;
		String tLine;
		try (
			BufferedReader expectedReader = new BufferedReader(new FileReader(expectedFile));
			BufferedReader testFileReader = new BufferedReader(new FileReader(testFile));
		){
			while ((eLine = expectedReader.readLine()) != null && (tLine = testFileReader.readLine()) != null) {
				if (!eLine.equalsIgnoreCase(tLine)) {
					return false;
				}
			}
			return true;
		} catch (IOException e) {
			SystemLogger.writeToSystemLog(Level.WARNING, CsvUtils.class.getName(), "Could not compare " + expectedFile + " and " + testFile);
		}
		return false;
	}
}
