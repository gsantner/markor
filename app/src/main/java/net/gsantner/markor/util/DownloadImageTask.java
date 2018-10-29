package net.gsantner.markor.util;
<<<<<<< HEAD
=======

>>>>>>> db8da316d2e3f71c8cb2f3f68f19ac041477de81
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
<<<<<<< HEAD
import java.io.InputStream;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;
    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }
=======

import java.io.InputStream;

/**
 * Created by Rado on 10/14/2018.
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

>>>>>>> db8da316d2e3f71c8cb2f3f68f19ac041477de81
    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }
<<<<<<< HEAD
    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}
=======

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
}
>>>>>>> db8da316d2e3f71c8cb2f3f68f19ac041477de81
