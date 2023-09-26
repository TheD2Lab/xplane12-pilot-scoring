package xplane12_data_parser;

import java.io.File;

import org.apache.commons.compress.utils.FileNameUtils;

public class Main {

	//Changes in the Airport: All you would need to do is change the field elevation and the minimums as shown in the approach chart\
	//Changes in Aircraft: All aircraft has different approach speed, the speeds for the method named SpeedILSCalcPenalty will need to be changed
	//Changes in the weight of scoring each section: Currently every single method of scoring is weighted the same. If you would like to change this
	//you would only need to change the  MAX_PTS_PER_DATA_POINT_ILS, MAX_PTS_PER_DATA_POINT_ROUNDOUT,  MAX_PTS_PER_DATA_POINT_LANDING
	public static void main(String[] args) {
		
		String txtFilePath;
		String outputFolderPath;

		// Initialize paths
		if (args.length == 2) {
			txtFilePath = args[0];
			outputFolderPath = args[1];
		}
		else {
			System.out.println("Text file and output directory not specified.");
			return;
		}

		// Check that input file and directory exist
		if (!new File(txtFilePath).exists()) {
			System.out.println("Input text file does not exist.");
			return;
		}
		else if (!new File(outputFolderPath).isDirectory()) {
			System.out.println("Output directory path does not exits.");
			return;
		}

		// Get name to append to directory and files
		String name = FileNameUtils.getBaseName(txtFilePath);
		String namedOutputFolder = outputFolderPath + "/" + name + "_scoring";
		new File(namedOutputFolder).mkdirs();


		// Parse the CSV files for the data points
		String originalCSVFilePath = parser.txtToCSV(txtFilePath, namedOutputFolder, name);
		String refactoredCSVFilePath = parser.parseData(originalCSVFilePath, namedOutputFolder, name);

		//initializes the start and stop time for the ILS, Roundout, and landing phases
		parser.initializeNumbers();
		parser.parseOutSections(refactoredCSVFilePath, namedOutputFolder, name);
		
		// Calculate the score for the approach to landing
		scoreCalculations score = new scoreCalculations();
		score.scoreCalc(namedOutputFolder, name);
		
		// Print out values
		//System.out.println("Data Points = " + score.getDataPoints() + " * 3 = Highest Possible Points = " + score.getHighestScore() + "\n");
		System.out.println(score.getTotalScore() + " / " + score.getHighestScorePossible() + ":");
		System.out.println(" %" + score.getPercentageScore());
	}
	
}