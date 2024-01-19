package scoring;

import java.time.LocalDateTime;
import java.util.List;

public class FlightData {

   // important timestamps
   private LocalDateTime beginFlightTimestamp = null;
   private LocalDateTime beginApproachTimestamp = null;
   private LocalDateTime beginRoundOutTimestamp = null;
   private LocalDateTime beginLandingTimestamp = null;
   private LocalDateTime endFlightTimestamp = null;

   // Stepdown portion
   private List<FlightDataPoint> stepdownData;
   
   // Final Approach portion
   private List<FlightDataPoint> approachData;

   // Roundout portion
   private List<FlightDataPoint> roundoutData;

   // Landing portion
   private List<FlightDataPoint> landingData;

   // Time data
   private double timeApproach = 0;
   private double timeLanding = 0;
   private double timeTotal = 0;

   public FlightData(List<FlightDataPoint> stepdown, List<FlightDataPoint> approach, List<FlightDataPoint> roundout,
         List<FlightDataPoint> landing, double timeApproach, double timeLanding, double timeTotal) {
      this.stepdownData = stepdown;
      this.approachData = approach;
      this.roundoutData = roundout;
      this.landingData = landing;
      this.timeApproach = timeApproach;
      this.timeLanding = timeLanding;
      this.timeTotal = timeTotal;
   }

   public FlightData(LocalDateTime beginFlightTimestamp, LocalDateTime beginApproachTimestamp,
         LocalDateTime beginRoundOutTimestamp, LocalDateTime beginLandingTimestamp, LocalDateTime endFlightTimestamp,
         List<FlightDataPoint> stepdown, List<FlightDataPoint> approach, List<FlightDataPoint> roundout,
         List<FlightDataPoint> landing, double timeApproach, double timeLanding, double timeTotal) {
      this.beginFlightTimestamp = beginFlightTimestamp;
      this.beginApproachTimestamp = beginApproachTimestamp;
      this.beginRoundOutTimestamp = beginRoundOutTimestamp;
      this.beginLandingTimestamp = beginLandingTimestamp;
      this.endFlightTimestamp = endFlightTimestamp;
      this.stepdownData = stepdown;
      this.approachData = approach;
      this.roundoutData = roundout;
      this.landingData = landing;
      this.timeApproach = timeApproach;
      this.timeLanding = timeLanding;
      this.timeTotal = timeTotal;
   }

   public void setBeginFlightTimestamp(LocalDateTime beginFlightTimestamp) {
      this.beginFlightTimestamp = beginFlightTimestamp;
   }

   public void setBeginApproachTimestamp(LocalDateTime beginApproachTimestamp) {
      this.beginApproachTimestamp = beginApproachTimestamp;
   }

   public void setBeginRoundOutTimestamp(LocalDateTime beginRoundOutTimestamp) {
      this.beginRoundOutTimestamp = beginRoundOutTimestamp;
   }

   public void setBeginLandingTimestamp(LocalDateTime beginLandingTimestamp) {
      this.beginLandingTimestamp = beginLandingTimestamp;
   }

   public void setEndFlightTimestamp(LocalDateTime endFlightTimestamp) {
      this.endFlightTimestamp = endFlightTimestamp;
   }

   public List<FlightDataPoint> getStepdownData() {
      return stepdownData;
   }

   public List<FlightDataPoint> getApproachData() {
      return approachData;
   }

   public List<FlightDataPoint> getRoundoutData() {
      return roundoutData;
   }

   public List<FlightDataPoint> getLandingData() {
      return landingData;
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
