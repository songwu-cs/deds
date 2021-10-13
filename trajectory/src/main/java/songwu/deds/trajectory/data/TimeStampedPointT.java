package songwu.deds.trajectory.data;

import java.sql.Time;
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

    public double avgSpeed(int index, int history){
        double speed_total = 0;
        double time_total = 0;
        for(int k = index - history + 1; k <= index; k++){
            speed_total += getUnit(k).getEucSpeed() * getUnit(k).getTimeGap();
            time_total += getUnit(k).getTimeGap();
        }
        return speed_total / time_total;
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

    public void addPoint(TimeStampedPoint point){
        points.add(point);
    }

    public void addPoints(TimeStampedPoint... points){
        this.points.addAll(Arrays.asList(points));
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

    @Override
    public List<TimeStampedPoint> subList(int start, int end){
        return points.subList(start, end);
    }

    @Override
    public List<TimeStampedPoint> getAllUnits() {
        return points;
    }
}
