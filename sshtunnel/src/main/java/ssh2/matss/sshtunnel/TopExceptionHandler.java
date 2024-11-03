package ssh2.matss.sshtunnel;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
* Reporta erros
* @author dFiR30n
*/
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {
	private static final String FILE_ERROR = "stack.trace";
	
	private static TopExceptionHandler mExceptionHandler;
	
    private Thread.UncaughtExceptionHandler defaultUEH;
    private Context mContext;
	private File mFileTemp;
	
	// inicia
	public static void init(Context context) {
		if (mExceptionHandler == null) {
			mExceptionHandler = new TopExceptionHandler(context);
		}
		Thread.setDefaultUncaughtExceptionHandler(mExceptionHandler);
	}

    private TopExceptionHandler(Context context) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.mContext = context;
		this.mFileTemp = new File(mContext.getFilesDir(), FILE_ERROR);
	}
	
	public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
		
        String report = e.toString()+"\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (int i = 0; i < arr.length; i++) {
            report += "    " + arr[i].toString() + "\n";
        }
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
		report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++) {
                report += "    " + arr[i].toString() + "\n";
            }
        }
        report += "-------------------------------\n\n";

		// salva logs
		writeToFileLog(report, mContext);

        defaultUEH.uncaughtException(t, e);
    }
	
	private void writeToFileLog(String logError, Context context) {
		
		// save log to external storage
		/*File dir = new File(context.getExternalFilesDir("erros"),
			"Android/data/" + context.getPackageName() + "/files");
		
		if (!dir.exists()) {
			dir.mkdir();
		}*/
		
		// save log if possible
		//if (dir.canWrite()) {
			File logFile = new File(context.getExternalFilesDir("erros"), "SocksHttpLogError.txt");
			writeToFile(logError, logFile);
		//}
		
		// save temporary log
		writeToFile(logError, mFileTemp);
	}
	
	private void writeToFile(String txt, File file) {
		// create file if it doesn't exist
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch(IOException error) {
				// ..
			}
		}
        //overwrite the log
        try {
            FileOutputStream trace = new FileOutputStream(file);
            trace.write(txt.getBytes());
            trace.close();
        } catch(IOException ioe) {
			// ..
        }
	}
}
