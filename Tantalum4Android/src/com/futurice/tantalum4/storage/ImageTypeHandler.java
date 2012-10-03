/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.futurice.tantalum4.storage;

import com.futurice.tantalum4.storage.DataTypeHandler;
import android.graphics.BitmapFactory;
import com.futurice.tantalum4.log.L;

/**
 * This is a helper class for creating an image class. It automatically converts
 * the byte[] to an Image as the data is loaded from the network or cache.
 *
 * @author tsaa
 */
public final class ImageTypeHandler implements DataTypeHandler {

    public ImageTypeHandler() {
    }

    public ImageTypeHandler(final boolean processAlpha, final boolean bestQuality, final int width) {
        /*
         * This constructor creates a resizing Image converter on S40, but on Android
         * we RAM cache the full sime image and let the resize be done at render
         * time, thus the "side" parameter is ignored.
         */
    }

    @Override
    public Object convertToUseForm(final byte[] bytes) {
        try {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            L.e("Exception converting bytes to image", bytes == null ? "" : "" + bytes.length, e);
            throw e;
        }
    }
}
