/*
 Copyright (c) 2013 Nokia Corporation. All rights reserved.
 Nokia and Nokia Connecting People are registered trademarks of Nokia Corporation.
 Oracle and Java are trademarks or registered trademarks of Oracle and/or its
 affiliates. Other product and company names mentioned herein may be trademarks
 or trade names of their respective owners.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package org.tantalum.util;

import java.lang.ref.WeakReference;
import java.util.Vector;

/**
 * Objects stored in the WeakRefernce hash table may be garbage collected at any
 * time if the phone needs more memory. Explicit memory managment is also
 * supported such that any object manually remove()d from the cache is pooled
 * and can be re-used via getFromPool().
 *
 * This class can be useful for example with procedural graphics paint
 * acceleration where an re-paintable graphics object is pooled as it goes off
 * one edge of the screen and re-used for an equal sized object appearing at the
 * bottom of the screen. If sufficient memory is available, the WeakReference
 * hashtable also allows still-valid objects that were not explicitly removed
 * (such as those still on-screen) to be re-used without repainting, but if
 * other parts of the program need more memory temporarily they can bump these
 * cache objects out of memory.
 *
 * @author phou
 */
public class PoolingWeakHashCache extends WeakHashCache {

    private final Vector pool = new Vector();

    /**
     * Remove an object from the cache. It may then be placed in the pool for
     * possible re-use to minimize heap memory thrash.
     *
     * @param key
     * @return true if removed. The WeakReference may have expired, so this does
     * not necessarily mean that the current pool size increases by one.
     */
    public boolean remove(final Object key) {
        synchronized (hash) {
            boolean removed = false;

            if (key == null) {
                //#debug
                L.i("PoolingWeakHashCache", "remove() with null key");
            } else {
                final WeakReference wr = (WeakReference) hash.get(key);

                if (wr != null) {
                    removed = hash.remove(key) != null;
                    if (wr.get() != null) {
                        //#debug
                        L.i("Adding to pool", key.toString());
                        pool.addElement(wr);
                    }
                }
            }

            return removed;
        }
    }

    /**
     * Get an object from the pool for re-use if one is available.
     *
     * @return - null if the pool is empty
     */
    public Object getFromPool() {
        synchronized (hash) {
            Object o = null;

            while (pool.size() > 0) {
                final WeakReference wr = (WeakReference) pool.firstElement();
                pool.removeElementAt(0);
                o = wr.get();
                if (o != null) {
                    break;
                }
            }

            return o;
        }
    }

    /**
     * Clear both the cache and the pool of re-use objects
     */
    public void clear() {
        synchronized (hash) {
            super.clear();

            pool.removeAllElements();
        }
    }
}
