package songwu.deds.trajectory.algo;

import songwu.deds.trajectory.data.Trajectory;
import songwu.deds.trajectory.data.TrajectoryUnit;
import songwu.deds.trajectory.similarity.SimilarityMeasure;

import java.util.*;

public class KNN<U extends TrajectoryUnit<U>, T extends Trajectory<U>>{
    private List<SimilarityMeasure<U, T>> sim_measures;
    private List<T> queries;
    private List<T> database;
    private int k_value;
    private int counter;
    private int number_threads = 1;
    private List<KNNelement> answer = new ArrayList<>();

    public KNN(int k){
        k_value = k;
    }

    private void workerQueries(){
        String name = Thread.currentThread().getName();
        T query;
        PriorityQueue<KNNelement> priority_queue = new PriorityQueue<>(Comparator.comparingDouble(KNNelement::getDistance));
        List<KNNelement> my_answer = new ArrayList<>();
        while (true){
            synchronized(this){
                if(counter < queries.size()){
                    query = queries.get(counter); counter++;
                }else {
                    break;
                }
            }
            for(SimilarityMeasure<U, T> measure : sim_measures){
                priority_queue.clear();
                my_answer.clear();
                my_answer.add(new KNNelement(query.trajId(), query.trajId(), measure.name(), 0));
                for(T candidate : database){
                    if(query == candidate)
                        continue;
                    priority_queue.add(new KNNelement(query.trajId(), candidate.trajId(), measure.name(), measure.apply(query, candidate)));
                }
                int rank = 1;
                while (rank <= this.k_value && priority_queue.size() > 0){
                    my_answer.add(priority_queue.poll().setRank(rank++));
                }
                synchronized (this){
                    answer.addAll(my_answer);
                }
                System.out.println(name + " : " + query.trajId() + " : " + measure.name());
            }
        }
    }

    public KNN<U,T> setSimMeasures(List<SimilarityMeasure<U, T>> sim_measures) {
        this.sim_measures = sim_measures;
        return this;
    }

    public KNN<U,T> setNumberThreads(int number_threads) {
        this.number_threads = number_threads;
        return this;
    }

    public KNN<U,T> setQueries(List<T> queries) {
        this.queries = queries;
        return this;
    }

    public KNN<U,T> setDatabase(List<T> database) {
        this.database = database;
        return this;
    }

    public List<KNNelement> go() throws InterruptedException {
        answer.clear();
        counter = 0;
        List<Thread> workers = new ArrayList<>();
        for(int i = 0; i < number_threads; i++){
            workers.add(new Thread(this::workerQueries, "worker#" + (i + 1)));
        }
        for(Thread worker : workers){
            worker.start();
        }
        for(Thread worker : workers){
            worker.join();
        }
        return answer;
    }

    public static class KNNelement{
        private String query;
        private String neighbor;
        private String measure_name;
        private int rank;
        private double distance;

        public KNNelement(String query, String neighbor, String measure_name, double distance) {
            this.query = query;
            this.neighbor = neighbor;
            this.measure_name = measure_name;
            this.distance = distance;
        }

        public KNNelement setRank(int rank){
            this.rank = rank;
            return this;
        }

        public String getQuery() {
            return query;
        }

        public String getNeighbor() {
            return neighbor;
        }

        public String getMeasureName() {
            return measure_name;
        }

        public int getRank() {
            return rank;
        }

        public double getDistance() {
            return distance;
        }
    }

    public static void main(String[] args){
    }
}
