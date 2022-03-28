package note;

import songwu.deds.trajectory.algo.KNN;
import songwu.deds.trajectory.clean.*;
import songwu.deds.trajectory.data.*;
import songwu.deds.trajectory.data.TimeStampedPoint;
import songwu.deds.trajectory.data.TimeStampedPointT;
import songwu.deds.trajectory.io.File2TimestampedPointT;
import songwu.deds.trajectory.similarity.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;

public class Demo {
    public static List<KNN.KNNelement> knn() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException, ParseException {
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
        measures.add(new DynamicTimeWarping<>(Euclidean.IDENTIFIER));
        measures.add(new Frechet<>(Euclidean.IDENTIFIER));

        KNN<TimeStampedPoint, TimeStampedPointT> knn = new KNN<>(20);
        knn.setSimMeasures(measures);
        knn.setQueries(queries);
        knn.setDatabase(trajs);
        knn.setNumberThreads(4);

        List<KNN.KNNelement> answer = knn.go();
        return answer;
    }

    public static void denoise() throws IOException, InterruptedException, ParseException {
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath("E:\\DEDS-DataLake\\2021-11-25-boost-validation\\202111_19to20_filtered.csv")
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

        try(PrintWriter writer = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021_convex\\ais_20210926_1km_Denoised.csv");
                PrintWriter writerRatio = new PrintWriter("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021_convex\\ais_20210926_1km_DenoisedRatio.csv")){

            writer.write("id,t,longitude,latitude,x,y,time_gap,distance_gap,euc_speed,signed_turn,bearing,pause,speed_change,turn\n");
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
                    writer.write(p.isPaused() + ",");
                    writer.write(p.isSpeedchanged() + ",");
                    writer.write(p.isTurned() + "\n");
                }
            }

            writerRatio.write("id,previous,now,ratio\n");
            PriorityQueue<UniversalRatio> queue = denoiser.getNoiseRatio();
            while (queue.size() > 0){
                UniversalRatio top = queue.poll();
                writerRatio.write(top.getId() + "," + top.getPrevious() + "," + top.getNow() + "," + top.getRatio() + "\n");
            }
        }
    }

    public static void critical7() throws IOException, InterruptedException, ParseException {
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

        CriticalTimeStampedPointT7 criticaler = new CriticalTimeStampedPointT7();
        criticaler.setHistory(7).setGap(600).setRadius(250)
                .setSmoothThreshold(20).setSpeedAlpha(0.25).setTrajs(denoised).setNumberThreads(4);
        List<CriticalPointT> criticaled = criticaler.go();

        UniversalRatio.saveRatio("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical7Ratio.csv", criticaler.getCriticalRatio());
        CriticalPointT.saveCriticalPointT("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical7.csv", criticaled);
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
        criticaler.setHistory(7).setGap(1800).setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeedMin(1).setSpeedSlowMotion(5).setTrajs(denoised).setNumberThreads(4);
        List<CriticalPointT> criticaled = criticaler.go();

        UniversalRatio.saveRatio("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical42Ratio.csv", criticaler.getCriticalRatio());
        CriticalPointT.saveCriticalPointT("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical42.csv", criticaled);
        CriticalPointInterval.saveIntervals("C:\\Users\\TJUer\\Desktop\\dk_csv_apr2021\\ais_20210926_filterTooShort_Critical42Intervals.csv", criticaler.getCriticalIntervals());

    }

    public static void criticalConvex() throws InterruptedException, IOException, ParseException{
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath("E:\\DEDS-DataLake\\2021-11-25-boost-validation\\20211116_final.csv")
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

        CriticalTimeStampedPointTConvex criticaler = new CriticalTimeStampedPointTConvex();
        criticaler.setHistory(7).setGap(1800)
                .setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeedMin(1).setSpeedSlowMotion(5).setTrajs(denoised).setNumberThreads(4);
        List<CriticalPointT> criticaled = criticaler.go();

        UniversalRatio.saveRatio("E:\\DEDS-DataLake\\2021-11-25-boost-validation\\20211116_final_criticalRatio.csv", criticaler.getCriticalRatio());
        CriticalPointT.saveCriticalPointT("E:\\DEDS-DataLake\\2021-11-25-boost-validation\\20211116_final_critical.csv", criticaled);
        CriticalPointInterval.saveIntervals("E:\\DEDS-DataLake\\2021-11-25-boost-validation\\20211116_final_criticalInterval.csv", criticaler.getCriticalIntervals());
    }

    public static void main(String[] args) throws ParseException, IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
//        denoise();
//        critical();
//        critical42();
        criticalConvex();
    }
}
