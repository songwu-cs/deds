package noteAAU.task_2022_09_16_6months;

import datetime.SimpleDateFormatExt;
import datetime.TwoTimestamp;
import io.LoadTrajectories;
import model.Point;
import model.Trajectory;
import model.TrajectoryIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;

public class LargeScaleViz {
    public static String workDIR = "H:\\UpanSky\\DEDS-DataLake\\suez-canal\\six-months\\";

//    public static void sampling(String flag, int gap) throws ParseException, IOException {
//        LoadTrajectories loadConfig = new LoadTrajectories();
//        loadConfig.filePath(workDIR + "rn-806-pos-shiptype-status-flag.csv")
//                .withHeader(true)
//                .xIndex(2).yIndex(3)
//                .datetimeIndex(1)
//                .trajIndices(new int[]{0,14})
//                .regularSampleGap("1625004000", "1641013200", gap);
//        Point.setDateTimeFormatter(SimpleDateFormatExt.UNIXSTAMP);
//
//        try(PrintWriter writer = new PrintWriter(workDIR + "rn-806-pos-" + gap + "s-" + flag + ".csv")) {
//            writer.write("ship_hash," + flag + ",unixstamp,lon,lat\n");
//
//            for(Trajectory trajectory : new TrajectoryIterator(loadConfig)){
//                String id = trajectory.getID();
//
//                int size = trajectory.size();
//                for(int i = 0; i < size; i++){
//                    Point p = trajectory.getPoint(i);
//                    writer.write(String.join(",", id, p.getDatetimeStr(), p.getX()+"", p.getY()+""));
//                    writer.write("\n");
//                }
//            }
//        }
//    }
    public static void main(String[] args) throws IOException, ParseException {
//        sampling("flag4", 60);
    }
}
