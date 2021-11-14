package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class DenoiseTimeStampedPointT_Song {
    private int counter = 0;
    private int number_threads = 1;
    private int gap;
    private double speed_max;
    private List<TimeStampedPointT> trajs = new ArrayList<>();
    private List<TimeStampedPointT> answer = new ArrayList<>();
    private PriorityQueue<NoiseProportion> queue = new PriorityQueue<>(Comparator.comparingDouble(NoiseProportion::getRatio));

    public class NoiseProportion{
        private String id;
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

        public NoiseProportion id(String id){
            this.id = id;
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

        public String getId() {
            return id;
        }
    }

    public DenoiseTimeStampedPointT_Song setSpeedMax(double speed_max) {
        this.speed_max = speed_max; return this;
    }

    public DenoiseTimeStampedPointT_Song setGap(int gap){
        this.gap = gap; return this;
    }

    public DenoiseTimeStampedPointT_Song setNumberThreads(int number_threads){
        this.number_threads = number_threads; return this;
    }

    public DenoiseTimeStampedPointT_Song setTrajs(List<TimeStampedPointT> trajs) {
        this.trajs = trajs; return this;
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
            denoised.addPoint(query.getUnit(2));
            number_of_good = 3;

            TimeStampedPoint p0 = denoised.getUnit(0),
                    p1 = denoised.getUnit(1),
                    p2 = denoised.getUnit(2);
            p1.euc_speed(p0).geography_angle(p0);
            p2.euc_speed(p1).geography_angle(p1).signed_turn(p1);
            p0.copy(p1);

            for(int k = 3; k < len; k++){
                TimeStampedPoint p_ = query.getUnit(k),
                                _p = denoised.getUnit(number_of_good - 1),
                                __p = denoised.getUnit(number_of_good - 2);

                p_.euc_speed(_p).geography_angle(_p).signed_turn(_p);

                //over-high speed
                if(p_.getEucSpeed() > speed_max)
                    continue;

                if(p_.getTimeGap() >= gap){
                    denoised.addPoint(p_);
                    number_of_good++;
                }else if((int)(p_.getTimeGap()) == 1){
                    double c = p_.getEucSpeed();
                    double c_angle = p_.getSignedTurn(); // _p --> p_
                    p_.euc_speed(__p).geography_angle(__p).signed_turn(__p);
                    double a = __p.getEucSpeed(),
                            b = _p.getEucSpeed(), // __p --> _p
                            b_angle = _p.getSignedTurn(), // __p --> _p
                            d = p_.getEucSpeed(),
                            d_angle = p_.getSignedTurn(); // __p --> p_
                    if((Math.abs(d - a) <= Math.min(Math.abs(b - a), Math.abs(c - a))) ||
                            (Math.abs(d_angle) < 90 && Math.abs(b_angle) > 90 && Math.abs(c_angle) > 90)){
                        denoised.getAllUnits().remove(number_of_good - 1);
                        denoised.addPoint(p_);
                    }
                }else {
                    if(k == len - 1){
                        denoised.addPoint(p_);
                        number_of_good++;
                    }else {
                        double a = _p.getEucSpeed();
                        double b = p_.getEucSpeed();
                        double b_angle = p_.getSignedTurn(); // _p --> p_
                        TimeStampedPoint p__ = query.getUnit(k + 1);
                        p__.euc_speed(p_).geography_angle(p_).signed_turn(p_);
                        double c = p__.getEucSpeed();
                        double c_angle = p__.getSignedTurn(); // p_ --> p__
                        p__.euc_speed(_p).geography_angle(_p).signed_turn(_p);
                        double d = p__.getEucSpeed();
                        double d_angle = p__.getSignedTurn(); // _p --> p__
                        boolean flag1 = (Math.abs(d - a) <= Math.min(Math.abs(b - a), Math.abs(c - a)));
                        boolean flag2 = (b > a) && (c > a) && (b > c) && (c < d) && (d < b);
                        boolean flag3 = (b < a) && (c < a) && (c > b) && (b < d) && (d < c);
                        if((flag1 || flag2 || flag3) ||
                                (Math.abs(d_angle) < 90 && Math.abs(b_angle) > 90 && Math.abs(c_angle) > 90)){
                            p__.euc_speed(_p).geography_angle(_p).signed_turn(_p);
                            denoised.addPoint(p__);
                            number_of_good++;
                            k++;
                        }else {
                            p_.euc_speed(_p).geography_angle(_p).signed_turn(_p);
                            denoised.addPoint(p_);
                            number_of_good++;
                        }
                    }
                }
            }

            synchronized (this){
                answer.add(denoised);
                queue.add(new NoiseProportion().id(query.trajId())
                                .mmsi(query.trajId().split("-")[0])
                        .previous(query.size()).now(denoised.size()).ratio()
                        .previousDuration(TimeStampedPoint.duration(query.getUnit(0), query.getUnit(query.size() - 1)))
                        .nowDuration(TimeStampedPoint.duration(denoised.getUnit(0), denoised.getUnit(denoised.size() - 1)))
                        .ratioDuration());
            }

//            System.out.println(query.trajId() + " : " + query.size() + " --> " + denoised.size());
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
