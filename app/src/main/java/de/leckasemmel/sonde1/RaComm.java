package de.leckasemmel.sonde1;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

public class RaComm {
    private SondeListItem partialItem;

    public RaComm() {
        partialItem = new SondeListItem();
    }

    public static class TargetInfo {
        boolean isLoader;
        String firmwareName;
        int firmwareMajor;
        int firmwareMinor;
        int firmwareMaxLineLength;
        int serialNumber;
        int bl652FirmwareVersion;
        Integer bl652HavePowerScript;
        Integer loaderVersion;
        long ephemerisAge;
    }

    public static class RxFrequency {
        double frequency;
        public RxFrequency(double frequency) {this.frequency = frequency;}
    }
    public static class ScanMode {
        int mode;
        public ScanMode(int mode) {this.mode = mode;}
    }
    public static class Rssi {
        double rssi;
        boolean lnaEnable;
        public Rssi(double rssi, boolean enable) {
            this.rssi = rssi;
            this.lnaEnable = enable;
        }
    }
    public static class DecoderCode {
        int code;
        public DecoderCode(int code) {this.code = code;}
    }
    public static class DebugAudioCode {
        int code;
        public DebugAudioCode(int code) {this.code = code;}
    }
    public static class RaBatteryVoltage {
        double vbat;
        public RaBatteryVoltage(double vbat) {this.vbat = vbat;}
    }
    public static class ScannerState {
        boolean enable;
        public ScannerState(boolean enable) {this.enable = enable;}
    }
    public static class EphemerisAge {
        String age;
        public EphemerisAge(String age) {this.age = age;}
    }
    public static class AudioMonitorState {
        boolean enable;
        public AudioMonitorState(boolean enable) {this.enable = enable;}
    }
    public static class FirmwareUpdateResponse {
        int phase;
        int param1;
        int param2;
        public FirmwareUpdateResponse(int phase, int param1, int param2) {
            this.phase = phase;
            this.param1 = param1;
            this.param2 = param2;
        }
    }

    public static class Spectrum {
        double frequency;
        double spacing;
        Double[] levels;
        public Spectrum(double frequency, double spacing, Double[] levels) {
            this.frequency = frequency;
            this.spacing = spacing;
            this.levels = levels;
        }
    }

    public static class Xdata {
        long id;
        int instrument;
        int chainPosition;
        String message;
        public Xdata(long id, int instrument, int chainPosition, String message) {
            this.id = id;
            this.instrument = instrument;
            this.chainPosition = chainPosition;
            this.message = message;
        }
    }

    public static class AudioSamples {
        String uuEncodedSamples;
        public AudioSamples(String value) {this.uuEncodedSamples = value;}
    }

    public static class RawFrameData {
        long id;
        String logLine;
        public RawFrameData(long id, String logLine) {
            this.id = id;
            this.logLine = logLine;
        }
    }

