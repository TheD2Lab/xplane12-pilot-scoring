package xplane12_data_parser;
/*
 * How the Scoring Works
 * For every data point, we will assign 3 possible points (latitude, height, speed)
 * All the measurements will be given the same values: 1 point for latitude, 1 for height, and 1 for speed
 * For every mistake in latitude, height, or speed, the deduction will either be 1/4, 1/2, or 1 point off
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.opencsv.CSVWriter;


public class scoreCalculations {

	private final static int MAX_PTS_PER_DATA_POINT_ILS = 3;
	private final static int MAX_PTS_PER_DATA_POINT_ROUNDOUT = 1; 
	private final static int MAX_PTS_PER_DATA_POINT_LANDING = 2;

	private String participant;

	// [0] = actual score, [1] = highest possible score
	private double[] approachScore = {0,1};
	private double[] landingScore = {0,1};
	private double[] overallScore = {0,1};

	private int numOfData = 0;
	private int numOfStepdownData = 0; 
	private int numOfFinalApproachData = 0; 
	private int numOfLandingData = 0; 
	private int numOfRoundoutData = 0;

	private String stepdownFile;
	private String finalApproachFile;
	private String roundoutFile;
	private String landingFile;

	private FlightData data;

	private final static List<Fix> STEPDOWN_FIXES;
	static {
		List<Fix> tmp = new ArrayList<>();
		tmp.add(new Fix(22.2, 7000));
		tmp.add(new Fix(19.1, 6000));
		tmp.add(new Fix(15.9, 5000));
		tmp.add(new Fix(12.5, 4000));
		tmp.add(new Fix(6.3, 2200));

		STEPDOWN_FIXES = Collections.unmodifiableList(tmp);
	}

	private enum scoreType {
		APPROACH,
		LANDING,
		OVERALL
	}
	
	/**
	 * 
	 * @param name
	 * @param sdFile
	 * @param faFile
	 * @param rFile
	 * @param lFile
	 * @param numData
	 * @param numSD
	 * @param numFA
	 * @param numR
	 * @param numL
	 */
	public scoreCalculations(String name, String sdFile, String faFile, String rFile, String lFile,
		int numData, int numSD, int numFA, int numR, int numL, FlightData data) {

		this.participant = name;
		this.stepdownFile = sdFile;
		this.finalApproachFile = faFile;
		this.roundoutFile = rFile;
		this.landingFile = lFile;
		this.numOfData = numData;
		this.numOfStepdownData = numSD;
		this.numOfFinalApproachData = numFA;
		this.numOfRoundoutData = numR;
		this.numOfLandingData = numL;
		this.data = data;
		setMaxPoints();
		scoreCalc();
	}

	/**
	 * Calculates the max points for the overall score and sub-scores.
	 */
	private void setMaxPoints() {
		// set approach score to max possible points
		this.approachScore[1] = (this.numOfStepdownData + this.numOfFinalApproachData)*MAX_PTS_PER_DATA_POINT_ILS;
		this.approachScore[0] = this.approachScore[1];

		// set roundout score to max possible points
		this.landingScore[1] = this.numOfRoundoutData * MAX_PTS_PER_DATA_POINT_ROUNDOUT
			+ this.numOfLandingData * MAX_PTS_PER_DATA_POINT_LANDING;
		this.landingScore[0] = this.landingScore[1];

		// set max score to max possible points
		this.overallScore[1] = this.approachScore[1] + this.landingScore[1];
		this.overallScore[0] = this.overallScore[1];
	}

	/**
	 * returns the total score penalty for the localizer portion of the ILS approach
	 * @param horizontalDef is all of the localizer position of the aircraft
	 * @return double Returns the total penalty
	 */
	//For the lateral/vertical of the plane
	//Check if its within +/- 2 degrees of the designated line
	private double localizerScorePenalty(List<Double> horizontalDef) {
		double penalty = 0;
		for(double hdef : horizontalDef) {
			double absValue = Math.abs(hdef);
			if(absValue  < 0.5) {
				penalty += 0;
			} else if (absValue  <= 1) {
				penalty += 0.2;
			} else if (absValue  <= 1.5) {
				penalty += 0.4;
			} else if (absValue  <= 2) {
				penalty += 0.6;
			} else if (absValue  <= 2.5) {
				penalty += 0.8;
			} else {
				penalty += 1;
			}
		}
		return penalty;
	}

	/**
	 * returns the total score penalty for the glideslope portion of the ILS approach
	 * @param verticalDef is all of the vertical position of the aircraft
	 * @return double Returns the total penalty
	 */
	//For the lateral/vertical of the plane
	//Check if its within +/- 2 degrees of the designated line
	private double glideSlopeScorePenalty(List<Double> verticalDef) {
		double penalty = 0;
		for(double vdef : verticalDef) {
			double absValue = Math.abs(vdef);

			if (absValue < 0.5) {
				penalty += 0;
			} else if (absValue <= 1.0) {
				penalty += 0.2;
			} else if (absValue <= 1.5) {
				penalty += 0.4;
			} else if (absValue <= 2.0) {
				penalty += 0.6;
			} else if (absValue <= 2.5) {
				penalty += 0.8;
			} else {
				penalty += 1;
			}
		}
		return penalty;
	}

	/**
	 * returns the total altitude penalty for the stepdown portion of the ILS approach
	 * @param altitude the altitude of the aircraft during the ILS approach
	 * @return
	 */
	private double altitudeILSCalcPenalty(List<Double> dmes, List<Double>altitudes) {
		double penalty = 0;
		int currentFix = 0;
		Iterator<Double> dmeIter = dmes.iterator();
		Iterator<Double> altIter = altitudes.iterator();
		Double dme;
		Double alt;

		while(dmeIter.hasNext() && altIter.hasNext()) {
			dme = dmeIter.next();
			alt = altIter.next();
			// check which fix plane is approaching
			while (dme < STEPDOWN_FIXES.get(currentFix).dme) {
				currentFix++;
			}
			
			if (alt > STEPDOWN_FIXES.get(currentFix).altitude)
			{
				penalty += 0;
			}
			else if (alt < STEPDOWN_FIXES.get(currentFix).altitude - 100) {
				penalty += (STEPDOWN_FIXES.get(currentFix).altitude - alt) / 100;
			}
			else
			{
				penalty += 1;
			}
		}
		return penalty;
	}

	/**
	 * returns the total speed penalty for the speed portion of the ILS approach
	 * @param speed The speed of the aircraft during the ILS approach
	 * @return double Returns the total penalty
	 */
	private double speedILSCalcPenalty(List<Double> speeds) {
		double penalty = 0;
		int assignedSpeed = 90;
		for(double speed : speeds) {
			double difference = Math.abs(speed - assignedSpeed);
			if (difference < 10)
			{
				penalty += (difference / 10);
			}
			else {
				penalty += 1;
			}
		}
		return penalty;
	}

	public double scoreStepdownCalc(List<Double> horiDef, List<Double> speed, List<Double> altitude, List<Double> dme) {
		return localizerScorePenalty(horiDef) + speedILSCalcPenalty(speed) + altitudeILSCalcPenalty(dme, altitude);
	}

	/**
	 * returns the total penalty for the final approach. Based on the localizer, glideslope, and speed
	 * @param horiDef all of the localizer position of the aircraft
	 * @param speed The speed of the aircraft during the ILS approach
	 * @param vertDef all of the glideslope position of the aircraft
	 * @return double Returns the total penalty
	 */
	public double scoreFinalApproachCalc(List<Double> horiDef, List<Double> speed, List<Double> vertDef) {	
		return localizerScorePenalty(horiDef) + speedILSCalcPenalty(speed) + glideSlopeScorePenalty(vertDef);
	}

	/**
	 * returns the total penalty for the roundout phase. Based on altitude. Looking to see that the plane is continuously descending
	 * @param altitude contains all the altitude information for the aircraft
	 * @return double Returns the total Penalty
	 */
	public double scoreRoundOut(List<Double> altitude)
	{
		double penalty =0;
		double previousAlt = altitude.get(0);
		for(int altIndex = 1; altIndex < altitude.size(); altIndex++) {
			previousAlt = altitude.get(altIndex);
			if(previousAlt >= altitude.get(altIndex)) {
				continue;
			} else {
				penalty = MAX_PTS_PER_DATA_POINT_ROUNDOUT;
			}

		}
		return penalty;
	}

	/**
	 * returns the total penalty for the landing Phase. Based on centerline and altitude
	 * @param altitude contains all the altitude information for the aircraft
	 * @return double Returns the total Penalty
	 */
	public double scoreLanding(List<Double> altitude, List<Double> horizontalDef)
	{
		double penalty = 0; 
		penalty += localizerScorePenalty(horizontalDef);
		return penalty;
	}


	/**
	 * returns the total penalty for the approach and landing
	 * @param outputFolderPath directory to save output files
	 * @param name name of participant
	 */
	public void scoreCalc() {

		this.approachScore[0] -= scoreStepdownCalc(
			data.getHDefStepdown(), 
			data.getSpeedStepdown(), 
			data.getAltStepdown(), 
			data.getDmeStepdown()
		);

		this.approachScore[0] -= scoreFinalApproachCalc(
			data.getHDefFinalApproach(),
			data.getSpeedFinalApproach(),
			data.getVDefFinalApproach()
		);

		this.landingScore[0] -= scoreRoundOut(data.getAltLanding());
		this.landingScore[0] -= scoreLanding(data.getAltLanding(), data.getHDefLanding());

		this.overallScore[0] = this.landingScore[0] + this.approachScore[0];
	}
	
	// Below are housekeeping items

	public void writeToFile(String outputLocation) {
		String outputFile = outputLocation + "/" + this.participant + "_score.csv";
		String[] headers = {
			"Total Time",
			"Overall Score",
			"Approach Time",
			"Approach Score",
			"Landing Time",
			"Landing Score"
		};

		String[] data = {
			String.valueOf(this.data.getTimeTotal()),
			String.valueOf(getPercentageScore(scoreType.OVERALL)),
			String.valueOf(this.data.getTimeApproach()),
			String.valueOf(getPercentageScore(scoreType.APPROACH)),
			String.valueOf(this.data.getTimeLanding()),
			String.valueOf(getPercentageScore(scoreType.LANDING))
		};

		try (
			FileWriter outputFileWriter = new FileWriter(new File (outputFile));
			CSVWriter outputCSVWriter = new CSVWriter(outputFileWriter);
		){
			outputCSVWriter.writeNext(headers);
			outputCSVWriter.writeNext(data);
		}
		catch (FileNotFoundException e) {
			System.out.println("Unable to open file '" + outputFile + "'");
		}
		catch(IOException e) {
			System.out.println("Error writing to file '" + outputFile + "'");
		}
	}

	public double getPercentageScore(scoreType val) {
		switch(val) {
			case APPROACH:
				return this.approachScore[0] / this.approachScore[1];
			case LANDING:
				return this.landingScore[0] / this.landingScore[1];
			case OVERALL:
				return this.overallScore[0] / this.overallScore[1];
			default:
				return -1;
		}
	}

	public String getParticipant() {
		return this.participant;
	}

	/**
	 * @return the numOfData
	 */
	public int getNumOfData() {
		return this.numOfData;
	}

	/**
	 * @param numOfData the numOfData to set
	 */
	public void setNumOfData(int numOfData) {
		this.numOfData = numOfData;
	}

	public void setNumOfStepDownData(int numOfStepdownData) {
		this.numOfStepdownData = numOfStepdownData;
	}

	public int getNumOfStepDownData() {
		return this.numOfStepdownData;
	}

	/**
	 * @return the numOfILSData
	 */
	public int getNumOfFinalApproachData() {
		return this.numOfFinalApproachData;
	}

	/**
	 * @param numOfILSData the numOfILSData to set
	 */
	public void setNumOfFinalApproachData(int numOfILSData) {
		this.numOfFinalApproachData = numOfILSData;
	}

	/**
	 * @return the numOfLandingData
	 */
	public int getNumOfLandingData() {
		return this.numOfLandingData;
	}

	/**
	 * @param numOfLandingData the numOfLandingData to set
	 */
	public void setNumOfLandingData(int numOfLandingData) {
		this.numOfLandingData = numOfLandingData;
	}

	/**
	 * @return the numOfRoundoutData
	 */
	public int getNumOfRoundoutData() {
		return this.numOfRoundoutData;
	}

	/**
	 * @param numOfRoundoutData the numOfRoundoutData to set
	 */
	public void setNumOfRoundoutData(int numOfRoundoutData) {
		this.numOfRoundoutData = numOfRoundoutData;
	}

	public void setStepdownFile(String filePath) {
		this.stepdownFile = filePath;
	}
	public String getStepdownFile() {
		return this.stepdownFile;
	}
	public void setFinalApproachFile(String filePath) {
		this.finalApproachFile = filePath;
	}
	public String getFinalApproachFile(String filePath) {
		return this.finalApproachFile;
	}
	public void setRoundoutFile(String filePath) {
		this.roundoutFile = filePath;
	}
	public String getRoundoutFile() {
		return this.roundoutFile;
	}
	public void setLandingFile(String filePath) {
		this.landingFile = filePath;
	}
	public String getLandingFile() {
		return this.landingFile;
	}
}
