package noteAAU.task_2022_09_06_CO2.EmissionModels;

import noteAAU.task_2022_09_06_CO2.CO2emission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class STEAM extends ModelInit{
    private Map<String, Map<String, Double>> answer;
    private static WeatherWave NorthSea, BalticSea;
    private boolean WithWeather;

    public STEAM(boolean withWeather) throws IOException, ParseException {
        answer = CO2emission.robustAllLoad();
        WithWeather = withWeather;
        if (withWeather && NorthSea == null)
            NorthSea = new WeatherWave(ModelInit.workdir + "weather-double\\MetO-NWS-WAV-hi.csv", 10, 9);
        if (withWeather && BalticSea == null)
            BalticSea = new WeatherWave(ModelInit.workdir + "weather-double\\dataset-bal-analysis-forecast-wav-hourly.csv", 4, 3);
    }



    public String emission(AISsegment segment, int thetaRange, double height){
        Map<String, Double> currentShip = answer.get(segment.mmsi);
        if(currentShip != null) {
            double maxSpeed = currentShip.get(attrMaxSpeed);
            double maxPower = currentShip.get(attrMaxPower);
            double speedSafety = 0.5;

            double mu = 0, delta = 0;

            double BN = 4.21794 * Math.pow(height, 0.31);

            if(thetaRange == 0)
                mu = 1;
            else if (thetaRange == 1) {
                mu = (1.7 - 0.03 * Math.pow(BN - 4, 2)) / 2;
            } else if (thetaRange == 2) {
                mu = (0.9 - 0.03 * Math.pow(BN - 6, 2)) / 2;
            }else
                mu = (1.7 - 0.03 * Math.pow(BN - 8, 2)) / 2;

            double C = 0.7;
            double displacement = displacement(segment, currentShip);
            delta = (C * BN + Math.pow(BN, 6.5) / 22 / Math.pow(displacement, 2.0 / 3.0)) / 100;
            delta = delta > 0.5 ? 0.5 : delta;

            segment.delta = delta;

            double speedEffective = (1 + mu * delta) * segment.speed;
            double denominator = maxSpeed + speedSafety;
            speedEffective = speedEffective > denominator ? denominator : speedEffective;

            double powerEffective = maxPower * Math.pow(speedEffective / denominator, 3);
            return "" + (powerEffective * segment.timeGap / 3600 * calcEF_CO2(FUEL) / 1000);
        }
        return super.emission(segment);
    }

    @Override
    public String emission(AISsegment segment) {//// in kilograms
        Map<String, Double> currentShip = answer.get(segment.mmsi);
        if(currentShip != null) {
            double maxSpeed = currentShip.get(attrMaxSpeed);
            double maxPower = currentShip.get(attrMaxPower);
            double speedSafety = 0.5;

            double mu = 0, delta = 0;
            if (WithWeather){
                String waveInfo = wave(segment);
                String[] waveParts = waveInfo.split(",");//height, direction

                double heading = segment.heading;
                double waveDir = (Double.parseDouble(waveParts[1]) + 180) % 360;
                double angleDiff = Math.abs(heading - waveDir);
                double theta = angleDiff > 180 ? 360 - angleDiff : angleDiff;
                theta = 180 - theta;

                double height = Double.parseDouble(waveParts[0]);
                double BN = 4.21794 * Math.pow(height, 0.31);

                if(theta <= 30)
                    mu = 1;
                else if (theta <= 60) {
                    mu = (1.7 - 0.03 * Math.pow(BN - 4, 2)) / 2;
                } else if (theta <= 150) {
                    mu = (0.9 - 0.03 * Math.pow(BN - 6, 2)) / 2;
                }else
                    mu = (1.7 - 0.03 * Math.pow(BN - 8, 2)) / 2;

                double C = 0.7;
                double displacement = displacement(segment, currentShip);
                delta = (C * BN + Math.pow(BN, 6.5) / 22 / Math.pow(displacement, 2.0 / 3.0)) / 100;
                delta = delta > 0.5 ? 0.5 : delta;

                segment.significantWaveHeight = height;
                segment.waveFromDirection = theta;
                segment.displacement = displacement;
            }

            double speedEffective = (1 + mu * delta) * segment.speed;
            double denominator = maxSpeed + speedSafety;
            speedEffective = speedEffective > denominator ? denominator : speedEffective;

            double powerEffective = maxPower * Math.pow(speedEffective / denominator, 3);
            return "" + (powerEffective * segment.timeGap / 3600 * calcEF_CO2(FUEL) / 1000);
        }
        return super.emission(segment);
    }

    public String emission(AISsegment segment, WeatherCurrent currentData) {//// in kilograms
        Map<String, Double> currentShip = answer.get(segment.mmsi);
        if(currentShip != null) {
            double maxSpeed = currentShip.get(attrMaxSpeed);
            double maxPower = currentShip.get(attrMaxPower);
            double speedSafety = 0.5;

            segment.speedCalibrated = currentData.calibratedSpeed(segment);

            double mu = 0, delta = 0;
            if (WithWeather){
                String waveInfo = wave(segment);
                String[] waveParts = waveInfo.split(",");//height, direction

                double heading = segment.heading;
                double waveDir = (Double.parseDouble(waveParts[1]) + 180) % 360;
                double angleDiff = Math.abs(heading - waveDir);
                double theta = angleDiff > 180 ? 360 - angleDiff : angleDiff;
                theta = 180 - theta;

                double height = Double.parseDouble(waveParts[0]);
                double BN = 4.21794 * Math.pow(height, 0.31);

                if(theta <= 30)
                    mu = 1;
                else if (theta <= 60) {
                    mu = (1.7 - 0.03 * Math.pow(BN - 4, 2)) / 2;
                } else if (theta <= 150) {
                    mu = (0.9 - 0.03 * Math.pow(BN - 6, 2)) / 2;
                }else
                    mu = (1.7 - 0.03 * Math.pow(BN - 8, 2)) / 2;

                double C = 0.7;
                double displacement = displacement(segment, currentShip);
                delta = (C * BN + Math.pow(BN, 6.5) / 22 / Math.pow(displacement, 2.0 / 3.0)) / 100;
                delta = delta > 0.5 ? 0.5 : delta;

                segment.significantWaveHeight = height;
                segment.waveFromDirection = theta;
                segment.displacement = displacement;
            }

            double speedEffective = (1 + mu * delta) * segment.speedCalibrated;
            double denominator = maxSpeed + speedSafety;
            speedEffective = speedEffective > denominator ? denominator : speedEffective;

            double powerEffective = maxPower * Math.pow(speedEffective / denominator, 3);

            segment.speedCalibrated = -1;
            return "" + (powerEffective * segment.timeGap / 3600 * calcEF_CO2(FUEL) / 1000);
        }
        return super.emission(segment);
    }

    private double calcEF_CO2(String fuelType){// in g/kWh
        if(fuelType.equals(HFO))
            return 644.93;
        else if (fuelType.equals(DIESEL)) {
            return 622.94;
        }
        return -1;
    }
    private double displacement(AISsegment s, Map<String, Double> attrs){
        double LWL = attrs.get(attrLength) * 0.97;
        double breadth = attrs.get(attrBeam);
        double draught = s.draught;
        return LWL * breadth * draught * blockCoef(s, attrs);
    }

    private double blockCoef(AISsegment s, Map<String, Double> attrs){
        return 0.7 + 1.0 / 8.0 * Math.atan((23 - froudeNumber(s, attrs)) / 4.0);
    }

    private double froudeNumber(AISsegment s, Map<String, Double> attrs){
        double speed = (s.speedCalibrated < 0 ? s.speed : s.speedCalibrated) * 1852 / 3600;
        double LWL = attrs.get(attrLength) * 0.97;
        double gravityConstant = 9.8;
        return speed / Math.sqrt(gravityConstant * LWL);
    }

    private String wave(AISsegment s){//in meters
        return s.midLongitude > 10 ? BalticSea.wave(s) : NorthSea.wave(s);
    }
}
