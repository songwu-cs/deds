package songwu.deds.trajectory.example;

import songwu.deds.trajectory.algo.KNN;
import songwu.deds.trajectory.clean.CriticalTimeStampedPointT;
import songwu.deds.trajectory.clean.CriticalTimeStampedPointT42;
import songwu.deds.trajectory.clean.DenoiseTimeStampedPointT;
import songwu.deds.trajectory.data.*;
import songwu.deds.trajectory.io.File2TimestampedPointT;
import songwu.deds.trajectory.io.File2TimestampedPointTWithAttrs;
import songwu.deds.trajectory.io.ToCSV;
import songwu.deds.trajectory.similarity.DynamicTimeWarping;
import songwu.deds.trajectory.similarity.Frechet;
import songwu.deds.trajectory.similarity.SimilarityMeasure;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;

public class Demo {
    public static void knn() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException, ParseException {
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
    public static void denoise() throws IOException, InterruptedException, ParseException {
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort.csv")
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

        DenoiseTimeStampedPointT denoiser = new DenoiseTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        try(PrintWriter writer = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Denoised.csv");
                PrintWriter writerRatio = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_DenoisedRatio.csv")){

            writer.write("mmsi,t,longitude,latitude,x,y,time_gap,distance_gap,euc_speed,signed_turn,bearing,pause,speed_change,turn\n");
            for(TimeStampedPointT traj : denoised){
                for(TimeStampedPoint p : traj.getAllUnits()){
                    writer.write(traj.trajId() + ",");
                    writer.write(p.getTimestamp() + ",");
                    writer.write(p.getLongitude() + ",");
                    writer.write(p.getLatitude() + ",");
                    writer.write(p.getX() + ",");
                    writer.write(p.getY() + ",");
                    writer.write(p.getTimeGap() + ",");
                    writer.write(p.getDistanceGap() + ",");
                    writer.write(p.getEucSpeed() + ",");
                    writer.write(p.getSignedTurn() + ",");
                    writer.write(p.getBearing() + ",");
                    writer.write(p.isPause() + ",");
                    writer.write(p.isSpeedChange() + ",");
                    writer.write(p.isTurn() + "\n");
                }
            }

            writerRatio.write("mmsi,previous,now,ratio,previous_duration,now_duration,ratio_duration\n");
            PriorityQueue<DenoiseTimeStampedPointT.NoiseProportion> queue = denoiser.getNoiseRatio();
            while (queue.size() > 0){
                DenoiseTimeStampedPointT.NoiseProportion top = queue.poll();
                writerRatio.write(top.getMmsi() + "," + top.getPrevious() + "," + top.getNow() + "," + top.getRatio() + ","
                        + top.getPreviousDuration() + "," + top.getNowDuration() + "," + top.getRatioDuration() + "\n");
            }
        }
    }
    public static void critical() throws IOException, InterruptedException, ParseException {
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort.csv")
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

        DenoiseTimeStampedPointT denoiser = new DenoiseTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        CriticalTimeStampedPointT criticaler = new CriticalTimeStampedPointT();
        criticaler.setHistory(7).setGap(600).setNumberThreads(4).setRadius(250)
                .setSmoothThreshold(20).setSpeedAlpha(0.25).setTrajs(denoised);
        List<CriticalPointT> criticaled = criticaler.go();

        try(PrintWriter writer = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical7.csv");
            PrintWriter writerRatio = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical7Ratio.csv")){

            writer.write("mmsi,t,longitude,latitude,x,y,euc_speed,total_duration,type\n");
            for(CriticalPointT traj : criticaled){
                for(CriticalPoint p : traj.getAllUnits()){
                    writer.write(traj.trajId() + ",");
                    writer.write(p.getTimestamp() + ",");
                    writer.write(p.getLongitude() + ",");
                    writer.write(p.getLatitude() + ",");
                    writer.write(p.getX() + ",");
                    writer.write(p.getY() + ",");
                    writer.write(p.getEucSpeed() + ",");
                    writer.write(p.getTotalDuration() + ",");
                    writer.write(p.getType() + "\n");
                }
            }

            writerRatio.write("mmsi,previous,now,ratio\n");
            PriorityQueue<CriticalTimeStampedPointT.CriticalRatio> queue = criticaler.getCriticalRatio();
            while (queue.size() > 0){
                CriticalTimeStampedPointT.CriticalRatio top = queue.poll();
                writerRatio.write(top.getMmsi() + "," + top.getPrevious() + "," + top.getNow() + "," + top.getRatio() + "\n");
            }
        }
    }
    public static void critical42() throws InterruptedException, IOException, ParseException {
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort.csv")
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

        DenoiseTimeStampedPointT denoiser = new DenoiseTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        CriticalTimeStampedPointT42 criticaler = new CriticalTimeStampedPointT42();
        criticaler.setHistory(7).setGap(1800).setNumberThreads(4)
                .setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeed_min(1).setSpeed_slow_motion(5).setTrajs(denoised);
        List<CriticalPointT> criticaled = criticaler.go();

        try(PrintWriter writer = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical42.csv");
            PrintWriter writerRatio = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical42Ratio.csv");
            PrintWriter writerInterval = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical42Intervals.csv")){

            writer.write("mmsi,t,longitude,latitude,x,y,euc_speed,type\n");
            for(CriticalPointT traj : criticaled){
                for(CriticalPoint p : traj.getAllUnits()){
                    writer.write(traj.trajId() + ",");
                    writer.write(p.getTimestamp() + ",");
                    writer.write(p.getLongitude() + ",");
                    writer.write(p.getLatitude() + ",");
                    writer.write(p.getX() + ",");
                    writer.write(p.getY() + ",");
                    writer.write(p.getEucSpeed() + ",");
                    writer.write(p.getType() + "\n");
                }
            }

            writerRatio.write("mmsi,previous,now,ratio\n");
            PriorityQueue<CriticalTimeStampedPointT42.CriticalRatio> queue = criticaler.getCriticalRatio();
            while (queue.size() > 0){
                CriticalTimeStampedPointT42.CriticalRatio top = queue.poll();
                writerRatio.write(top.getMmsi() + "," + top.getPrevious() + "," + top.getNow() + "," + top.getRatio() + "\n");
            }

            writerInterval.write("mmsi,type,startTime,endTime,id\n");
            List<CriticalPointInterval> intervals = criticaler.getCriticalIntervals();
            for(CriticalPointInterval interval : intervals){
                writerInterval.write(interval.getMid() + "," + interval.getType() + "," + interval.getStartTime() + "," + interval.getEndTime() + "," + interval.getId() + "\n");
            }
        }
    }

    public static void main(String[] args) throws ParseException, IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
//        denoise();
//        critical();
        critical42();
    }
}
