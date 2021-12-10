package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.data.*;
import songwu.deds.trajectory.data.TimeStampedPointT;

import java.util.*;

//不区分加减速，stop和slow-motion使用不同阈值，速度变化采用alpha
public class CriticalTimeStampedPointTConvex extends CriticalTimestampedPoint{
    private int gap;
    private int history;
    private double smooth_threshold;
    private double speed_alpha;
    private double speed_min;
    private double speed_slow_motion;

    public CriticalTimeStampedPointTConvex setGap(int gap) {
        this.gap = gap; return this;
    }

    public CriticalTimeStampedPointTConvex setSpeedMin(double speed_min) {
        this.speed_min = speed_min; return this;
    }

    public CriticalTimeStampedPointTConvex setSpeedSlowMotion(double speed_slow_motion) {
        this.speed_slow_motion = speed_slow_motion; return this;
    }

    public CriticalTimeStampedPointTConvex setHistory(int history) {
        this.history = history; return this;
    }

    public CriticalTimeStampedPointTConvex setSmoothThreshold(double smooth_threshold) {
        this.smooth_threshold = smooth_threshold; return this;
    }

    public CriticalTimeStampedPointTConvex setSpeedAlpha(double speed_alpha) {
        this.speed_alpha = speed_alpha; return this;
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
            boolean speedchange_flag = false;
            boolean stop_flag = false;
            boolean slow_motion_flag = false;
            boolean signal_loss_flag = false;
            int signal_loss_end = 0;

            critical.addPoint(new CriticalPoint().setType("tripStart").copy(query.getUnit(0)));
            critical.addPoint(new CriticalPoint().setType("tripEnd").copy(query.getUnit(query.size() - 1)));

            for(int k = 1; k <= history; k++){
                TimeStampedPoint current = query.getUnit(k);
                TimeStampedPoint previous = query.getUnit(k - 1);

                if(current.getTimeGap() > gap){
                    critical.addPoint(new CriticalPoint().setType("gapStart").copy(previous));
                    critical.addPoint(new CriticalPoint().setType("gapEnd").copy(current));
                }
            }

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
                    if(speedchange_flag){
                        critical.addPoint(new CriticalPoint().setType("speedChangeEnd").copy(previous));
                        speedchange_flag = false;
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
                    if(speedchange_flag){
                        critical.addPoint(new CriticalPoint().setType("speedChangeEnd").copy(current));
                        speedchange_flag = false;
                    }
                }else if (current.getEucSpeed() > speed_min && stop_flag){
                    critical.addPoint(new CriticalPoint().setType("stopEnd").copy(current));
                    stop_flag = false;
                }

                //slow-motion
                double _s = current.getEucSpeed();
                double avg_speed = query.avgSpeed(k-1, history);
                double deviation_speed = query.deviationSpeed(k-1, history, avg_speed);
                if(_s > speed_min && _s <= speed_slow_motion && (!slow_motion_flag)){
                    critical.addPoint(new CriticalPoint().setType("slowMotionStart").copy(current));
                    slow_motion_flag = true;
                }else if((_s <= speed_min || _s > speed_slow_motion) && slow_motion_flag){
                    critical.addPoint(new CriticalPoint().setType("slowMotionEnd").copy(current));
                    slow_motion_flag = false;
                }

                //speed-change
                if(_s > speed_min && avg_speed > speed_min){
                    double variation = Math.abs((_s - avg_speed) / avg_speed);
                    if(variation > speed_alpha && (! speedchange_flag)){
                        critical.addPoint(new CriticalPoint().setType("speedChangeStart").copy(current));
                        speedchange_flag = true;
                    }else if(variation < speed_alpha && speedchange_flag){
                        critical.addPoint(new CriticalPoint().setType("speedChangeEnd").copy(current));
                        speedchange_flag = false;
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
                queue.add(new UniversalRatio().id(query.trajId()).previous(query.size()).now(critical.size()).ratio());
                criticalIntervals.addAll(point2interval(critical));
            }
        }
    }

    public List<CriticalPointInterval> point2interval(CriticalPointT ct){
        HashMap<String, List<CriticalPoint>> buffer = new HashMap<>();
        buffer.put("stopStart", new ArrayList<>());
        buffer.put("stopEnd", new ArrayList<>());
        buffer.put("speedChangeStart", new ArrayList<>());
        buffer.put("speedChangeEnd", new ArrayList<>());
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
                        .setRank(smoothTurn.size() + "")
                        .setId(ct.trajId()));
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
        List<CriticalPoint> speedS = buffer.get("speedChangeStart");
        List<CriticalPoint> speedE = buffer.get("speedChangeEnd");

        List<CriticalPointInterval> intervals = new ArrayList<>();
        int counterInterval = 0;
        for(CriticalPoint cp : stopS){
            if(stopE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("stop")
                        .setRank(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(stopE.get(counterInterval).getTimestamp())
                        .setId(ct.trajId()));
                counterInterval++;
            }else {
                break;
            }
        }
        counterInterval = 0;
        for(CriticalPoint cp : slowS){
            if(slowE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("slowMotion")
                        .setRank(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(slowE.get(counterInterval).getTimestamp())
                        .setId(ct.trajId()));
                counterInterval++;
            }else {
                break;
            }
        }
        counterInterval = 0;
        for(CriticalPoint cp : gapS){
            if(gapE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("gap")
                        .setRank(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(gapE.get(counterInterval).getTimestamp())
                        .setId(ct.trajId()));
                counterInterval++;
            }else {
                System.out.println(ct.trajId() + " --> gap: [" + gapS.size() + "," + gapE.size() + "]");
                break;
            }
        }
        counterInterval = 0;
        for(CriticalPoint cp : speedS){
            if(speedE.size() > counterInterval){
                intervals.add(new CriticalPointInterval().setType("speedChange")
                        .setRank(counterInterval + "")
                        .setStartTime(cp.getTimestamp())
                        .setEndTime(speedE.get(counterInterval).getTimestamp())
                        .setId(ct.trajId()));
                counterInterval++;
            }else {
                System.out.println(ct.trajId() + " --> speedChange: [" + speedS.size() + "," + speedE.size() + "]");
                break;
            }
        }

        intervals.addAll(smoothTurn);
        intervals.add(new CriticalPointInterval().setType("trip")
                .setRank(counterInterval + "")
                .setStartTime(tripStart)
                .setEndTime(tripEnd)
                .setId(ct.trajId()));
        return intervals;
    }


}
