package songwu.deds.trajectory.io;

import songwu.deds.trajectory.data.TimeStampedPoint;

import java.text.ParseException;

public class File2TimestampedPointBasicT extends File2TimestampedPointT{
    private int time_gap;
    private int distance_gap;
    private int euc_speed;
    private int bearing;
    private int signed_turn;

    public File2TimestampedPointT timeGap(int index){
        this.time_gap = index;
        return this;
    }

    public File2TimestampedPointT distanceGap(int index){
        this.distance_gap = index;
        return this;
    }

    public File2TimestampedPointT eucSpeed(int index){
        this.euc_speed = index;
        return this;
    }

    public File2TimestampedPointT bearing(int index){
        this.bearing = index;
        return this;
    }

    public File2TimestampedPointT signedTurn(int index){
        this.signed_turn = index;
        return this;
    }

    public TimeStampedPoint modify(TimeStampedPoint point, String[] parts) throws ParseException {
        super.modify(point, parts);
        point.time_gap(Double.parseDouble(parts[time_gap]));
        point.distance_gap(Double.parseDouble(parts[distance_gap]));
        point.euc_speed(Double.parseDouble(parts[euc_speed]));
        point.signed_turn(Double.parseDouble(parts[signed_turn]));
        point.bearing(Double.parseDouble(parts[bearing]));
        return point;
    }

}
