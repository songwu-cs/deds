package note.task_2022_06_20_denmark_integration;

import db.pg.DenmarkCoast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Task {
    public static final String base_dir = "H:\\UpanSky\\DEDS-DataLake\\2022-06-20-Denmark-Integration\\";
    public static final String base_dir2 = "H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\";


    //step 1：添加distance to shore
    public static void distToShore() throws IOException, SQLException {
        List<String> lines = Files.readAllLines(Paths.get(base_dir + "aisdk_onemonth_four_passengers.csv"));

        double lonMIN = 5, lonMAX = 20, latMIN = 52, latMAX = 60;

        try(PrintWriter writer = new PrintWriter(base_dir + "aisdk_onemonth_four_passengers_xy_dist.csv");
            DenmarkCoast denmarkCoast = new DenmarkCoast("localhost",
                    "bmda22",
                    "postgres",
                    "wusong",
                    25832,
                    100,
                    "denmark_administrative_national_boundary",
                    "geom25832");){
            writer.write("mmsi,timestamp,longitude,latitude,shiptype,status,x,y,distanceToShore\n");

            for(String line : lines.subList(1, lines.size())){
                String[] parts = line.split(",");
                double lon = Double.parseDouble(parts[2]);
                double lat = Double.parseDouble(parts[3]);
                if(lon >= lonMIN && lon <= lonMAX && lat >= latMIN && lat <= latMAX){
                    String coord = denmarkCoast.getDenmarkCoordinate(lon, lat);
                    double x = Double.parseDouble(coord.split(",")[0]);
                    double y = Double.parseDouble(coord.split(",")[1]);
                    double distance = denmarkCoast.getDistanceToShore(x, y);
                    writer.write(String.join(",", line, x+"", y+"", distance+"") + "\n");
                }
            }
        }
    }

    //step 2: 抽取船只尺寸信息
    public static void shipHeightWidth() throws IOException {
        try(PrintWriter writer = new PrintWriter(base_dir2 + "aisdk_onemonth_shipHeightWidth.csv")) {
            HashMap<String, HashSet<String>> database = new HashMap<>();
            for(int i = 0; i <= 102; i++){
                List<String> lines = Files.readAllLines(Paths.get(base_dir2 + "tmp" + i));
                for(String line : lines){
                    String[] parts = line.split(",");
                    if(! database.containsKey(parts[2]))
                        database.put(parts[2], new HashSet<>());
                    database.get(parts[2]).add(parts[15] + "," + parts[16]);
                }

                System.out.println(i);
            }

            for(String ship : database.keySet()){
                for(String item : database.get(ship)){
                    writer.write(ship + "," + item + "\n");
                }
            }
        }
    }

    public static void main(String[] args) throws Exception{
//        distToShore();
//        shipHeightWidth();
    }
}
