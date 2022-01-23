package songwu.deds.trajectory.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeStampedPointT implements Trajectory<TimeStampedPoint>{
    private List<TimeStampedPoint> points;
    private String id;

    public TimeStampedPointT(String id) {
        points = new ArrayList<>();
        this.id = id;
    }

    public double avgX(int index, int history){
        double x_total = 0;
        for(int k = index - history + 1; k <= index; k++){
            x_total += getUnit(k).getX();
        }
        return x_total / history;
    }

    public double avgY(int index, int history){
        double y_total = 0;
        for(int k = index - history + 1; k <= index; k++){
            y_total += getUnit(k).getY();
        }
        return y_total / history;
    }

    public double avgLongitude(int index, int history){
        double longitude_total = 0;
        for(int k = index - history + 1; k <= index; k++){
            longitude_total += getUnit(k).getLongitude();
        }
        return longitude_total / history;
    }

    public double avgLaitude(int index, int history){
        double latitude_total = 0;
        for(int k = index - history + 1; k <= index; k++){
            latitude_total += getUnit(k).getLatitude();
        }
        return latitude_total / history;
    }

    public double avgSpeed(int index, int history){
        double speed_total = 0;
        double time_total = 0;
        for(int k = index - history + 1; k <= index; k++){
            speed_total += getUnit(k).getEucSpeed() * getUnit(k).getTimeGap();
            time_total += getUnit(k).getTimeGap();
        }
        return speed_total / time_total;
    }

    public double deviationSpeed(int index, int history, double meanSpeed){
        double numerator = 0;
        double denominator = 0;
        for(int k = index - history + 1; k <= index; k++){
            numerator += Math.pow(getUnit(k).getEucSpeed() - meanSpeed ,2) * getUnit(k).getTimeGap();
            denominator += getUnit(k).getTimeGap();
        }
        return Math.sqrt(numerator / denominator);
    }

    public void addPoint(TimeStampedPoint point){
        points.add(point);
    }

    public void addPoints(TimeStampedPoint... points){
        this.points.addAll(Arrays.asList(points));
    }

    public TimeStampedPointT setId(String id){
        this.id = id;
        return this;
    }

    @Override
    public int size() {
        return points.size();
    }

    @Override
    public TimeStampedPoint getUnit(int index) {
        return points.get(index);
    }

    @Override
    public String trajId() {
        return id;
    }

    public TimeStampedPointT subTraj(int start, int end){
        TimeStampedPointT t = new TimeStampedPointT(id);
        t.points.addAll(points.subList(start, end));
        return t;
    }

    @Override
    public List<TimeStampedPoint> subList(int start, int end){
        return points.subList(start, end);
    }

    @Override
    public List<TimeStampedPoint> getAllUnits() {
        return points;
    }
}
