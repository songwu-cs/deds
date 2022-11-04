package noteAAU.task_2022_09_06_CO2.EmissionModels;

import java.util.HashMap;
import java.util.Map;

public class NTNU_ResistanceAllWeather extends ModelInit{
    Map<String, Map<String, Double>> answer = new HashMap<>();
    Map<String, Double> currentShip;
    public static final double rhoSEA = 1.025; //tons per m3
    public static final double gravity = 9.8; // m per s3
    public static final double airDensity = 0.0012; //ton per m3
    public static final double viscosity = 0.00000118; //m2 per s

    public NTNU_ResistanceAllWeather() {
    }

    @Override
    public String emission(AISsegment segment) {//in kilograms
        if((currentShip = answer.get(segment.mmsi)) != null){

        }
        return super.emission(segment);
    }

    private double totalResistance(AISsegment s){
        double speed = speedCorrectedByCurrent(s);
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        double LWL = calcLWL(s);
        double displacement = calcDisplacement(s);

        double item1 = 0.5 * rhoSEA * speed * speed * calcCF(s) * calcSbh(s);

        double item2 = 0.93 + 0.487118 * calcc14() * Math.pow(breadth / LWL, 1.06806)
                * Math.pow(draught / LWL, 0.46106) * Math.pow(LWL / calcLR(s), 0.121563)
                * Math.pow(LWL * LWL * LWL / displacement, 0.36486)
                * Math.pow(1 - calcCP(s), -0.60247);

        double item3 = 0.5 * rhoSEA * speed * speed * calcSapp() * calcOnePlusK2() * calcCF(s);

        double froudeNumber = calcFroudeNumber(s);
        double item4 = rhoSEA * gravity * displacement * calcc1(s) * calcc2(s) * calcc5(s)
                * Math.exp(calcm1(s) * Math.pow(froudeNumber, calcd()) + calcm2(s) * Math.cos(calcLambda(s) * Math.pow(froudeNumber, -2)));

        double item5 = 0.11 * Math.exp(-3.0 * Math.pow(calcpb(s), -2)) * Math.pow(calcfni(s), 3) * Math.pow(calcABT(s), 1.5) * rhoSEA * gravity
                / (1 + Math.pow(calcfni(s), 2));

        double item6 = 0.5 * rhoSEA * speed * speed * calcAT(s) * calcc6(s);

        double item7 = 0.5 * rhoSEA * speed * speed * calcS(s) * calcCA(s);

        return 0;
    }

    private double calcS(AISsegment s){
        double LWL = calcLWL(s);
        double draught = currentShip.get(attrDRAUGHT);
        double breadth = currentShip.get(attrBreadth);
        double cm = calcCM(s);
        double cb = calcCB(s);
        double cwp = calcCWP(s);
        double abt = calcABT(s);
        return LWL * (2 * draught + breadth) * Math.sqrt(cm) * (0.453 + 0.4425 * cb - 0.2862 * cm - 0.003467 * (breadth / draught) + 0.3696 * cwp) + 2.38 * abt / cb;
    }

    private double calcCA(AISsegment s){
        double LWL = calcLWL(s);
        double cb = calcCB(s);
        return 0.006 * Math.pow(100.0 + LWL, -0.16) - 0.00205 + 0.003 * Math.sqrt(LWL / 7.5) * Math.pow(cb, 4) * calcc1(s) * (0.04 - calcc4(s));
    }

    private double calcDisplacement(AISsegment s){
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        return calcLWL(s) * breadth * draught * calcCB(s);
    }

    private double calcpb(AISsegment s){
        double abt = calcABT(s);
        double draught = currentShip.get(attrDRAUGHT);
        double cob = calcCobKeel(s);
        return 0.56 * Math.sqrt(abt) / (draught - 1.5 * cob);
    }

    private double calcfni(AISsegment s){
        double abt = calcABT(s);
        double draught = currentShip.get(attrDRAUGHT);
        double cob = calcCobKeel(s);
        return speedCorrectedByCurrent(s) / Math.sqrt(gravity * (draught - cob - 0.25 * Math.sqrt(abt)) + 0.15 * Math.pow(speedCorrectedByCurrent(s), 2));
    }

