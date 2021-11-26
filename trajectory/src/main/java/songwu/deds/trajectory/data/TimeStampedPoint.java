package songwu.deds.trajectory.data;

import songwu.deds.trajectory.similarity.Euclidean;
import songwu.deds.trajectory.similarity.Manhattan;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeStampedPoint implements TrajectoryUnit<TimeStampedPoint>, Euclidean, Manhattan {
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String timestamp_string;
    private Date timestamp;
    private double longitude;
    private double latitude;
    private double x;
    private double y;
    private double time_gap;
    private double distance_gap;
    private double euc_speed;
    private double bearing;
    private double signed_turn;
    private boolean turned;
    private boolean speedchanged;
    private boolean paused;

    public TimeStampedPoint(){
    }

    public TimeStampedPoint(double x, double y){
        this.x = x;
        this.y = y;
    }

    public static void setTimestampFormat(String format){
        formatter = new SimpleDateFormat(format);
    }

    //    in hours
    public static double duration(TimeStampedPoint from, TimeStampedPoint to){
        long start = from.timestamp.getTime();
        long end = to.timestamp.getTime();
        return 1.0 * (end - start) / 1000 / 3600;
    }

    public static String mid_timestamp(TimeStampedPoint from, TimeStampedPoint to){
        long start = from.timestamp.getTime();
        long end = to.timestamp.getTime();
        Date date = new Date ((start + end) / 2);
        return formatter.format(date);
    }

    public static double geography_angle(TimeStampedPoint from, TimeStampedPoint to){
        double rad = Math.PI / 180;
        double lat1 = from.latitude * rad, lon1 = from.longitude * rad;
        double lat2 = to.latitude * rad, lon2 = to.longitude * rad;

        double _1 = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double _2 = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
        double answer = Math.atan2(_1, _2);
        answer = answer % (2 * Math.PI) * 180 / Math.PI;
        return answer < 0 ? answer + 360 : answer;
    }

    public static double signed_turn(TimeStampedPoint from, TimeStampedPoint to){
        double signed_turn = Math.abs(to.bearing - from.bearing);
        signed_turn = signed_turn <= 180 ? signed_turn : (360 - signed_turn);
        if(to.bearing < from.bearing && to.bearing > from.bearing - 180)
            signed_turn *= -1;
        else if(to.bearing > from.bearing + 180)
            signed_turn *= -1;
        return signed_turn;
    }

    public TimeStampedPoint signed_turn(double signed_turn){
        this.signed_turn = signed_turn; return this;
    }

    public TimeStampedPoint bearing(double bearing){
        this.bearing = bearing; return this;
    }

    public TimeStampedPoint euc_speed(double euc_speed){
        this.euc_speed = euc_speed; return this;
    }

    public TimeStampedPoint distance_gap(double distance_gap){
        this.distance_gap = distance_gap; return this;
    }

    public TimeStampedPoint time_gap(double time_gap){
        this.time_gap = time_gap; return this;
    }

    //in units: "n" knots / hour
    public TimeStampedPoint euc_speed(TimeStampedPoint source){
        this.time_gap = (getTimestampLong() - source.getTimestampLong()) / 1000.0;
        this.distance_gap = Euclidean.distance(this, source);
        this.euc_speed =  distance_gap / time_gap * 3600 / 1852;
        return this;
    }

    //右是正，左是负
    public TimeStampedPoint signed_turn(TimeStampedPoint source){
        this.signed_turn = signed_turn(source, this);
        return this;
    }

    public TimeStampedPoint geography_angle(TimeStampedPoint source){
        this.bearing = geography_angle(source, this);
        return this;
    }

    public TimeStampedPoint setX(double x) {
        this.x = x; return this;
    }

    public TimeStampedPoint setY(double y){
        this.y = y; return this;
    }

    public TimeStampedPoint timestamp_string(String timestamp_string) throws ParseException {
        this.timestamp_string = timestamp_string;
        if(this.timestamp_string != null){
            this.timestamp = formatter.parse(this.timestamp_string);
        }
        return this;
    }

    public TimeStampedPoint setTurned(boolean turned) {
        this.turned = turned; return this;
    }

    public TimeStampedPoint setSpeedchanged(boolean speedchanged) {
        this.speedchanged = speedchanged; return this;
    }

    public TimeStampedPoint setPaused(boolean paused) {
        this.paused = paused; return this;
    }

    public TimeStampedPoint longitude(double longitude){
        this.longitude = longitude; return this;
    }

    public TimeStampedPoint latitide(double latitude){
        this.latitude = latitude; return this;
    }

    public double getTimeGap() {
        return time_gap;
    }

    public double getDistanceGap() {
        return distance_gap;
    }

    public double getBearing() {
        return bearing;
    }

    public double getSignedTurn() {
        return signed_turn;
    }

    public double getEucSpeed() {
        return euc_speed;
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

    public long getTimestampLong(){
        return timestamp.getTime();
    }

    public boolean isTurned() {
        return turned;
    }

    public boolean isSpeedchanged() {
        return speedchanged;
    }

    public boolean isPaused() {
        return paused;
    }

    public void copy(TimeStampedPoint point){
        this.time_gap = point.time_gap;
        this.distance_gap = point.distance_gap;
        this.euc_speed = point.euc_speed;
        this.bearing = point.bearing;
    }

    @Override
    public double[] vectorizedEuclidean() {
        return new double[]{x, y};
    }

    @Override
    public double[] vectorizedManhattan() {
        return vectorizedEuclidean();
    }

    public static void main(String[] args) {

    }
}
