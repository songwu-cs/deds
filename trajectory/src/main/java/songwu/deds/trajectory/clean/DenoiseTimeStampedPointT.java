package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class DenoiseTimeStampedPointT {
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
    private PriorityQueue<NoiseProportion> queue = new PriorityQueue<>(Comparator.comparingDouble(NoiseProportion::getRatio));

    public class NoiseProportion{
        private String mmsi;
        private int previous;
        private int now;
        private double ratio;
        private double previous_duration;
        private double now_duration;
        private double ratio_duration;

        public NoiseProportion mmsi(String mmsi){
            this.mmsi = mmsi;
            return this;
        }

        public NoiseProportion previous(int previous){
            this.previous = previous;
            return this;
        }

        public NoiseProportion now(int now){
            this.now = now;
            return this;
        }

        public NoiseProportion ratio(){
            this.ratio = 1.0 * now / previous;
            return this;
        }

        public NoiseProportion previousDuration(double previous_duration) {
            this.previous_duration = previous_duration; return this;
        }

        public NoiseProportion nowDuration(double now_duration) {
            this.now_duration = now_duration; return this;
        }

        public NoiseProportion ratioDuration() {
            this.ratio_duration = now_duration / previous_duration; return this;
        }

        public double getRatio() {
            return ratio;
        }

        public int getPrevious() {
            return previous;
        }

        public int getNow() {
            return now;
        }

        public String getMmsi() {
            return mmsi;
        }

        public double getPreviousDuration() {
            return previous_duration;
        }

        public double getRatioDuration() {
            return ratio_duration;
        }

        public double getNowDuration() {
            return now_duration;
        }
    }

    public DenoiseTimeStampedPointT setHistory(int history) {
        this.history = history; return this;
    }

    public DenoiseTimeStampedPointT setSpeedMax(double speed_max) {
        this.speed_max = speed_max; return this;
    }

    public DenoiseTimeStampedPointT setSpeenMin(double speen_min) {
        this.speen_min = speen_min; return this;
    }

    public DenoiseTimeStampedPointT setAngleThreshold(double angle_threshold) {
        this.angle_threshold = angle_threshold; return this;
    }

    public DenoiseTimeStampedPointT setNumberThreads(int number_threads){
        this.number_threads = number_threads; return this;
    }

    public DenoiseTimeStampedPointT setTrajs(List<TimeStampedPointT> trajs) {
        this.trajs = trajs; return this;
    }

    public DenoiseTimeStampedPointT setTurnThreshold(double turn_threshold) {
        this.turn_threshold = turn_threshold; return this;
    }

    public DenoiseTimeStampedPointT setSpeedAlpha(double speed_alpha){
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

                if(p.getEucSpeed() > speed_max){
                    continue;
                }
                if(p.getEucSpeed() > speen_min){
                    double bearing_mean = TimeStampedPoint.geography_angle(possible_drop, last);
                    double angle_diff = Math.abs(p.getBearing() - bearing_mean);
                    angle_diff = angle_diff < 180 ? angle_diff : 360 - angle_diff;
                    if(angle_diff > angle_threshold)
                        continue;
                }

                if(p.getEucSpeed() < speen_min)
                    p.pause(true);
                if(Math.abs(p.getEucSpeed() - last.getEucSpeed()) / p.getEucSpeed() > speed_alpha)
                    p.speedChange(true);
                if(Math.abs(p.getSignedTurn()) > turn_threshold)
                    p.turn(true);
                denoised.addPoint(p);
                number_of_good++;
            }

            synchronized (this){
                answer.add(denoised);
                queue.add(new NoiseProportion().mmsi(query.trajId())
                        .previous(query.size()).now(denoised.size()).ratio()
                        .previousDuration(TimeStampedPoint.duration(query.getUnit(0), query.getUnit(query.size() - 1)))
                        .nowDuration(TimeStampedPoint.duration(denoised.getUnit(0), denoised.getUnit(denoised.size() - 1)))
                        .ratioDuration());
            }

            System.out.println(query.trajId() + " : " + query.size() + " --> " + denoised.size());
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

    public PriorityQueue<NoiseProportion> getNoiseRatio(){
        return queue;
    }
}
