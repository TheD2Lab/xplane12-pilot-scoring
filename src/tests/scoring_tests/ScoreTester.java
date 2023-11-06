package tests.scoring_tests;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import utils.SystemLogger;
import scoring.Parser;
import scoring.ScoreCalculation;
import scoring.ScoreCalculation.scoreType;
import utils.TestUtils;

public class ScoreTester {
	
	private static String inputFile;
	private static String outputFolder;
	private static String expectedFolder;
	private static String name = "Test";
	public static void main(String[] args) {
		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString() + "/src/tests/scoring_tests/";
		inputFile = currentPath + "XPLaneTest.csv";
		outputFolder = currentPath + "output/";
		new File(outputFolder).mkdirs();
		expectedFolder = currentPath + "expected_output/";
		SystemLogger.createSystemLog(outputFolder);
		ScoreCalculation score = generateOutput();
		checkApproach(score);
	}

	private static ScoreCalculation generateOutput() {
		ScoreCalculation score = Parser.parseOutSections(inputFile, outputFolder, name);
		score.writeToFile(outputFolder);
		SystemLogger.writeToSystemLog(Level.INFO, inputFile, expectedFolder);
		return score;
	}

	private static void checkApproach(ScoreCalculation score) {
		assert score.getPercentageScore(scoreType.APPROACH) == 32.493672/45 : "Incorrect Approach Score";
	}

	private static void checkScoresFile() {
		String expectedFile = expectedFolder + "/Expected_aoi_transitionFeatures.csv";
		String testFile = outputFolder + "/Test_aoi_transitionFeatures.csv";
		assert TestUtils.compareFiles(expectedFile, testFile) : "Incorrect Transition Features";
	}
	
}
