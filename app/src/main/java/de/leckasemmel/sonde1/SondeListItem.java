package de.leckasemmel.sonde1;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;


public class SondeListItem {
    public Scalars scalars;
    public LinkedList<WayPoint> way;
    public WayPoint position;
    public LinkedList<WayPoint> ascentWay;
    public LinkedList<WayPoint> descentWay;
    public Date timeEta;
    public HashMap<Integer, Xdata> xdata;

    // Derived values
    private boolean validPosition;
    private boolean validPtu;
    private boolean validFrequencyOffset;
    private boolean validCpuTemperature;
    private boolean validExtra;     // Frame number, #sats, CPU temperature
    private int imageResId;

    public SondeListItem() {
        scalars = new Scalars();
        way = new LinkedList<>();
        xdata = new HashMap<>();
    }

    public enum SondeDecoder {
        SONDE_DECODER_BEACON,
        SONDE_DECODER_C34_C50,
        SONDE_DECODER_GRAW,
        SONDE_DECODER_IMET,
        SONDE_DECODER_M10,
        SONDE_DECODER_M20,
        SONDE_DECODER_PILOT,
        SONDE_DECODER_RS41,
        SONDE_DECODER_RS92,
        SONDE_DECODER_MEISEI,
        SONDE_DECODER_JINYANG,
        SONDE_DECODER_MRZ,
        SONDE_DECODER_CF06,
        SONDE_DECODER_GTH3,
        SONDE_DECODER_PSB3,
        SONDE_DECODER_IMET54,
        SONDE_DECODER_S1,
        SONDE_DECODER_MTS01,
        SONDE_DECODER_LMS6,
    }

    public static class WayPoint {
        double latitude;
        double longitude;
        double altitude;
        double climbRate;
        long timeStamp;

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getAltitude() { return altitude; }
        public double getClimbRate() { return climbRate; }
        public long getTimeStamp() { return timeStamp; }
    }

    public long id;

    public static class Scalars {
        // Common
        int time;
        String name;
        SondeDecoder sondeDecoder;
        double frequency;
        int visibleSats;
        int usedSats;
        double frequencyOffset;
        int special;
        double hdop;
        int frameNumber;
        int killTimer;
        double vbat;
        double groundSpeed;
        double direction;
        double temperature;
        double pressure;
        double humidity;
        double rssi;
        double temperatureCpu;
        double temperatureU;
        double descentRate;

        // Fields in some sonde types, but not necessary in all
        double dewPoint;
        double pdop;
        double vdop;
        double extra1;
        String modelName;

        // C50
        double c50TemperatureRef;
        double c50TemperatureChamber;
        double c50TemperatureO3;
        double c50CurrentO3;
        int c50USensorHeating;
        int c50USensorFrequency;
        int c50ErrorFlags;
        int c50state;
        int c50serialSensor;
        int c50firmwareVersion;
        int c50gpsInterferer;

        // DFM
        double dfmEhpe;
        double dfmEvpe;
        int dfmGpsMode;

        // RS41
        int rs41GpsAcc;
        int rs41burstKillFrames;
        int rs41killCountdown;
        double rs41temperatureTx;
        double rs41temperatureRef;
        int rs41xdataNumInstruments;
        String rs41BoardName;
        int rs41FirmwareVersion;
        int rs41GpsJammer;

        // MODEM (M10/M20)
        int modemXdataNumInstruments = 0;
        double m20TemperatureBoard;

        // Meisei
        String meiseiSerialSensorBoom;
        String meiseiSerialPcb;
        double meiseiTemperatureTx;

        // MRZ
        int mrzSerialSensor;
        double mrzPAcc;

        // iMet
        double imetTemperatureInner;
        double imetTemperatureP;

        // iMet-54

        // Windsond S1
        int s1id;
        int s1sid;

        // Meteosis MTS-01
        double mts01_innerTemperature;

        // Beacon
        String beaconHex15;
        String beaconId;
    }

    public static class Xdata {
        int instrument;
        String message;
        public Xdata(int instrument, String message) {
            this.instrument = instrument;
            this.message = message;
        }
    }

