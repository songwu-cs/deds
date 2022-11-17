package noteAAU.task_2022_09_06_CO2.EmissionModels;

public class AISsegment {
    public final String mmsi;
    public final double distance;
    public final double timeGap;
    public final double speed;
    public final double draught;
    public final String midTimestamp;
    public final double midLongitude;
    public final double midLatitude;
    public final double heading;
    public double speedCorrected = -1;
    public double draughtCorrected = -1;
    public double cargoTons = -1;
    public double significantWaveHeight = -1;
    public double waveFromDirection = -1;
    public double displacement = -1;

    public AISsegment(String mmsi, double distance, double timeGap, double speed, double draught, double heading, String midT, double midLon, double midLat) {
        this.mmsi = mmsi;
        this.distance = distance;
        this.timeGap = timeGap;
        this.speed = speed;
        this.draught = draught;
        this.heading = heading;
        this.midTimestamp = midT;
        this.midLatitude = midLat;
        this.midLongitude = midLon;
    }

    public String draughtCorrect(){
        return draughtCorrected < 0 ? "" : (draughtCorrected < draught ? "true" : "false");
    }

    public String speedCorrect(){
        return speedCorrected < 0 ? "" : (speedCorrected < speed ? "true" : "false");
    }

    public String cargoCarriedTons(){
        return cargoTons < 0 ? "" : (""+cargoTons);
    }
}