    private HashMap<Integer, SondeListItem.SondeDecoder> map1 = new HashMap<Integer, SondeListItem.SondeDecoder>() {{
        put(0  , SondeListItem.SondeDecoder.SONDE_DECODER_RS92);
        put(1  , SondeListItem.SondeDecoder.SONDE_DECODER_RS41);
        put(2  , SondeListItem.SondeDecoder.SONDE_DECODER_GRAW);
        put(3  , SondeListItem.SondeDecoder.SONDE_DECODER_GRAW);
        put(4  , SondeListItem.SondeDecoder.SONDE_DECODER_GRAW);
        put(5  , SondeListItem.SondeDecoder.SONDE_DECODER_C34_C50);
        put(6  , SondeListItem.SondeDecoder.SONDE_DECODER_IMET);
        put(7  , SondeListItem.SondeDecoder.SONDE_DECODER_M10);
        put(8  , SondeListItem.SondeDecoder.SONDE_DECODER_C34_C50);
        put(9  , SondeListItem.SondeDecoder.SONDE_DECODER_BEACON);
        put(10 , SondeListItem.SondeDecoder.SONDE_DECODER_PILOT);
        put(11 , SondeListItem.SondeDecoder.SONDE_DECODER_MEISEI);
        put(12 , SondeListItem.SondeDecoder.SONDE_DECODER_JINYANG);
        put(13 , SondeListItem.SondeDecoder.SONDE_DECODER_M20);
        put(14 , SondeListItem.SondeDecoder.SONDE_DECODER_MRZ);
        put(15 , SondeListItem.SondeDecoder.SONDE_DECODER_PSB3);
        put(16 , SondeListItem.SondeDecoder.SONDE_DECODER_CF06);
        put(17 , SondeListItem.SondeDecoder.SONDE_DECODER_GTH3);
        put(18 , SondeListItem.SondeDecoder.SONDE_DECODER_IMET54);
        //put(19 , SondeListItem.SondeDecoder.SONDE_DECODER_);
        //put(20 , SondeListItem.SondeDecoder.SONDE_DECODER_);
        put(21 , SondeListItem.SondeDecoder.SONDE_DECODER_S1);
        put(22 , SondeListItem.SondeDecoder.SONDE_DECODER_MTS01);
        put(23 , SondeListItem.SondeDecoder.SONDE_DECODER_LMS6);
    }};

