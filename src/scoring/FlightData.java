package scoring;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains important flight data for success scoring. Used to manage and keep data
 * in memory until it is no longer needed. Please note, this class purposefully has no knowledge
 * of the units of measurement for the data it holds. 
 */
public class FlightData {
   // important timestamps

   /** Time flight simulation started. */
   private LocalDateTime beginFlightTimestamp = null;

   /** Time approach phase started. */
   private LocalDateTime beginApproachTimestamp = null;

   /** Time roundout phase started. */
   private LocalDateTime beginRoundOutTimestamp = null;

   /** Time landing phase started. */
   private LocalDateTime beginLandingTimestamp = null;

   /** Time flight ended. */
   private LocalDateTime endFlightTimestamp = null;

   // Stepdown portion
   /** List of altitude data for stepdown phase. */
   private List<Double> altStepdown;

   /** List of DME distance data for stepdown phase. */
   private List<Double> dmeStepdown;

   /** List of speed data for stepdown phase. */
   private List<Double> speedStepdown;

   /** List of localizer (horizontal) deflection data for stepdown phase. */
   private List<Double> hDefStepdown;

   /** List of roll angle data for stepdown phase. */
   private List<Double> rollBankStepdown;

   /** List of vertical speed data for stepdown phase. */
   private List<Double> verticalSpeedStepdown;
   
   // Final Approach portion

   /** List of glideslope (vertical) deflection data for final approach phase. */
   private List<Double> vDefFinalApproach;

   /** List of speed data for final approach phase. */
   private List<Double> speedFinalApproach;

   /** List of localizer (horizontal) deflection data for final approach phase. */
   private List<Double> hDefFinalApproach;

   /** List of vertical speed data for final approach phase. */
   private List<Double> verticalSpeedFinalApp;

   /** List of roll (bank) angle for final approach phase. */
   private List<Double> rollBankFinalApp;

   // Roundout portion
   /** List of altitude data for roundout phase. */
   private List<Double> altRoundout;

   /** List of localizer (horizontal) deflection data for roundout phase. */
   private List<Double> hDefRoundout;

   /** List of roll (bank) angle data for roundout phase. */
   private List<Double> rollBankRoundout;

   /** List of vertical speed data for roundout phase.  */
   private List<Double> verticalSpeedRoundout;

   // Landing portion
   /** List of altitude data for landing phase. */
   private List<Double> altLanding;

   /** List of localizer (horizontal) deflection for landing phase. */
   private List<Double> hDefLanding;

   // Time data

   /** Duration of time to complete approach phase (stepdown + final approach). */
   private double durApproach = 0;

   /** Duration of time to complete roundout + landing phase. */
   private double durLanding = 0;

   /** Duration of time to complete entire ILS approach and landing. */
   private double durTotal = 0;

   /**
    * Constructs empty data lists.
    */
   FlightData() {
      // implemented as LinkedList because insertion at end is O(1)
      // ArrayLists were not used intentionally!!!
      this.altStepdown = new LinkedList<>();
      this.dmeStepdown = new LinkedList<>();
      this.speedStepdown = new LinkedList<>();
      this.hDefStepdown = new LinkedList<>();
      this.vDefFinalApproach = new LinkedList<>();
      this.speedFinalApproach = new LinkedList<>();
      this.hDefFinalApproach = new LinkedList<>();
      this.verticalSpeedFinalApp = new LinkedList<>();
      this.verticalSpeedStepdown = new LinkedList<>();
      this.verticalSpeedRoundout = new LinkedList<>();
      this.altRoundout = new LinkedList<>();
      this.hDefRoundout = new LinkedList<>();
      this.altLanding = new LinkedList<>();
      this.hDefLanding = new LinkedList<>();
      this.rollBankStepdown = new LinkedList<>();
      this.rollBankFinalApp = new LinkedList<>();
      this.rollBankRoundout = new LinkedList<>();
   }

