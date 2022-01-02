package note.task_2021_12_11_fishgear_classification;

public class NovelPoint {
    public final String timestamp;
    public final double x;
    public final double y;
    private double feature;
    public final String mmsi;
    public final double latiude;
    public final double longitude;

    public NovelPoint(String mmsi, String timestamp, double x, double y, double lat, double longitude) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.mmsi = mmsi;
        this.latiude = lat;
        this.longitude = longitude;
    }

    public double getFeature() {
        return feature;
    }

    public void setFeature(double feature) {
        this.feature = feature;
    }


    public double eucDistance(NovelPoint point){
        return Math.hypot(x - point.x, y  - point.y);
    }
}