    private double calcLCB(AISsegment s){
        return -0.75 * (currentShip.get(attrLBP) / 2.0) / 100.0;
    }

    private double calcc14(){
        return 1 + 0.011 * 10;
    }

    private double calcSapp(){
        return 50;
    }

    private double calcOnePlusK2(){
        return 1.5;
    }

    private double calcm1(AISsegment s){
        double LWL = calcLWL(s);
        double displacement = calcDisplacement(s);
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        return 0.0140407 * LWL / draught - 1.75254 * Math.pow(displacement, 1.0 / 3.0) / LWL - 4.79323 * breadth / LWL - calcc16(s);
    }

    private double calcm2(AISsegment s){
        double cp = calcCP(s);
        return calcc15(s) * Math.pow(cp, 2.0) * Math.exp(-0.1 * Math.pow(calcFroudeNumber(s), -2.0));
    }

    private double calcd(){
        return -0.9;
    }

    private double calcLambda(AISsegment s){
        double LWL = calcLWL(s);
        double breadth = currentShip.get(attrBreadth);
        double cp = calcCP(s);
        if(LWL / breadth < 12.0)
            return 1.446 * cp - 0.03 * LWL / breadth;
        return 1.446 * cp - 0.36;
    }

    private double calcLR(AISsegment s){
        return calcLWL(s) * (1 - calcCP(s) + 0.06 * calcCP(s) * calcLCB(s) / (4 * calcCP(s) - 1));
    }

    private double calcCP(AISsegment s){
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        return calcDisplacement(s) / (calcCM(s) * breadth * draught * calcLWL(s));
    }

    private double calcSbh(AISsegment s){
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        return calcLWL(s) * (2 * draught + breadth) * Math.sqrt(calcCM(s))
                * (0.453 + 0.4425 * calcCB(s) - 0.2862 * calcCM(s) - 0.003467 * (breadth / draught) + 0.3696 * calcCWP(s)) + 2.38 * calcABT(s) / calcCB(s);
    }

    private double calcLWL(AISsegment s){
        return currentShip.get(attrLBP) / 0.97;
    }

    private double calcCF(AISsegment s){
        return 0.075 / Math.pow(Math.log10(calcReynoldsNumber(s)) - 2, 2);
    }

    private double calcCB(AISsegment s){
        double froudeNumber = calcFroudeNumber(s);
        return -4.22 + 27.8 * Math.sqrt(froudeNumber) - 39.1 * froudeNumber + 46.6 * Math.pow(froudeNumber, 3);
    }

    private double calcFroudeNumber(AISsegment s){
        double speedByCurrent = speedCorrectedByCurrent(s);
        return speedByCurrent / Math.sqrt(gravity * calcLWL(s));
    }

    private double calcReynoldsNumber(AISsegment s){
        double speed = speedCorrectedByCurrent(s);
        return speed * calcLWL(s) / 0.00000118831;
    }

    private double calcCM(AISsegment s){
        return 0.977 + 0.085 * (calcCB(s) - 0.6);
    }

    private double calcCWP(AISsegment s){
        return (1 + 2 * calcCB(s)) / 2.0;
    }

    private double calcAM(AISsegment s){
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        return breadth * draught * calcCM(s);
    }

    private double calcABT(AISsegment s){
        return 0.08 * calcAM(s);
    }

    private double calcAT(AISsegment s){
        return 0.051 * calcAM(s);
    }

    private double calcc1(AISsegment s){
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        double ie = calcie(s);
        return 2223105.0 * Math.pow(calcc7(), 3.78613) * Math.pow(draught / breadth, 1.07961) * Math.pow(90.0 - ie, -1.37566);
    }

    private double calcc2(AISsegment s){
        return Math.exp(-1.89 * Math.sqrt(calcc3(s)));
    }

    private double calcc3(AISsegment s){
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);

