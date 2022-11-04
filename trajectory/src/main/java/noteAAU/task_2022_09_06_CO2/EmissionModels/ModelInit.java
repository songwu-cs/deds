package noteAAU.task_2022_09_06_CO2.EmissionModels;

public class ModelInit {
    public static String workdir = "H:\\UpanSky\\DEDS_DenmarkAIS_May_2022\\";

    public static final String DIESEL = "DIESEL";
    public static final String HFO = "HFO";
    public static final String FUEL = DIESEL;

    public static final String SSD = "SSD";
    public static final String MSD = "MSD";
    public static final String HSD = "HSD";
    public static final String attrTPC = "tpc";
    public static final String attrDRAUGHT = "draught";
    public static final String attrDWT = "dwt";
    public static final String attrMaxPower = "power";
    public static final String attrMaxSpeed = "speed";
    public static final String attrGrossTonnage = "grossTonnage";
    public static final String attrLBP = "lbp";

    public static final String attrRPM = "rpm";

    public static final String attrLength = "loa";

    public static final String attrBeam = "beam";
    public static final String attrBreadth = "breadth";

    public static final String attrYear = "year";

    public String rpmLEVEL(double rpm){
        if(rpm <= 300)
            return SSD;
        else if (rpm <= 900)
            return MSD;
        return HSD;
    }
    public String emission(AISsegment segment){
        return "";
    } //in kilograms

}
