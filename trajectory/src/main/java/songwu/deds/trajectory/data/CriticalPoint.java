package songwu.deds.trajectory.data;

public class CriticalPoint implements TrajectoryUnit<CriticalPoint>{
    private String timestamp;
    private double longitude;
    private double latitude;
    private double x;
    private double y;
    private double euc_speed;
    private double total_duration;
    private String type;
    private long order;

    public String getTimestamp() {
        return timestamp;
    }

    public CriticalPoint setTimestamp(String timestamp) {
        this.timestamp = timestamp; return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public CriticalPoint setLongitude(double longitude) {
        this.longitude = longitude; return this;
    }

    public double getLatitude() {
        return latitude;
    }

    public CriticalPoint setLatitude(double latitude) {
        this.latitude = latitude; return this;
    }

    public double getX() {
        return x;
    }

    public CriticalPoint setX(double x) {
        this.x = x; return this;
    }

    public double getY() {
        return y;
    }

    public CriticalPoint setY(double y) {
        this.y = y; return this;
    }

    public double getEucSpeed() {
        return euc_speed;
    }

    public CriticalPoint setEucSpeed(double euc_speed) {
        this.euc_speed = euc_speed; return this;
    }

    public double getTotalDuration() {
        return total_duration;
    }

    public CriticalPoint setTotalDuration(double total_duration) {
        this.total_duration = total_duration; return this;
    }

    public long getOrder(){
        return order;
    }

    public CriticalPoint setOrder(long order){
        this.order = order;
        return this;
    }

    public String getType() {
        return type;
    }

    public CriticalPoint setType(String type) {
        this.type = type; return this;
    }

    public CriticalPoint copy(TimeStampedPoint point){
        this.timestamp = point.getTimestamp();
        this.longitude = point.getLongitude();
        this.latitude = point.getLatitude();
        this.x = point.getX();
        this.y = point.getY();
        this.euc_speed = point.getEucSpeed();
        this.order = point.getTimestampLong();
        return this;
    }

    @Override
    public double distance(CriticalPoint criticalPoint) {
        return 0;
    }
}
