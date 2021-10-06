package songwu.deds.trajectory.example;

import songwu.deds.trajectory.algo.KNN;
import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;
import songwu.deds.trajectory.io.File2TimestampedPointT;
import songwu.deds.trajectory.io.ToCSV;
import songwu.deds.trajectory.similarity.DynamicTimeWarping;
import songwu.deds.trajectory.similarity.Frechet;
import songwu.deds.trajectory.similarity.SimilarityMeasure;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Demo {
    public static void main(String[] args) throws ParseException, IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath("C:\\Users\\TJUer\\Desktop\\ais_data\\ais1209963_four_columns_used_avg_clean_position_speed_xy.csv")
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(10)
                .y(11);
        List<TimeStampedPointT> trajs = input.go();

        Collections.shuffle(trajs);
        List<TimeStampedPointT> queries = trajs.subList(0, 67);

        List<SimilarityMeasure<TimeStampedPoint, TimeStampedPointT>> measures = new ArrayList<>();
        measures.add(new DynamicTimeWarping<>());
        measures.add(new Frechet<>());

        KNN<TimeStampedPoint, TimeStampedPointT> knn = new KNN<>(20);
        knn.setSimMeasures(measures);
        knn.setQueries(queries);
        knn.setDatabase(trajs);
        knn.setNumberThreads(4);

        List<KNN.KNNelement> answer = knn.go();

        ToCSV<KNN.KNNelement> toCSV = new ToCSV<>();
        toCSV.setSplitter(",")
                .setHeader("query,neighbor,measure_name,rank,distance")
                .setOutputPath("C:\\Users\\TJUer\\Desktop\\ais_data\\ais1209963_four_columns_used_avg_clean_position_speed_xy_knn_67_20.csv")
                .setLines(answer).go();
    }
}