   /**
    * Constructor for when using Data.txt created by X-Plane 12 software without system time.
    * @param altStepdown         list of altitude data for stepdown phase.
    * @param dmeStepdown         list of DME distance data for stepdown phase.
    * @param speedStepdown       list of speed data for stepdown phase.
    * @param hDefStepdown        list of localizer (horizontal) deflection data for stepdown phase.
    * @param vDefFinalApproach   list of glideslope (vertical) deflection data for final approach phase.
    * @param speedFinalApproach  list of speed data for final approach phase.
    * @param hDefFinalApproach   list of localizer (horizontal) deflection data for final approach phase.
    * @param altRoundout         list of altitude data for roundout phase.
    * @param hDefRoundout        list of localizer (horizontal) deflection data for roundout phase.
    * @param altLanding          list of altitude data for landing phase.
    * @param hDefLanding         list of localizer (horizontal) deflection for landing phase.
    * @param verticalSpeedFinalApp  list of vertical speed data for final approach phase.
    * @param verticalSpeedStepdown  list of vertical speed data for stepdown phase.
    * @param verticalSpeedRoundout  list of vertical speed data for roundout phase.
    * @param rollBankStepdown    list of roll angle data for stepdown phase. 
    * @param rollBankFinalApp    list of roll (bank) angle for final approach phase.
    * @param rollBankRoundout    list of roll (bank) angle data for roundout phase.
    * @param timeApproach        duration of time to complete approach phase (stepdown + final approach).
    * @param timeLanding         duration of time to complete roundout + landing phase.
    * @param timeTotal           duration of time to complete entire ILS approach and landing.
    */
   public FlightData(List<Double> altStepdown, List<Double> dmeStepdown, List<Double> speedStepdown,
         List<Double> hDefStepdown, List<Double> vDefFinalApproach, List<Double> speedFinalApproach,
         List<Double> hDefFinalApproach, List<Double> altRoundout, List<Double> hDefRoundout, 
         List<Double> altLanding, List<Double> hDefLanding, List<Double> verticalSpeedFinalApp,
         List<Double> verticalSpeedStepdown, List<Double> verticalSpeedRoundout, List<Double> rollBankStepdown,
         List<Double> rollBankFinalApp, List<Double> rollBankRoundout,
         double timeApproach, double timeLanding, double timeTotal) {
      this.altStepdown = altStepdown;
      this.dmeStepdown = dmeStepdown;
      this.speedStepdown = speedStepdown;
      this.hDefStepdown = hDefStepdown;
      this.vDefFinalApproach = vDefFinalApproach;
      this.speedFinalApproach = speedFinalApproach;
      this.hDefFinalApproach = hDefFinalApproach;
      this.altRoundout = altRoundout;
      this.hDefRoundout = hDefRoundout;
      this.altLanding = altLanding;
      this.hDefLanding = hDefLanding;
      this.verticalSpeedFinalApp = verticalSpeedFinalApp;
      this.verticalSpeedStepdown = verticalSpeedStepdown;
      this.verticalSpeedRoundout = verticalSpeedRoundout;
      this.rollBankStepdown = rollBankStepdown;
      this.rollBankFinalApp = rollBankFinalApp;
      this.rollBankRoundout = rollBankRoundout;
      this.durApproach = timeApproach;
      this.durLanding = timeLanding;
      this.durTotal = timeTotal;
   }

