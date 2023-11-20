package scoring;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.opencsv.CSVWriter;

/**
 * Handles scoring pilot success and calculating additional measures that may help draw correlations between
 * pilot performance and their gaze data. The ILS approach can be broken in to the approach (stepdown phase + final approach phase)
 * and landing (roundout phase and landing phase). <br>
 * TODO: Complete scoring overview
 * <u> How scoring works: </u><br>
 * - For each data entry stepdown phase, there is a maximum of 3 points to earn.
 */
public class ScoreCalculation {

	/**
	 * Max number of points per data entry during ILS approach.
	 * 1 pt each for speed, localizer, and glideslope.
	 */
	private final static int MAX_PTS_PER_DATA_POINT_ILS = 3;
	/**
	 * Max number of points per data entry during Roundout. 
	 */
	private final static int MAX_PTS_PER_DATA_POINT_ROUNDOUT = 2; 
	/**
	 * Max number of points per data entry during landing.
	 */
	private final static int MAX_PTS_PER_DATA_POINT_LANDING = 2;

	/**
	 * Participant ID used for naming files.
	 */
	private String participant;

	/**
	 * Approach score received by pilot [0] and highest possible approach score [1].
	 */
	private double[] approachScore = {0,1};

	/**
	 * Landing score received by pilot [0] and highest possible landing score [1].
	 */
	private double[] landingScore = {0,1};

	/**
	 * Overall success score received by pilot [0] and highest possible success score [1].
	 */
	private double[] overallScore = {0,1};
	
	/**
	 * Flight data used in calculations.
	 */
	private FlightData data;

	/**
	 * Number of all data entry lines.
	 */
	private int numOfData = 0;

	/**
	 * Number of data entries during stepdown phase.
	 */
	private int numOfStepdownData = 0; 

	/**
	 * Number of data entries during final approach phase.
	 */
	private int numOfFinalApproachData = 0;

	/**
	 * Number of data entries during roundout phase.
	 */
	private int numOfRoundoutData = 0;

	/**
	 * Number of data entries during landing phase.
	 */
	private int numOfLandingData = 0;
	
	/**
	 * Combined average speed during stepdown and final approach phase.
	 */
	private double averageILSSpeed = 0;

	/**
	 * Accumulator for speed during stepdown and final approach phase.
	 */
	private double speedAddedTotal = 0;

	/**
	 * Average vertical speed during final approach phase.
	 */
	private double averageILSVerticalSpeed = 0; // Does not include stepdown portion

	/**
	 * Accumulator for vertical speed during final approach phase.
	 */
	private double verticalSpeedAddedTotal = 0;

	/**
	 * Combined average localizer (horizontal) deflection during stepdown and final approach phase.
	 */
	private double averageLocalizerDeflection = 0; // Includes stepdown portion and final approach portion

	/**
	 * Accumulator for localizer (horizontal deflection during stepdown and final approach phase.
	 */
	private double localizerAddedTotal = 0;

	/**
	 * Average glideslope (vertical) deflection during final approach phase.
	 */
	private double averageGlideslopeDeflection = 0; // Does not include stepdown portion

	/** Accumulator for glideslope (vertical) deflection during final approach phase */
	private double glideslopeAddedTotal = 0;

	/**
	 * Combined average bank angle for stepdown and final approach phase.
	 */
	private double averageBankAngle = 0; // Includes stepdown portion and final approach portion

	/** Accumulator for bank angle for stepdown and final approach phase */
	private double bankAngleAddedTotal = 0;

	/**
	 * Maximum bank angle that occurred throughout the stepdown, final approach, and roundout phase.
	 */
	private double maxBankAngle = 0; // Includes ILS and final descent to runway

	/**
	 * List of important fixes during stepdown sequence. Can be found on approach plate.
	 */
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

	/** Enumerated list denoting types of pilot success scores. */
	public enum scoreType {
		/** Approach portion (stepdown + final approach phases) */
		APPROACH,
		/** Landing portion (roundout + landing phases) */
		LANDING,
		/** Flight from JIPOX to end of landing phase */
		OVERALL
	}
	
