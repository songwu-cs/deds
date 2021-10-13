package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.algo.SmallestEnclosingCircle;
import songwu.deds.trajectory.data.CriticalPoint;
import songwu.deds.trajectory.data.CriticalPointT;
import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;

import java.util.*;
import java.util.List;

public class CriticalTimeStampedPointT {
    private int gap;
    private int history;
    private double radius;
    private double smooth_threshold;
    private double speed_alpha;

    private int counter = 0;
    private int number_threads = 1;

    private List<TimeStampedPointT> trajs = new ArrayList<>();
    private List<CriticalPointT> answer = new ArrayList<>();
    private PriorityQueue<CriticalRatio> queue = new PriorityQueue<>(Comparator.comparingDouble(CriticalRatio::getRatio));

    public class CriticalRatio{
        private String mmsi;
        private int previous;
        private int now;
        private double ratio;

        public CriticalRatio mmsi(String mmsi){
            this.mmsi = mmsi;
            return this;
        }

        public CriticalRatio previous(int previous){
            this.previous = previous;
            return this;
        }

        public CriticalRatio now(int now){
            this.now = now;
            return this;
        }

        public CriticalRatio ratio(){
            this.ratio = 1.0 * now / previous;
            return this;
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
    }

    public CriticalTimeStampedPointT setGap(int gap) {
        this.gap = gap; return this;
    }

    public CriticalTimeStampedPointT setHistory(int history) {
        this.history = history; return this;
    }

    public CriticalTimeStampedPointT setRadius(int radius) {
        this.radius = radius; return this;
    }

    public CriticalTimeStampedPointT setSmoothThreshold(double smooth_threshold) {
        this.smooth_threshold = smooth_threshold; return this;
    }

    public CriticalTimeStampedPointT setSpeedAlpha(double speed_alpha) {
        this.speed_alpha = speed_alpha; return this;
    }

    public CriticalTimeStampedPointT setNumberThreads(int number_threads) {
        this.number_threads = number_threads; return this;
    }

    public CriticalTimeStampedPointT setTrajs(List<TimeStampedPointT> trajs) {
        this.trajs = trajs; return this;
    }

    public void worker(){
        TimeStampedPointT query;
        while (true){
            synchronized(this){
                if(counter < trajs.size()){
                    query = trajs.get(counter); counter++;
                }else {
                    break;
                }
            }

            CriticalPointT critical = new CriticalPointT(query.trajId());
            Set<String> already = new HashSet<>();
            int len = query.size();

            for(int k = history + 1; k < len; k++){
                TimeStampedPoint current = query.getUnit(k);
                TimeStampedPoint previous = query.getUnit(k - 1);
                double total_turn = 0;
                int positive_turn = 0, negative_turn = 0, paused = 0;
                for(int q = k - history; q <= k; q++){
                    double angle = query.getUnit(q).getSignedTurn();
                    total_turn += angle;
                    if(angle > 0)
                        positive_turn += 1;
                    else
                        negative_turn += 1;
                    if(query.getUnit(q).isPause())
                        paused += 1;
                }

                if(current.getTimeGap() > gap){
                    critical.addPoint(new CriticalPoint().setType("gapStart").copy(previous));
                    critical.addPoint(new CriticalPoint().setType("gapEnd").copy(current));
                    continue;
                }

                if((! current.isPause()) && previous.isPause()){
                    int left = k - 1;
                    for(; left >= 0; left--){
                        if(! query.getUnit(left).isPause())
                            break;
                    }
                    left++;

                    if(k - left >= history && TimeStampedPoint.duration(query.getUnit(left), previous) > gap / 3600.0){
                        List<SmallestEnclosingCircle.Point> points = new ArrayList<>();
                        for(int pos = left; pos <= k - 1; pos++){
                            TimeStampedPoint _pos = query.getUnit(pos);
                            points.add(new SmallestEnclosingCircle.Point(_pos.getX(), _pos.getY()));
                        }
                        SmallestEnclosingCircle.Circle circle = SmallestEnclosingCircle.makeCircle(points);
                        if(circle.r <= radius){
                            critical.addPoint(new CriticalPoint().setType("long_stop")
                                    .setLongitude(query.avgLongitude(k-1, k - left))
                                    .setLatitude(query.avgLaitude(k-1, k - left))
                                    .setX(query.avgX(k-1, k - left))
                                    .setY(query.avgY(k - 1, k - left))
                                    .setTotalDuration(TimeStampedPoint.duration(query.getUnit(left), query.getUnit(k-1)))
                                    .setTimestamp(TimeStampedPoint.mid_timestamp(query.getUnit(left), query.getUnit(k - 1)))
                                    .setOrder((query.getUnit(left).getTimestampLong() + query.getUnit(k-1).getTimestampLong()) / 2));
                        }else {
                            critical.addPoint(new CriticalPoint().setType("low_motion_start")
                                    .copy(query.getUnit(left)));
                            critical.addPoint(new CriticalPoint().setType("low_motion_end")
                                    .copy(current));
                        }
                        continue;
                    }
                }

                if(Math.abs(total_turn) > smooth_threshold &&
                        Math.min(negative_turn, positive_turn) <= 1 &&
                        paused <= 1){
                    boolean good = true;
                    for(int q = k - history; q <= k; q++){
                        if(Math.abs(total_turn) < Math.abs(query.getUnit(q).getSignedTurn())){
                            good = false;
                            break;
                        }
                    }
                    if(good){
                        for(int q = k - history; q <= k; q++){
                            TimeStampedPoint _q = query.getUnit(q);
                            if(! already.contains(_q.getTimestamp() + "smooth_turn")){
                                critical.addPoint(new CriticalPoint().setType("smooth_turn").copy(_q));
                                already.add(_q.getTimestamp() + "smooth_turn");
                            }
                        }
                        continue;
                    }
                }

                if(current.isTurn() && (!current.isPause())){
                    double bearing_mean = TimeStampedPoint.geography_angle(query.getUnit(k - history), query.getUnit(k - 1));
                    double angle_diff = Math.abs(current.getBearing() - bearing_mean);
                    angle_diff = angle_diff < 180 ? angle_diff : 360 - angle_diff;
                    if(angle_diff > smooth_threshold){
                        critical.addPoint(new CriticalPoint().setType("smooth_turn").copy(current));
                        already.add(current.getTimestamp() + "smooth_turn");
                        continue;
                    }
                }

                if(current.isSpeedChange() && (!current.isPause())){
                    double avg_speed = query.avgSpeed(k - 1, history);
                    if(Math.abs(current.getEucSpeed() - avg_speed) / current.getEucSpeed() > speed_alpha){
                        critical.addPoint(new CriticalPoint().setType("speedChange").copy(current));
                    }
                }
            }

            critical.sort();
            synchronized (this){
                answer.add(critical);
                queue.add(new CriticalRatio().mmsi(query.trajId()).previous(query.size()).now(critical.size()).ratio());
            }
            System.out.println(query.trajId() + " : " + query.size() + " --> " + critical.size());
        }
    }

    public List<CriticalPointT> go() throws InterruptedException {
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

    public PriorityQueue<CriticalRatio> getCriticalRatio() {
        return queue;
    }
}