   /**
    * Constructor for when using data created using Datarefs over network that includes system time.
    * @param beginFlightTimestamp   time flight simulator started.
    * @param beginApproachTimestamp time approach phase started.
    * @param beginRoundOutTimestamp time roundout phase started.
    * @param beginLandingTimestamp  time landing phase started.
    * @param endFlightTimestamp     time flight ended.
    * @param altStepdown         list of altitude data for stepdown phase.
    * @param dmeStepdown         list of DME distance data for stepdown phase.
    * @param speedStepdown       list of speed data for stepdown phase.
    * @param hDefStepdown        list of localizer (horizontal) deflection data for stepdown phase.
    * @param vDefFinalApproach   list of glideslope (vertical) deflection data for final approach phase.
    * @param speedFinalApproach  list of speed data for final approach phase.
    * @param hDefFinalApproach   list of localizer (horizontal) deflection data for final approach phase.
    * @param altRoundout         list of altitude data for roundout phase.
    * @param hDefRoundout        list of localizer (horizontal) deflection data for roundout phase.
    * @param altLanding          list of altitude data for landing phase.
    * @param hDefLanding         list of localizer (horizontal) deflection for landing phase.
    * @param verticalSpeedFinalApp  list of vertical speed data for final approach phase.
    * @param verticalSpeedStepdown  list of vertical speed data for stepdown phase.
    * @param verticalSpeedRoundout  list of vertical speed data for roundout phase.
    * @param rollBankStepdown    list of roll angle data for stepdown phase. 
    * @param rollBankFinalApp    list of roll (bank) angle for final approach phase.
    * @param rollBankRoundout    list of roll (bank) angle data for roundout phase.
    * @param timeApproach        duration of time to complete approach phase (stepdown + final approach).
    * @param timeLanding         duration of time to complete roundout + landing phase.
    * @param timeTotal           duration of time to complete entire ILS approach and landing.
    */
   public FlightData(LocalDateTime beginFlightTimestamp, LocalDateTime beginApproachTimestamp,
         LocalDateTime beginRoundOutTimestamp, LocalDateTime beginLandingTimestamp, LocalDateTime endFlightTimestamp,
         List<Double> altStepdown, List<Double> dmeStepdown, List<Double> speedStepdown,
         List<Double> hDefStepdown, List<Double> vDefFinalApproach, List<Double> speedFinalApproach,
         List<Double> hDefFinalApproach, List<Double> altRoundout, List<Double> hDefRoundout, 
         List<Double> altLanding, List<Double> hDefLanding, List<Double> verticalSpeedFinalApp,
         List<Double> verticalSpeedStepdown, List<Double> verticalSpeedRoundout, List<Double> rollBankStepdown,
         List<Double> rollBankFinalApp, List<Double> rollBankRoundout,
         double timeApproach, double timeLanding, double timeTotal) {
      
      this.beginFlightTimestamp = beginFlightTimestamp;
      this.beginApproachTimestamp = beginApproachTimestamp;
      this.beginRoundOutTimestamp = beginRoundOutTimestamp;
      this.beginLandingTimestamp = beginLandingTimestamp;
      this.endFlightTimestamp = endFlightTimestamp;
      this.altStepdown = altStepdown;
      this.dmeStepdown = dmeStepdown;
      this.speedStepdown = speedStepdown;
      this.hDefStepdown = hDefStepdown;
      this.vDefFinalApproach = vDefFinalApproach;
      this.speedFinalApproach = speedFinalApproach;
      this.hDefFinalApproach = hDefFinalApproach;
      this.altRoundout = altRoundout;
      this.hDefRoundout = hDefRoundout;
      this.altLanding = altLanding;
      this.hDefLanding = hDefLanding;
      this.verticalSpeedFinalApp = verticalSpeedFinalApp;
      this.verticalSpeedStepdown = verticalSpeedStepdown;
      this.verticalSpeedRoundout = verticalSpeedRoundout;
      this.rollBankStepdown = rollBankStepdown;
      this.rollBankFinalApp = rollBankFinalApp;
      this.rollBankRoundout = rollBankRoundout;
      this.durApproach = timeApproach;
      this.durLanding = timeLanding;
      this.durTotal = timeTotal;
   }

   /**
    * Returns timestamp of when the flight began.
    * @return time flight simulation started.
    */
   public LocalDateTime getBeginFlightTimestamp() {
      return beginFlightTimestamp;
   }

   /**
    * Returns the time of when the approach (stepdown) phase of the simulation began.
    * @return timestamp
    */
   public LocalDateTime getBeginApproachTimestamp() {
      return beginApproachTimestamp;
   }

   /**
    * Returns the time of when the roundout phase of the simulation began.
    * @return timestamp
    */
   public LocalDateTime getBeginRoundOutTimestamp() {
      return beginRoundOutTimestamp;
   }

   /**
    * Returns the time when the landing phase of the simulation began.
    * @return timestamp
    */
   public LocalDateTime getBeginLandingTimestamp() {
      return beginLandingTimestamp;
   }

