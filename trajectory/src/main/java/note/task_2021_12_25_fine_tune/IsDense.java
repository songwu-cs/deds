package note.task_2021_12_25_fine_tune;

import calculation.Array1DBoolean;
import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;
import songwu.deds.trajectory.similarity.Euclidean;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class IsDense {
    public final int toleranceMax;
    public final int minpts;
    public final int radius;
    public final int timeLimit;


    public IsDense(int toleranceMax, int minpts, int radius, int timeLimit) {
        this.toleranceMax = toleranceMax;
        this.minpts = minpts;
        this.radius = radius;
        this.timeLimit = timeLimit;
    }

    //噪声点的时间间隔也考虑在内
    public boolean[] label(TimeStampedPointT traj){
        int i = 0;
        boolean[] answer = new boolean[traj.size()];
        while (i < traj.size()){
            int j = i;
            int tolerance = 0;
            TimeStampedPoint left = traj.getUnit(i);
            HashSet<Integer> far = new HashSet<>();
            while (j + 1 < traj.size()){
                TimeStampedPoint right = traj.getUnit(j + 1);
                if(Euclidean.distance(left, right) > radius){
                    tolerance++;
                    far.add(j + 1);
                    if (tolerance > toleranceMax){
                        j -= toleranceMax;
                        tolerance = 0;
                        break;
                    }
                    j++;
                }else {
                    tolerance = 0;
                    j++;
                }
            }
            j -= tolerance;

            boolean bigTimeGap = false;
            for(int _ = i + 1; _ <= j; _++){
                if(traj.getUnit(_).getTimeGap() > timeLimit){
                    bigTimeGap = true;
                    break;
                }
            }
            if(bigTimeGap || (j - i + 1) > minpts){
                for(int _ = i; _ <= j; _++){
                    answer[_] = ! far.contains(_);
                }
                i = j;
            }else {
                i++;
            }
        }

        return answer;
    }

    public static void main(String[] args) {
//        System.out.println(Euclidean.distance(()->new double[]{0,0}, ()->new double[]{3,4}));
//
//        System.out.println(Array1DBoolean.firstTrueIndices(new boolean[]{true,true,false,false,true,true}));
//        System.out.println(Array1DBoolean.lastTrueIndices(new boolean[]{true,true,false,false,true,true}));

//        IsDense isDense = new IsDense(2,0,0,0);
//        System.out.println(Arrays.toString(isDense.polish(new boolean[]{true,false,false,false,true,false,true})));

//        System.out.println(Arrays.toString(Array1DBoolean.false2trueWithTolerance(
//                new boolean[]{false,true,false,true,false,false,true},1
//        )));

        System.out.println(Arrays.toString(new boolean[10]));
    }

}