    private String safeStringFromStringRange(String s, int from, int to, String defaultValue) {
        String value;
        try {
            value = s.substring(from, to);
        } catch (IndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    private String safeStringFromStringArray(String[] payload, int index, String defaultValue) {
        String value;
        try {
            if (index >= payload.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            value = payload[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    private int safeIntFromStringRange(String s, int from, int to, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(s.substring(from, to));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    private Integer safeIntFromStringArray(String[] payload, int index, Integer defaultValue) {
        Integer value;
        try {
            if (index >= payload.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            value = Integer.parseInt(payload[index]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    private long safeLongFromStringArray(String[] payload, int index, long defaultValue) {
        long value;
        try {
            if (index >= payload.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            value = Long.parseLong(payload[index]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    private long safeHexLongFromStringArray(String[] payload, int index, long defaultValue) {
        long value;
        try {
            if (index >= payload.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            value = Long.parseLong(payload[index], 16);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    private double safeDoubleFromStringArray(String[] payload, int index, double defaultValue) {
        double value;
        try {
            if (index >= payload.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            value = Double.parseDouble(payload[index]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    // Process a received line
    Object processRxLine(String line) {
        // Validate (format, checksum)
        if (line.charAt(0) != '#') {
            return null;
        }
        String[] payload = line.substring(1).split(",");
        if (payload.length < 2) {
            return null;
        }
        //TODO checksum

        String raw = "";
        Object retVal = null;

        // Separate opcode and payload fields
        int opcode = safeIntFromStringArray(payload, 0, -1);

        // Opcode processing
        if (opcode == 0) {
            TargetInfo info = new TargetInfo();
            int type = safeIntFromStringArray(payload, 1, -1);
            if (type == 0) {
                info.isLoader = true;
                info.loaderVersion = safeIntFromStringArray(payload, 2, 0);
                retVal = info;
            } else if (type == 1) {
                info.isLoader = false;
                info.firmwareMajor = safeIntFromStringArray(payload, 2, 0);
                info.firmwareMaxLineLength = (info.firmwareMajor >= 21) ? 200 : 80;
                info.firmwareMinor = safeIntFromStringArray(payload, 4, 0);
                if (payload.length >= 6) {
                    info.firmwareName = payload[5];
                }
                info.serialNumber = safeIntFromStringArray(payload, 6, -1);
                info.bl652FirmwareVersion = safeIntFromStringArray(payload, 7, 0);
                info.bl652HavePowerScript = safeIntFromStringArray(payload, 8, null);
                info.loaderVersion = safeIntFromStringArray(payload, 9, null);
                retVal = info;
            }
        } else if (opcode == 1) {
            // Empty item. Main activity will merge existing way if applicable.
            SondeListItem item = new SondeListItem();
            item.way = new LinkedList<>();

            item.id = safeLongFromStringArray(payload, 1, -1);
            int code = safeIntFromStringArray(payload, 2, 0);
            item.scalars.sondeDecoder = map1.get(code);
            if (item.scalars.sondeDecoder == null) {
                item.scalars.sondeDecoder = SondeListItem.SondeDecoder.SONDE_DECODER_RS41;
            }
            item.scalars.frequency = safeDoubleFromStringArray(payload, 3, 0.0) * 1e6;
            item.scalars.usedSats = safeIntFromStringArray(payload, 4, 0);
            item.scalars.direction = safeDoubleFromStringArray(payload, 9, Double.NaN);
            item.scalars.groundSpeed = safeDoubleFromStringArray(payload, 10, Double.NaN);
            item.scalars.temperature = safeDoubleFromStringArray(payload, 11, Double.NaN);
            item.scalars.pressure = safeDoubleFromStringArray(payload, 12, Double.NaN);
            item.scalars.special = safeIntFromStringArray(payload, 13, 0);
            item.scalars.humidity = safeDoubleFromStringArray(payload, 15, Double.NaN);
            item.scalars.hdop = safeDoubleFromStringArray(payload, 16, Double.NaN);
            item.scalars.rssi = safeDoubleFromStringArray(payload, 17, -174.0);
            item.scalars.frequencyOffset = safeDoubleFromStringArray(payload, 18, 0.0);
            item.scalars.visibleSats = safeIntFromStringArray(payload, 19, 0);
            item.scalars.frameNumber = safeIntFromStringArray(payload, 20, 0);
            item.scalars.killTimer = safeIntFromStringArray(payload, 21, -1);
            item.scalars.vbat = safeDoubleFromStringArray(payload, 22, Double.NaN);

            if (item.scalars.sondeDecoder == SondeListItem.SondeDecoder.SONDE_DECODER_C34_C50) {
                item.scalars.extra1 = safeDoubleFromStringArray(payload, 14, Double.NaN);
            }
            item.scalars.temperatureCpu = safeDoubleFromStringArray(payload, 23, Double.NaN);

            SondeListItem.WayPoint point = new SondeListItem.WayPoint();
            point.latitude = safeDoubleFromStringArray(payload, 5, Double.NaN);
            if (!Double.isNaN(point.latitude)) {
                if (abs(point.latitude) > 90.0) {
                    point.latitude = Double.NaN;
                }
            }
            point.longitude = safeDoubleFromStringArray(payload, 6, Double.NaN);
            if (!Double.isNaN(point.longitude)) {
                if (abs(point.longitude) > 180.0) {
                    point.longitude = Double.NaN;
                }
            }
            point.altitude = safeDoubleFromStringArray(payload, 7, Double.NaN);
            point.climbRate = safeDoubleFromStringArray(payload, 8, Double.NaN);
            point.timeStamp = new Date().getTime();
            item.way.add(point);

            // Remember this partial item. Will be completed by opcode=2 for same id.
            // Return null for the current opcode.
            partialItem = item;
        } else if (opcode == 2) {
            // Continue if id matches the partial item.
            if (safeLongFromStringArray(payload, 1, -1) == partialItem.id) {
                int code = safeIntFromStringArray(payload, 2, 0);
                SondeListItem.SondeDecoder decoder = map1.get(code);
                if (decoder != null) {
                    // NOTE  validate sonde type? should be redundant...

                    int infoType = safeIntFromStringArray(payload, 3, -1);
                    if (infoType == 0) {    // T=0
                        switch (decoder) {
                            case SONDE_DECODER_RS92:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                break;
                            case SONDE_DECODER_RS41:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.modelName = safeStringFromStringArray(payload, 6, "");
                                partialItem.scalars.rs41temperatureTx = safeDoubleFromStringArray(payload, 7, Double.NaN);
                                partialItem.scalars.rs41temperatureRef = safeDoubleFromStringArray(payload, 8, Double.NaN);
                                partialItem.scalars.dewPoint = safeDoubleFromStringArray(payload, 9, Double.NaN);
                                partialItem.scalars.rs41GpsAcc = safeIntFromStringArray(payload, 10, 255);
                                partialItem.scalars.rs41burstKillFrames = safeIntFromStringArray(payload, 11, -1);
                                partialItem.scalars.rs41killCountdown = safeIntFromStringArray(payload, 12, -1);
                                partialItem.scalars.pdop = safeDoubleFromStringArray(payload, 13, Double.NaN);
                                partialItem.scalars.rs41FirmwareVersion = safeIntFromStringArray(payload, 14, 0);
                                partialItem.scalars.rs41BoardName = safeStringFromStringArray(payload, 15, "");
                                partialItem.scalars.rs41GpsJammer = safeIntFromStringArray(payload, 16, 0);
                                partialItem.scalars.rs41xdataNumInstruments = safeIntFromStringArray(payload, 17, 0);
                                break;
                            case SONDE_DECODER_GRAW:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.dfmEhpe = safeDoubleFromStringArray(payload, 5, Double.NaN);
                                partialItem.scalars.dfmEvpe = safeDoubleFromStringArray(payload, 6, Double.NaN);
                                partialItem.scalars.dfmGpsMode = safeIntFromStringArray(payload, 7, -1);
                                break;
                            case SONDE_DECODER_IMET:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.imetTemperatureInner = safeDoubleFromStringArray(payload, 5, Double.NaN);
                                partialItem.scalars.imetTemperatureP = safeDoubleFromStringArray(payload, 6, Double.NaN);
                                partialItem.scalars.temperatureU = safeDoubleFromStringArray(payload, 7, Double.NaN);
                                break;
                            case SONDE_DECODER_M10:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                break;
                            case SONDE_DECODER_C34_C50:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.c50TemperatureRef = safeDoubleFromStringArray(payload, 6, Double.NaN);
                                partialItem.scalars.temperatureU = safeDoubleFromStringArray(payload, 7, Double.NaN);
                                partialItem.scalars.c50TemperatureChamber = safeDoubleFromStringArray(payload, 8, Double.NaN);
                                partialItem.scalars.c50TemperatureO3 = safeDoubleFromStringArray(payload, 9, Double.NaN);
                                partialItem.scalars.c50CurrentO3 = safeDoubleFromStringArray(payload, 10, Double.NaN);
                                partialItem.scalars.c50USensorHeating = safeIntFromStringArray(payload, 12, 0);
                                partialItem.scalars.c50USensorFrequency = safeIntFromStringArray(payload, 13, 0);
                                partialItem.scalars.c50state = safeIntFromStringArray(payload, 14, 0);
                                partialItem.scalars.c50ErrorFlags = safeIntFromStringArray(payload, 15, 0);
                                partialItem.scalars.vdop = safeDoubleFromStringArray(payload, 16, Double.NaN);
                                partialItem.scalars.c50serialSensor = safeIntFromStringArray(payload, 18, 0);
                                partialItem.scalars.c50firmwareVersion = safeIntFromStringArray(payload, 19, 0);
                                partialItem.scalars.c50gpsInterferer = safeIntFromStringArray(payload, 20, -1);
                                break;
                            case SONDE_DECODER_BEACON:
                                partialItem.scalars.beaconHex15 = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.beaconId = safeStringFromStringArray(payload, 5, "");
                                break;
                            case SONDE_DECODER_PILOT:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                break;
                            case SONDE_DECODER_MEISEI:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.meiseiSerialSensorBoom = safeStringFromStringArray(payload, 5, "");
                                partialItem.scalars.meiseiSerialPcb = safeStringFromStringArray(payload, 6, "");
                                partialItem.scalars.modelName = safeStringFromStringArray(payload, 7, "");
                                partialItem.scalars.meiseiTemperatureTx = safeDoubleFromStringArray(payload, 8, Double.NaN);
                                break;
                            case SONDE_DECODER_JINYANG:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                break;
                            case SONDE_DECODER_M20:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.modemXdataNumInstruments = safeIntFromStringArray(payload, 5, 0);
                                partialItem.scalars.temperatureU = safeDoubleFromStringArray(payload, 7, Double.NaN);
                                partialItem.scalars.m20TemperatureBoard = safeDoubleFromStringArray(payload, 8, Double.NaN);
                                break;
                            case SONDE_DECODER_PSB3:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.pdop = safeDoubleFromStringArray(payload, 5, Double.NaN);
                                break;
                            case SONDE_DECODER_CF06:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                break;
                            case SONDE_DECODER_GTH3:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                break;
                            case SONDE_DECODER_IMET54:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.temperatureU = safeDoubleFromStringArray(payload, 5, Double.NaN);
                                break;
                            case SONDE_DECODER_S1:
                                partialItem.scalars.s1id = safeIntFromStringArray(payload, 4, 0);
                                partialItem.scalars.s1sid = safeIntFromStringArray(payload, 5, 0);
                                partialItem.scalars.name = String.format(Locale.US, "%d/S%d",
                                        partialItem.scalars.s1id, partialItem.scalars.s1sid);
                                break;
                            case SONDE_DECODER_MTS01:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                partialItem.scalars.mts01_innerTemperature = safeDoubleFromStringArray(payload, 5, Double.NaN);
                                break;
                            case SONDE_DECODER_LMS6:
                                partialItem.scalars.name = safeStringFromStringArray(payload, 4, "");
                                break;
                        }

                        retVal = partialItem;
                    }

                    if (infoType == 1) {    // T=1
                        switch (decoder) {
                            case SONDE_DECODER_RS92:
                                raw = safeStringFromStringArray(payload, 6, "");
                                if (raw.length() == 234) {
                                    StringBuilder logLine = new StringBuilder("10 ");
                                    for (int i = 0; i < 234/2; i++) {
                                        logLine.append(raw.substring(2 * i, 2 * (i + 1)));
                                    }
                                    logLine.append("2A 2A 2A 2A 2A 00\n");

                                    retVal = new RawFrameData(partialItem.id, logLine.toString());
                                }
                                break;

                            case SONDE_DECODER_RS41:
                                raw = safeStringFromStringArray(payload, 6, "");
                                if ((raw.length() % 4) == 0) {
                                    StringBuilder logLine = new StringBuilder();
                                    byte[] uu;
                                    byte[] bb = new byte[3];
                                    for (int k = 0; k < raw.length(); k += 4) {
                                        uu = raw.substring(k, k + 4).getBytes();

                                        for (int kk = 0; kk < 4; kk++) {
                                            if (uu[kk] == 0x20) {
                                                uu[kk] = 0x2C;
                                            }
                                            uu[kk] = (byte) ((uu[kk] & ~0x40) ^ 0x20);
                                        }

                                        bb[0] = (byte) ((uu[0] & 0x3F) | ((uu[1] << 6) & 0xC0));
                                        bb[1] = (byte) (((uu[1] >> 2) & 0x0F) | ((uu[2] << 4) & 0xF0));
                                        bb[2] = (byte) (((uu[2] >> 4) & 0x03) | ((uu[3] << 2) & 0xFC));

                                        logLine.append(String.format(
                                                Locale.US, "%02X %02X %02X ",
                                                bb[0],
                                                bb[1],
                                                bb[2]));
                                    }
                                    logLine.append("\n");

                                    retVal = new RawFrameData(partialItem.id, logLine.toString());
                                }
                                break;

                            case SONDE_DECODER_GRAW:
                                raw = safeStringFromStringArray(payload, 4, "");
                                if (raw.length() == 33) {
                                    String logLine = String.format(
                                            Locale.US, "%s %s %s t=%d\n",
                                            raw.substring(0, 7),
                                            raw.substring(7, 20),
                                            raw.substring(20, 33),
                                            System.nanoTime() / 1000000
                                            );

                                    retVal = new RawFrameData(partialItem.id, logLine);
                                }
                                break;

                            case SONDE_DECODER_BEACON:
                                raw = safeStringFromStringArray(payload, 4, "");
                                if (raw.length() == 30) {
                                    String logLine = String.format(
                                            Locale.US, "%s t=%d\n",
                                            raw,
                                            System.nanoTime() / 1000000
                                    );

                                    retVal = new RawFrameData(partialItem.id, logLine);
                                }
                                break;

                            case SONDE_DECODER_MEISEI:
                                retVal = new RawFrameData(
                                        partialItem.id,
                                        String.format(
                                                Locale.US, "%s %s t=%d\n",
                                                safeStringFromStringArray(payload, 4, ""),
                                                safeStringFromStringArray(payload, 5, ""),
                                                System.nanoTime() / 1000000
                                        ));
                                break;

                            case SONDE_DECODER_JINYANG, SONDE_DECODER_IMET:
                                retVal = new RawFrameData(
                                        partialItem.id,
                                        String.format(
                                                Locale.US, "%s t=%d\n",
                                                safeStringFromStringArray(payload, 4, ""),
                                                System.nanoTime() / 1000000
                                        ));
                                break;

                            case SONDE_DECODER_M10:
                                raw = safeStringFromStringArray(payload, 5, "");
                                if (raw.length() > 50) {
                                    StringBuilder logLine = new StringBuilder("00 00 C0 64");
                                    for (int i = 0; i < raw.length() / 2; i++) {
                                        logLine.append(" ");
                                        logLine.append(raw.substring(2 * i, 2 * (i + 1)));
                                    }
                                    logLine.append("\n");

                                    retVal = new RawFrameData(partialItem.id, logLine.toString());
                                }
                                break;

                            case SONDE_DECODER_M20:
                                raw = safeStringFromStringArray(payload, 5, "");
                                if (raw.length() > 50) {
                                    StringBuilder logLine = new StringBuilder("00 00 C0 45");
                                    for (int i = 0; i < raw.length() / 2; i++) {
                                        logLine.append(" ");
                                        logLine.append(raw.substring(2 * i, 2 * (i + 1)));
                                    }
                                    logLine.append("\n");

                                    retVal = new RawFrameData(partialItem.id, logLine.toString());
                                }
                                break;

                            case SONDE_DECODER_C34_C50:
                                int channel = safeIntFromStringArray(payload, 4, -1);
                                if ((channel >= 0) && (channel <= 255)) {
                                    long value = safeHexLongFromStringArray(payload, 5, 0);
                                    value = (value & 0xff) << 24 | (value & 0xff00) << 8 | (value & 0xff0000) >> 8 | (value >> 24) & 0xff;
                                    long crc1 = channel;
                                    long crc2 = crc1;
                                    crc1 = (crc1 + ((value >> 24) & 0xFF)) & 0xFF;
                                    crc2 = (crc2 + crc1) & 0xFF;
                                    crc1 = (crc1 + ((value >> 16) & 0xFF)) & 0xFF;
                                    crc2 = (crc2 + crc1) & 0xFF;
                                    crc1 = (crc1 + ((value >> 8) & 0xFF)) & 0xFF;
                                    crc2 = (crc2 + crc1) & 0xFF;
                                    crc1 = (crc1 + ((value) & 0xFF)) & 0xFF;
                                    crc2 = (crc2 + crc1) & 0xFF;
                                    crc2 = crc2 ^ 0xFF;

                                    retVal = new RawFrameData(
                                            partialItem.id,
                                            String.format(
                                                    Locale.US, "00FF %02X %08X %02X%02X t=%d\n",
                                                    channel,
                                                    value,
                                                    crc1,
                                                    crc2,
                                                    System.nanoTime() / 1000000
                                            ));
                                }
                                break;

                            case SONDE_DECODER_IMET54:
                                raw = safeStringFromStringArray(payload, 6, "");
                                if ((raw.length() % 4) == 0) {
                                    StringBuilder logLine = new StringBuilder();
                                    byte[] uu;
                                    byte[] bb = new byte[3];
                                    for (int k = 0; k < raw.length(); k += 4) {
                                        uu = raw.substring(k, k + 4).getBytes();

                                        for (int kk = 0; kk < 4; kk++) {
                                            if (uu[kk] == 0x20) {
                                                uu[kk] = 0x2C;
                                            }
                                            uu[kk] = (byte) ((uu[kk] & ~0x40) ^ 0x20);
                                        }

                                        bb[0] = (byte) ((uu[0] & 0x3F) | ((uu[1] << 6) & 0xC0));
                                        bb[1] = (byte) (((uu[1] >> 2) & 0x0F) | ((uu[2] << 4) & 0xF0));
                                        bb[2] = (byte) (((uu[2] >> 4) & 0x03) | ((uu[3] << 2) & 0xFC));

                                        logLine.append(String.format(
                                                Locale.US, "%02X %02X %02X ",
                                                bb[0],
                                                bb[1],
                                                bb[2]));
                                    }
                                    logLine.append("\n");

                                    retVal = new RawFrameData(partialItem.id, logLine.toString());
                                }
                                break;

                            case SONDE_DECODER_MRZ:
                                raw = safeStringFromStringArray(payload, 4, "");
                                if (raw.length() > 94) {
                                    StringBuilder logLine = new StringBuilder();
                                    for (int i = 0; i < raw.length() / 2; i++) {
                                        logLine.append(" ");
                                        logLine.append(raw.substring(2 * i, 2 * (i + 1)));
                                    }
                                    logLine.append("\n");

                                    retVal = new RawFrameData(partialItem.id, logLine.toString());
                                }
                                break;

                            case SONDE_DECODER_PILOT:
                                //TODO
                                break;
                        }
                    }

                    if (infoType == 2) {    // T=2
                        switch (decoder) {
                            case SONDE_DECODER_RS41:
                                String xdata = safeStringFromStringArray(payload, 4, "");
                                if (xdata.length() > 4) {
                                    retVal = new Xdata(
                                            partialItem.id,
                                            safeIntFromStringRange(xdata, 0,2, -1),
                                            safeIntFromStringRange(xdata, 2,4, -1),
                                            safeStringFromStringRange(xdata, 4, xdata.length(), ""));
                                }
                                break;
                            case SONDE_DECODER_GRAW:
                                break;
                            case SONDE_DECODER_M20:
                                break;
                        }
                    }
                }
            }
        } else if (opcode == 3) {   // Single values
            int param = safeIntFromStringArray(payload, 1, -1);

            if (param == 1) {   // P=1
                retVal = new RxFrequency(safeDoubleFromStringArray(payload, 2, Double.NaN));
            }
            else if (param == 2) {   // P=2
                retVal = new ScanMode(safeIntFromStringArray(payload, 2, 0));
            }
            else if (param == 3) {   // P=3
                retVal = new Rssi(safeDoubleFromStringArray(payload, 2, -174.0), true);
            }
            else if (param == 4) {   // P=4
                retVal = new Rssi(safeDoubleFromStringArray(payload, 2, -174.0), false);
            }
            else if (param == 5) {   // P=5
                retVal = new DecoderCode(safeIntFromStringArray(payload, 2, 0));
            }
            else if (param == 6) {   // P=6
                retVal = new RaBatteryVoltage(safeDoubleFromStringArray(payload, 2, Double.NaN));
            }
            else if (param == 7) {   // P=7
                retVal = new ScannerState(safeIntFromStringArray(payload, 2, 0) != 0);
            }
            else if (param == 8) {   // P=8
                retVal = new EphemerisAge(safeStringFromStringArray(payload, 2, ""));
            }
            else if (param == 9) {   // P=9
                retVal = new AudioMonitorState(safeIntFromStringArray(payload, 2, 0) != 0);
            }
        } else if (opcode == 4) {   // Ephemeris download
        } else if (opcode == 5) {   // Spectrum
            double frequency = safeDoubleFromStringArray(payload, 1, Double.NaN);
            double spacing = safeDoubleFromStringArray(payload, 2, 0.01);

            ArrayList<Double> levelsList = new ArrayList<>();
            for (int i = 3; i < payload.length; i++) {
                double level = safeDoubleFromStringArray(payload, i, Double.NaN);
                if (Double.isNaN(level)) {
                    levelsList.add(Double.NaN);
                } else {
                    levelsList.add(level / 10.0 - 140.0);
                }
            }
            Double[] levels = levelsList.toArray(new Double[0]);
            if (!Double.isNaN(frequency) && (levels.length > 0)) {
                retVal = new Spectrum(frequency, spacing, levels);
            }
        } else if (opcode == 8) {   // Audio samples
            retVal = new AudioSamples(safeStringFromStringArray(payload, 1, ""));
        } else if (opcode == 9) {   // Firmware download
            int phase = safeIntFromStringArray(payload, 1, -1);
            int param1 = safeIntFromStringArray(payload, 2, -1);
            int param2 = safeIntFromStringArray(payload, 3, -1);
            retVal = new FirmwareUpdateResponse(phase, param1, param2);
        } else if (opcode == 10) {  // Config download
        }

        return retVal;
    }

    public String ScannerRemoveSonde(SondeListItem item) {
        String command = "99";
        int code = switch (item.getSondeDecoder()) {
            case SONDE_DECODER_RS41 -> 0;
            case SONDE_DECODER_RS92 -> 1;
            case SONDE_DECODER_GRAW -> 2;
            case SONDE_DECODER_C34_C50 -> 3;
            case SONDE_DECODER_IMET -> 4;
            case SONDE_DECODER_M10, SONDE_DECODER_M20 -> 5;
            case SONDE_DECODER_BEACON -> 6;
            case SONDE_DECODER_PILOT -> 7;
            case SONDE_DECODER_MEISEI -> 8;
            case SONDE_DECODER_JINYANG -> 9;
            case SONDE_DECODER_IMET54 -> 10;
            case SONDE_DECODER_MRZ -> 11;
            case SONDE_DECODER_CF06 -> 12;
            case SONDE_DECODER_GTH3 -> 13;
            case SONDE_DECODER_PSB3 -> 14;
            case SONDE_DECODER_S1 -> 15;
            case SONDE_DECODER_MTS01 -> 16;
            case SONDE_DECODER_LMS6 -> 17;
            default -> -1;
        };
        if (code != -1) {
            command = String.format(Locale.US, "6,1,0,%d,%d", item.id, code);
        }
        return command;
    }

    public String setFrequency(double frequency) {
        return String.format(Locale.US, "1,%f", frequency);
    }

    public String setDetector(SondeListItem.SondeDecoder decoder) {
        String command = "99";
        int code = switch (decoder) {
            case SONDE_DECODER_RS41, SONDE_DECODER_RS92 -> 0;
            case SONDE_DECODER_GRAW -> 1;
            case SONDE_DECODER_C34_C50 -> 2;
            case SONDE_DECODER_IMET -> 3;
            case SONDE_DECODER_M10 -> 4;
            case SONDE_DECODER_BEACON -> 5;
            case SONDE_DECODER_MEISEI -> 6;
            case SONDE_DECODER_PILOT -> 7;
            case SONDE_DECODER_JINYANG -> 8;
            case SONDE_DECODER_IMET54 -> 9;
            case SONDE_DECODER_MRZ -> 10;
            case SONDE_DECODER_CF06, SONDE_DECODER_GTH3 -> 11;
            case SONDE_DECODER_PSB3 -> 12;
            case SONDE_DECODER_S1 -> 13;
            case SONDE_DECODER_MTS01 -> 14;
            case SONDE_DECODER_LMS6 -> 15;
            default -> -1;
        };
        if (code != -1) {
            command = String.format(Locale.US, "7,4,%d", code);
        }
        return command;
    }

    public String makePingResponse(long currentTime) {
        return String.format(Locale.US, "0,%d", currentTime);
    }
}
