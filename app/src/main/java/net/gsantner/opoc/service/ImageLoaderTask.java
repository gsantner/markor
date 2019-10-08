/*#######################################################
 *
 *   Maintained by Gregor Santner, 2016-
 *   https://gsantner.net/
 *
 *   License of this file: GNU GPLv3 (Commercial upon request)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
#########################################################*/
package net.gsantner.opoc.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;

import java.io.File;


public class ImageLoaderTask<T> extends AsyncTask<File, Void, Bitmap> {
    private final static int MAX_DIMENSION = 5000;
    private final static int MAX_SIZE = 64;

    public interface OnImageLoadedListener<T> {
        void onImageLoaded(Bitmap bitmap, T callbackParam);
    }

    private final Context _context;
    private final OnImageLoadedListener _listener;
    private final T _callbackParam;
    private final boolean _loadThumbnail;

    public ImageLoaderTask(OnImageLoadedListener listener, Context context, boolean loadThumbnail, T callbackParam) {
        _listener = listener;
        _context = context;
        _callbackParam = callbackParam;
        _loadThumbnail = loadThumbnail;
    }

    private Bitmap loadStorageImage(File pathToImage) {
        File cacheFile = new File(_context.getCacheDir(), pathToImage.getAbsolutePath().substring(1));
        ContextUtils cu = ContextUtils.get();
        Bitmap bitmap;
        try {
            if (_loadThumbnail) {
                if (cacheFile.exists()) {
                    bitmap = cu.loadImageFromFilesystem(cacheFile, MAX_SIZE);
                } else {
                    bitmap = cu.loadImageFromFilesystem(pathToImage, MAX_SIZE);
                    cu.writeImageToFile(cacheFile, bitmap, Bitmap.CompressFormat.JPEG, 65);
                }
            } else {
                bitmap = cu.loadImageFromFilesystem(pathToImage, MAX_SIZE);
            }
        } catch (NullPointerException nul) {
            int expImage = AppSettings.get().isDarkThemeEnabled() ? R.drawable.ic_file_white_24dp : R.drawable.ic_file_gray_24dp;
            bitmap = cu.drawableToBitmap(ContextCompat.getDrawable(_context, expImage));
        }

        return bitmap;
    }

    @Override
    protected Bitmap doInBackground(File... params) {
        return loadStorageImage(params[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (_listener != null)
            _listener.onImageLoaded(bitmap, _callbackParam);
    }
}
