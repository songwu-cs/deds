package songwu.deds.trajectory.similarity;

public interface Manhattan <T extends Manhattan<T>>{
    double manhattanDistance(T t);
}
