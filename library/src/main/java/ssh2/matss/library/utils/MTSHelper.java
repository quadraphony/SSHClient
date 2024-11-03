package ssh2.matss.library.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import com.scottyab.rootbeer.RootBeer;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MTSHelper {

    private final Context c;

    public MTSHelper(Context context){
        this.c = context;
    }

    public boolean isRooted(){
        RootBeer rootBeer = new RootBeer(c);
        return rootBeer.isRooted();
    }

    public boolean isValidityExpired(long validateDateMillis) {
        if (validateDateMillis == 0) {
            return false;
        }

        // Get Current Date
        long current_date = Calendar.getInstance()
                .getTime().getTime();

        if (current_date >= validateDateMillis) {
            return true;
        }

        return false;
    }

    @SuppressLint("DefaultLocale")
    public String secondsToString(long seconds)
    {
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24L);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
        long secs = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

        return String.format("%02d:%02d:%02d:%02d", day, hours, minute, secs);
    }

}
