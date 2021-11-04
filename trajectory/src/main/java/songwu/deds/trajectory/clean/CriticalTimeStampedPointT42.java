package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.algo.SmallestEnclosingCircle;
import songwu.deds.trajectory.data.*;

import java.util.*;

public class CriticalTimeStampedPointT42 {
    private int gap;
    private int history;
    private double smooth_threshold;
    private double speed_alpha;
    private double speed_min;
    private double speed_slow_motion;

    private int counter = 0;
    private int number_threads = 1;

    private List<TimeStampedPointT> trajs = new ArrayList<>();
    private List<CriticalPointT> answer = new ArrayList<>();
    private PriorityQueue<CriticalRatio> queue = new PriorityQueue<>(Comparator.comparingDouble(CriticalRatio::getRatio));
    private List<CriticalPointInterval> criticalIntervals = new ArrayList<>();

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

    public CriticalTimeStampedPointT42 setGap(int gap) {
        this.gap = gap; return this;
    }

    public CriticalTimeStampedPointT42 setSpeed_min(double speed_min) {
        this.speed_min = speed_min; return this;
    }

    public CriticalTimeStampedPointT42 setSpeed_slow_motion(double speed_slow_motion) {
        this.speed_slow_motion = speed_slow_motion; return this;
    }

    public CriticalTimeStampedPointT42 setHistory(int history) {
        this.history = history; return this;
    }

    public CriticalTimeStampedPointT42 setSmoothThreshold(double smooth_threshold) {
        this.smooth_threshold = smooth_threshold; return this;
    }

    public CriticalTimeStampedPointT42 setSpeedAlpha(double speed_alpha) {
        this.speed_alpha = speed_alpha; return this;
    }

    public CriticalTimeStampedPointT42 setNumberThreads(int number_threads) {
        this.number_threads = number_threads; return this;
    }

