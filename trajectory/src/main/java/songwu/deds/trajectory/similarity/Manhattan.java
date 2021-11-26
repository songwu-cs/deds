package songwu.deds.trajectory.similarity;

public interface Manhattan{
    static String IDENTIFIER = "manhattan";

    static double distance(Manhattan t1, Manhattan t2){
        double[] vec1 = t1.vectorizedManhattan();
        double[] vec2 = t2.vectorizedManhattan();

        int length = vec1.length;
        double total = 0;
        for(int i = 0; i < length; i++){
            total += Math.abs(vec1[i] - vec2[i]);
        }
        return total;
    };

    double[] vectorizedManhattan();
}
