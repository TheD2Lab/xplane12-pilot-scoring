package scoring;
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
import java.util.List;
import com.opencsv.CSVWriter;


public class ScoreCalculation {

	private final static int MAX_PTS_PER_DATA_POINT_ILS = 3;
	private final static int MAX_PTS_PER_DATA_POINT_ROUNDOUT = 2; 
	private final static int MAX_PTS_PER_DATA_POINT_LANDING = 2;
	private final static int TARGET_SPEED = 90;

	private String participant;

	// [0] = actual score, [1] = highest possible score
	private double[] approachScore = {0,1};
	private double[] landingScore = {0,1};
	private double[] overallScore = {0,1};
	
	// The data for this particular score
	private FlightData data;
		
	// Files
	private String stepdownFile;
	private String finalApproachFile;
	private String roundoutFile;
	private String landingFile;
	
	// Additional X-Plane measures
	private double averageILSSpeed = 0; // Includes stepdown portion and final approach portion
	private double speedAddedTotal = 0;
	private double averageILSVerticalSpeed = 0; // Does not include stepdown portion
	private double verticalSpeedAddedTotal = 0;
	private double averageLocalizerDeflection = 0; // Includes stepdown portion and final approach portion
	private double localizerAddedTotal = 0;
	private double averageGlideslopeDeflection = 0; // Does not include stepdown portion
	private double glideslopeAddedTotal = 0;
	private double averageBankAngle = 0; // Includes stepdown portion and final approach portion
	private double bankAngleAddedTotal = 0;
	private double maxBankAngle = 0; // Includes ILS and final descent to runway

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

	public enum scoreType {
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
	public ScoreCalculation(String name, String sdFile, String faFile, String rFile, String lFile, FlightData data) {

		this.participant = name;
		this.stepdownFile = sdFile;
		this.finalApproachFile = faFile;
		this.roundoutFile = rFile;
		this.landingFile = lFile;
		this.data = data;
		setMaxPoints();
		scoreCalc();
	}

	/**
	 * Calculates the max points for the overall score and sub-scores.
	 */
	private void setMaxPoints() {

		double possible_points;
		// set approach score to max possible points
		possible_points = (this.data.getApproachData().size() + this.data.getStepdownData().size())*MAX_PTS_PER_DATA_POINT_ILS; 

		if(possible_points > 0) {
			this.approachScore[1] = possible_points;
			this.approachScore[0] = possible_points;
		} else {
			this.approachScore[1] = 1;
			this.approachScore[0] = 0;
		}

		// set roundout score to max possible points
		possible_points = this.data.getRoundoutData().size() * MAX_PTS_PER_DATA_POINT_ROUNDOUT
			+ this.data.getLandingData().size() * MAX_PTS_PER_DATA_POINT_LANDING; 
		if (possible_points > 0) {
			this.landingScore[1] = possible_points;
			this.landingScore[0] = possible_points;
		}
		

		// set max score to max possible points
		this.overallScore[1] = this.approachScore[1] + this.landingScore[1];
		this.overallScore[0] = this.approachScore[0] + this.landingScore[0];
	}

	/**
	 * Returns the localizer penalty for a single data point.
	 * @param hdef is all of the localizer position of the aircraft in dots
	 * @return returns the penalty
	 */
	private double localizerScorePenalty(double hdef, double bankAngle) {
		double penalty = 0;
		double absValueLoc = Math.abs(hdef);
		double absValueBank = Math.abs(bankAngle);

		localizerAddedTotal += absValueLoc;
		bankAngleAddedTotal += absValueBank;
		
		if (absValueBank > Math.abs(this.maxBankAngle))
		{
			this.maxBankAngle = absValueBank;
		}
		
		if (absValueBank < 15 && absValueLoc < 2.5) {
			penalty = absValueLoc / 2.5;	
		} else {
			penalty = 1;
		}
		
		return penalty;
	}
	
	/**
	 * returns the total score penalty for the localizer in the landing portion
	 * @param hdef is all of the localizer position of the aircraft in dots
	 * @return double Returns the total penalty
	 */
	private double localizerScorePenaltyLanding(double hdef) {
		double penalty = 0;
		double absValueHdef = Math.abs(hdef);
		localizerAddedTotal += absValueHdef;	

		if(absValueHdef  < 2.5) {
			penalty = absValueHdef / 2.5;
		} else {
			penalty = 1;
		}
		
		return penalty;
	}

