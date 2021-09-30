package songwu.deds.trajectory.similarity;

public interface Euclidean <T extends Euclidean<T>>{
    double eucDistance(T t);
}
