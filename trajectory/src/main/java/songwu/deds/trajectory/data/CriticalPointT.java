package songwu.deds.trajectory.data;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class CriticalPointT implements Trajectory<CriticalPoint> {
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

    public static void saveCriticalPointT(String outputPath, List<CriticalPointT> ts) throws FileNotFoundException {
        try(PrintWriter writer = new PrintWriter(outputPath)){
            writer.write("id,t,longitude,latitude,x,y,type\n");
            for(CriticalPointT traj : ts){
                for(CriticalPoint p : traj.getAllUnits()){
                    writer.write(traj.trajId() + ",");
                    writer.write(p.getTimestamp() + ",");
                    writer.write(p.getLongitude() + ",");
                    writer.write(p.getLatitude() + ",");
                    writer.write(p.getX() + ",");
                    writer.write(p.getY() + ",");
                    writer.write(p.getType() + "\n");
                }
            }
        }
    }
}