    public void findDescentRate() {
        double descentRate = Double.NaN;

        SondeListItem.WayPoint current = way.peekLast();
        if (current != null) {
            descentRate = current.climbRate;
            int N = way.size();
            int numDescentFrames = 0;
            for (int i = N - 1; i >= 0; i--) {
                SondeListItem.WayPoint p = way.get(i);
                if (p.climbRate >= 0) {
                    break;
                }
                ++numDescentFrames;

                // Limit to the last x frames
                if (numDescentFrames >= 10) {
                    break;
                }
            }

            if (numDescentFrames >= 5) {  //TODO
                double[] h = new double[numDescentFrames];
                double[] t = new double[numDescentFrames];

                for (int i = 0; i < numDescentFrames; i++) {
                    SondeListItem.WayPoint p = way.get(N - 1 - i);
                    h[numDescentFrames - 1 - i] = p.altitude;
                    t[numDescentFrames - 1 - i] = p.timeStamp / 1e3;
                }

                double t_mean = 0;
                double h_mean = 0;
                for (int i = 0; i < numDescentFrames; i++) {
                    t_mean += t[i];
                    h_mean += h[i];
                }
                t_mean /= numDescentFrames;
                h_mean /= numDescentFrames;

                double temp1 = 0;
                double temp2 = 0;
                for (int i = 0; i < numDescentFrames; i++) {
                    temp1 += (t[i] - t_mean) * (h[i] - h_mean);
                    temp2 += (t[i] - t_mean) * (t[i] - t_mean);
                }
                descentRate = temp1 / temp2;
            }
        }

        scalars.descentRate = descentRate;
    }

    public double getLatitude() { return (position == null) ? Double.NaN : position.latitude; }
    public void setLatitude(double latitude) { position.latitude = latitude; }
    public double getLongitude() { return (position == null) ? Double.NaN : position.longitude; }
    public void setLongitude(double longitude) { position.longitude = longitude; }
    public double getAltitude() { return (position == null) ? Double.NaN : position.altitude; }
    public void setAltitude(double altitude) { position.altitude = altitude; }
    public double getClimbRate() { return (position == null) ? Double.NaN : position.climbRate; }
    public void setClimbRate(double rate) { position.climbRate = rate; }
    public double getGroundSpeed() { return scalars.groundSpeed; }
    public void setGroundSpeed(double speed) { scalars.groundSpeed = speed; }
    public double getDirection() { return scalars.direction; }
    public void setDirection(double direction) { scalars.direction = direction; }
    public double getPdop() { return scalars.pdop; }
    public void setPdop(double value) { scalars.pdop = value; }

    public double getPressure() { return scalars.pressure; }
    public void setPressure(double pressure) { scalars.pressure = pressure; }
    public double getTemperature() { return scalars.temperature; }
    public void setTemperature(double temperature) { scalars.temperature = temperature; }
    public double getHumidity() { return scalars.humidity; }
    public void setHumidity(double humidity) { scalars.humidity = humidity; }

    public String getName() { return scalars.name; }
    public void setName(String name) { scalars.name = name; }
    public SondeDecoder getSondeDecoder() { return scalars.sondeDecoder; }
    public void setSondeDecoder(SondeListItem.SondeDecoder decoder) { scalars.sondeDecoder = decoder; }
    public double getFrequency() { return scalars.frequency; }
    public void setFrequency(double frequency) { scalars.frequency = frequency; }
    public double getFrequencyOffset() { return scalars.frequencyOffset; }
    public void setFrequencyOffset(double offset) { scalars.frequencyOffset = offset; }
    public double getRssi() { return scalars.rssi; }
    public void setRssi(double value) { scalars.rssi = value; }
    public double getVbat() { return scalars.vbat; }
    public void setVbat(double value) { scalars.vbat = value; }
    public double getTemperatureCpu() { return scalars.temperatureCpu; }
    public void setTemperatureCpu(double value) { scalars.temperatureCpu = value; }
    public double getTemperatureU() { return scalars.temperatureU; }
    public void setTemperatureU(double value) { scalars.temperatureU = value; }
    public int getFrameNumber() { return scalars.frameNumber; }
    public void setFrameNumber(int value) { scalars.frameNumber = value; }
    public int getUsedSats() { return scalars.usedSats; }
    public void setUsedSats(int value) { scalars.usedSats = value; }
    public int getVisibleSats() { return scalars.visibleSats; }
    public void setVisibleSats(int value) { scalars.visibleSats = value; }
    public String getModelName() { return scalars.modelName; }
    public void setModelName(String name) { scalars.modelName = name; }
    public int getSpecial() { return scalars.special; }
    public void setSpecial(int value) { scalars.special = value; }

