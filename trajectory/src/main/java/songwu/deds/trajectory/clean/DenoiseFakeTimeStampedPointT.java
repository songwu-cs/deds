package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;
import songwu.deds.trajectory.data.UniversalRatio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class DenoiseFakeTimeStampedPointT {
    private int counter = 0;
    private int number_threads = 1;

    private int history;
    private double speed_max;
    private double speen_min;
    private double angle_threshold;
    private double turn_threshold;
    private double speed_alpha;
    private List<TimeStampedPointT> trajs = new ArrayList<>();
    private List<TimeStampedPointT> answer = new ArrayList<>();
    private PriorityQueue<UniversalRatio> queue = new PriorityQueue<>(Comparator.comparingDouble(UniversalRatio::getRatio));

    public DenoiseFakeTimeStampedPointT setHistory(int history) {
        this.history = history; return this;
    }

    public DenoiseFakeTimeStampedPointT setSpeedMax(double speed_max) {
        this.speed_max = speed_max; return this;
    }

    public DenoiseFakeTimeStampedPointT setSpeenMin(double speen_min) {
        this.speen_min = speen_min; return this;
    }

    public DenoiseFakeTimeStampedPointT setAngleThreshold(double angle_threshold) {
        this.angle_threshold = angle_threshold; return this;
    }

    public DenoiseFakeTimeStampedPointT setNumberThreads(int number_threads){
        this.number_threads = number_threads; return this;
    }

    public DenoiseFakeTimeStampedPointT setTrajs(List<TimeStampedPointT> trajs) {
        this.trajs = trajs; return this;
    }

    public DenoiseFakeTimeStampedPointT setTurnThreshold(double turn_threshold) {
        this.turn_threshold = turn_threshold; return this;
    }

    public DenoiseFakeTimeStampedPointT setSpeedAlpha(double speed_alpha){
        this.speed_alpha = speed_alpha; return this;
    }

    private void worker(){
        TimeStampedPointT query;
        while (true){
            synchronized(this){
                if(counter < trajs.size()){
                    query = trajs.get(counter); counter++;
                }else {
                    break;
                }
            }

            TimeStampedPointT denoised = new TimeStampedPointT(query.trajId());
            int len = query.size();
            int number_of_good;

            denoised.addPoint(query.getUnit(0));
            denoised.addPoint(query.getUnit(1));
            number_of_good = 2;

            TimeStampedPoint p0 = denoised.getUnit(0), p1 = denoised.getUnit(1);
            p0.copy(p1.euc_speed(p0).geography_angle(p0));

            for(int k = 2; k <= history; k++){
                TimeStampedPoint p = query.getUnit(k), last = denoised.getUnit(number_of_good - 1);
                p.euc_speed(last).geography_angle(last).signed_turn(last);

                denoised.addPoint(p);
                number_of_good++;
            }

            for(int k = history + 1; k < len; k++){
                TimeStampedPoint p = query.getUnit(k),
                        last = denoised.getUnit(number_of_good - 1),
                        possible_drop = denoised.getUnit(number_of_good - history);
                p.euc_speed(last).geography_angle(last).signed_turn(last);

                if(p.getEucSpeed() < speen_min)
                    p.setPaused(true);
                if(Math.abs(p.getEucSpeed() - last.getEucSpeed()) / last.getEucSpeed() > speed_alpha)
                    p.setSpeedchanged(true);
                if(Math.abs(p.getSignedTurn()) > turn_threshold)
                    p.setTurned(true);
                denoised.addPoint(p);
                number_of_good++;
            }

            synchronized (this){
                answer.add(denoised);
                queue.add(new UniversalRatio().id(query.trajId())
                        .previous(query.size()).now(denoised.size()).ratio());
            }
        }
    }

    public List<TimeStampedPointT> go() throws InterruptedException {
        answer.clear();
        queue.clear();
        counter = 0;
        List<Thread> workers = new ArrayList<>();
        for(int i = 0; i < number_threads; i++){
            workers.add(new Thread(this::worker, "worker#" + (i + 1)));
        }
        for(Thread worker : workers){
            worker.start();
        }
        for(Thread worker : workers){
            worker.join();
        }
        return answer;
    }

    public PriorityQueue<UniversalRatio> getNoiseRatio(){
        return queue;
    }
}
