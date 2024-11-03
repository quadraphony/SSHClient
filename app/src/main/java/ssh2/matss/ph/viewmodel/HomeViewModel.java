package ssh2.matss.ph.viewmodel;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<Pair<String, String>> _bytesInOut = new MutableLiveData<>();
    public LiveData<Pair<String, String>> bytesInOut = _bytesInOut;

    public void setBytesInOut(Pair<String, String> label) {
        _bytesInOut.setValue(label);
    }

    private final MutableLiveData<String> _btnConnectLabel = new MutableLiveData<>();
    public LiveData<String> btnConnectLabel = _btnConnectLabel;

    private final MutableLiveData<String> _userTimer = new MutableLiveData<>();
    public LiveData<String> getUserTimer = _userTimer;

    private final MutableLiveData<Boolean> _enableViews = new MutableLiveData<>();
    public LiveData<Boolean> enableViews = _enableViews;

    public void setEnableViews(boolean is){
        _enableViews.setValue(is);
    }

    private final MutableLiveData<Pair<Boolean, Integer>> _widgetCustom = new MutableLiveData<>();
    public LiveData<Pair<Boolean, Integer>> widgetCustom = _widgetCustom;

    public void enableWidgetCustom(Pair<Boolean, Integer> is){
        _widgetCustom.setValue(is);
    }

    private final MutableLiveData<Pair<String, Integer>> _updateAuthor = new MutableLiveData<>();
    public LiveData<Pair<String, Integer>> getAuthorView = _updateAuthor;

    public void setAuthorView(Pair<String, Integer> pair){
        _updateAuthor.setValue(pair);
    }

    private final MutableLiveData<Boolean> _updateImgTheme = new MutableLiveData<>();

    public LiveData<Boolean> getImgTheme() {
           return _updateImgTheme;
    }

    public void setImgTheme(Boolean is){
        _updateImgTheme.setValue(is);
    }

    /**
     * Functions String
     */

    public void setBtnConnectLabel(String label){
        _btnConnectLabel.setValue(label);
    }


    public void setTimerUser(String label){
        _userTimer.setValue(label);
    }


}
