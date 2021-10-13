package songwu.deds.trajectory.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CriticalPointT implements Trajectory<CriticalPoint>{
    private List<CriticalPoint> points;
    private String id;

    public CriticalPointT(String id) {
        points = new ArrayList<>();
        this.id = id;
    }

    public void sort(){
        points.sort(Comparator.comparingLong(CriticalPoint::getOrder));
    }

    public void addPoint(CriticalPoint point){
        points.add(point);
    }

    public void addPoints(CriticalPoint... points){
        this.points.addAll(Arrays.asList(points));
    }

    @Override
    public int size() {
        return points.size();
    }

    @Override
    public CriticalPoint getUnit(int index) {
        return points.get(index);
    }

    @Override
    public String trajId() {
        return id;
    }

    @Override
    public List<CriticalPoint> subList(int start, int end) {
        return points.subList(start, end);
    }

    @Override
    public List<CriticalPoint> getAllUnits() {
        return points;
    }
}
