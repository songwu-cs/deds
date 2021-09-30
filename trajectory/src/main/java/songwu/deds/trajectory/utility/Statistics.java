package songwu.deds.trajectory.utility;

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

    public static void main(String[] args) {
        System.out.println(min(1,2,0));
    }
}
