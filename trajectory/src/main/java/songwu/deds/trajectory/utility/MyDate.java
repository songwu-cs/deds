package songwu.deds.trajectory.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyDate {
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static int timestampDiff(String t1, String t2) throws ParseException {
        Date d1 = formatter.parse(t1);
        Date d2 = formatter.parse(t2);
        return (int)((d1.getTime() - d2.getTime()) / 1000);
    }

    public static void main(String[] args) throws ParseException {
        System.out.println(timestampDiff("2021-01-01 00:00:00", "2021-01-01 01:00:00"));
    }
}
