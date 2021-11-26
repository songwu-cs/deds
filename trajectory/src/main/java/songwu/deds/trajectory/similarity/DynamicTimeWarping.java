package songwu.deds.trajectory.similarity;

import songwu.deds.trajectory.data.Trajectory;
import songwu.deds.trajectory.data.TrajectoryUnit;
import songwu.deds.trajectory.utility.Statistics;

public class DynamicTimeWarping<U extends TrajectoryUnit<U>, T extends Trajectory<U>> implements SimilarityMeasure<U, T>{
    private String innerDistanceName;

    public DynamicTimeWarping(String innerDistanceName) {
        this.innerDistanceName = innerDistanceName;
    }

    @Override
    public String similarityName() {
        return "dtw";
    }

    @Override
    public String innerDistanceName() {
        return innerDistanceName;
    }


    @Override
    public double apply(T t1, T t2){
        if(t1 == t2)
            return 0;

        T t;
        int len1 = t1.size();
        int len2 = t2.size();
        if(len1 > len2){
            t = t1; t1 = t2; t2 = t;
            len1 ^= len2; len2 ^= len1; len1 ^= len2;
        }

        if(len1 == 0){
            return 0;
        }

        double answer = 0;
        double cell_previous;
        double cell_now;
        U u;
        if(len1 == 1){
            u = t1.getUnit(0);
            for(int pos = 0; pos < len2; pos++){
                answer += innerDistance(u, t2.getUnit(pos));
            }
            return answer;
        }else {
            double[] row = new double[len1];
            u = t2.getUnit(0);
            for(int pos = 0; pos < len1; pos++){
                row[pos] = innerDistance(u, t1.getUnit(pos));
            }
            for(int pos = 1; pos < len1; pos++){
                row[pos] += row[pos-1];
            }

            for(int outer = 1; outer < len2; outer++){
                u = t2.getUnit(outer);
                cell_previous = innerDistance(u, t1.getUnit(0)) + row[0];

                for(int inner = 1; inner < len1; inner++){
                    cell_now = innerDistance(u, t1.getUnit(inner)) + Statistics.min(cell_previous, row[inner], row[inner-1]);
                    row[inner-1] = cell_previous;
                    cell_previous = cell_now;
                }
                row[len1-1] = cell_previous;
            }

            return row[len1-1];
        }
    }



    public static void main(String[] args) {
    }
}
