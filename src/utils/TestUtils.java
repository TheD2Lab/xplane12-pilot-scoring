package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import utils.systemLogger;

public class TestUtils {

   /**
    * Compares two files and returns true if their contents are the same.
    * @param expectedFile known file
    * @param testFile file to test
    * @return if the file contents match
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
			systemLogger.writeToSystemLog(Level.WARNING, TestUtils.class.getName(), "Could not compare " + expectedFile + " and " + testFile);
		}
		return false;
	}
}