    public boolean getValidPosition() { return validPosition; }
    public void setValidPosition(boolean enable) { validPosition = enable; }
    public boolean getValidPtu() { return validPtu; }
    public void setValidPtu(boolean enable) { validPtu = enable; }
    public boolean getValidFrequencyOffset() { return validFrequencyOffset; }
    public void setValidFrequencyOffset(boolean enable) { validFrequencyOffset = enable; }
    public boolean getValidCpuTemperature() { return validCpuTemperature; }
    public void setValidCpuTemperature(boolean enable) { validCpuTemperature = enable; }
    public boolean getValidExtra() { return validExtra; }
    public void setValidExtra(boolean enable) { validExtra = enable; }
    public int getImageResId() { return imageResId; }
    public void setImageResId(int resId) { imageResId = resId; }

    public double getDfmEhpe() { return scalars.dfmEhpe; }
    public void setDfmEhpe(double value) { scalars.dfmEhpe = value; }
    public double getDfmEvpe() { return scalars.dfmEvpe; }
    public void setDfmEvpe(double value) { scalars.dfmEvpe = value; }

    public int getMrzSerialSensor() { return scalars.mrzSerialSensor; }
    public void setMrzSerialSensor(int value) { scalars.mrzSerialSensor = value; }
    public double getMrzPAcc() { return scalars.mrzPAcc; }
    public void setMrzPAcc(double value) { scalars.mrzPAcc = value; }

    public double getMts01InnerTemperature() { return scalars.mts01_innerTemperature; }
    public void setMts01InnerTemperature(double value) { scalars.mts01_innerTemperature = value; }

    public double getRs41temperatureRef() { return scalars.rs41temperatureRef; }
    public void setRs41temperatureRef(double value) { scalars.rs41temperatureRef = value; }
    public double getRs41temperatureTx() { return scalars.rs41temperatureTx; }
    public void setRs41temperatureTx(double value) { scalars.rs41temperatureTx = value; }
    public int getRs41FirmwareVersion() { return scalars.rs41FirmwareVersion; }
    public void setRs41FirmwareVersion(int value) { scalars.rs41FirmwareVersion = value; }
    public String getRs41BoardName() { return scalars.rs41BoardName; }
    public void setRs41BoardName(String value) { scalars.rs41BoardName = value; }
    public int getRs41XdataNumInstruments() { return scalars.rs41xdataNumInstruments; }
    public void setRs41XdataNumInstruments(int value) { scalars.rs41xdataNumInstruments = value; }
    public HashMap<Integer, Xdata> getXdata() { return xdata; }
    public void setXdata(HashMap<Integer, Xdata> value) { xdata = value; }

    public int getModemXdataNumInstruments() { return scalars.modemXdataNumInstruments; }
    public void setModemXdataNumInstruments(int value) { scalars.modemXdataNumInstruments = value; }

    public String getBeaconHex15() { return scalars.beaconHex15; }
    public void setBeaconHex15(String value) { scalars.beaconHex15 = value; }
    public String getBeaconId() { return scalars.beaconId; }
    public void setBeaconId(String value) { scalars.beaconId = value; }

    public double getImetTemperatureInner() { return scalars.imetTemperatureInner; }
    public void setImetTemperatureInner(double value) { scalars.imetTemperatureInner = value; }
    public double getImetTemperatureP() { return scalars.imetTemperatureP; }
    public void setImetTemperatureP(double value) { scalars.imetTemperatureP = value; }

    public double getM20TemperatureBoard() { return scalars.m20TemperatureBoard; }
    public void setM20TemperatureBoard(double value) { scalars.m20TemperatureBoard = value; }

    public String getMeiseiSerialSensorBoom() { return scalars.meiseiSerialSensorBoom; }
    public void setMeiseiSerialSensorBoom(String value) { scalars.meiseiSerialSensorBoom = value; }
    public String getMeiseiSerialPcb() { return scalars.meiseiSerialPcb; }
    public void setMeiseiSerialPcb(String value) { scalars.meiseiSerialPcb = value; }
    public double getMeiseiTemperatureTx() { return scalars.meiseiTemperatureTx; }
    public void setMeiseiTemperatureTx(double value) { scalars.meiseiTemperatureTx = value; }
}