	/**
	 * returns the total score penalty for the glideslope portion of the ILS approach
	 * @param vdef is all of the vertical position of the aircraft in dots
	 * @return double Returns the total penalty
	 */
	private double glideSlopeScorePenalty(double vdef, double vspeed) {
		double penalty = 0;
		verticalSpeedAddedTotal += vspeed;
		
		double absValueVDef = Math.abs(vdef);
		glideslopeAddedTotal += absValueVDef;

		if (vspeed > -1000)	// if descending at rate greater than 1000 ft/min, unstable
		{
			if(absValueVDef  < 2.5) {
				penalty += absValueVDef / 2.5;
			} 
		}
		else {
			penalty += 1;
		}
		
		return penalty;
	}

	/**
	 * returns the total altitude penalty for the stepdown portion of the ILS approach
	 * @param altitude the altitude of the aircraft during the ILS approach
	 * @return
	 */
	private double altitudeILSCalcPenalty(double dme, double altitude, double vspeed) {
		double penalty = 0;
		int currentFix = 0;

		this.verticalSpeedAddedTotal += vspeed;

		// TODO: do this more efficiently with new scoring algo
		// check which fix plane is approaching
		while (dme < STEPDOWN_FIXES.get(currentFix).dme) {
			currentFix++;
		}
		
		if (vspeed < 1000)
		{
			if (altitude > STEPDOWN_FIXES.get(currentFix).altitude)
			{
				penalty = 0;
			}
			else if (altitude > STEPDOWN_FIXES.get(currentFix).altitude - 100) {
				penalty = (STEPDOWN_FIXES.get(currentFix).altitude - altitude) / 100;
			}
			else {
				penalty = 1;
			}
		}
		else
		{
			penalty = 1;
		}
		
		return penalty;
	}

	/**
	 * returns the total speed penalty for the speed portion of the ILS approach
	 * @param speed The speed of the aircraft during the ILS approach
	 * @return double Returns the total penalty
	 */
	private double speedILSCalcPenalty(double speed) {
		double penalty = 0;

		speedAddedTotal += speed;
		double difference = Math.abs(speed - TARGET_SPEED);
		if (difference < 10)
		{
			penalty += (difference / 10);
		}
		else {
			penalty += 1;
		}
		
		return penalty;
	}

	public double scoreStepdownCalc() {
		double penalty = 0;

		for (FlightDataPoint point : this.data.getStepdownData()) {
			penalty += localizerScorePenalty(point.getHdef(), point.getBank()) 
				+ speedILSCalcPenalty(point.getAirspeed())
				+ altitudeILSCalcPenalty(point.getDme(), point.getAltitude(), point.getVertSpeed());
		}

		return penalty;
	}

	/**
	 * returns the total penalty for the final approach. Based on the localizer, glideslope, and speed
	 * @return double Returns the total penalty
	 */
	public double scoreFinalApproachCalc() {	
		double penalty = 0;
		for (FlightDataPoint point : this.data.getApproachData()) {
			penalty += localizerScorePenalty(point.getHdef(), point.getBank())
			+ speedILSCalcPenalty(point.getAirspeed())
			+ glideSlopeScorePenalty(point.getVdef(), point.getVertSpeed());
		}
		return penalty;
	}

	/**
	 * returns the total penalty for the roundout phase. Based on altitude. Looking to see that the plane is continuously descending
	 * @return the total penalty for the roundout stage
	 */
	public double scoreRoundOut()
	{
		double penalty = 0;

		for (FlightDataPoint point : this.data.getRoundoutData()) {
			penalty += scoreVerticalSpeed(point.getVertSpeed());
			localizerScorePenalty(point.getHdef(), point.getBank());
		}

		return penalty;
	}

	/**
	 * Returns the vertical speed penalty given a single speed value.
	 * @param vspeed vertical speed of the aircraft
	 * @return the vertical speed penalty
	 */
	public double scoreVerticalSpeed(double vspeed) {
		double penalty = 0;

		if (vspeed < -1000) {	// descending faster than 1000 ft/min
			penalty = 1;
		}

		return penalty;
	}

	/**
	 * returns the total penalty for the landing Phase. Based on centerline and altitude
	 * @return the penalty for the landing stage
	 */
	public double scoreLanding() {
		double penalty = 0; 
		for (FlightDataPoint point : this.data.getLandingData()) {
			penalty += localizerScorePenaltyLanding(point.getHdef());
		}
		return penalty;
	}

