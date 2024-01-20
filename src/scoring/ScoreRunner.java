package scoring;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.Pair;
import utils.GazeTrimmer;
import java.time.LocalDateTime;

public class ScoreRunner {

	//Changes in the Airport: All you would need to do is change the field elevation and the minimums as shown in the approach chart\
	//Changes in Aircraft: All aircraft has different approach speed, the speeds for the method named SpeedILSCalcPenalty will need to be changed
	//Changes in the weight of scoring each section: Currently every single method of scoring is weighted the same. If you would like to change this
	//you would only need to change the  MAX_PTS_PER_DATA_POINT_ILS, MAX_PTS_PER_DATA_POINT_ROUNDOUT,  MAX_PTS_PER_DATA_POINT_LANDING

	/**
	 * entry point of scoring calculation program
	 * @param args[1] output directory path
	 * @param args[1] xplane data file path
	 */
	public static void main(String[] args) {

		String outputFolderPath;
		String xplaneFilePath;

		// Initialize paths
		if (args.length >= 2) {
			outputFolderPath = args[0];
			xplaneFilePath = args[1];
		}
		else {
			System.out.println("Text file or output directory not specified.");
			return;
		}

		// Check that input file and directory exist
		if (!new File(xplaneFilePath).exists()) {
			System.out.println("Input text file does not exist.");
			return;
		}
		else if (!new File(outputFolderPath).isDirectory()) {
			System.out.printf("Output directory path %s does not exits.");
			return;
		}

		// Get name to append to directory and files
		String pid = FileNameUtils.getBaseName(xplaneFilePath).split("_")[0];
		String xplaneExtension = FileNameUtils.getExtension(xplaneFilePath);
		String outputFolder = outputFolderPath + "/" + pid;
		String trimOutputFolder = outputFolder + "/" + pid + "_trim";
		new File(outputFolder).mkdirs();
		new File(trimOutputFolder).mkdirs();

		if (xplaneExtension.equals("txt")) {
			// Change txt to csv file
			String originalCSVFilePath = Parser.txtToCSV(xplaneFilePath, trimOutputFolder, pid);
			// file to grade is the new cleansed csv from the original text file
			xplaneFilePath = Parser.parseData(originalCSVFilePath, trimOutputFolder, pid);

			//initializes the start and stop time for the ILS, Roundout, and landing phase

		} else {
			System.out.println("Xplane data is not in a supported file type");
			return;
		}

		// generate pilot success score and other metrics
		System.out.printf("Scoring %s's data... ", pid);
		ScoreCalculation score = Parser.parseOutSections(xplaneFilePath, trimOutputFolder, pid);
		score.writeToFile(outputFolder);
		System.out.println("Done scoring...");

		FlightData flightData = score.getFlightData();
		// trim gaze files
		if (flightData.getBeginFlightTimestamp() == null) {
			System.out.println("Did not find system timestamps...");
			return;
		} else if (args.length < 3){
			System.out.println("No files to trim...");
			return;
		}

		runTrim(flightData, trimOutputFolder, Arrays.copyOfRange(args, 2, args.length));
	}
		


	private static void runTrim(FlightData flightData, String outputFolder, String[] gazeFiles) {

		List<Pair<String, LocalDateTime>> times = new LinkedList<>();
		
		// Change timestamps in times to change how the gaze files are cut/trimmed
		if (flightData.getBeginApproachTimestamp() != null)
			times.add(new Pair<>("approach", flightData.getBeginApproachTimestamp()));
		if (flightData.getBeginRoundOutTimestamp() != null)
			times.add(new Pair<>("roundout", flightData.getBeginRoundOutTimestamp()));
		if (flightData.getBeginLandingTimestamp() != null)
			times.add(new Pair<>("landing", flightData.getBeginLandingTimestamp()));
		if (flightData.getEndFlightTimestamp() != null)
			times.add(new Pair<>("end flight", flightData.getEndFlightTimestamp()));

		if (times.size() > 0) {
			// trim files
			for (int i = 0; i < gazeFiles.length; i++) {
				GazeTrimmer.trimGazeFile(gazeFiles[i], outputFolder, times);
			}
		}
	}
}