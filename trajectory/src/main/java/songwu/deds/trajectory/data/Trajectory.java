package songwu.deds.trajectory.data;

public interface Trajectory <T extends TrajectoryUnit<T>>{
    int size();
    T getUnit(int index);
    String trajId();
}