   /**
    * Returns the time when the flight simulation ended.
    * @return
    */
   public LocalDateTime getEndFlightTimestamp() {
      return endFlightTimestamp;
   }

   /**
    * Returns the altitude data for stepdown phase.
    * @return list of altitudes.
    */
   public List<Double> getAltStepdown() {
      return this.altStepdown;
   }

   /**
    * Returns the DME distance data for stepdown phase.
    * @return list of DME distance.
    */
   public List<Double> getDmeStepdown() {
      return dmeStepdown;
   }

   /**
    * Returns the speed data for stepdown phase.
    * @return list of speed data.
    */
   public List<Double> getSpeedStepdown() {
      return speedStepdown;
   }

   /**
    * Returns the localizer (horizontal) deflection data for stepdown phase.
    * @return list of horizontal deflection data.
    */
   public List<Double> getHDefStepdown() {
      return hDefStepdown;
   }

   /**
    * Returns the vertical (glideslope) deflection data for the final approach phase.
    * @return list of glideslope deflection data.
    */
   public List<Double> getVDefFinalApproach() {
      return vDefFinalApproach;
   }

   /**
    * Returns the speed data for the final approach phase.
    * @return list of speed data.
    */
   public List<Double> getSpeedFinalApproach() {
      return speedFinalApproach;
   }

   /**
    * Returns the localizer (horizontal) deflection for the final approach phase.
    * @return list of horizontal deflection data.
    */
   public List<Double> getHDefFinalApproach() {
      return hDefFinalApproach;
   }

   /**
    * Returns the altitude data for the roundout phase.
    * @return list of altitude data.
    */
   public List<Double> getAltRoundout() {
      return altRoundout;
   }

   /**
    * Returns the localizer (horizontal) deflection data for the roundout phase.
    * @return list of horizontal deflection data.
    */
   public List<Double> getHDefRoundout() {
	   return hDefRoundout;
   }

   /**
    * Returns the altitude data for the landing phase.
    * @return list of altitude data.
    */
   public List<Double> getAltLanding() {
      return altLanding;
   }

   /**
    * Returns the localizer (horizontal) deflection data for landing phase.
    * @return list of horizontal deflection data.
    */
   public List<Double> getHDefLanding() {
      return hDefLanding;
   }

   /**
    * Returns the vertical speed data for the final approach phase.
    * @return list of vertical speed data.
    */
   public List<Double> getVerticalSpeedFinalApp() {
	  return verticalSpeedFinalApp;
   }

   /**
    * Returns the vertical speed data for the stepdown phase.
    * @return list of vertical speed data.
    */
   public List<Double> getVerticalSpeedStepdown() {
	   return verticalSpeedStepdown;
   }

   /**
    * Returns the vertical speed for the roundout phase.
    * @return list of vertical speed data.
    */
   public List<Double> getVerticalSpeedRoundout() {
	   return verticalSpeedRoundout;
   }

   /**
    * Returns the roll (bank) angle data for the stepdown phase.
    * @return list of roll angle data.
    */
   public List<Double> getRollBankStepdown() {
	   return rollBankStepdown;
   }

   /**
    * Returns the roll (bank) angle data for the final approach phase.
    * @return list of roll angle data.
    */
   public List<Double> getRollFinalApp() {
	   return rollBankFinalApp;
   }

   /**
    * Returns the roll (bank) angle data for the roundout phase.
    * @return list of roll angle data.
    */
   public List<Double> getRollBankRoundout() {
	   return rollBankRoundout;
   }

   /**
    * Returns the duration of time to complete approach (stepdown phase + final approach phase).
    * @return duration
    */
   public double getDurApproach() {
      return durApproach;
   }

   /**
    * Returns the duration of time to complete landing (stepdown phase + landing phase).
    * @return duration
    */
   public double getDurLanding() {
      return durLanding;
   }

   /**
    * Returns the duration of time to complete the ILS approach and landing.
    * @return duration
    */
   public double getDurTotal() {
      return durTotal;
   }
}
