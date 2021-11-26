package songwu.deds.trajectory.io;

import songwu.deds.trajectory.data.TimeStampedPoint;

import java.text.ParseException;

public class File2TimestampedPointBasic7T extends File2TimestampedPointBasicT{
    private int pause;
    private int turn;
    private int speed_change;

    public File2TimestampedPointBasic7T turn(int index){
        this.turn = index;
        return this;
    }

    public File2TimestampedPointBasic7T pause(int index){
        this.pause = index;
        return this;
    }

    public File2TimestampedPointBasic7T speedChange(int index){
        this.speed_change = index;
        return this;
    }


    public TimeStampedPoint modify(TimeStampedPoint point, String[] parts) throws ParseException {
        super.modify(point, parts);
        point.setTurned(Boolean.parseBoolean(parts[turn]));
        point.setPaused(Boolean.parseBoolean(parts[pause]));
        point.setSpeedchanged(Boolean.parseBoolean(parts[speed_change]));
        return point;
    }

}
