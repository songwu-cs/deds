package songwu.deds.trajectory.data;

public interface TrajectoryUnit <T extends TrajectoryUnit<T>>{
    double distance(T t);
}
