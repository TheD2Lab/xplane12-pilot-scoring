package scoring.scoringUpdated;
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
import scoring.FlightDataPoint;
import scoring.Fix;

/**
 * Modified version of {@link ScoreCalculation}.
 *
 * Changes from the original:
 * <p>
 * 1. Per-segment scores: adds separate score arrays for Stepdown, Final
 *    Approach, Roundout, and Landing Phase
 *    ({@code stepdownScore}, {@code finalApproachScore},
 *    {@code roundoutScore}, {@code landingPhaseScore}). The original
 *    implementation only tracked aggregate Approach and Landing scores.
 * <p>
 * 2. Landing segment file: accepts and stores the path to the trimmed
 *    landing-phase data file ({@code landingFile}), which was not tracked
 *    previously.
 * <p>
 * 3. Renamed CSV output headers: per-segment columns written by
 *    {@code writeToFile()} now include a {@code _Segment_} infix, producing
 *    {@code Stepdown_Segment_Score/Time},
 *    {@code FinalApproach_Segment_Score/Time},
 *    {@code Roundout_Segment_Score/Time},
 *    and {@code Landing_Segment_Score/Time}.
 * <p>
 * 4. Uses {@link FlightDataUpdated}: operates on the extended data model
 *    that provides individual phase durations via
 *    {@code getTimeStepdown()}, {@code getTimeFinalApproach()},
 *    {@code getTimeRoundout()}, and {@code getTimeLandingPhase()}.
 * <p>
 * 5. Per-segment getters: adds {@code getStepdownPercent()},
 *    {@code getFinalApproachPercent()}, {@code getRoundoutPercent()},
 *    and {@code getLandingPhasePercent()} to expose normalized scores
 *    for each segment.
 */

public class ScoreCalculationUpdated {

	private final static int MAX_PTS_PER_DATA_POINT_ILS = 3;
	private final static int MAX_PTS_PER_DATA_POINT_ROUNDOUT = 2;
	private final static int MAX_PTS_PER_DATA_POINT_LANDING = 2;
	private final static int TARGET_SPEED = 90;
	private final static int TARGET_HEADING = 344;

	private String participant;

	// [0] = actual score, [1] = highest possible score
	private double[] approachScore 		= {0,1};
	private double[] landingScore 		= {0,1};
	private double[] overallScore 		= {0,1};
	private double[] stepdownScore      = {0,1};
	private double[] finalApproachScore = {0,1};
	private double[] roundoutScore      = {0,1};
	private double[] landingPhaseScore  = {0,1};

	// The data for this particular score
	private FlightDataUpdated data;

	// Files
	private String stepdownFile;
	private String finalApproachFile;
	private String roundoutFile;
	private String landingFile;

	// Additional X-Plane measures

	// Includes stepdown and final approach portion
	private double minILSSpeed = Double.POSITIVE_INFINITY;
	private double maxILSSpeed = 0;
	private double avgILSSpeed = 0;
	private double speedAddedTotal = 0;
	private double countProperILSSpeed = 0; // Number of data points speed +/-10 TARGET_SPEED
	private double percentProperSpeed = 0;
	private double avgILSVspeed = 0;
	private double minILSVspeed = Double.POSITIVE_INFINITY;
	private double maxILSVspeed = 0;
	private double vspeedAddedTotal = 0;
	private double avgILSHdef = 0;
	private double hdefAddedTotal = 0;
	private double avgILSBankAngle = 0;
	private double bankAngleAddedTotal = 0;
	private double maxILSBankAngle = 0;

	// Includes final approach portion
	private double avgFinAppVdef = 0;
	private double vdefAddedTotal = 0;

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
	public ScoreCalculationUpdated(String name, String sdFile, String faFile, String rFile, String lFile, FlightDataUpdated data) {

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

		int nSD = this.data.getStepdownData().size();
		int nFA = this.data.getApproachData().size();
		int nRO = this.data.getRoundoutData().size();
		int nLD = this.data.getLandingData().size();

		// Per‑segment max points
		stepdownScore[1]      = Math.max(1, nSD * MAX_PTS_PER_DATA_POINT_ILS);
		finalApproachScore[1] = Math.max(1, nFA * MAX_PTS_PER_DATA_POINT_ILS);
		roundoutScore[1]      = Math.max(1, nRO * MAX_PTS_PER_DATA_POINT_ROUNDOUT);
		landingPhaseScore[1]  = Math.max(1, nLD * MAX_PTS_PER_DATA_POINT_LANDING);

		stepdownScore[0]      = stepdownScore[1];
		finalApproachScore[0] = finalApproachScore[1];
		roundoutScore[0]      = roundoutScore[1];
		landingPhaseScore[0]  = landingPhaseScore[1];

		// Existing aggregate approach/landing scores
		double possible_points =
			(nFA + nSD) * MAX_PTS_PER_DATA_POINT_ILS;
		if (possible_points > 0) {
			this.approachScore[1] = possible_points;
			this.approachScore[0] = possible_points;
		} else {
			this.approachScore[1] = 1;
			this.approachScore[0] = 0;
		}

		possible_points =
			nRO * MAX_PTS_PER_DATA_POINT_ROUNDOUT +
			nLD * MAX_PTS_PER_DATA_POINT_LANDING;
		if (possible_points > 0) {
			this.landingScore[1] = possible_points;
			this.landingScore[0] = possible_points;
		}

		this.overallScore[1] = this.approachScore[1] + this.landingScore[1];
		this.overallScore[0] = this.approachScore[0] + this.landingScore[0];
	}

