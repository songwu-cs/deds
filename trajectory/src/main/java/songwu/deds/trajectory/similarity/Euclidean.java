package songwu.deds.trajectory.similarity;

public interface Euclidean{
    static String IDENTIFIER = "euc";

    static double distance(Euclidean t1, Euclidean t2){
        double[] vec1 = t1.vectorizedEuclidean();
        double[] vec2 = t2.vectorizedEuclidean();

        int length = vec1.length;
        double total = 0;
        for(int i = 0; i < length; i++){
            total += Math.pow(vec1[i] - vec2[i], 2);
        }
        return Math.sqrt(total);
    };

    double[] vectorizedEuclidean();
}