    public CriticalTimeStampedPointT42 setTrajs(List<TimeStampedPointT> trajs) {
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
            int speedup_flag = 0;
            boolean stop_flag = false;
            boolean slow_motion_flag = false;
            boolean signal_loss_flag = false;
            int signal_loss_end = 0;

            critical.addPoint(new CriticalPoint().setType("tripStart").copy(query.getUnit(0)));
            critical.addPoint(new CriticalPoint().setType("tripEnd").copy(query.getUnit(query.size() - 1)));

            for(int k = history + 1; k < len; k++){
                TimeStampedPoint current = query.getUnit(k);
                TimeStampedPoint previous = query.getUnit(k - 1);

                //signal loss
                if(current.getTimeGap() > gap){
                    critical.addPoint(new CriticalPoint().setType("gapStart").copy(previous));
                    critical.addPoint(new CriticalPoint().setType("gapEnd").copy(current));

                    signal_loss_flag = true;
                    signal_loss_end = k;

                    //reset speed status
                    if(speedup_flag < 0){
                        critical.addPoint(new CriticalPoint().setType("speedDownEnd").copy(previous));
                        speedup_flag = 0;
                    }else if(speedup_flag > 0){
                        critical.addPoint(new CriticalPoint().setType("speedUpEnd").copy(previous));
                        speedup_flag = 0;
                    }
                    //reset stop and slow-motion status
                    if(slow_motion_flag){
                        critical.addPoint(new CriticalPoint().setType("slowMotionEnd").copy(query.getUnit(k - 1)));
                        slow_motion_flag = false;
                    }
                    if(stop_flag){
                        critical.addPoint(new CriticalPoint().setType("stopEnd").copy(query.getUnit(k - 1)));
                        stop_flag = false;
                    }

                    continue;
                }else{
                    if(signal_loss_flag && (k - signal_loss_end < history + 2))
                        continue;
                    else {
                        signal_loss_end = Integer.MIN_VALUE;
                        signal_loss_flag = false;
                    }
                }

                double total_turn = 0;
                int positive_turn = 0, negative_turn = 0, paused = 0;
                for(int q = k - history; q <= k; q++){
                    double angle = query.getUnit(q).getSignedTurn();
                    total_turn += angle;
                    if(angle > 0)
                        positive_turn += 1;
                    else
                        negative_turn += 1;
                    if(query.getUnit(q).getEucSpeed() <= speed_min)
                        paused += 1;
                }

                //stop
                if(current.getEucSpeed() <= speed_min && (!stop_flag)){
                    critical.addPoint(new CriticalPoint().setType("stopStart").copy(current));
                    stop_flag = true;
                    //reset speed status
                    if(speedup_flag < 0){
                        critical.addPoint(new CriticalPoint().setType("speedDownEnd").copy(current));
                        speedup_flag = 0;
                    }else if(speedup_flag > 0){
                        critical.addPoint(new CriticalPoint().setType("speedUpEnd").copy(current));
                        speedup_flag = 0;
                    }
                }else if (current.getEucSpeed() > speed_min && stop_flag){
                    critical.addPoint(new CriticalPoint().setType("stopEnd").copy(current));
                    stop_flag = false;
                }

                //slow-motion
                double _s = current.getEucSpeed();
                double __s = query.avgSpeed(k-1, history);
                if(_s > speed_min && _s <= speed_slow_motion && (!slow_motion_flag)){
                    critical.addPoint(new CriticalPoint().setType("slowMotionStart").copy(current));
                    slow_motion_flag = true;
                }else if((_s <= speed_min || _s > speed_slow_motion) && slow_motion_flag){
                    critical.addPoint(new CriticalPoint().setType("slowMotionEnd").copy(current));
                    slow_motion_flag = false;
                }

                //speed-change
                if(_s > speed_min && __s > speed_min){
                    double variation = (_s - __s) / __s;
                    if(variation > speed_alpha){
                        if(speedup_flag == 0){
                            critical.addPoint(new CriticalPoint().setType("speedUpStart").copy(current));
                            speedup_flag = 1;
                        }else if (speedup_flag < 0){
                            critical.addPoint(new CriticalPoint().setType("speedDownEnd").copy(current));
                            critical.addPoint(new CriticalPoint().setType("speedUpStart").copy(current));
                            speedup_flag = 1;
                        }
                    }else if(variation <= -1 * speed_alpha){
                        if(speedup_flag == 0){
                            critical.addPoint(new CriticalPoint().setType("speedDownStart").copy(current));
                            speedup_flag = -1;
                        }else if (speedup_flag > 0){
                            critical.addPoint(new CriticalPoint().setType("speedUpEnd").copy(current));
                            critical.addPoint(new CriticalPoint().setType("speedDownStart").copy(current));
                            speedup_flag = -1;
                        }
                    }else {
                        if(speedup_flag < 0){
                            critical.addPoint(new CriticalPoint().setType("speedDownEnd").copy(current));
                            speedup_flag = 0;
                        }else if (speedup_flag > 0){
                            critical.addPoint(new CriticalPoint().setType("speedUpEnd").copy(current));
                            speedup_flag = 0;
                        }
                    }
                }

                //heading
                if(Math.abs(total_turn) > smooth_threshold &&
                        Math.min(negative_turn, positive_turn) <= 1 &&
                        paused <= 2){
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
                    }
                }

                if(current.getEucSpeed() > speed_min){
                    double bearing_mean = TimeStampedPoint.geography_angle(query.getUnit(k - history), query.getUnit(k - 1));
                    double angle_diff = Math.abs(current.getBearing() - bearing_mean);
                    angle_diff = angle_diff < 180 ? angle_diff : 360 - angle_diff;
                    if(angle_diff > smooth_threshold && (! already.contains(current.getTimestamp() + "smooth_turn"))){
                        critical.addPoint(new CriticalPoint().setType("smooth_turn").copy(previous));
                        already.add(previous.getTimestamp() + "smooth_turn");
                    }
                }
            }

            if(slow_motion_flag)
                critical.addPoint(new CriticalPoint().setType("slowMotionEnd").copy(query.getUnit(query.size() - 1)));
            if(stop_flag)
                critical.addPoint(new CriticalPoint().setType("stopEnd").copy(query.getUnit(query.size() - 1)));

