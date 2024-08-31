package de.leckasemmel.sonde1.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


public class HeardListViewModel extends ViewModel {
    public MutableLiveData<Long> heardListUpdated = new MutableLiveData<>(0L);
    public MutableLiveData<Boolean> heardListEmpty = new MutableLiveData<>(false);
    public int emptyHeardListImageResId;

    public HeardListViewModel() {
    }

    public void setEmptyHeardListImageResId (int id) {
        emptyHeardListImageResId = id;
    }

    public void setHeardListUpdated() {
        Long value = heardListUpdated.getValue();
        if (value != null) {
            heardListUpdated.setValue(value + 1);
        }
    }
}
