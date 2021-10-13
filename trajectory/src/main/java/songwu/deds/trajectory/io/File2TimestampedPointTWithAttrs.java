package songwu.deds.trajectory.io;

import songwu.deds.trajectory.data.TimeStampedPoint;

public class File2TimestampedPointTWithAttrs extends File2TimestampedPointT{
    private int time_gap;
    private int distance_gap;
    private int euc_speed;
    private int bearing;
    private int signed_turn;
    private int pause;
    private int turn;
    private int speed_change;

    public File2TimestampedPointTWithAttrs timeGap(int index){
        this.time_gap = index;
        return this;
    }

    public File2TimestampedPointTWithAttrs distanceGap(int index){
        this.distance_gap = index;
        return this;
    }

    public File2TimestampedPointTWithAttrs eucSpeed(int index){
        this.euc_speed = index;
        return this;
    }

    public File2TimestampedPointTWithAttrs bearing(int index){
        this.bearing = index;
        return this;
    }

    public File2TimestampedPointTWithAttrs signedTurn(int index){
        this.signed_turn = index;
        return this;
    }

    public File2TimestampedPointTWithAttrs turn(int index){
        this.turn = index;
        return this;
    }

    public File2TimestampedPointTWithAttrs pause(int index){
        this.pause = index;
        return this;
    }

    public File2TimestampedPointTWithAttrs speedChange(int index){
        this.speed_change = index;
        return this;
    }

    @Override
    public void extra(TimeStampedPoint point, String[] parts) {
        point.time_gap(Double.parseDouble(parts[time_gap]));
        point.distance_gap(Double.parseDouble(parts[distance_gap]));
        point.euc_speed(Double.parseDouble(parts[euc_speed]));
        point.signed_turn(Double.parseDouble(parts[signed_turn]));
        point.bearing(Double.parseDouble(parts[bearing]));
        point.turn(Boolean.parseBoolean(parts[turn]));
        point.pause(Boolean.parseBoolean(parts[pause]));
        point.speedChange(Boolean.parseBoolean(parts[speed_change]));
    }
}
