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
}
