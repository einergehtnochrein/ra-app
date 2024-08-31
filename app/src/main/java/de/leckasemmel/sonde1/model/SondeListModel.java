package de.leckasemmel.sonde1.model;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

import de.leckasemmel.sonde1.R;
import de.leckasemmel.sonde1.SondeListItem;


public class SondeListModel {
    private static SondeListModel instance;
    public ArrayList<SondeListItem> heardList;
    public MutableLiveData<Boolean> updated;


    // Private constructor to prevent instantiation
    private SondeListModel() {
        heardList = new ArrayList<>();
        updated = new MutableLiveData<>(false);
    }

    public static synchronized SondeListModel getInstance() {
        if (instance == null) {
            instance = new SondeListModel();
        }
        return instance;
    }

    public void setUpdated(Boolean value) {
        updated.setValue(value);
    }
    public void postUpdated(Boolean value) {
        updated.postValue(value);
    }

    public void heardListUpdate(SondeListItem item) {
        // Already in list?
        SondeListItem existingItem = heardListFind(item.id);
        if (existingItem == null) {
            heardList.add(item);
        } else {
            heardList.set(heardList.indexOf(existingItem), item);
        }

        item.position = item.way.peekLast();

        // Derived values
        item.setValidPtu(
                !Double.isNaN(item.getPressure())
                        || !Double.isNaN(item.getTemperature())
                        || !Double.isNaN(item.getHumidity())
        );
        item.setValidFrequencyOffset(!Double.isNaN(item.getFrequencyOffset()));
        item.setValidPosition(
                !Double.isNaN(item.getLatitude())
                        || !Double.isNaN(item.getLongitude())
        );
        item.setValidExtra(!Double.isNaN(item.getTemperatureCpu()));
        item.setValidExtra(
                (item.getUsedSats() > 0)
                        || (item.getVisibleSats() > 0)
                        || item.getValidCpuTemperature()
        );

        switch (item.getSondeDecoder()) {
            case SONDE_DECODER_RS41:
                if ((item.getSpecial() & 0x01) != 0) {
                    item.setImageResId(R.drawable.rs41o3);
                }
                else if ((item.getSpecial() & 0x02) != 0) {
                    item.setImageResId(R.drawable.rs41sgm);
                }
                else {
                    item.setImageResId(R.drawable.rs41);
                }
                break;
            case SONDE_DECODER_C34_C50:
                if ((item.getSpecial() & 0x04) != 0) {
                    if ((item.getSpecial() & 0x01) != 0) {
                        item.setImageResId(R.drawable.c34o3);
                    }
                    else {
                        item.setImageResId(R.drawable.c34);
                    }
                }
                else if ((item.getSpecial() & 0x08) != 0) {
                    if ((item.getSpecial() & 0x01) != 0) {
                        item.setImageResId(R.drawable.c50o3);
                    }
                    else {
                        item.setImageResId(R.drawable.c50_wide);
                    }
                }
                else {
                    item.setImageResId(R.drawable.cxx);
                }
                break;
            case SONDE_DECODER_MEISEI:
                if ((item.getSpecial() & 0x04) != 0) {
                    item.setImageResId(R.drawable.ims100);
                }
                else {
                    item.setImageResId(R.drawable.rs11g);
                }
                break;
            case SONDE_DECODER_BEACON: item.setImageResId(R.drawable.epirb); break;
            case SONDE_DECODER_MRZ: item.setImageResId(R.drawable.mrz_mp3h1); break;
            case SONDE_DECODER_M10: item.setImageResId(R.drawable.m10); break;
            case SONDE_DECODER_M20: item.setImageResId(R.drawable.m20); break;
            case SONDE_DECODER_PSB3: item.setImageResId(R.drawable.psb3); break;
            case SONDE_DECODER_IMET54: item.setImageResId(R.drawable.imet54); break;
            case SONDE_DECODER_MTS01: item.setImageResId(R.drawable.mts01); break;
            case SONDE_DECODER_RS92:
                if ((item.getSpecial() & 0x01) != 0) {
                    item.setImageResId(R.drawable.rs92o3);
                }
                else {
                    item.setImageResId(R.drawable.rs92);
                }
                break;
            case SONDE_DECODER_GRAW:
                if ((item.getSpecial() & 0x04) != 0) {
                    item.setImageResId(R.drawable.dfm09_bf);
                }
                else if ((item.getSpecial() & 0x08) != 0) {
                    item.setImageResId(R.drawable.dfm09);
                }
                else if ((item.getSpecial() & 0x10) != 0) {
                    item.setImageResId(R.drawable.dfm06);
                }
                else if ((item.getSpecial() & 0x20) != 0) {
                    item.setImageResId(R.drawable.ps15);
                }
                else if ((item.getSpecial() & 0x200) != 0) {
                    item.setImageResId(R.drawable.dfm17);
                }
                else {
                    item.setImageResId(R.drawable.dfmx);
                }
                break;
            case SONDE_DECODER_IMET: item.setImageResId(R.drawable.imet4); break;
            case SONDE_DECODER_PILOT: item.setImageResId(R.drawable.pilot); break;
            case SONDE_DECODER_JINYANG: item.setImageResId(R.drawable.rsg20); break;
            case SONDE_DECODER_CF06: item.setImageResId(R.drawable.cf06); break;
            case SONDE_DECODER_GTH3: item.setImageResId(R.drawable.gth3); break;
            case SONDE_DECODER_S1: item.setImageResId(R.drawable.s1); break;
            case SONDE_DECODER_LMS6: item.setImageResId(R.drawable.lms6); break;
        }
    }

    public void heardListRemove(long id) {
        SondeListItem item = heardListFind(id);
        if (item != null) {
            heardList.remove(item);
        }
    }

    public SondeListItem heardListFind(long id) {
        SondeListItem result = null;

        for (SondeListItem item : heardList) {
            if (id == item.id) {
                result = item;
                break;
            }
        }

        return result;
    }

    public ArrayList<SondeListItem> getItems() {
        return this.heardList;
    }
    public int getNumItems() { return this.heardList.size(); }
}
