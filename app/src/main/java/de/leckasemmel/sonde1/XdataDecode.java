package de.leckasemmel.sonde1;

import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import java.util.HashMap;
import java.util.Locale;


public class XdataDecode {
    private static XdataDecode instance;
    private final int INSTRUMENT_ID_ECCO3 = 0x01;
    private final int INSTRUMENT_ID_OIF411 = 0x05;
    private final int INSTRUMENT_ID_CFH = 0x08;
    private final int INSTRUMENT_ID_COBALD = 0x19;
    private final int INSTRUMENT_ID_PCFH = 0x3C;
    private final int INSTRUMENT_ID_SKYDEW = 0x3F;
    private final int INSTRUMENT_ID_KNMI_TACHO = 0x80;

    public XdataDecode() {}

    public static synchronized XdataDecode getInstance() {
        if (instance == null) {
            instance = new XdataDecode();
        }
        return instance;
    }

    private int safeIntFromHexStringRange(String s, int from, int to, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(s.substring(from, to), 16);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            value = defaultValue;
        }

        return value;
    }

    public String decode(
            SondeListItem.SondeDecoder decoder,
            HashMap<Integer, SondeListItem.Xdata> xdata
    ) {
        StringBuilder result = new StringBuilder();

        if (xdata != null) {
            for (HashMap.Entry<Integer, SondeListItem.Xdata> entry : xdata.entrySet()) {
                SondeListItem.Xdata value = entry.getValue();

                // Create a default result if we don't know better.
                StringBuilder item = new StringBuilder();
                item.append(String.format(Locale.US, "ID=0x%02X  %s\n", value.instrument, value.message));

                if (SondeListItem.SondeDecoder.SONDE_DECODER_RS41 == decoder) {
                    if (value.instrument == INSTRUMENT_ID_ECCO3) {
                        item = new StringBuilder();
                        int cellCurrent = safeIntFromHexStringRange(value.message, 0, 4, 0);
                        int pumpTemperature = safeIntFromHexStringRange(value.message, 4, 8, 0);
                        int pumpCurrent = safeIntFromHexStringRange(value.message, 8, 10, 0);
                        int batteryVoltage = safeIntFromHexStringRange(value.message, 10, 12, 0);
                        item.append(String.format(Locale.US,
                                "ID=0x%02X (ECC Ozone)\n  cell=%.3fµA  vbat=%.1fV\n  pump: %.0fmA  %.2f°C\n",
                                value.instrument,
                                cellCurrent * 0.001,
                                batteryVoltage * 0.1,
                                pumpCurrent * 1.0,
                                pumpTemperature * 0.01
                        ));
                    }

                    if (value.instrument == INSTRUMENT_ID_OIF411) {
                        if (value.message.length() == 16) {
                            item = new StringBuilder();

                            int pumpT = safeIntFromHexStringRange(value.message, 0, 4, 0);
                            if (pumpT > 32768) {
                                pumpT = -(pumpT - 32768);
                            }
                            int ozoneCurrent = safeIntFromHexStringRange(value.message, 4, 9, 0);
                            int batteryVoltage = safeIntFromHexStringRange(value.message, 9, 11, 0);
                            int pumpCurrent = safeIntFromHexStringRange(value.message, 11, 14, 0);
                            int extVoltage = safeIntFromHexStringRange(value.message, 14, 16, 0);
                            item.append(String.format(Locale.US,
                                    "ID=0x%02X (OIF411)\n  o3=%.4fµA  ti=%.1f°C  Pump=%.0fmA  %.1fV  ext=%.1fV\n",
                                    INSTRUMENT_ID_OIF411,
                                    ozoneCurrent / 10000.0,
                                    pumpT / 100.0,
                                    pumpCurrent * 1.0,
                                    batteryVoltage / 10.0,
                                    extVoltage / 10.0));
                        }
                        else if (value.message.length() == 17) {
                            item = new StringBuilder();

                            int diagnostic = safeIntFromHexStringRange(value.message, 8, 12, 0);
                            int softwareVersion = safeIntFromHexStringRange(value.message, 12, 16, 0);
                            item.append(String.format(Locale.US,
                                    "ID=0x%02X (OIF411)\n  %s %04X SW=%.2f\n",
                                    INSTRUMENT_ID_OIF411,
                                    value.message.substring(0, 8),
                                    diagnostic,
                                    softwareVersion / 100.0));
                        }
                    }

                    if (value.instrument == INSTRUMENT_ID_CFH) {
                        item = new StringBuilder();
                        int mirrorTemperature = safeIntFromHexStringRange(value.message, 0, 6, 0);
                        int detectorSignal = safeIntFromHexStringRange(value.message, 6, 12, 0);
                        int opticsTemperature = safeIntFromHexStringRange(value.message, 12, 16, 0);
                        int cfhBattery = safeIntFromHexStringRange(value.message, 16, 20, 0);
                        item.append(String.format(Locale.US,
                                "ID=0x%02X (CFH)\n  %.1f°C  %.2fV  %.1f°C  %.1fV\n",
                                value.instrument,
                                mirrorTemperature * 1.132488e-5 - 140.0,
                                detectorSignal * 2.980232e-7,
                                53.23 - opticsTemperature * 9.765625e-3,
                                cfhBattery * 2.9296875e-2
                        ));
                    }

                    if (value.instrument == INSTRUMENT_ID_COBALD) {
                        item = new StringBuilder();
                        int serial = safeIntFromHexStringRange(value.message, 0, 3, 0);
                        //int temperature = ???
                        int signalBlue = safeIntFromHexStringRange(value.message, 6, 12, 0);
                        int signalRed = safeIntFromHexStringRange(value.message, 12, 18, 0);
                        int monitorBlue = safeIntFromHexStringRange(value.message, 18, 22, 0);
                        int monitorRed = safeIntFromHexStringRange(value.message, 22, 26, 0);
                        item.append(String.format(Locale.US,
                                "ID=0x%02X (COBALD)\n  serial=%d  T=?\n  blue: %d / %d\n  red:  %d / %d\n",
                                INSTRUMENT_ID_COBALD,
                                serial,
                                signalBlue, monitorBlue,
                                signalRed, monitorRed
                                ));
                    }

                    if (value.instrument == INSTRUMENT_ID_PCFH) {
                        item = new StringBuilder();
                        item.append(String.format(Locale.US,
                                "ID=0x%02X (Peltier CFH)\n  %s\n",
                                value.instrument,
                                value.message
                        ));
                    }

                    if (value.instrument == INSTRUMENT_ID_SKYDEW) {
                        item = new StringBuilder();
                        item.append(String.format(Locale.US,
                                "ID=0x%02X (SKYDEW)\n  %s\n",
                                value.instrument,
                                value.message
                        ));
                    }

                    if (value.instrument == INSTRUMENT_ID_KNMI_TACHO) {
                        item = new StringBuilder();
                        item.append(String.format(Locale.US,
                                "ID=0x%02X (KNMI Tachometer)\n  %s\n",
                                value.instrument,
                                value.message
                        ));
                    }
                }

                result.append(item);
            }
        }

        return result.toString();
    }

    @BindingAdapter({"xdataSonde", "xdata"})
    public static void handleXdata(TextView view, SondeListItem.SondeDecoder sonde, HashMap<Integer, SondeListItem.Xdata> xdata) {
        String text = XdataDecode.getInstance().decode(sonde, xdata);
        if (!view.getText().equals(text)) {
            view.setText(text);
        }
    }
}
