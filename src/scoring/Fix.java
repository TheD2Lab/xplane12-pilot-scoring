package scoring;

/**
 * Geographical position, or Fix, marked on an approach plate. Each labeled fix is a specified distance in nautical
 * miles away from the runway. Pilots rely on their Distance measuring equipment (DME) in the cockpit to determine
 * when they have passed a Fix. During the stepdown portion of an ILS approach and landing, there is a minimum Mean Sea Level (MSL)
 * altitude pilots must fly above that decreases at each fix closer to the runway. 
 */
public class Fix {
   /**
    * Distance from the runway in nautical miles.
    */
   public double dme;
   /**
    * Minimum stepdown MSL altitude in feet immediately before the fix.
    */
   public int altitude;

   /**
    * Constructs a new Fix and sets the DME and altitude.
    * @param dme DME distance in nautical miles.
    * @param altitude Minimum stepdown MSL altitude in feet immediately before the fix.
    */
   Fix(double dme, int altitude) {
      this.dme = dme;
      this.altitude = altitude;
   }
}
