package songwu.deds.trajectory.algo;

import songwu.deds.trajectory.data.Trajectory;
import songwu.deds.trajectory.data.TrajectoryUnit;
import songwu.deds.trajectory.similarity.SimilarityMeasure;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class KNN<U extends TrajectoryUnit<U>, T extends Trajectory<U>>{
    private List<SimilarityMeasure<U, T>> sim_measures;
    private List<T> queries;
    private List<T> database;
    private int k_value;

    public KNN(int k){
        k_value = k;
    }

    public KNN<U,T> setSimMeasures(List<SimilarityMeasure<U, T>> sim_measures) {
        this.sim_measures = sim_measures;
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

    public List<KNNelement> go(){
        List<KNNelement> answer = new ArrayList<>();
        PriorityQueue<KNNelement> priority_queue = new PriorityQueue<>(Comparator.comparingDouble(KNNelement::getDistance));
        for(SimilarityMeasure<U, T> measure : sim_measures){
            for(T query : queries){
                answer.add(new KNNelement(query.trajId(), query.trajId(), measure.name(), 0));
                for(T candidate : database){
                    if(query == candidate)
                        continue;
                    priority_queue.add(new KNNelement(query.trajId(), candidate.trajId(), measure.name(), measure.apply(query, candidate)));
                }
                int rank = 1;
                for(KNNelement e : priority_queue){
                    answer.add(e.setRank(rank++));
                    if(rank > this.k_value)
                        break;
                }
                priority_queue.clear();
            }
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
