package scoring;

public class DataIndex {
   // the indexes for the X-Plane data we use for scoring and statistics
   private int iSysTime = -1;
   private int iAlt = -1;
   private int iDme = -1;
   private int iHdef = -1;
   private int iVdef = -1;
   private int iASpeed = -1;
   private int iMTime = -1;
   private int iGroll = -1;
   private int iVspeed = -1;
   private int iBank = -1;
   private int iEng = -1;
   private int iHead = -1;
   private int iLongitude = -1;
   private int iLatitude = -1;

   /**
    * Default constructor.
    */
   public DataIndex() {
   };

   /**
    * Constructor.
    * 
    * @param iSysTime system time index
    * @param iAlt     altitude index
    * @param iDme     DME index
    * @param iHdef    localizer deflection index
    * @param iVdef    glideslope deflection index
    * @param iSpeed   airspeed index
    * @param iMTime   mission time index
    * @param iGroll   ground roll index
    * @param iVspeed  vertical speed index
    * @param iBank    roll (bank) angle index
    * @param iEng     engine setting index
    * @param iHead    magnetic heading index
    */
   public DataIndex(int iSysTime, int iAlt, int iDme, int iHdef, int iVdef, int iASpeed, int iMTime, int iGroll,
         int iVspeed, int iBank, int iEng, int iHead, int iLongitude, int iLatitude) {
      this.iSysTime = iSysTime;
      this.iAlt = iAlt;
      this.iDme = iDme;
      this.iHdef = iHdef;
      this.iVdef = iVdef;
      this.iASpeed = iASpeed;
      this.iMTime = iMTime;
      this.iGroll = iGroll;
      this.iVspeed = iVspeed;
      this.iBank = iBank;
      this.iEng = iEng;
      this.iHead = iHead;
      this.iLongitude = iLongitude;
      this.iLatitude = iLatitude;
   }

   public int getiLongitude() {
      return iLongitude;
   }

   public void setiLongitude(int iLongitude) {
      this.iLongitude = iLongitude;
   }

   public int getiLatitude() {
      return iLatitude;
   }

   public void setiLatitude(int iLatitude) {
      this.iLatitude = iLatitude;
   }

   public int getiSysTime() {
      return iSysTime;
   }

   public void setiSysTime(int iSysTime) {
      this.iSysTime = iSysTime;
   }

   public int getiAlt() {
      return iAlt;
   }

   public void setiAlt(int iAlt) {
      this.iAlt = iAlt;
   }

   public int getiDme() {
      return iDme;
   }

   public void setiDme(int iDme) {
      this.iDme = iDme;
   }

   public int getiHdef() {
      return iHdef;
   }

   public void setiHdef(int iHdef) {
      this.iHdef = iHdef;
   }

   public int getiVdef() {
      return iVdef;
   }

   public void setiVdef(int iVdef) {
      this.iVdef = iVdef;
   }

   public int getiASpeed() {
      return iASpeed;
   }

   public void setiASpeed(int iSpeed) {
      this.iASpeed = iSpeed;
   }

   public int getiMTime() {
      return iMTime;
   }

   public void setiMTime(int iMTime) {
      this.iMTime = iMTime;
   }

   public int getiGroll() {
      return iGroll;
   }

   public void setiGroll(int iGroll) {
      this.iGroll = iGroll;
   }

   public int getiVspeed() {
      return iVspeed;
   }

   public void setiVspeed(int iVspeed) {
      this.iVspeed = iVspeed;
   }

   public int getiBank() {
      return iBank;
   }

   public void setiBank(int iBank) {
      this.iBank = iBank;
   }

   public int getiEng() {
      return iEng;
   }

   public void setiEng(int iEng) {
      this.iEng = iEng;
   }

   public int getiHead() {
      return iHead;
   }

   public void setiHead(int iHead) {
      this.iHead = iHead;
   }

}
