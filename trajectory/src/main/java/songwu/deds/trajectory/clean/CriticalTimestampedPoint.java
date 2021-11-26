package songwu.deds.trajectory.clean;

import songwu.deds.trajectory.data.CriticalPointInterval;
import songwu.deds.trajectory.data.CriticalPointT;
import songwu.deds.trajectory.data.UniversalRatio;
import songwu.deds.trajectory.data.TimeStampedPointT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class CriticalTimestampedPoint {
    protected int counter = 0;
    protected int number_threads = 1;

    protected List<TimeStampedPointT> trajs = new ArrayList<>();
    protected List<CriticalPointT> answer = new ArrayList<>();
    protected List<CriticalPointInterval> criticalIntervals = new ArrayList<>();
    protected PriorityQueue<UniversalRatio> queue = new PriorityQueue<>(Comparator.comparingDouble(UniversalRatio::getRatio));

    public CriticalTimestampedPoint setNumberThreads(int number_threads) {
        this.number_threads = number_threads; return this;
    }

    public CriticalTimestampedPoint setTrajs(List<TimeStampedPointT> trajs) {
        this.trajs = trajs; return this;
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

    public void worker(){}

    public List<CriticalPointInterval> getCriticalIntervals(){
        return criticalIntervals;
    }

    public PriorityQueue<UniversalRatio> getCriticalRatio() {
        return queue;
    }

}