        double abt = calcABT(s);
        double centerofbulb = calcCobKeel(s);
        return 0.56 * Math.pow(abt, 1.5) / (breadth * draught * (0.31 * Math.sqrt(abt) + draught - centerofbulb));
    }

    private double calcCobKeel(AISsegment s){
        return currentShip.get(attrDRAUGHT) * 0.4;
    }

    private double calcfnt(AISsegment s){
        double at = calcAT(s);
        double breadth = currentShip.get(attrBreadth);
        double cwp = calcCWP(s);
        return speedCorrectedByCurrent(s) / Math.sqrt(2 * gravity * at / (breadth + breadth * cwp));
    }

    private double calcc4(AISsegment s){
        double LWL = calcLWL(s);
        double draught = currentShip.get(attrDRAUGHT);
        if(draught / LWL <= 0.04)
            return draught / LWL;
        return 0.04;
    }

    private double calcc5(AISsegment s){
        double at = calcAT(s);
        double cm = calcCM(s);
        double breadth = currentShip.get(attrBreadth);
        double draught = currentShip.get(attrDRAUGHT);
        return 1.0 - 0.8 * at / (breadth * draught * cm);
    }

    private double calcc6(AISsegment s){
        double fnt = calcfnt(s);
        if (fnt < 5.0)
            return  0.2 * (1.0 - 0.2 * fnt);
        return 0.0;
    }

    private double calcc7(){
        double breadth = currentShip.get(attrBreadth);
        double LWL = currentShip.get(attrLBP) / 0.97;
        if((breadth / LWL) < 0.11){
            return 0.229577 * Math.pow(breadth / LWL, 0.33333);
        }else if (breadth / LWL < 0.25){
            return breadth / LWL;
        }else
            return 0.5 - 0.625 * (breadth / LWL);
    }

    private double calcc15(AISsegment s){
        double disp = calcDisplacement(s);
        double LWL = calcLWL(s);
        if(Math.pow(LWL, 3) / disp < 512)
            return  -1.69385;
        else if (Math.pow(LWL, 3) / disp > 1727.0) {
            return 0;
        }else
            return -1.69385 + (LWL / Math.pow(disp, 1.0 / 3.0) - 8.0) / 2.36;
    }

    private double calcc16(AISsegment s){
        double cp = calcCP(s);
        if(cp < 0.8){
            return 8.07981 * cp - 13.8673 * Math.pow(cp, 2.0) + 6.984388 * Math.pow(cp, 3);
        }else
            return 1.73014 - 0.7067 * cp;
    }

    private double calcie(AISsegment s){
        double breadth = currentShip.get(attrBreadth);
        double lpp = currentShip.get(attrLBP);
        double LWL = calcLWL(s);
        double cwp = calcCWP(s);
        double cp = calcCP(s);
        double lcb = calcLCB(s);
        double lr = calcLR(s);
        double displacement = calcDisplacement(s);

        return 1 + 89.0 * Math.exp(-1 * Math.pow(LWL / breadth, 0.80856)
            * Math.pow(1 - cwp, 0.30484) * Math.pow(1 - cp - 0.0225 * lcb, 0.6367)
            * Math.pow(lr / breadth, 0.34574) * Math.pow(100.0 * displacement / Math.pow(LWL, 3), 0.16302)
        );
    }
    private double propellerEfficiency(){

        return 0;
    }

    private double additionalResistance(){

        return 0;
    }

    private double speedCorrectedByCurrent(AISsegment segment){// in meters / second
        double northS = segment.speed * Math.cos(segment.heading) * 1852 / 3600;
        double eastS = segment.speed * Math.sin(segment.heading) * 1852 / 3600;
        double currentNorth = currentSpeedNorthward(segment.midTimestamp, segment.midLongitude, segment.midLatitude);
        double currentEast = currentSpeedEastward(segment.midTimestamp, segment.midLongitude, segment.midLatitude);
        double northCorrected = northS - currentNorth;
        double eastCorrected = eastS - currentEast;
        return Math.sqrt(Math.pow(northCorrected, 2) + Math.pow(eastCorrected, 2));
    }

    //in meters / second
    private double currentSpeedEastward(String t, double lon, double lat){
        return 1;
    }
    //in meters / second
    private double currentSpeedNorthward(String t, double lon, double lat){
        return 1;
    }
}
