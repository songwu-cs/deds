package songwu.deds.trajectory.data;

public class CriticalPointInterval {
    private String id0;
    private String mid;
    private String id;
    private String type;
    private String startTime;
    private String endTime;

    public String getId() {
        return id;
    }

    public String getId0() {
        return id0;
    }

    public CriticalPointInterval setId(String id) {
        this.id = id; return this;
    }

    public CriticalPointInterval setId0(String id0) {
        this.id0 = id0; return this;
    }

    public String getMid(){
        return mid;
    }

    public CriticalPointInterval setMid(String mid){
        this.mid = mid; return this;
    }

    public String getType() {
        return type;
    }

    public CriticalPointInterval setType(String type) {
        this.type = type; return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public CriticalPointInterval setStartTime(String startTime) {
        this.startTime = startTime; return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public CriticalPointInterval setEndTime(String endTime) {
        this.endTime = endTime; return this;
    }
}
