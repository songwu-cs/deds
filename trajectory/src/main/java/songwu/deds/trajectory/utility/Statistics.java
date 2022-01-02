package songwu.deds.trajectory.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Statistics {
    public static double min(double... doubles){
        double answer = Double.MAX_VALUE;
        for(double d : doubles){
            if(d < answer)
                answer = d;
        }
        return answer;
    }

    public static double max(double... doubles){
        double answer = Double.MIN_VALUE;
        for(double d : doubles){
            if(d > answer)
                answer = d;
        }
        return answer;
    }

    public static double entropy(double[] probs){
        double entropy = 0;
        for(double prob : probs){
            if(prob != 0)
                entropy += (-1 * prob * (Math.log(prob) / Math.log(2)));
        }
        return entropy;
    }

    public static List<int[]> select(boolean[] booleans){
        List<int[]> tureIntervals = new ArrayList<>();
        boolean has = false;
        int start = 0;
        int end = 0;
        for(int i = 0; i < booleans.length; i++){
            if(booleans[i]){
                if(has)
                    end = i;
                else {
                    start = i;
                    end = i;
                    has = true;
                }
            }else{
                if(has){
                    has = false;
                    tureIntervals.add(new int[]{start, end});
                }
            }
        }
        if(has){
            tureIntervals.add(new int[]{start, end});
        }
        return tureIntervals;
    }

    public static void main(String[] args) {
//        System.out.println(min(1,2,0));

        for(int[] ints : select(new boolean[]{true,true,false,false,true,false,true})){
            System.out.println(Arrays.toString(ints));
        }
    }
}
