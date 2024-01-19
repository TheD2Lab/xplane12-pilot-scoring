package scoring;

/**
 * Holds data for a single flight data point/entry.
 */
public class FlightDataPoint {

   private double missn_time = 0;
   private double airspeed = 0;
   private double engine = 0;
   private double bank = 0;
   private double groll = 0;
   private double heading = 0;
   private double vertSpeed = 0;
   private double altitude = 0;
   private double latitude = 0;
   private double longitude = 0;
   private double dme = 0;
   private double hdef = 0;
   private double vdef = 0;

   /**
    * Constructor.
    * 
    * @param missn_time mission time
    * @param airspeed   airspeed
    * @param engine     engine speed
    * @param bank       Roll (bank) angle
    * @param groll      Landing distance "ground roll"
    * @param vvi        vertical speed
    * @param altitude   altitude
    * @param heading    magnetic heading
    * @param latitude   latitude
    * @param longitude  longitude
    * @param dme        DME distance
    * @param hdef       localizer deflection
    * @param vdef       glideslope deflection
    */
   public FlightDataPoint(double missn_time, double airspeed, double engine, double bank, double groll,
         double vvi, double altitude, double heading, double latitude, double longitude,
         double dme, double hdef, double vdef) {
      this.missn_time = missn_time;
      this.airspeed = airspeed;
      this.engine = engine;
      this.bank = bank;
      this.groll = groll;
      this.vertSpeed = vvi;
      this.altitude = altitude;
      this.heading = heading;
      this.latitude = latitude;
      this.longitude = longitude;
      this.dme = dme;
      this.hdef = hdef;
      this.vdef = vdef;
   };

   public double getMissn_time() {
      return missn_time;
   }

   public double getAirspeed() {
      return airspeed;
   }

   public double getEngine() {
      return engine;
   }

   public double getBank() {
      return bank;
   }

   public double getGroll() {
      return groll;
   }

   public double getVertSpeed() {
      return vertSpeed;
   }

   public double getAltitude() {
      return altitude;
   }

   public double getHeading() {
      return heading;
   }

   public double getLatitude() {
      return latitude;
   }

   public double getLongitude() {
      return longitude;
   }

   public double getDme() {
      return dme;
   }

   public double getHdef() {
      return hdef;
   }

   public double getVdef() {
      return vdef;
   }

}
