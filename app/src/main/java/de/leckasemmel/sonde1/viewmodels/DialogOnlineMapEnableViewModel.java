package de.leckasemmel.sonde1.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


public class DialogOnlineMapEnableViewModel extends ViewModel {

    public MutableLiveData<Boolean> enableMap1 = new MutableLiveData<>();
    public MutableLiveData<Boolean> enableMap2 = new MutableLiveData<>();
    public MutableLiveData<Boolean> enableMap3 = new MutableLiveData<>();
    public MutableLiveData<String> nameMap1 = new MutableLiveData<>();
    public MutableLiveData<String> nameMap2 = new MutableLiveData<>();
    public MutableLiveData<String> nameMap3 = new MutableLiveData<>();

    public void setEnableMap1 (Boolean enable) {
        enableMap1.setValue(enable);
    }
    public void setEnableMap2 (Boolean enable) {
        enableMap2.setValue(enable);
    }
    public void setEnableMap3 (Boolean enable) {
        enableMap3.setValue(enable);
    }
    public void setNameMap1 (String name) {
        nameMap1.setValue(name);
    }
    public void setNameMap2 (String name) {
        nameMap2.setValue(name);
    }
    public void setNameMap3 (String name) {
        nameMap3.setValue(name);
    }
}