	/**
	 * Constructs and adds flight data to calculations. Pre-sets max number of points that can be scored.
	 * @param name 		participant name or identifier.
	 * @param numData		total number of data entries.
	 * @param numSD		number of data entries during stepdown phase.
	 * @param numFA		number of data entries during final approach phase.
	 * @param numR			number of data entries during roundout phase.
	 * @param numL			number of data entries during landing phase.
	 * @param data			flight data to score.
	 */
	public ScoreCalculation(String name, int numData, int numSD, int numFA, int numR, int numL, FlightData data) {
		this.participant = name;
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
	 * Calculates the max points for the overall score and sub-scores. Uses the number
	 * of entries and max number of points possible for each type of entry to calculate max possible points.
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
	 * Returns the total score penalty for the localizer portion of the ILS approach.
	 * TODO: add how scoring works
	 * @param horizontalDef all of the localizer position of the aircraft in dots.
	 * @return the total penalty
	 */
	private double localizerScorePenalty(List<Double> horizontalDef, List<Double> bankAngle) {
		double penalty = 0;
		double hdef;
		double bank;
		Iterator<Double> hDefIter = horizontalDef.iterator();
		Iterator<Double> baIterator = bankAngle.iterator();

		while(hDefIter.hasNext()) {		// horizontalDef and bankAngle are the same length. We only check one.
			hdef = hDefIter.next();
			bank = baIterator.next();
			
			localizerAddedTotal += hdef;
			bankAngleAddedTotal += bank;
			
			double absValueLoc = Math.abs(hdef);
			double absValueBank = Math.abs(bank);
			
			if (absValueBank > Math.abs(maxBankAngle))
			{
				maxBankAngle = bank;
			}
			
			if (absValueBank < 15)
			{
				if(absValueLoc  < 2.5) {
					penalty += absValueLoc / 2.5;
				}
				else {
					penalty += 1;
				}
			}
			else {
				penalty += 1;
			}
		}
		
		averageLocalizerDeflection = localizerAddedTotal / (numOfStepdownData + numOfFinalApproachData
				+ numOfRoundoutData + numOfLandingData);
		averageBankAngle = bankAngleAddedTotal / (numOfStepdownData + numOfFinalApproachData
				+ numOfRoundoutData + numOfLandingData);
		
		return penalty;
	}
	
	/**
	 * Returns the total score penalty for the localizer in the landing portion.
	 * TODO: Add how scoring works
	 * @param horizontalDef all of the localizer position of the aircraft in dots
	 * @return the total penalty
	 */
	private double localizerScorePenaltyLanding(List<Double> horizontalDef) {
		double penalty = 0;
		for(double hdef : horizontalDef) {
			
			localizerAddedTotal += hdef;
			
			double absValue = Math.abs(hdef);
			
			if(absValue  < 2.5) {
				penalty += absValue / 2.5;
			} else {
				penalty += 1;
			}
		}
		averageLocalizerDeflection = localizerAddedTotal / (numOfStepdownData + numOfFinalApproachData
				+ numOfRoundoutData + numOfLandingData);
		
		return penalty;
	}

	/**
	 * Returns the total score penalty for the glideslope portion of the ILS approach.
	 * TODO: add how scoring works
	 * @param verticalDef all of the vertical position of the aircraft in dots
	 * @return the total penalty
	 */
	private double glideSlopeScorePenalty(List<Double> verticalDef, List<Double> verticalSpeed) {
		double penalty = 0;
		double vSpeed;
		double vDef;
		Iterator<Double> vDefIter = verticalDef.iterator();		// verticalDef is a LinkedList
		Iterator<Double> vSpeedIter = verticalSpeed.iterator();	// verticalSpeed is a LinkedList

		while(vDefIter.hasNext())	{	// verticalDef and vertical speed are the same length. We only check one.
			vSpeed = vSpeedIter.next();
			verticalSpeedAddedTotal += vSpeed;
			
			vDef = vDefIter.next();
			glideslopeAddedTotal += vDef;
			
			double absValue = Math.abs(vDef);

			if (vSpeed < 1000)
			{
				if(absValue  < 2.5) {
					penalty += absValue / 2.5;
				} 
			}
			else {
				penalty += 1;
			}
		}
		
		averageGlideslopeDeflection = glideslopeAddedTotal / verticalDef.size();
		averageILSVerticalSpeed = verticalSpeedAddedTotal / verticalSpeed.size();
		
		return penalty;
	}

	/**
	 * Returns the total altitude penalty for the stepdown portion of the ILS approach. 
	 * TODO: add how scoring works
	 * @param dmes list of DME distance in nautical miles
	 * @param altitudes list of altitude MSL in feet
	 * @param verticalSpeed	list of vertical speed in feet per second
	 * @return the total penalty
	 */
	private double altitudeILSCalcPenalty(List<Double> dmes, List<Double>altitudes, List<Double> verticalSpeed) {
		double penalty = 0;
		int currentFix = 0;

		// Use iterators to optimize LinkedList support. Flight Data uses LinkedList.
		// Indexing a LinkedList using list[i] is O(n).
		// If this does not make sense to you, please stop programming and review data structures 101
		Iterator<Double> dmeIter = dmes.iterator();
		Iterator<Double> altIter = altitudes.iterator();
		Iterator<Double> vsIter = verticalSpeed.iterator();
		double dmeVal;
		double altVal;
		double vertSpeed;

		while(dmeIter.hasNext() && altIter.hasNext()) {
			dmeVal = dmeIter.next();
			altVal = altIter.next();
			vertSpeed = vsIter.next();	

			// check which fix plane is approaching
			while (dmeVal < STEPDOWN_FIXES.get(currentFix).dme) {
				currentFix++;
			}
			
			if (vertSpeed < 1000)
			{
				if (altVal > STEPDOWN_FIXES.get(currentFix).altitude)
				{
					penalty += 0;
				}
				else if (altVal > STEPDOWN_FIXES.get(currentFix).altitude - 100) {
					penalty += (STEPDOWN_FIXES.get(currentFix).altitude - altVal) / 100;
				}
				else {
					penalty += 1;
				}
				
			}
			else
			{
				penalty += 1;
			}
		}
		return penalty;
	}

	/**
	 * Returns the total speed penalty for the speed portion of the ILS approach.
	 * TODO: add how scoring works
	 * @param speeds list of airspeed data in knot
	 * @return the total penalty
	 */
	private double speedILSCalcPenalty(List<Double> speeds) {
		double penalty = 0;
		int assignedSpeed = 90;
		for(double speed : speeds) {
			speedAddedTotal += speed;
			double difference = Math.abs(speed - assignedSpeed);
			if (difference < 10)
			{
				penalty += (difference / 10);
			}
			else {
				penalty += 1;
			}
		}
		
		averageILSSpeed = speedAddedTotal / (numOfStepdownData + numOfFinalApproachData);
		return penalty;
	}

	/**
	 * Returns the total penalty for the stepdown phase.
	 * TODO: add how scoring works
	 * @param horiDef		list of horizontal deflection data
	 * @param speed		list of airspeed data in knots
	 * @param altitude 	list of altitude MSL data in feet
	 * @param dme			list of DME distance in nautical miles
	 * @param rollBank 	list of roll (bank) angles in degrees
	 * @param verticalSpeed	list of vertical speed in feet per seconds
	 * @return the total penalty
	 */
	public double scoreStepdownCalc(List<Double> horiDef, List<Double> speed, List<Double> altitude, List<Double> dme,
			List<Double> rollBank, List<Double> verticalSpeed) {
		return localizerScorePenalty(horiDef, rollBank) + speedILSCalcPenalty(speed) + altitudeILSCalcPenalty(dme, altitude, verticalSpeed);
	}

	/**
	 * returns the total penalty for the final approach phase. Based on the localizer, glideslope, and speed
	 * @param horiDef all of the localizer position of the aircraft
	 * @param speed The speed of the aircraft during the ILS approach
	 * @param vertDef all of the glideslope position of the aircraft
	 * @return double Returns the total penalty
	 */
	public double scoreFinalApproachCalc(List<Double> horiDef, List<Double> speed, List<Double> vertDef,
			List<Double> verticalSpeed, List<Double> rollBankFinalApp) {	
		return localizerScorePenalty(horiDef, rollBankFinalApp) + speedILSCalcPenalty(speed) + glideSlopeScorePenalty(vertDef, verticalSpeed);
	}

	/**
	 * Returns the total penalty for the roundout phase. 
	 * TODO: add how scoring works
	 * @param horiDef list of horizontal deflection data in dots
	 * @param rollBank	list of bank angles data in degrees
	 * @param verticalSpeed	list of vertical speed in feet per seconds
	 * @return the total penalty.
	 */
	public double scoreRoundOut(List<Double> horiDef, List<Double> rollBank, List<Double> verticalSpeed)
	{
		double penalty = 0;
		double vert;
		Iterator<Double> vsIter = verticalSpeed.iterator();

		
		while(vsIter.hasNext()) {
			
			vert = vsIter.next();
			
			if(vert < 1000) {
				penalty += 0;
			} else {
				penalty += 1;
			}
		}
		
		penalty += localizerScorePenalty(horiDef, rollBank);
		return penalty;
	}

	/**
	 * Returns the total penalty for the landing Phase. Based on centerline.
	 * @param horiDef list of horizontal deflection data during landing phase.
	 * @return the total penalty
	 */
	public double scoreLanding(List<Double> horiDef)
	{
		double penalty = 0; 
		penalty += localizerScorePenaltyLanding(horiDef);
		return penalty;
	}

	/**
	 * Runs the pilot success score calculations and updates object state.
	 */
	public void scoreCalc() {

		this.approachScore[0] -= scoreStepdownCalc(
			data.getHDefStepdown(), 
			data.getSpeedStepdown(), 
			data.getAltStepdown(), 
			data.getDmeStepdown(),
			data.getRollBankStepdown(),
			data.getVerticalSpeedStepdown()
		);

		this.approachScore[0] -= scoreFinalApproachCalc(
			data.getHDefFinalApproach(),
			data.getSpeedFinalApproach(),
			data.getVDefFinalApproach(),
			data.getVerticalSpeedFinalApp(),
			data.getRollFinalApp()
		);

		this.landingScore[0] -= scoreRoundOut(
				data.getHDefRoundout(),
				data.getRollBankRoundout(),
				data.getVerticalSpeedRoundout()
		);
		
		this.landingScore[0] -= scoreLanding(
				data.getHDefLanding()
		);

		this.overallScore[0] = this.landingScore[0] + this.approachScore[0];
	}
	
	
	/**
	 * Writes the scores and addition measures to a file/
	 * @param outputLocation path to output file.
	 */
	public void writeToFile(String outputLocation) {
		String outputFile = outputLocation + "/" + this.participant + "_score.csv";
		String[] headers = {
			"Metric",
			"Outcome",
			". . . . . . . . . . . . . .",
			"Overall Score",
			"Total Time",
			"Approach Score",
			"Approach Time",
			"Landing Score",
			"Landing Time",
		};

		String[] data = {
			String.valueOf("AVG ILS Speed"),
			String.valueOf(String.valueOf(averageILSSpeed)),
			"",
			String.valueOf(getPercentageScore(scoreType.OVERALL)),
			String.valueOf(this.data.getDurTotal()),
			String.valueOf(getPercentageScore(scoreType.APPROACH)),
			String.valueOf(this.data.getDurApproach()),
			String.valueOf(getPercentageScore(scoreType.LANDING)),
			String.valueOf(this.data.getDurLanding())
		};

		try (
			FileWriter outputFileWriter = new FileWriter(new File (outputFile));
			CSVWriter outputCSVWriter = new CSVWriter(outputFileWriter);
		){
			outputCSVWriter.writeNext(headers);
			outputCSVWriter.writeNext(data);
			outputCSVWriter.writeNext(new String []{"AVG VSI Final Approach", String.valueOf(averageILSVerticalSpeed)});
			outputCSVWriter.writeNext(new String []{"AVG Glideslope Deflection", String.valueOf(averageGlideslopeDeflection)});
			outputCSVWriter.writeNext(new String []{"AVG Localizer Deflection", String.valueOf(averageLocalizerDeflection)});
			outputCSVWriter.writeNext(new String []{"AVG Roll Bank Angle", String.valueOf(averageBankAngle)});
			outputCSVWriter.writeNext(new String []{"MAX Roll Bank Angle", String.valueOf(maxBankAngle)});
		}
		catch (FileNotFoundException e) {
			System.out.println("Unable to open file '" + outputFile + "'");
		}
		catch(IOException e) {
			System.out.println("Error writing to file '" + outputFile + "'");
		}
	}

	/**
	 * Returns the specified pilot success score as a percentage.
	 * @param type the wanted score type
	 * @return	the percentage score
	 */
	public double getPercentageScore(scoreType type) {
		switch(type) {
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


	/**
	 * Returns the participant identifier.
	 * @return participant identifier
	 */
	public String getParticipant() {
		return this.participant;
	}

	/**
	 * Return the flight data associated with calculation.
	 * @return flight data
	 */
	public FlightData getFlightData() {
		return data;
	}

	/**
	 * Returns the total number of data entries.
	 * @return number of data entries
	 */
	public int getNumOfData() {
		return this.numOfData;
	}

	/**
	 * Returns the number of data entries during the stepdown phase.
	 * @return number of data entries
	 */
	public int getNumOfStepDownData() {
		return this.numOfStepdownData;
	}

	/**
	 * Returns the number of data entries during final approach.
	 * @return number of data entries
	 */
	public int getNumOfFinalApproachData() {
		return this.numOfFinalApproachData;
	}

	/**
	 * Returns the number of data entries during landing phase.
	 * @return number of data entries
	 */
	public int getNumOfLandingData() {
		return this.numOfLandingData;
	}

	/**
	 * Returns the number of data entries during roundout phase.
	 * @return number of data entries
	 */
	public int getNumOfRoundoutData() {
		return this.numOfRoundoutData;
	}
}
