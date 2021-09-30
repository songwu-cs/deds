package songwu.deds.trajectory.data;

import songwu.deds.trajectory.similarity.Euclidean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TimeStampedPoint implements TrajectoryUnit<TimeStampedPoint>, Euclidean<TimeStampedPoint> {
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static void setTimestampFormat(String format){
        formatter = new SimpleDateFormat(format);
    }

    private String timestamp_string;
    private Date timestamp;
    private double longitude;
    private double latitude;
    private double x;
    private double y;

    public TimeStampedPoint(double x, double y){
        this.x = x;
        this.y = y;
    }

    public TimeStampedPoint timestamp_string(String timestamp_string) throws ParseException {
        this.timestamp_string = timestamp_string;
        if(this.timestamp_string != null){
            this.timestamp = formatter.parse(this.timestamp_string);
        }
        return this;
    }

    public TimeStampedPoint longitude(double longitude){
        this.longitude = longitude; return this;
    }

    public TimeStampedPoint latitide(double latitude){
        this.latitude = latitude; return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getTimestamp() {
        return timestamp_string;
    }

    @Override
    public double eucDistance(TimeStampedPoint timeStampedPoint) {
        double x_ = timeStampedPoint.getX();
        double y_ = timeStampedPoint.getY();
        return Math.sqrt((x - x_) * (x - x_) + (y - y_) * (y - y_));
    }

    @Override
    public double distance(TimeStampedPoint timeStampedPoint) {
        return eucDistance(timeStampedPoint);
    }
}
