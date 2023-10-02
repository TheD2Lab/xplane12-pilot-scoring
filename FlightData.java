package xplane12_data_parser;

import java.util.LinkedList;
import java.util.List;

public class FlightData {
   private List<Double> altStepdown;
   private List<Double> dmeStepdown;
   private List<Double> speedStepdown;
   private List<Double> hDefStepdown;
   private List<Double> vDefFinalApproach;
   private List<Double> speedFinalApproach;
   private List<Double> hDefFinalApproach;
   private List<Double> altRoundout;
   private List<Double> altLanding;
   private List<Double> hDefLanding;
   private double timeApproach = 0;
   private double timeLanding = 0;
   private double timeTotal = 0;

   FlightData() {
      this.altStepdown = new LinkedList<>();
      this.dmeStepdown = new LinkedList<>();
      this.speedStepdown = new LinkedList<>();
      this.hDefStepdown = new LinkedList<>();
      this.vDefFinalApproach = new LinkedList<>();
      this.speedFinalApproach = new LinkedList<>();
      this.hDefFinalApproach = new LinkedList<>();
      this.altRoundout = new LinkedList<>();
      this.altLanding = new LinkedList<>();
      this.hDefLanding = new LinkedList<>();
   }

   
   public FlightData(List<Double> altStepdown, List<Double> dmeStepdown, List<Double> speedStepdown,
         List<Double> hDefStepdown, List<Double> vDefFinalApproach, List<Double> speedFinalApproach,
         List<Double> hDefFinalApproach, List<Double> altRoundout, List<Double> altLanding, List<Double> hDefLanding,
         double timeApproach, double timeLanding, double timeTotal) {
      this.altStepdown = altStepdown;
      this.dmeStepdown = dmeStepdown;
      this.speedStepdown = speedStepdown;
      this.hDefStepdown = hDefStepdown;
      this.vDefFinalApproach = vDefFinalApproach;
      this.speedFinalApproach = speedFinalApproach;
      this.hDefFinalApproach = hDefFinalApproach;
      this.altRoundout = altRoundout;
      this.altLanding = altLanding;
      this.hDefLanding = hDefLanding;
      this.timeApproach = timeApproach;
      this.timeLanding = timeLanding;
      this.timeTotal = timeTotal;
   }




   public List<Double> getAltStepdown() {
      return altStepdown;
   }
   public void setAltStepdown(List<Double> altStepdown) {
      this.altStepdown = altStepdown;
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
   public void setSpeedStepdown(List<Double> speedStepdown) {
      this.speedStepdown = speedStepdown;
   }
   public List<Double> getHDefStepdown() {
      return hDefStepdown;
   }
   public void setHDefStepdown(List<Double> hDefStepdown) {
      this.hDefStepdown = hDefStepdown;
   }
   public List<Double> getVDefFinalApproach() {
      return vDefFinalApproach;
   }
   public void setVDefFinalApproach(List<Double> vDefFinalApproach) {
      this.vDefFinalApproach = vDefFinalApproach;
   }
   public List<Double> getSpeedFinalApproach() {
      return speedFinalApproach;
   }
   public void setSpeedFinalApproach(List<Double> speedFinalApproach) {
      this.speedFinalApproach = speedFinalApproach;
   }
   public List<Double> getHDefFinalApproach() {
      return hDefFinalApproach;
   }
   public void setHDefFinalApproach(List<Double> hDefFinalApproach) {
      this.hDefFinalApproach = hDefFinalApproach;
   }
   public List<Double> getAltRoundout() {
      return altRoundout;
   }
   public void setAltRoundout(List<Double> altRoundout) {
      this.altRoundout = altRoundout;
   }
   public List<Double> getAltLanding() {
      return altLanding;
   }
   public void setAltLanding(List<Double> altLanding) {
      this.altLanding = altLanding;
   }
   public List<Double> getHDefLanding() {
      return hDefLanding;
   }
   public void setHDefLanding(List<Double> hDefLanding) {
      this.hDefLanding = hDefLanding;
   }


   public List<Double> gethDefStepdown() {
      return hDefStepdown;
   }


   public void sethDefStepdown(List<Double> hDefStepdown) {
      this.hDefStepdown = hDefStepdown;
   }


   public List<Double> getvDefFinalApproach() {
      return vDefFinalApproach;
   }


   public void setvDefFinalApproach(List<Double> vDefFinalApproach) {
      this.vDefFinalApproach = vDefFinalApproach;
   }


   public List<Double> gethDefFinalApproach() {
      return hDefFinalApproach;
   }


   public void sethDefFinalApproach(List<Double> hDefFinalApproach) {
      this.hDefFinalApproach = hDefFinalApproach;
   }


   public List<Double> gethDefLanding() {
      return hDefLanding;
   }


   public void sethDefLanding(List<Double> hDefLanding) {
      this.hDefLanding = hDefLanding;
   }


   public double getTimeApproach() {
      return timeApproach;
   }


   public void setTimeApproach(double timeApproach) {
      this.timeApproach = timeApproach;
   }


   public double getTimeLanding() {
      return timeLanding;
   }


   public void setTimeLanding(double timeLanding) {
      this.timeLanding = timeLanding;
   }


   public double getTimeTotal() {
      return timeTotal;
   }


   public void setTimeTotal(double timeTotal) {
      this.timeTotal = timeTotal;
   }

}