	/**
	 * returns the total penalty for the approach and landing
	 * @param outputFolderPath directory to save output files
	 * @param name name of participant
	 */
	public void scoreCalc() {

		this.approachScore[0] -= scoreStepdownCalc();

		this.approachScore[0] -= scoreFinalApproachCalc();

		this.landingScore[0] -= scoreRoundOut();
		
		this.landingScore[0] -= scoreLanding();

		this.overallScore[0] = this.landingScore[0] + this.approachScore[0];
		
		// bank angle in all stages
		this.averageBankAngle = this.bankAngleAddedTotal / this.getNumOfData();
		// glideslope (vertical) defections during final approach stage
		this.averageGlideslopeDeflection = this.glideslopeAddedTotal / this.getNumOfFinalApproachData();
		// airspeed during stepdown and final approach stages
		this.averageILSSpeed = this.speedAddedTotal / (this.getNumOfStepDownData() + this.getNumOfFinalApproachData());
		// vertical speed during stepdown and final approach stages
		this.averageILSVerticalSpeed = this.verticalSpeedAddedTotal / (this.getNumOfStepDownData() + this.getNumOfFinalApproachData());
		// average localizer (horizontal) deflections in all stages
		this.averageLocalizerDeflection = this.localizerAddedTotal / this.getNumOfData();
	}
	
	// Below are housekeeping items

	public void writeToFile(String outputLocation) {
		String outputFile = outputLocation + "/" + this.participant + "_score.csv";
		String[] headers = {
			"Overall Score",
			"Total Time",
			"Approach Score",
			"Approach Time",
			"Landing Score",
			"Landing Time",
			"AVG ILS Speed",
			"AVG VSI Final Approach",
			"AVG ABS Glideslope Deflection",
			"AVG ABS Localizer Deflection",
			"AVG ABS Roll Bank Angle",
			"MAX ABS Roll Bank Angle",
		};

		String[] data = {
			String.valueOf(getPercentageScore(scoreType.OVERALL)),
			String.valueOf(this.data.getTimeTotal()),
			String.valueOf(getPercentageScore(scoreType.APPROACH)),
			String.valueOf(this.data.getTimeApproach()),
			String.valueOf(getPercentageScore(scoreType.LANDING)),
			String.valueOf(this.data.getTimeLanding()),
			String.valueOf(averageILSSpeed),
			String.valueOf(averageILSVerticalSpeed),
			String.valueOf(averageGlideslopeDeflection),
			String.valueOf(averageLocalizerDeflection),
			String.valueOf(averageBankAngle),
			String.valueOf(maxBankAngle)
		};

		try (
			FileWriter outputFileWriter = new FileWriter(new File (outputFile));
			CSVWriter outputCSVWriter = new CSVWriter(outputFileWriter);
		){
			outputCSVWriter.writeNext(headers);
			outputCSVWriter.writeNext(data);
			outputCSVWriter.writeNext(new String []{});
			outputCSVWriter.writeNext(new String []{});
			outputCSVWriter.writeNext(new String []{});
			outputCSVWriter.writeNext(new String []{});
		}
		catch (FileNotFoundException e) {
			System.out.println("Unable to open file '" + outputFile + "'");
		}
		catch(IOException e) {
			System.out.println("Error writing to file '" + outputFile + "'");
		}
	}

	public double getPercentageScore(scoreType val) {
		double score;
		switch(val) {
			case APPROACH:
				score = this.approachScore[0] / this.approachScore[1];
				break;
			case LANDING:
				score =  this.landingScore[0] / this.landingScore[1];
				break;
			case OVERALL:
				score = this.overallScore[0] / this.overallScore[1];
				break;
			default:
				return -1;
		}

		return score;
	}

	public String getParticipant() {
		return this.participant;
	}

	public FlightData getFlightData() {
		return data;
	}

	/**
	 * @return the numOfData
	 */
	public int getNumOfData() {
		return this.getNumOfStepDownData()
			+ this.getNumOfFinalApproachData()
			+ this.getNumOfRoundoutData()
			+ this.getNumOfLandingData();
	}

	public int getNumOfStepDownData() {
		return this.data.getStepdownData().size();
	}

	/**
	 * @return the numOfILSData
	 */
	public int getNumOfFinalApproachData() {
		return this.data.getApproachData().size();
	}

	/**
	 * @return the numOfLandingData
	 */
	public int getNumOfLandingData() {
		return this.data.getLandingData().size();
	}

	/**
	 * @return the numOfRoundoutData
	 */
	public int getNumOfRoundoutData() {
		return this.data.getApproachData().size();
	}

	public String getStepdownFile() {
		return this.stepdownFile;
	}

	public String getFinalApproachFile(String filePath) {
		return this.finalApproachFile;
	}

	public String getRoundoutFile() {
		return this.roundoutFile;
	}

	public String getLandingFile() {
		return this.landingFile;
	}
}
