package de.leckasemmel.sonde1.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


public class MainViewModel extends ViewModel {

    public MutableLiveData<String> raName = new MutableLiveData<>();
    public MutableLiveData<Boolean> bleTransferVisible = new MutableLiveData<>();
    public MutableLiveData<String> bleTransferTitle = new MutableLiveData<>();
    public MutableLiveData<String> bleTransferProgressInfo = new MutableLiveData<>();
    public MutableLiveData<Boolean> bleTransferProgressVisible = new MutableLiveData<>();
    public MutableLiveData<Integer> bleTransferProgress = new MutableLiveData<>();

    public void setRaName (String value) {
        raName.setValue(value);
    }
    public void setBleTransferVisible (Boolean value) {
        bleTransferVisible.setValue(value);
    }
    public void setBleTransferTitle (String value) {
        bleTransferTitle.setValue(value);
    }
    public void setBleTransferProgressInfo (String value) {
        bleTransferProgressInfo.setValue(value);
    }
    public void setBleTransferProgressVisible (Boolean value) {
        bleTransferProgressVisible.setValue(value);
    }
    public void setBleTransferProgress (Integer value) {
        bleTransferProgress.setValue(value);
    }
}
