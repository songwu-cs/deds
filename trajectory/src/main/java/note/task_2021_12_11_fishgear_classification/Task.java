package note.task_2021_12_11_fishgear_classification;

import songwu.deds.trajectory.clean.CriticalTimeStampedPointTConvex;
import songwu.deds.trajectory.clean.DenoiseFakeTimeStampedPointT;
import songwu.deds.trajectory.clean.DenoiseTimeStampedPointT;
import songwu.deds.trajectory.data.*;
import songwu.deds.trajectory.io.File2TimestampedPointT;
import songwu.deds.trajectory.utility.MyDate;
import songwu.deds.trajectory.utility.Statistics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class Task {
    public static void retainFishingAIS() throws IOException {
        Set<String> fishingVessels = new HashSet<>();

        File file = new File("H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\mmsi-fishgear.csv");
        List<String> lines = Files.readAllLines(file.toPath());
        for(String line : lines.subList(1, lines.size())){
            fishingVessels.add(line.split(",")[1]);
        }

        int counterHit = 0;
        int counterTotal = 0;

        int MMSI_INDEX = 0;
        int T_INDEX = 1;
        int LON_INDEX = 2;
        int LAT_INDEX = 3;
        try(BufferedReader reader = new BufferedReader(new FileReader("E:\\aisdk_oneweek.csv"));
            PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\oneweek-fishing.csv")){
            writer.write("mmsi,t,longitude,latitude\n");

            String line = "";
            while ((line = reader.readLine()) != null){
                if(line.startsWith("mmsi"))
                    continue;
                String parts[] = line.split(",");
                if(fishingVessels.contains(parts[MMSI_INDEX])) {
                    writer.write(String.join(",",
                            new String[]{parts[MMSI_INDEX],
                                    parts[T_INDEX],
                                    parts[LON_INDEX],
                                    parts[LAT_INDEX]
                            }) + "\n");
                    counterHit++;
                }
                counterTotal++;
                if(counterTotal % 10000 == 0)
                    System.out.println("hit: " + counterHit + " ; total: " + counterTotal);
            }
        }
    }

    public static void denoise() throws IOException, ParseException, InterruptedException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
//        String pathRaw = baseDir + "oneweek-fishing-filtered-ge1000.csv";
//        String pathDenoised = baseDir + "oneweek-fishing-filtered-ge1000-denoised.csv";
//        String pathDenoisedRatio = baseDir + "oneweek-fishing-filtered-ge1000-denoisedRatio.csv";

        String pathRaw = baseDir + "good_fish_ais.csv";
        String pathDenoised = baseDir + "good_fish_ais-denoisedFake.csv";
        String pathDenoisedRatio = baseDir + "good_fish_ais-denoisedFakeRatio.csv";

        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath(pathRaw)
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(3)
                .latitude(2)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

//        DenoiseTimeStampedPointT denoiser = new DenoiseTimeStampedPointT();
        DenoiseFakeTimeStampedPointT denoiser = new DenoiseFakeTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        try(PrintWriter writer = new PrintWriter(pathDenoised);
            PrintWriter writerRatio = new PrintWriter(pathDenoisedRatio)){

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

    public static void criticalConvex() throws InterruptedException, IOException, ParseException{
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
        String pathRaw = baseDir + "good_fish_ais.csv";
        String pathCritical = baseDir + "good_fish_ais-critical.csv";
        String pathCriticalRatio = baseDir + "good_fish_ais-criticalRatio.csv";
        String pathCriticalInterval = baseDir + "good_fish_ais-criticalInterval.csv";

        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath(pathRaw)
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(3)
                .latitude(2)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

        DenoiseFakeTimeStampedPointT denoiser = new DenoiseFakeTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        CriticalTimeStampedPointTConvex criticaler = new CriticalTimeStampedPointTConvex();
        criticaler.setHistory(7).setGap(1800)
                .setSmoothThreshold(10).setSpeedAlpha(0.25)
                .setSpeedMin(1).setSpeedSlowMotion(5).setTrajs(denoised).setNumberThreads(4);
        List<CriticalPointT> criticaled = criticaler.go();

        UniversalRatio.saveRatio(pathCriticalRatio, criticaler.getCriticalRatio());
        CriticalPointT.saveCriticalPointT(pathCritical, criticaled);
        CriticalPointInterval.saveIntervals(pathCriticalInterval, criticaler.getCriticalIntervals());
    }

    public static void retainTrawlersFixed() throws IOException {
        Set<String> fishingVessels = new HashSet<>();

        File file = new File("H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\mmsi-fishgear.csv");
        List<String> lines = Files.readAllLines(file.toPath());
        for(String line : lines.subList(1, lines.size())){
            String parts[] = line.split(",");
            if(parts[0].equals("fixed_gear") || parts[0].equals("trawlers"))
                fishingVessels.add(parts[1]);
        }

        try(BufferedReader reader = new BufferedReader(new FileReader("H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\oneweek-fishing-filtered-ge1000.csv"));
            PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\oneweek-fishing-filtered-ge1000-tralwer-fixed.csv")){
            writer.write("mmsi,t,longitude,latitude,x,y\n");

            String line = "";
            while ((line = reader.readLine()) != null){
                if(line.startsWith("mmsi"))
                    continue;
                if(fishingVessels.contains(line.split(",")[0]))
                    writer.write(line + "\n");
            }
        }
    }

    public static int indexStartWith(List<String> ss, String prefix){
        for(int i = 0; i < ss.size(); i++){
            if(ss.get(i).startsWith(prefix))
                return i;
        }
        return -1;
    }

    public static int indexStartWithFromRight(List<String> ss, String prefix){
        for(int i = ss.size() - 1; i >= 0; i--){
            if(ss.get(i).startsWith(prefix))
                return i;
        }
        return -1;
    }

    public static void toWindowTraining() throws IOException, ParseException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";

        File file = new File(baseDir + "manual_label-1km.csv");
        List<ManulLabel> labels = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());
        for(String line : lines.subList(1, lines.size())){
            labels.add(new ManulLabel(line));
        }

        File dataFile = new File(baseDir + "oneweek-fishing-filtered-ge1000-tralwer-fixed-1km.csv");
        List<String> data = Files.readAllLines(dataFile.toPath());

        try(PrintWriter writer = new PrintWriter(baseDir + "raw1km-manual-labels.csv")){
            writer.write("mmsi,t,longitude,latitude,x,y,label\n");
            Map<String, Integer> mmsi2identifier = new HashMap<>();
            for(ManulLabel ml : labels){
                int start = indexStartWith(data, ml.mmsi + "," + ml.startTime);
                int end = indexStartWith(data, ml.mmsi + "," + ml.endTime);

                int duration = 0;
                int numPoints = 1;
                int currentStart = start;
                int currentEnd = start;
                while (currentEnd <= end){
                    if(numPoints >= 100 && duration >= 60 * 20){
                        int counter = mmsi2identifier.getOrDefault(ml.mmsi, 0);
                        for(String line : data.subList(currentStart, currentEnd + 1)){
                            writer.write(line.replace(ml.mmsi, ml.mmsi + "-" + counter) + "," + ml.label + "\n");
                        }
                        currentStart = currentEnd;
                        duration = 0;
                        numPoints = 1;
                        mmsi2identifier.put(ml.mmsi, ++counter);
                        continue;
                    }
                    currentEnd++;
                    numPoints++;
                    duration += MyDate.timestampDiff(data.get(currentEnd).split(",")[1],
                            data.get(currentEnd-1).split(",")[1]);
                }
            }
        }
    }

    public static void toWindowTesting() throws IOException, ParseException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
        Set<String> mmsiTrain = new HashSet<>();
        Set<String> mmsiAll = new HashSet<>();

        List<String> lines = Files.readAllLines(new File(baseDir + "manual_label-1km.csv").toPath());
        for(String line : lines.subList(1, lines.size())){
            mmsiTrain.add(line.split(",")[0]);
        }
        lines = Files.readAllLines(new File(baseDir + "trawler-fixed-mmsi2gear-1km.csv").toPath());
        for(String line : lines.subList(1, lines.size())){
            mmsiAll.add(line.split(",")[0]);
        }

        File dataFile = new File(baseDir + "oneweek-fishing-filtered-ge1000-tralwer-fixed-1km.csv");
        List<String> data = Files.readAllLines(dataFile.toPath());

        try(PrintWriter writer = new PrintWriter(baseDir + "raw1km-manual-nolabels.csv")){
            writer.write("mmsi,t,longitude,latitude,x,y\n");
            for(String mmsi : mmsiAll){
                if(mmsiTrain.contains(mmsi))
                    continue;

                int start = indexStartWith(data, mmsi);
                int end = indexStartWithFromRight(data, mmsi);
                System.out.println(mmsi + "," + start + "," + end);
                int counter = 0;
                int duration = 0;
                int numPoints = 1;
                int currentStart = start;
                int currentEnd = start;
                while (currentEnd <= end){
                    if(numPoints >= 100 && duration >= 60 * 20){
                        for(String line : data.subList(currentStart, currentEnd + 1)){
                            writer.write(line.replace(mmsi, mmsi + "-" + counter) + "\n");
                        }
                        currentStart = currentEnd;
                        duration = 0;
                        numPoints = 1;
                        counter++;
                        continue;
                    }
                    currentEnd++;
                    numPoints++;
                    if(currentEnd > end)
                        break;
                    duration += MyDate.timestampDiff(data.get(currentEnd).split(",")[1],
                            data.get(currentEnd-1).split(",")[1]);
                }
            }
        }
    }

    public static void buildFishingParts() throws IOException {
        List<WindowLabel> windowLabels = new ArrayList<>();
        List<String> lines = Files.readAllLines(new File("D:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\without1kmlabels_features.csv").toPath());
        for(String line : lines.subList(1, lines.size())){
            String parts[] = line.split(",");
            windowLabels.add(new WindowLabel(parts[0], parts[22]));
        }
        windowLabels.sort(Comparator.comparing(WindowLabel::getWholeId).thenComparing(WindowLabel::getPartId));
//        for(WindowLabel label : windowLabels.subList(0,2000))
//            System.out.println(label);


        List<WindowLabelHelp> windowLabelHelps = new ArrayList<>();
        String currentWhole = windowLabels.get(0).getWholeId();
        String currentLabel = windowLabels.get(0).label;
        int start = 0;
        int end = 0;
        for(int i = 1; i < windowLabels.size(); i++){
            String whole = windowLabels.get(i).getWholeId();
            String label = windowLabels.get(i).label;
            if((! whole.equals(currentWhole)) ||
                    (! label.equals(currentLabel))){
                windowLabelHelps.add(new WindowLabelHelp(currentWhole, currentLabel, start, end));
                currentWhole = whole;
                currentLabel = label;
                start = end = i;
            }else{
                end = i;
            }
        }
        windowLabelHelps.add(new WindowLabelHelp(currentWhole, currentLabel, start, end));
//        for(WindowLabelHelp help : windowLabelHelps)
//            System.out.println(help);


        List<WindowLabelAid> windowLabelAids = new ArrayList<>();
        int quantity = 6;
        int currentPos = 0;
        while (currentPos < windowLabelHelps.size() && (windowLabelHelps.get(currentPos).label.equals("sailing")))
            currentPos++;
        if(currentPos == windowLabelHelps.size())
            return;
        currentWhole = windowLabelHelps.get(currentPos).whole;
        int endPos = currentPos;
        while (currentPos < windowLabelHelps.size()){
            while (endPos + 2 < windowLabelHelps.size()){
                WindowLabelHelp help__ = windowLabelHelps.get(endPos);
                WindowLabelHelp help_ = windowLabelHelps.get(endPos + 1);
                WindowLabelHelp help = windowLabelHelps.get(endPos + 2);
                if(help.whole.equals(currentWhole) && help.label.equals("fishing") && help.size >= help_.size && help__.size >= help_.size){
                    endPos+=2;
                }else {
                    break;
                }
            }
            int total = 0;
            for(int i = currentPos; i <= endPos; i++)
                total += windowLabelHelps.get(i).size;
            if(total >= quantity){
                windowLabelAids.add(new WindowLabelAid(currentWhole,
                        windowLabelHelps.get(currentPos).start,
                        windowLabelHelps.get(endPos).end));
            }

            currentPos = endPos + 1;
            while (currentPos < windowLabelHelps.size() && (windowLabelHelps.get(currentPos).label.equals("sailing")))
                currentPos++;
            if(currentPos == windowLabelHelps.size())
                break;
            currentWhole = windowLabelHelps.get(currentPos).whole;
            endPos = currentPos;
        }
        System.out.println(windowLabelAids.size());
//        for(WindowLabelAid aid : windowLabelAids)
//            System.out.println(aid.whole + " " + windowLabels.get(aid.start).getPartId() + " " + windowLabels.get(aid.end).getPartId());


        lines = Files.readAllLines(new File("D:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\trawler-fixed-mmsi2gear.csv").toPath());
        Map<String, String> mmsi2gear = new HashMap<>();
        for(String line : lines.subList(1, lines.size())){
            String parts[] = line.split(",");
            mmsi2gear.put(parts[0], parts[2]);
        }
        lines = Files.readAllLines(new File("D:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\raw1km-manual-nolabels-criticalInterval.csv").toPath());
        Map<String, String> traj2startTime = new HashMap<>();
        Map<String, String> traj2endTime = new HashMap<>();
        for(String line : lines){
            if(line.contains("trip")){
                String parts[] = line.split(",");
                traj2startTime.put(parts[4], parts[2]);
                traj2endTime.put(parts[4], parts[3]);
            }
        }
        try(PrintWriter writer = new PrintWriter("D:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\build1km-sandwich.csv")){
            writer.write("mmsi,t,longitude,latitude,x,y,label\n");
            lines = Files.readAllLines(new File("D:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\oneweek-fishing-filtered-ge1000-tralwer-fixed-1km.csv").toPath());

            int counter = 0;
            for(WindowLabelAid aid : windowLabelAids){
                String whole = aid.whole;
                String startTime = traj2startTime.get(windowLabels.get(aid.start).id);
                String endTime = traj2endTime.get(windowLabels.get(aid.end).id);
                int startIndex = indexStartWith(lines, whole + "," + startTime);
                int endIndex = indexStartWith(lines, whole + "," + endTime);
                for(int i = startIndex; i <= endIndex; i++){
                    String line = lines.get(i);
                    writer.write(line.replace(whole, whole + "-" + counter) + "," + mmsi2gear.get(whole.substring(0,9)) + "\n");
                }
                counter++;
            }
        }
    }

    public static List<NovelPoint> getLabPoints() throws IOException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
        List<String> lines = Files.readAllLines(new File(baseDir + "lab-oneweek1km-219000618-1.csv").toPath());
        List<NovelPoint> points = new ArrayList<>();
        for(String line : lines.subList(1, lines.size())){
            String[] parts = line.split(",");
            points.add(new NovelPoint(parts[0], parts[2],
                    Double.parseDouble(parts[5]),
                    Double.parseDouble(parts[6]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4])));
        }
        return points;
    }

    public static int speedlevel(double speed){
        if(speed < 1)
            return 0;
        else if (speed < 5)
            return 1;
        return 2;
    }

    public static int directionlevel(double direction, int number_of_slots){
        return (int)(direction / (360.0 / number_of_slots));
    }

    public static void novelFeatureLookBack() throws IOException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
        double spaceLimit = 2000;
        List<NovelPoint> points = getLabPoints();

        for(int k = 0; k < points.size(); k++){
            double distanceTotal = 0;
            NovelPoint np = points.get(k);
            np.setFeature(1);
            for(int q = k - 1; q >= 0; q--){
                distanceTotal += points.get(q).eucDistance(points.get(k));
                if(distanceTotal > spaceLimit)
                    break;
                NovelPoint np1 = points.get(q + 1);
                NovelPoint np2 = points.get(q);
                double farDistance = np.eucDistance(np2);
                double nearDistance = np.eucDistance(np1);
                if(nearDistance > farDistance){
                    np.setFeature(distanceTotal);
                    break;
                }
            }
        }
        try(PrintWriter writer = new PrintWriter(baseDir + "lab-oneweek1km-219000618-1-novelFeatures.csv")){
            writer.write("mmsi,t,lookback\n");
            for(NovelPoint p : points)
                writer.write(p.mmsi + "," + p.timestamp + "," + p.getFeature() + "\n");
        }
    }

    public static void novelFeatureSinuosity() throws IOException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
        double spaceLimit = 200;
        List<NovelPoint> points = getLabPoints();

        for(int k = 0; k < points.size(); k++){
            double distanceTotal = 0;
            NovelPoint np = points.get(k);
            np.setFeature(1);
            for(int q = k - 1; q >= 0; q--){
                if(! points.get(q).mmsi.equals(np.mmsi))
                    break;
                distanceTotal += points.get(q).eucDistance(points.get(q+1));
                if(distanceTotal > spaceLimit){
                    np.setFeature(distanceTotal / np.eucDistance(points.get(q)));
                    break;
                }
            }
        }
        try(PrintWriter writer = new PrintWriter(baseDir + "lab-oneweek1km-219000618-1-novelFeatures.csv")){
            writer.write("mmsi,t,lookback\n");
            for(NovelPoint p : points)
                writer.write(p.mmsi + "," + p.timestamp + "," + p.getFeature() + "\n");
        }
    }

    public static void novelFeaturesProbe() throws IOException, ParseException, InterruptedException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
        String pathRaw = baseDir + "oneweek-fishing-filtered-ge1000-tralwer-fixed-1km.csv";
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath(pathRaw)
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

        DenoiseFakeTimeStampedPointT denoiser = new DenoiseFakeTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        double spaceLimit = 5000;
        int number_of_slots = 16;
        try(PrintWriter writer = new PrintWriter(baseDir + "novel1km-probability.csv")){
            writer.write("mmsi,t,entropy");
            for(int i = 0; i < number_of_slots * 3; i++)
                writer.write("," + "_" + ((i >= 10) ? i : ("0" + i)));
            writer.write("\n");

            for(TimeStampedPointT traj : denoised){
                int[] slots = new int[traj.size()];
                for (int i = 0; i < traj.size(); i++){
                    TimeStampedPoint point = traj.getUnit(i);
                    slots[i] = speedlevel(point.getEucSpeed()) * number_of_slots + directionlevel(point.getBearing(), number_of_slots);
                }
                for(int i = 0; i < traj.size(); i++){
                    double[] distribution = new double[3 * number_of_slots];
                    Arrays.fill(distribution, 0);

                    double total = 0;
                    int left = i;
                    while (left >= 0){
                        total += traj.getUnit(left).getDistanceGap();
                        distribution[slots[left]] += 1;
                        if(total > spaceLimit)
                            break;
                        left--;
                    }

                    int population = i - Math.max(0,left) + 1;
                    for(int ii = 0; ii < distribution.length; ii++){
                        distribution[ii] /= population;
                    }

                    writer.write(traj.trajId() + "," + traj.getUnit(i).getTimestamp());
                    writer.write("," + Statistics.entropy(distribution));
                    for(double prob : distribution)
                        writer.write("," + prob);
                    writer.write("\n");
                }
            }
        }

    }

    public static void novelFeaturesBbox() throws IOException, ParseException, InterruptedException {
        String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2021-12-08-fishing-classification\\";
        String pathRaw = baseDir + "oneweek-fishing-filtered-ge1000-tralwer-fixed-1km.csv";
        File2TimestampedPointT input = new File2TimestampedPointT();
        input.filePath(pathRaw)
                .splitter(",")
                .withHeader(true)
                .trajId(0)
                .timestamp(1)
                .longitude(2)
                .latitude(3)
                .x(4)
                .y(5);
        List<TimeStampedPointT> trajs = input.go();

        DenoiseFakeTimeStampedPointT denoiser = new DenoiseFakeTimeStampedPointT();
        denoiser.setAngleThreshold(160).setHistory(7).setNumberThreads(4)
                .setSpeedMax(50).setSpeenMin(1).setTrajs(trajs)
                .setTurnThreshold(5).setSpeedAlpha(0.25);
        List<TimeStampedPointT> denoised = denoiser.go();

        double spaceLimit = 5000;
        int number_of_slots = 4;
        try(PrintWriter writer = new PrintWriter(baseDir + "novel1km-bbox.csv")){
            writer.write("mmsi,id,order,x,y,ratio,area\n");
            for(TimeStampedPointT traj : denoised){
                int id = 10;
                int i = 0;
                while (i + 1 < traj.size()){
                    double total = 0;
                    for(int ii = i + 1; ii < traj.size(); ii++){
                        total += traj.getUnit(ii).getDistanceGap();
                        if(total > spaceLimit || ii == traj.size() - 1){
                            double[] corners = bbox(traj, i, ii, number_of_slots);
                            for(int iii = 0; iii < 5; iii++) {
                                writer.write(traj.trajId() + "," + id + "," + iii + ","
                                        + corners[iii*2] + "," + corners[iii*2+1] + "," + corners[10] + "," + corners[11] + "\n");
                            }
                            id++;
                            i = ii;
                            break;
                        }
                    }
                }
            }
        }

    }

    public static double[] bbox(TimeStampedPointT traj, int start, int end, int number_of_slots){
        double[] oldCorners = new double[6];//xmin,xmax,ymin,ymax,ratio,area
        double[] newCorners = new double[6];
        int which_slot = -1;
        Arrays.fill(oldCorners, Double.MAX_VALUE);
        Arrays.fill(newCorners, 0);

        double avgLongitude = 0;
        double avgLatitude = 0;
        for(TimeStampedPoint point : traj.subList(start, end + 1)){
            avgLongitude += point.getLongitude();
            avgLatitude += point.getLatitude();
        }
        avgLongitude /= (end - start + 1);
        avgLatitude /= (end - start + 1);

        double angleStep = (Math.PI / 2) / number_of_slots;
        for(int i = 0; i < number_of_slots; i++){
            for(TimeStampedPoint point : traj.subList(start, end + 1)){
                double originalX = (point.getLongitude() - avgLongitude);
                double originalY = (point.getLatitude() - avgLatitude);
                double rotatedX = originalX * Math.cos(i * angleStep) - originalY * Math.sin(i * angleStep);
                double rotatedY = originalX * Math.sin(i * angleStep) + originalY * Math.cos(i * angleStep);
                newCorners[0] = Math.min(newCorners[0], rotatedX);
                newCorners[1] = Math.max(newCorners[1], rotatedX);
                newCorners[2] = Math.min(newCorners[2], rotatedY);
                newCorners[3] = Math.max(newCorners[3], rotatedY);
            }
            newCorners[5]  = (newCorners[1] - newCorners[0]) * (newCorners[3] - newCorners[2]);
            newCorners[4]  = (newCorners[1] - newCorners[0]) / (newCorners[3] - newCorners[2]);
            newCorners[4] = newCorners[4] > 1 ? 1 / newCorners[4] : newCorners[4];
            if(newCorners[5] < oldCorners[5]){
                double[] tmp = oldCorners;
                oldCorners = newCorners;
                newCorners = tmp;
                Arrays.fill(newCorners, 0);
                which_slot = i;
            }else {
                Arrays.fill(newCorners, 0);
            }
        }
        double answer0 = oldCorners[0] * Math.cos(-which_slot * angleStep) - oldCorners[2] * Math.sin(-which_slot * angleStep);
        double answer1 = oldCorners[0] * Math.sin(-which_slot * angleStep) + oldCorners[2] * Math.cos(-which_slot * angleStep);
        double answer2 = oldCorners[0] * Math.cos(-which_slot * angleStep) - oldCorners[3] * Math.sin(-which_slot * angleStep);
        double answer3 = oldCorners[0] * Math.sin(-which_slot * angleStep) + oldCorners[3] * Math.cos(-which_slot * angleStep);
        double answer4 = oldCorners[1] * Math.cos(-which_slot * angleStep) - oldCorners[3] * Math.sin(-which_slot * angleStep);
        double answer5 = oldCorners[1] * Math.sin(-which_slot * angleStep) + oldCorners[3] * Math.cos(-which_slot * angleStep);
        double answer6 = oldCorners[1] * Math.cos(-which_slot * angleStep) - oldCorners[2] * Math.sin(-which_slot * angleStep);
        double answer7 = oldCorners[1] * Math.sin(-which_slot * angleStep) + oldCorners[2] * Math.cos(-which_slot * angleStep);
        double answer8 = answer0;
        double answer9 = answer1;

        return new double[]{answer0 + avgLongitude,answer1 + avgLatitude,
                answer2 + avgLongitude,answer3 + avgLatitude,
                answer4 + avgLongitude,answer5 + avgLatitude,
                answer6 + avgLongitude,answer7 + avgLatitude,
                answer8 + avgLongitude,answer9 + avgLatitude,
                oldCorners[4], oldCorners[5]};
    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
//        retainFishingAIS();
//        denoise();
//        criticalConvex();
//        retainTrawlersFixed();
//        toWindowTraining();
//        toWindowTesting();
//        buildFishingParts();
//        novelFeatureSinuosity();
//        novelFeaturesProbe();
        novelFeaturesBbox();
    }
}
