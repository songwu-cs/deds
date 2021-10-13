package songwu.deds.trajectory.test;

import songwu.deds.trajectory.data.TimeStampedPoint;

public class TestData {
    public static void test_signed_turn(){
        TimeStampedPoint p1 = new TimeStampedPoint(100,100);
        TimeStampedPoint p2 = new TimeStampedPoint(100,100);
        p1.bearing(20); p2.bearing(60);
        System.out.println( TimeStampedPoint.signed_turn(p1, p2) );
        p1.bearing(60); p2.bearing(20);
        System.out.println( TimeStampedPoint.signed_turn(p1, p2) );
        p1.bearing(20); p2.bearing(300);
        System.out.println( TimeStampedPoint.signed_turn(p1, p2) );
        p1.bearing(300); p2.bearing(20);
        System.out.println( TimeStampedPoint.signed_turn(p1, p2) );
    }

    public static void test_geography_angle(){
        TimeStampedPoint p1 = new TimeStampedPoint(100,100);
        TimeStampedPoint p2 = new TimeStampedPoint(100,100);
        p1.longitude(0).latitide(0); p2.longitude(1).latitide(1); p2.geography_angle(p1);
        System.out.println(p2.getBearing());
        p1.longitude(0).latitide(1); p2.longitude(1).latitide(0); p2.geography_angle(p1);
        System.out.println(p2.getBearing());
        p1.longitude(1).latitide(1); p2.longitude(0).latitide(0); p2.geography_angle(p1);
        System.out.println(p2.getBearing());
        p1.longitude(1).latitide(0); p2.longitude(0).latitide(1); p2.geography_angle(p1);
        System.out.println(p2.getBearing());
    }
    public static void main(String[] args) {
//        test_geography_angle();
//        test_signed_turn();
    }
}