            critical.sort();
            synchronized (this){
                answer.add(critical);
                queue.add(new CriticalRatio().mmsi(query.trajId()).previous(query.size()).now(critical.size()).ratio());
                criticalIntervals.addAll(point2interval(critical));
            }
//            System.out.println(query.trajId() + " : " + query.size() + " --> " + critical.size());
        }
    }

    private List<CriticalPointInterval> point2interval(CriticalPointT ct){
        HashMap<String, List<CriticalPoint>> buffer = new HashMap<>();
        buffer.put("stopStart", new ArrayList<>());
        buffer.put("stopEnd", new ArrayList<>());
        buffer.put("speedUpStart", new ArrayList<>());
        buffer.put("speedUpEnd", new ArrayList<>());
        buffer.put("speedDownStart", new ArrayList<>());
        buffer.put("speedDownEnd", new ArrayList<>());
        buffer.put("gapStart", new ArrayList<>());
        buffer.put("gapEnd", new ArrayList<>());
        buffer.put("slowMotionStart", new ArrayList<>());
        buffer.put("slowMotionEnd", new ArrayList<>());
        List<CriticalPointInterval> smoothTurn = new ArrayList<>();
        String tripStart = "", tripEnd = "";

        for (CriticalPoint cp : ct.getAllUnits()){
            if(cp.getType().equals("smooth_turn")){
                smoothTurn.add(new CriticalPointInterval().setType("smooth_turn")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(cp.getTimestamp())
                        .setId(smoothTurn.size() + "")
                        .setMid(ct.trajId())
                );
            }
            else if(cp.getType().equals("tripStart"))
                tripStart = cp.getTimestamp();
            else if(cp.getType().equals("tripEnd"))
                tripEnd = cp.getTimestamp();
            else
                buffer.get(cp.getType()).add(cp);
        }
        List<CriticalPoint> stopS = buffer.get("stopStart");
        List<CriticalPoint> stopE = buffer.get("stopEnd");
        List<CriticalPoint> slowS = buffer.get("slowMotionStart");
        List<CriticalPoint> slowE = buffer.get("slowMotionEnd");
        List<CriticalPoint> gapS = buffer.get("gapStart");
        List<CriticalPoint> gapE = buffer.get("gapEnd");
        List<CriticalPoint> sUS = buffer.get("speedUpStart");
        List<CriticalPoint> sUE = buffer.get("speedUpEnd");
        List<CriticalPoint> sDS = buffer.get("speedDownStart");
        List<CriticalPoint> sDE = buffer.get("speedDownEnd");

        List<CriticalPointInterval> intervals = new ArrayList<>();
        int counterInterval = 0;
        for(CriticalPoint cp : stopS){
            if(stopE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("stop")
                        .setId(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(stopE.get(counterInterval).getTimestamp())
                        .setMid(ct.trajId()));
                counterInterval++;
            }else {
                break;
            }
        }
        counterInterval = 0;
        for(CriticalPoint cp : slowS){
            if(slowE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("slowMotion")
                        .setId(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(slowE.get(counterInterval).getTimestamp())
                        .setMid(ct.trajId()));
                counterInterval++;
            }else {
                break;
            }
        }
        counterInterval = 0;
        for(CriticalPoint cp : gapS){
            if(gapE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("gap")
                        .setId(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(gapE.get(counterInterval).getTimestamp())
                        .setMid(ct.trajId()));
                counterInterval++;
            }else {
                System.out.println(ct.trajId() + " --> gap: [" + gapS.size() + "," + gapE.size() + "]");
                break;
            }
        }
        counterInterval = 0;
        for(CriticalPoint cp : sUS){
            if(sUE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("speedUp")
                        .setId(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(sUE.get(counterInterval).getTimestamp())
                        .setMid(ct.trajId()));
                counterInterval++;
            }else {
                System.out.println(ct.trajId() + " --> speedUp: [" + sUS.size() + "," + sUE.size() + "]");
                break;
            }
        }
        counterInterval = 0;
        for(CriticalPoint cp : sDS){
            if(sDE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("speedDown")
                        .setId(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(sDE.get(counterInterval).getTimestamp())
                        .setMid(ct.trajId()));
                counterInterval++;
            }else {
                System.out.println(ct.trajId() + " --> speedDown: [" + sDS.size() + "," + sDE.size() + "]");
                break;
            }
        }
        intervals.addAll(smoothTurn);
        intervals.add(new CriticalPointInterval().setType("trip")
                .setId(counterInterval + "")
                .setStartTime(tripStart)
                .setEndTime(tripEnd)
                .setMid(ct.trajId()));
        return intervals;
    }

    public List<CriticalPointT> go() throws InterruptedException {
        answer.clear();
        queue.clear();
        criticalIntervals.clear();
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

    public List<CriticalPointInterval> getCriticalIntervals(){
        return criticalIntervals;
    }
}
