package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.algo.SmallestEnclosingCircle;
import songwu.deds.trajectory.data.*;

import java.util.*;
import java.util.List;

//不区分加减速，stop和slow-motion使用相同阈值，速度变化采用alpha
public class CriticalTimeStampedPointT7 extends CriticalTimestampedPoint{
    private int gap;
    private int history;
    private double radius;
    private double smooth_threshold;
    private double speed_alpha;

    public CriticalTimeStampedPointT7 setGap(int gap) {
        this.gap = gap; return this;
    }

    public CriticalTimeStampedPointT7 setHistory(int history) {
        this.history = history; return this;
    }

    public CriticalTimeStampedPointT7 setRadius(int radius) {
        this.radius = radius; return this;
    }

    public CriticalTimeStampedPointT7 setSmoothThreshold(double smooth_threshold) {
        this.smooth_threshold = smooth_threshold; return this;
    }

    public CriticalTimeStampedPointT7 setSpeedAlpha(double speed_alpha) {
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
                    if(query.getUnit(q).isPaused())
                        paused += 1;
                }

                if(current.getTimeGap() > gap){
                    critical.addPoint(new CriticalPoint().setType("gapStart").copy(previous));
                    critical.addPoint(new CriticalPoint().setType("gapEnd").copy(current));
                    continue;
                }

                if((! current.isPaused()) && previous.isPaused()){
                    int left = k - 1;
                    for(; left >= 0; left--){
                        if(! query.getUnit(left).isPaused())
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

                if(current.isTurned() && (!current.isPaused())){
                    double bearing_mean = TimeStampedPoint.geography_angle(query.getUnit(k - history), query.getUnit(k - 1));
                    double angle_diff = Math.abs(current.getBearing() - bearing_mean);
                    angle_diff = angle_diff < 180 ? angle_diff : 360 - angle_diff;
                    if(angle_diff > smooth_threshold){
                        critical.addPoint(new CriticalPoint().setType("smooth_turn").copy(current));
                        already.add(current.getTimestamp() + "smooth_turn");
                        continue;
                    }
                }

                if(current.isSpeedchanged() && (!current.isPaused())){
                    double avg_speed = query.avgSpeed(k - 1, history);
                    if(Math.abs(current.getEucSpeed() - avg_speed) / current.getEucSpeed() > speed_alpha){
                        critical.addPoint(new CriticalPoint().setType("speedChange").copy(current));
                    }
                }
            }

            critical.sort();
            synchronized (this){
                answer.add(critical);
                queue.add(new UniversalRatio().id(query.trajId()).previous(query.size()).now(critical.size()).ratio());
            }
        }
    }

}
