package songwu.deds.trajectory.similarity;

import songwu.deds.trajectory.data.Trajectory;
import songwu.deds.trajectory.data.TrajectoryUnit;

public interface SimilarityMeasure<U extends TrajectoryUnit, T extends Trajectory<U>> {
    double apply(T t1, T t2);
    String similarityName();
    String innerDistanceName();

    default double innerDistance(U u1, U u2){
        switch (innerDistanceName()){
            case Euclidean.IDENTIFIER:
                return Euclidean.distance((Euclidean)u1, (Euclidean)u2);
            case Manhattan.IDENTIFIER:
                return Manhattan.distance((Manhattan)u1, (Manhattan)u2);
        }
        return 0;
    }
}
