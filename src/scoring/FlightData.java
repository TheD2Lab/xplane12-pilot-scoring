package scoring;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class FlightData {
   // important timestamps
   private LocalDateTime beginFlightTimestamp = null;
   private LocalDateTime beginApproachTimestamp = null;
   private LocalDateTime beginRoundOutTimestamp = null;
   private LocalDateTime beginLandingTimestamp = null;
   private LocalDateTime endFlightTimestamp = null;

   // Stepdown portion
   private List<Double> altStepdown;
   private List<Double> dmeStepdown;
   private List<Double> speedStepdown;
   private List<Double> hDefStepdown;
   private List<Double> rollBankStepdown;
   private List<Double> verticalSpeedStepdown;
   
   // Final Approach portion
   private List<Double> vDefFinalApproach;
   private List<Double> speedFinalApproach;
   private List<Double> hDefFinalApproach;
   private List<Double> verticalSpeedFinalApp;
   private List<Double> rollBankFinalApp;
   // Roundout portion
   private List<Double> altRoundout;
   private List<Double> hDefRoundout;
   private List<Double> rollBankRoundout;
   private List<Double> verticalSpeedRoundout;
   // Landing portion
   private List<Double> altLanding;
   private List<Double> hDefLanding;
   // Time data
   private double timeApproach = 0;
   private double timeLanding = 0;
   private double timeTotal = 0;

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
    * Constructor for when using Data.txt created by xplane software.
    * @param altStepdown
    * @param dmeStepdown
    * @param speedStepdown
    * @param hDefStepdown
    * @param vDefFinalApproach
    * @param speedFinalApproach
    * @param hDefFinalApproach
    * @param altRoundout
    * @param hDefRoundout
    * @param altLanding
    * @param hDefLanding
    * @param verticalSpeedFinalApp
    * @param verticalSpeedStepdown
    * @param verticalSpeedRoundout
    * @param rollBankStepdown
    * @param rollBankFinalApp
    * @param rollBankRoundout
    * @param timeApproach
    * @param timeLanding
    * @param timeTotal
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
      this.timeApproach = timeApproach;
      this.timeLanding = timeLanding;
      this.timeTotal = timeTotal;
   }

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
      this.timeApproach = timeApproach;
      this.timeLanding = timeLanding;
      this.timeTotal = timeTotal;
   }

   public LocalDateTime getBeginFlightTimestamp() {
      return beginFlightTimestamp;
   }

   public LocalDateTime getBeginApproachTimestamp() {
      return beginApproachTimestamp;
   }

   public LocalDateTime getBeginRoundOutTimestamp() {
      return beginRoundOutTimestamp;
   }

   public LocalDateTime getBeginLandingTimestamp() {
      return beginLandingTimestamp;
   }

   public LocalDateTime getEndFlightTimestamp() {
      return endFlightTimestamp;
   }

   public List<Double> getAltStepdown() {
      return this.altStepdown;
   }

   public List<Double> getDmeStepdown() {
      return dmeStepdown;
   }

   public void setDmeStepdown(List<Double> dmeStepdown) {
      this.dmeStepdown = dmeStepdown;
   }

   public List<Double> getSpeedStepdown() {
      return speedStepdown;
   }

   public List<Double> getHDefStepdown() {
      return hDefStepdown;
   }

   public List<Double> getVDefFinalApproach() {
      return vDefFinalApproach;
   }

   public List<Double> getSpeedFinalApproach() {
      return speedFinalApproach;
   }

   public List<Double> getHDefFinalApproach() {
      return hDefFinalApproach;
   }

   public List<Double> getAltRoundout() {
      return altRoundout;
   }

   public List<Double> getHDefRoundout() {
	   return hDefRoundout;
   }

   public List<Double> getAltLanding() {
      return altLanding;
   }

   public List<Double> getHDefLanding() {
      return hDefLanding;
   }

   public List<Double> getVerticalSpeedFinalApp() {
	  return verticalSpeedFinalApp;
   }

   public List<Double> getVerticalSpeedStepdown() {
	   return verticalSpeedStepdown;
   }

   public List<Double> getVerticalSpeedRoundout() {
	   return verticalSpeedRoundout;
   }

   public List<Double> getRollBankStepdown() {
	   return rollBankStepdown;
   }

   public List<Double> getRollFinalApp() {
	   return rollBankFinalApp;
   }

   public List<Double> getRollBankRoundout() {
	   return rollBankRoundout;
   }

   // Returning portions of the approach and landing
   public List<Double> gethDefStepdown() {
      return hDefStepdown;
   }

   public List<Double> getvDefFinalApproach() {
      return vDefFinalApproach;
   }

   public List<Double> gethDefFinalApproach() {
      return hDefFinalApproach;
   }

   public List<Double> gethDefLanding() {
      return hDefLanding;
   }

   public double getTimeApproach() {
      return timeApproach;
   }

   public double getTimeLanding() {
      return timeLanding;
   }

   public double getTimeTotal() {
      return timeTotal;
   }
}
