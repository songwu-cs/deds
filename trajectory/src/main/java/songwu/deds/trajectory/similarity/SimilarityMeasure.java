package songwu.deds.trajectory.similarity;

import songwu.deds.trajectory.data.Trajectory;
import songwu.deds.trajectory.data.TrajectoryUnit;

public interface SimilarityMeasure<U extends TrajectoryUnit<U>, T extends Trajectory<U>> {
    double apply(T t1, T t2);
    String name();
}
