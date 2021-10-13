package songwu.deds.trajectory.data;

import java.util.List;

public interface Trajectory <T extends TrajectoryUnit<T>>{
    int size();
    T getUnit(int index);
    String trajId();
    List<T> subList(int start, int end);
    List<T> getAllUnits();
}
