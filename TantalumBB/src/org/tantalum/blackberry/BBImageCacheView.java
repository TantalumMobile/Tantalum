/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tantalum.blackberry;

import net.rim.device.api.system.Bitmap;
import org.tantalum.storage.ImageCacheView;
import org.tantalum.util.L;
import org.tantalum.util.LOR;

/**
 *
 * @author ADIKSONLINE
 */
public class BBImageCacheView extends ImageCacheView {

    public Object convertToUseForm(Object key, LOR bytesReference) {
        if (bytesReference == null){
            throw new NullPointerException("cannot convert null bytes to image");
        }
        try {
            byte[] bytes = bytesReference.getBytes();
            bytesReference.clear();
            Bitmap b = Bitmap.createBitmapFromBytes(bytes, 0, bytes.length, 1);
            if (maxWidth == SCALING_DISABLED || maxHeight == SCALING_DISABLED) {
                return b;
            }
            Bitmap scaled = new Bitmap(maxWidth, maxHeight);
            b.scaleInto(scaled, Bitmap.FILTER_BOX);
            return scaled;
        } catch (IllegalArgumentException e) {
            //#debug
            L.e("Exception converting bytes to image", "key=" + key, e);
            throw e;
        }
    }
    
}