	/**
	 * Returns the localizer penalty for a single data point.
	 * @param hdef is all of the localizer position of the aircraft in dots
	 * @return returns the penalty
	 */
	private double localizerScorePenalty(double hdef, double bankAngle, double heading) {
		double penalty = 0;
		double absHdef = Math.abs(hdef);
		double absBankAngle = Math.abs(bankAngle);
		double headingDiff = Math.abs(TARGET_HEADING - heading);

		if (absBankAngle > Math.abs(this.maxILSBankAngle))
		{
			this.maxILSBankAngle = absBankAngle;
		}

		if (absBankAngle < 15 && absHdef < 2.5 && headingDiff <= 25) {
			penalty = absHdef / 2.5;
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
		double absHdef = Math.abs(hdef);

		if(absHdef  < 2.5) {
			penalty = absHdef / 2.5;
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
		double absVdef = Math.abs(vdef);

		// if descending at rate greater than 1000 ft/min, unstable
		// if vdef == -0.0, no points
		if (vspeed < -1000 || (vdef == 0 && 1/vdef < 0))
		{
			penalty = 1;
		}
		else {
			if(absVdef  < 2.5) {
				penalty += absVdef / 2.5;
			}
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

		// TODO: do this more efficiently with new scoring algo
		// check which fix plane is approaching
		while (dme < STEPDOWN_FIXES.get(currentFix).dme) {
			currentFix++;
		}

		if (vspeed > -1000)	// FAA recommends decent rate less than 1000fpm (so greater than -1000)
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
		double difference = Math.abs(speed - TARGET_SPEED);

		if (difference <= 10)
		{
			penalty += (difference / 10);
			this.countProperILSSpeed++;
		}
		else {
			penalty += 1;
		}

		return penalty;
	}

	public double scoreStepdownCalc() {
		double penalty = 0;

		for (FlightDataPoint point : this.data.getStepdownData()) {
			this.bankAngleAddedTotal += Math.abs(point.getBank());
			this.hdefAddedTotal += Math.abs(point.getHdef());
			this.speedAddedTotal += point.getAirspeed();
			this.vspeedAddedTotal += point.getVertSpeed();

			if (point.getAirspeed() < this.minILSSpeed) {
				this.minILSSpeed = point.getAirspeed();
			} else if (point.getAirspeed() > this.maxILSSpeed) {
				this.maxILSSpeed = point.getAirspeed();
			}

			if (point.getVertSpeed() < this.minILSVspeed) {
				this.minILSVspeed = point.getVertSpeed();
			} else if (point.getVertSpeed() > this.maxILSVspeed) {
				this.maxILSVspeed = point.getVertSpeed();
			}

			if (point.getHdef() == 0.0 && 1/point.getHdef() < 0) {	// equals -0.0
				penalty += 3;
			} else {
				penalty += localizerScorePenalty(point.getHdef(), point.getBank(), point.getHeading())
					+ speedILSCalcPenalty(point.getAirspeed())
					+ altitudeILSCalcPenalty(point.getDme(), point.getAltitude(), point.getVertSpeed());
			}
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
			this.bankAngleAddedTotal += Math.abs(point.getBank());
			this.hdefAddedTotal += Math.abs(point.getHdef());
			this.speedAddedTotal += point.getAirspeed();
			this.vspeedAddedTotal += point.getVertSpeed();
			this.vdefAddedTotal += Math.abs(point.getVdef());

			if (point.getAirspeed() < this.minILSSpeed) {
				this.minILSSpeed = point.getAirspeed();
			} else if (point.getAirspeed() > this.maxILSSpeed) {
				this.maxILSSpeed = point.getAirspeed();
			}

			if (point.getVertSpeed() < this.minILSVspeed) {
				this.minILSVspeed = point.getVertSpeed();
			} else if (point.getVertSpeed() > this.maxILSVspeed) {
				this.maxILSVspeed = point.getVertSpeed();
			}

			if (point.getHdef() == 0.0 && 1/point.getHdef() < 0) {	// equals -0.0
				penalty += 3;
			} else {
				penalty += localizerScorePenalty(point.getHdef(), point.getBank(), point.getHeading())
				+ speedILSCalcPenalty(point.getAirspeed())
				+ glideSlopeScorePenalty(point.getVdef(), point.getVertSpeed());
			}
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
			if (point.getHdef() == 0.0 && 1/point.getHdef() < 0) {	// equals -0
				penalty += 3;
			} else {
				penalty += scoreVerticalSpeed(point.getVertSpeed())
					+ localizerScorePenalty(point.getHdef(), point.getBank(), point.getHeading());
			}
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

		double stepdownPenalty      = scoreStepdownCalc();
		double finalApproachPenalty = scoreFinalApproachCalc();
		double roundoutPenalty      = scoreRoundOut();
		double landingPenalty       = scoreLanding();

		// Per‑segment scores
		stepdownScore[0]      -= stepdownPenalty;
		finalApproachScore[0] -= finalApproachPenalty;
		roundoutScore[0]      -= roundoutPenalty;
		landingPhaseScore[0]  -= landingPenalty;

		// Existing aggregated scores (unchanged logic)
		this.approachScore[0] -= (stepdownPenalty + finalApproachPenalty);
		this.landingScore[0]  -= (roundoutPenalty + landingPenalty);
		this.overallScore[0]   = this.landingScore[0] + this.approachScore[0];

		int numApproachData = this.getNumOfStepDownData() + this.getNumOfFinalApproachData();

		// bank angle in all stages
		this.avgILSBankAngle = this.bankAngleAddedTotal / numApproachData;
		// glideslope (vertical) defections during final approach stage
		this.avgFinAppVdef = this.vdefAddedTotal / this.getNumOfFinalApproachData();
		// airspeed during stepdown and final approach stages
		this.avgILSSpeed = this.speedAddedTotal / numApproachData;
		// vertical speed during stepdown and final approach stages
		this.avgILSVspeed = this.vspeedAddedTotal / numApproachData;
		// average localizer (horizontal) deflections in all stages
		this.avgILSHdef = this.hdefAddedTotal / numApproachData;
		// percentage of data points with speed within +/- 10 target speed
		this.percentProperSpeed = this.countProperILSSpeed / numApproachData;
	}

	// Below are housekeeping items

	public void writeToFile(String outputLocation) {
		String outputFile = outputLocation + "/" + this.participant + "_score.csv";
		String[] headers = {
			"Overall_Score",
			"Total_Time",
			"Approach_Score",
			"Approach_Time",
			"Landing_Score",
			"Landing_Time",
			"Stepdown_Segment_Score",
			"Stepdown_Segment_Time",
			"FinalApproach_Segment_Score",
			"FinalApproach_Segment_Time",
			"Roundout_Segment_Score",
			"Roundout_Segment_Time",
			"Landing_Segment_Score",
			"Landing_Segment_Time",
			"MIN_ILS_Airspeed",
			"MAX_ILS_Airspeed",
			"AVG_ILS_Airspeed",
			"Percent_Proper_Airspeed",
			"MIN_ILS_VSI",
			"MAX_ILS_VSI",
			"AVG_ILS_VSI",
			"AVG_ILS_ABS_Glideslope_Deflection",
			"AVG_ILS_ABS_Localizer_Deflection",
			"AVG_ILS_ABS_Bank_Angle",
			"MAX_ILS_ABS_Bank_Angle",
		};

		String[] data = {
			String.valueOf(getPercentageScore(scoreType.OVERALL)),
			String.valueOf(this.data.getTimeTotal()),
			String.valueOf(getPercentageScore(scoreType.APPROACH)),
			String.valueOf(this.data.getTimeApproach()),
			String.valueOf(getPercentageScore(scoreType.LANDING)),
			String.valueOf(this.data.getTimeLanding()),
			String.valueOf(getStepdownPercent()),
			String.valueOf(this.data.getTimeStepdown()),
			String.valueOf(getFinalApproachPercent()),
			String.valueOf(this.data.getTimeFinalApproach()),
			String.valueOf(getRoundoutPercent()),
			String.valueOf(this.data.getTimeRoundout()),
			String.valueOf(getLandingPhasePercent()),
			String.valueOf(this.data.getTimeLandingPhase()),
			String.valueOf(this.minILSSpeed),
			String.valueOf(this.maxILSSpeed),
			String.valueOf(this.avgILSSpeed),
			String.valueOf(this.percentProperSpeed),
			String.valueOf(this.minILSVspeed),
			String.valueOf(this.maxILSVspeed),
			String.valueOf(this.avgILSVspeed),
			String.valueOf(this.avgFinAppVdef),
			String.valueOf(this.avgILSHdef),
			String.valueOf(this.avgILSBankAngle),
			String.valueOf(this.maxILSBankAngle)
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

	public FlightDataUpdated getFlightData() {
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
		return this.data.getRoundoutData().size();
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

	public double getStepdownPercent() {
		return stepdownScore[0] / stepdownScore[1];
	}

	public double getFinalApproachPercent() {
		return finalApproachScore[0] / finalApproachScore[1];
	}

	public double getRoundoutPercent() {
		return roundoutScore[0] / roundoutScore[1];
	}

	public double getLandingPhasePercent() {
		return landingPhaseScore[0] / landingPhaseScore[1];
	}
}
