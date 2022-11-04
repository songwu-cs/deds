package note.task_2022_04_22_convoy_detection;

import calculation.*;
import clustering.DBSCAN;
import clustering.DBSCANwithAngle;
import com.vividsolutions.jts.geom.Coordinate;
import convoy.s3.EvolvingConvoy;
import convoy.s3.S3;
import convoy.s3.S3NoStaticPoints;
import datetime.OneTimestamp;
import datetime.TwoTimestamp;
import gis.TransformCoordinates;
import io.LoadTrajectories;
import io.bigdata.BatchFileReader;
import io.bigdata.ExtractColumns;
import io.newcolumns.TransformXY;
import model.Point;
import model.Trajectory;
import model.TrajectoryIterator;
import org.hsqldb.lib.HsqlTaskQueue;
import org.opengis.referencing.operation.TransformException;
import songwu.deds.trajectory.algo.UnionFind;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class Task {
//    public static String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2022-04-22-convoy-detection\\";
    public static String baseDir = "H:\\UpanSky\\DEDS-DataLake\\2022-04-22-convoy-detection\\";

    public static String rawFile = "suez_data.csv";
    public static String rawFile3857 = "suez_data_EPSG3857.csv";

    public static String rawFileDK = "aisdk_oneweek_sorted.csv";
    public static String rawFileDK25832 = "dk_2021_11_14to2021_11_21_600.csv";
    private static String similarECfile = "similarEC18000.csv";

    //第一步: add x and y coordinates, from suez_data.csv to suez_data_EPSG3857.csv
    //转换完之后排序以及去重
    //suez_data.csv中的时间戳是埃及当地时间
    public static void addXY() throws TransformException, IOException {
        TransformXY transformXY = new TransformXY(true, ",", 2, 3, 4326, 25832);
        transformXY.addXY(baseDir + rawFileDK, baseDir + rawFileDK25832);
    }

    //第二步: read trajectories from file
    public static List<Trajectory> readTrajs() throws IOException, ParseException {
        LoadTrajectories loadTrajectories = new LoadTrajectories();
        loadTrajectories.filePath(baseDir + rawFile3857)
                .withHeader(true)
                .trajIndex(0)
                .datetimeIndex(1)
                .xIndex(4)
                .yIndex(5);
        return Trajectory.load(loadTrajectories);
    }

    //第三步: 检测evolving convoys
    public static void detectConvoy() throws IOException, ParseException, TransformException {
        List<Trajectory> trajectories = readTrajs();
        Map<String, Trajectory> trajMAP = new HashMap<>();
        for(Trajectory trajectory : trajectories)
            trajMAP.put(trajectory.getID(), trajectory);

        TransformCoordinates transformCoordinates = new TransformCoordinates(3857, 4326);

//        String outFile = "evolvingCluster_2021-03-15_2021-03-16_600_5_8000_6_4_6.csv";
//        DBSCAN dbscan = new DBSCAN(5, 8000);
//        S3 s3 = new S3("2021-03-15 00:00:00", "2021-03-23 00:00:00", 600,
//                trajectories, 6, 4, 6, true, dbscan);

        String outFile = "evolvingCluster_2021-03-15_2021-03-16_600_5_18000_6_4_6_NoStaticPoints_WithAngle.csv";
        DBSCAN dbscan = new DBSCANwithAngle(5, 18000, 45);
        S3 s3 = new S3NoStaticPoints("2021-03-15 00:00:00", "2021-03-23 00:00:00", 600,
                trajectories, 6, 4, 6, true, 1, dbscan);

        List<EvolvingConvoy> answer = s3.go();

        int ecID = 0;
        try(PrintWriter writer = new PrintWriter(baseDir + outFile)) {
            writer.write("ecID,t,tID,shipID,count,role,stageID,clusterID,lon,lat,locationValid\n");
            for(EvolvingConvoy convoy : answer){
                for(String line : convoy.toString(ecID + "")){
                    writer.write(line);

                    String[] parts = line.split(",");
                    if(trajMAP.get(parts[3]).contains(parts[1])){
                        double[] coord = trajMAP.get(parts[3]).getVector2(parts[1]);
                        Coordinate coordinate = transformCoordinates.go(coord[0], coord[1]);
                        writer.write("," + coordinate.x + "," + coordinate.y + "," + "yes");
                    }else
                        writer.write(",0,0,no");

                    writer.write("\n");
                }
                ecID++;
            }
        }
    }

    //第三步: 检测三个哨兵(1)
    public static List<Trajectory> readSoliders() throws ParseException {
        List<Trajectory> answer = new ArrayList<>();
        Trajectory trajectoryA = new Trajectory("a");
        Trajectory trajectoryB = new Trajectory("b");
        Trajectory trajectoryC = new Trajectory("c");
        Trajectory trajectoryD = new Trajectory("d");
        Trajectory trajectoryE = new Trajectory("e");
        List<Point> pointsA = new ArrayList<>();
        for(int i = 0; i <= 9; i++)
            pointsA.add(new Point("2021-01-01 00:0" + i + ":00", 0, 10));
        for(int i = 10; i <= 49; i++)
            pointsA.add(new Point("2021-01-01 00:" + i + ":00", -1, 0));
        List<Point> pointsB = new ArrayList<>();
        for(int i = 0; i <= 9; i++)
            pointsB.add(new Point("2021-01-01 00:0" + i + ":00", 0, 1));
        for(int i = 10; i <= 19; i++)
            pointsB.add(new Point("2021-01-01 00:" + i + ":00", 0, 10));
        for(int i = 20; i <= 49; i++)
            pointsB.add(new Point("2021-01-01 00:" + i + ":00", 0, 1));
        List<Point> pointsC = new ArrayList<>();
        for(int i = 0; i <= 9; i++)
            pointsC.add(new Point("2021-01-01 00:0" + i + ":00", 1, 0));
        for(int i = 10; i <= 19; i++)
            pointsC.add(new Point("2021-01-01 00:" + i + ":00", 1, 0));
        for(int i = 20; i <= 29; i++)
            pointsC.add(new Point("2021-01-01 00:" + i + ":00", 0, 10));
        for(int i = 30; i <= 49; i++)
            pointsC.add(new Point("2021-01-01 00:" + i + ":00", 1, 0));
        List<Point> pointsD = new ArrayList<>();
        for(int i = 0; i <= 9; i++)
            pointsD.add(new Point("2021-01-01 00:0" + i + ":00", 0, -1));
        for(int i = 10; i <= 29; i++)
            pointsD.add(new Point("2021-01-01 00:" + i + ":00", 0, -1));
        for(int i = 30; i <= 39; i++)
            pointsD.add(new Point("2021-01-01 00:" + i + ":00", 0, 10));
        for(int i = 40; i <= 49; i++)
            pointsD.add(new Point("2021-01-01 00:" + i + ":00", 0, -1));
        List<Point> pointsE = new ArrayList<>();
        for(int i = 0; i <= 9; i++)
            pointsE.add(new Point("2021-01-01 00:0" + i + ":00", -1, 0));
        for(int i = 10; i <= 19; i++)
            pointsE.add(new Point("2021-01-01 00:" + i + ":00", 0, 1));
        for(int i = 20; i <= 29; i++)
            pointsE.add(new Point("2021-01-01 00:" + i + ":00", 1, 0));
        for(int i = 30; i <= 39; i++)
            pointsE.add(new Point("2021-01-01 00:" + i + ":00", 0, -1));
        for(int i = 40; i <= 49; i++)
            pointsE.add(new Point("2021-01-01 00:" + i + ":00", 0, 10));
        trajectoryA.setPoints(pointsA); answer.add(trajectoryA);
        trajectoryB.setPoints(pointsB); answer.add(trajectoryB);
        trajectoryC.setPoints(pointsC); answer.add(trajectoryC);
        trajectoryD.setPoints(pointsD); answer.add(trajectoryD);
        trajectoryE.setPoints(pointsE); answer.add(trajectoryE);
        return answer;
    }

    //第三步: 检测三个哨兵(2)
    public static void detectConvoyToyDataset() throws IOException, ParseException {
        List<Trajectory> trajectories = readSoliders();

        String outFile = "evolvingCluster_soliders.csv";
        DBSCAN dbscan = new DBSCAN(4, 2);
        S3 s3 = new S3("2021-01-01 00:00:00", "2021-01-01 00:49:00", 60,
                trajectories, 2, 10, 20, true, dbscan);

        List<EvolvingConvoy> answer = s3.go();

        int ecID = 0;
        try(PrintWriter writer = new PrintWriter(baseDir + outFile)) {
            writer.write("ecID,t,tID,shipID,count,role,stageID\n");
            for(EvolvingConvoy convoy : answer){
                for(String line : convoy.toString(ecID + "")){
                    writer.write(line); writer.write("\n");
                }
            }
        }
    }

    //第四步: 生成原始轨迹坐标文件
    public static void originalCoords() throws Exception {
        List<Trajectory> trajs = readTrajs();
        TransformCoordinates transformCoordinates = new TransformCoordinates(3857, 4326);
        String outFile = "originalCoords.csv";
        int gapInSeconds = 600;
        double headingDefault = 0;
        double speedDefault = 100;

        try(PrintWriter writer = new PrintWriter(baseDir + outFile)){
            writer.write("timestamp,ship,x,y,lon,lat,speedInKnots,heading\n");
            for(String ts = "2021-03-15 00:00:00"; ts.compareTo("2021-03-23 00:00:00") <= 0;){
                String tsCOPY = ts;
                String lastTS = OneTimestamp.add(ts, 0, 0, -1 * gapInSeconds, OneTimestamp.formatter1);
                List<Trajectory> snapshot = ListGeneric.filter(trajs, t -> t.contains(tsCOPY));

                for(Trajectory traj : snapshot){
                    double speed = 0;
                    double heading = 0;
                    double[] xy = traj.getVector2(ts);
                    if(traj.contains(ts) && traj.contains(lastTS)){
                        double[] from = traj.getVector2(lastTS);
                        double[] to = traj.getVector2(ts);
                        xy = to;
                        double[] coord1 = traj.getVector2(lastTS);
                        double[] coord2 = traj.getVector2(ts);
                        double distance = ArrayDoubleTwo.euclidean(coord1, coord2);
                        speed = distance / gapInSeconds * 3.6 / 1.852;
                        if(coord1[0] == coord2[0] && coord1[1] == coord2[1])
                            heading = headingDefault;
                        else
                            heading = ArrayDoubleTwo.heading(from, to);
                    }
                    else{heading = headingDefault; speed = speedDefault;}

                    Coordinate coord = transformCoordinates.go(xy[0], xy[1]);
                    writer.write(String.join(",", ts, traj.getID(),
                            xy[0]+"", xy[1]+"", coord.x+"", coord.y+"",
                            speed+"", heading+""));
                    writer.write("\n");
                }
                ts = OneTimestamp.add(ts, 0, 0, gapInSeconds, OneTimestamp.formatter1);
            }
        }

    }

    //第五步: 检查哪些EC高度相似
    public static void similarEC() throws Exception{
        Map<String, Map<String, String>> ec2cluster = new HashMap<>();
        Map<String, Map<String, Set<String>>> ec2members = new HashMap<>();

        try (BatchFileReader reader = new BatchFileReader(baseDir + "evolvingCluster_2021-03-15_2021-03-16_600_5_18000_6_4_6_NoStaticPoints_WithAngle.csv", ",", true, 0, 1)){
            for(List<String> lines : reader){
                String ecID = lines.get(0).split(",")[0];
                String timestamp = lines.get(0).split(",")[1];
                String clusterID = lines.get(0).split(",")[7];
                Set<String> members = new HashSet<>();
                for(String line : lines){
                    members.add(line.split(",")[3]);
                }

                if(! ec2cluster.containsKey(ecID))
                    ec2cluster.put(ecID, new HashMap<>());
                if(! ec2members.containsKey(ecID))
                    ec2members.put(ecID, new HashMap<>());
                ec2cluster.get(ecID).put(timestamp, clusterID);
                ec2members.get(ecID).put(timestamp, members);
            }
        }

        try(PrintWriter writer = new PrintWriter(baseDir + similarECfile)) {
            writer.write("ec1,ec2,similarCluster,similarMembers\n");
            for(String ec1 : ec2cluster.keySet()){
                for(String ec2 : ec2cluster.keySet()){
                    double sCluster = similarCluster(ec2cluster.get(ec1), ec2cluster.get(ec2));
                    double sMember = similarMembers(ec2members.get(ec1), ec2members.get(ec2));
                    writer.write(String.join(",", ec1, ec2, sCluster+"", sMember+""));
                    writer.write("\n");
                }
            }
        }
    }

    //第五步: 辅助1
    public static double similarCluster(Map<String, String> clu1, Map<String, String> clu2){
        Set<String> allTimestamps = new HashSet<>();
        allTimestamps.addAll(clu1.keySet());
        allTimestamps.addAll(clu2.keySet());
        int counter = 0;
        for(String ts : clu1.keySet()){
            if(clu2.containsKey(ts))
                counter += clu1.get(ts).equals(clu2.get(ts)) ? 1 : 0;
        }
        return 1.0 * counter / allTimestamps.size();
    }

    //第五步: 辅助2
    public static double similarMembers(Map<String, Set<String>> members1, Map<String, Set<String>> members2){
        Set<String> allTimestamps = new HashSet<>();
        allTimestamps.addAll(members1.keySet());
        allTimestamps.addAll(members2.keySet());
        double counter = 0;
        for(String ts : members1.keySet()){
            if(members2.containsKey(ts)){
                Set<String> m1 = members1.get(ts);
                Set<String> m2 = members2.get(ts);

                Set<String> members = SetGeneric.copy(m1);
                members.addAll(m2);
                int total = members.size();
                int common = m1.size() + m2.size() - total;
                counter += 1.0 * common / total;
            }
        }
        return counter / allTimestamps.size();
    }

    //第六步 合并相似的EC
    public static void mergeSimilarEC() throws IOException {
        Map<String, Map<String, String>> ec2cluster = new HashMap<>();
        Map<String, Map<String, Set<String>>> ec2members = new HashMap<>();
        try (BatchFileReader reader = new BatchFileReader(baseDir + "evolvingCluster_2021-03-15_2021-03-16_600_5_18000_6_4_6_NoStaticPoints_WithAngle.csv", ",", true, 0, 1)){
            for(List<String> lines : reader){
                String ecID = lines.get(0).split(",")[0];
                String timestamp = lines.get(0).split(",")[1];
                String clusterID = lines.get(0).split(",")[7];
                Set<String> members = new HashSet<>();
                for(String line : lines){
                    members.add(line.split(",")[3]);
                }

                if(! ec2cluster.containsKey(ecID))
                    ec2cluster.put(ecID, new HashMap<>());
                if(! ec2members.containsKey(ecID))
                    ec2members.put(ecID, new HashMap<>());
                ec2cluster.get(ecID).put(timestamp, clusterID);
                ec2members.get(ecID).put(timestamp, members);
            }
        }

        UnionFind unionFind = new UnionFind(ec2cluster.size());
        for(String ec1 : ec2cluster.keySet()){
            for(String ec2 : ec2cluster.keySet()){
                if(! ec1.equals(ec2)){
                    Set<String> allTimestamps = new HashSet<>();
                    allTimestamps.addAll(ec2cluster.get(ec1).keySet());
                    allTimestamps.addAll(ec2cluster.get(ec2).keySet());
                    List<String> allTS = new ArrayList<>();
                    allTS.addAll(allTimestamps);
                    Collections.sort(allTS);

                    boolean[] isSame = new boolean[allTS.size()];
                    Map<String, String> ts2cluster1 = ec2cluster.get(ec1);
                    Map<String, String> ts2cluster2 = ec2cluster.get(ec2);
                    for(int i = 0; i < allTS.size(); i++){
                        if(ts2cluster1.containsKey(allTS.get(i)) &&
                            ts2cluster2.containsKey(allTS.get(i)) &&
                            ts2cluster1.get(allTS.get(i)).equals(ts2cluster2.get(allTS.get(i))))
                            isSame[i] = true;
                    }
                    List<Integer> isSameRunLength = Array1DBoolean.runLengthEncoding(isSame);
                    if (ListInteger.sum(isSameRunLength) >= 0)
                        unionFind.union(Integer.parseInt(ec1), Integer.parseInt(ec2));
                }
            }
        }

        System.out.println(Arrays.toString(unionFind.status()));
        int[] status = unionFind.status();
        try(PrintWriter writer = new PrintWriter(baseDir + "mergedEC18000.csv")){
            writer.write("ecID,t,shipID,lon,lat,locationValid\n");
            List<String> lines = Files.readAllLines(Paths.get(baseDir + "evolvingCluster_2021-03-15_2021-03-16_600_5_18000_6_4_6_NoStaticPoints_WithAngle.csv"));
            Set<String> mergedSet = new HashSet<>();
            for(String line : lines.subList(1, lines.size())){
                line = UnitString.subset(line, ",", 0, 1, 3, 8, 9, 10);
                String[] parts = line.split(",");
                parts[0] = status[Integer.parseInt(parts[0])] + "";
                mergedSet.add(String.join(",", parts));
            }
            List<String> mergedList = new ArrayList<>(mergedSet);
            Collections.sort(mergedList);
            for(String s : mergedList)
                writer.write(s + "\n");
        }
    }

    //第七步: prepare 2021-11-20丹麦数据集
    public static void prepareDenmark() throws IOException {
        ExtractColumns extractColumns = new ExtractColumns(true);
        extractColumns.extract(baseDir + rawFileDK, baseDir + rawFileDK25832, "timestamp,mmsi,lat,lon", 0,2,3,4);
    }

    //第七步: read trajectories from file丹麦数据集
    public static List<Trajectory> readTrajsDenmark() throws IOException, ParseException {
        LoadTrajectories loadTrajectories = new LoadTrajectories();
        loadTrajectories.filePath(baseDir + rawFileDK25832)
                .withHeader(true)
                .trajIndex(0)
                .datetimeIndex(1)
                .xIndex(2)
                .yIndex(3)
                .regularSampleGap("2021-11-14 00:00:00", "2021-11-21 00:00:00", 600);
        return Trajectory.load(loadTrajectories);
    }

    //第七步: 检测evolving convoys丹麦
    public static void detectConvoyDenmark() throws IOException, ParseException, TransformException {
        List<Trajectory> trajectories = readTrajsDenmark();

        Map<String, Trajectory> trajMAP = new HashMap<>();
        for(Trajectory trajectory : trajectories)
            trajMAP.put(trajectory.getID(), trajectory);

        TransformCoordinates transformCoordinates = new TransformCoordinates(25832, 4326);

//        String outFile = "dk_2021-11-14_2021-11-21_600_5_10000_6_6_13_NoStaticPoints_WithAngle45.csv";
        String outFile = "out.csv";

        DBSCAN dbscan = new DBSCANwithAngle(5, 10000, 45);
        S3 s3 = new S3NoStaticPoints("2021-11-14 00:00:00", "2021-11-21 00:00:00", 600,
                trajectories, 6, 6, 13, true, 1, dbscan);

        List<EvolvingConvoy> answer = s3.go();

        int ecID = 0;
        try(PrintWriter writer = new PrintWriter(baseDir + outFile)) {
            writer.write("ecID,t,tID,shipID,count,role,stageID,clusterID,lon,lat,locationValid\n");
            for(EvolvingConvoy convoy : answer){
                for(String line : convoy.toString(ecID + "")){
                    writer.write(line);

                    String[] parts = line.split(",");
                    if(trajMAP.get(parts[3]).contains(parts[1])){
                        double[] coord = trajMAP.get(parts[3]).getVector2(parts[1]);
                        Coordinate coordinate = transformCoordinates.go(coord[0], coord[1]);
                        writer.write("," + coordinate.x + "," + coordinate.y + "," + "yes");
                    }else
                        writer.write(",0,0,no");

                    writer.write("\n");
                }
                ecID++;
            }
        }
    }

    //第七步,预先保存均匀采样的轨迹
    public static void unifromSamplingDenmark() throws IOException {
        LoadTrajectories loadTrajectories = new LoadTrajectories();
        loadTrajectories.filePath(baseDir + rawFileDK25832)
                .withHeader(true)
                .trajIndex(0)
                .datetimeIndex(1)
                .xIndex(3)
                .yIndex(4)
                .regularSampleGap("2021-11-14 00:00:00", "2021-11-21 00:00:00", 600);

        try(TrajectoryIterator iterator = new TrajectoryIterator(loadTrajectories);
            PrintWriter writer = new PrintWriter(baseDir + "dk_2021_11_14to2021_11_21_600.csv")
        ) {
            int counter = 0;

            writer.write("mmsi,timestamp,x,y\n");
            for(Trajectory trajectory : iterator){
                String id = trajectory.getID();
                int size = trajectory.size();
                for(int i = 0; i < size; i++){
                    Point p = trajectory.getPoint(i);
                    writer.write(String.join(",", id, p.getDatetimeStr(), p.getX()+"", p.getY()+""));
                    writer.write("\n");
                }
                System.out.println(++counter);
            }
        }
    }

    public static void shiptypeExtract() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(baseDir + "aisdk_oneweek_shiptype.csv"));
        lines = lines.subList(1, lines.size());
        try(PrintWriter writer = new PrintWriter(baseDir + "aisdk_oneweek_shiptype2.csv")){
            writer.write("mmsi,shiptype\n");
            Map<String,String> map = new HashMap<>();

            for(String line : lines){
                String[] parts = line.split(",");
                if(map.containsKey(parts[0]) && map.get(parts[0]).equals("Undefined")){
                    map.put(parts[0], parts[1]);
                }else
                    map.put(parts[0], parts[1]);
            }

            for(String s : map.keySet()){
                writer.write(s + "," + map.get(s) + "\n");
            }
        }
    }

    public static void shiptypeOneMonth() throws IOException {
        BatchFileReader reader = new BatchFileReader("H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\aisdk_onemonth_sorted.csv",
                ",", true, 0);
        try(PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\aisdk_onemonth_shiptype.csv")){
            writer.write("mmsi,shiptype\n");

            for(List<String> lines : reader){
                int max = 0;
                String type = "";
                String mmsi = lines.get(0).split(",")[0];
                Map<String, Integer> counter = new HashMap<>();

                for(String line : lines){
                    String[] parts = line.split(",");
                    String shiptype = parts[4];
                    shiptype = (shiptype == null) ? "NULL" : shiptype;
                    counter.put(shiptype, counter.getOrDefault(shiptype, 0) + 1);
                    if((!type.equals(shiptype)) && max <= counter.get(shiptype)){
                        max = counter.get(shiptype);
                        type = shiptype;
                    }
                }

                writer.write(mmsi + "," + type + "\n");
            }
        }
    }

    public static void splitOneMonthByShiptype() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\aisdk_onemonth_shiptype.csv"));
        Map<String, String> map = new HashMap<>();
        for(String line : lines){
            String[] parts = line.split(",");
            map.put(parts[0], parts[1]);
        }

        BatchFileReader reader = new BatchFileReader("H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\aisdk_onemonth_sorted.csv",
                ",", true, 2);
        try(PrintWriter writer = new PrintWriter("H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\aisdk_onemonth_sorted_cargoAllColumns.csv")){
            writer.write("timestamp,typeOfMobile,mmsi,latitude,longitude,navigationalStatus,ROT,SOG,COG,heading,IMO,Callsign,Name,ShipType,CargoType,Width,Length,TypeOfPositionFixingDevice,Draught,Destination,ETA,DataSourceType,A,B,C,D\n");

            for(List<String> traj : reader){
                String mmsi = traj.get(0).split(",")[2];
                if(map.get(mmsi).equals("Cargo") || map.get(mmsi).equals("cargo")){
                    writer.write(String.join("\n", traj));
                    writer.write("\n");
                }
            }
        }
    }

    public static void distanceBetweenMilitary() throws IOException, ParseException {
        List<Trajectory> trajs = readTrajsDenmark();

        Trajectory ship1 = ListGeneric.filter(trajs, t->t.getID().equals("316143000")).get(0);
        Trajectory ship2 = ListGeneric.filter(trajs, t->t.getID().equals("263024000")).get(0);
        Trajectory ship3 = ListGeneric.filter(trajs, t->t.getID().equals("257092200")).get(0);

        int counter = 0;
        double sum = 0;
        for(String ts = "2021-11-14 19:00:00"; ts.compareTo("2021-11-15 00:00:00") <= 0;
            ts = OneTimestamp.add(ts, 0, 0, 600, OneTimestamp.formatter1)){

            ++counter;
            double[] coord1 = ship1.getVector2(ts);
            double[] coord2 = ship2.getVector2(ts);
            double[] coord3 = ship3.getVector2(ts);

            sum += ArrayDoubleTwo.euclidean(coord1, coord2);
            sum += ArrayDoubleTwo.euclidean(coord3, coord2);
        }

        System.out.println(sum / 2 / counter);
    }

    public static void main(String[] args) throws Exception {
//        addXY();
//        detectConvoy();
//        originalCoords();
//        similarEC();
//        mergeSimilarEC();

//        prepareDenmark();
//        addXY();
//        unifromSamplingDenmark();
//        shiptypeExtract();
//        detectConvoyDenmark();
//        distanceBetweenMilitary();
//        shiptypeOneMonth();
        splitOneMonthByShiptype();


    }
}
