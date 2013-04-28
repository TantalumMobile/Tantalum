/*
 Copyright (c) 2013, Paul Houghton and Futurice Oy
 All rights reserved.

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
package org.tantalum.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.UnsupportedEncodingException;
import org.tantalum.Task;
import org.tantalum.storage.FlashCache;
import org.tantalum.storage.FlashDatabaseException;
import org.tantalum.storage.FlashFullException;
import org.tantalum.util.CryptoUtils;
import org.tantalum.util.L;

/**
 * Android implementation of cross-platform persistent storage using an SQLite
 * database. All web services you access over the net using a StaticWebCache
 * will be automatically stored here for faster access and offline use.
 *
 * You should not access this class directly. Use
 * <code>PlatformUtils.getInstance()...</code> instead.
 *
 * @author phou
 */
public final class AndroidCache extends FlashCache {

    private SQLiteOpenHelper helper;
    /**
     * Database version number
     */
    private static final int DB_VERSION = 2;
    /**
     * Database name
     */
    private static final String databaseName = "Tantalum";
    /**
     * Database table name
     */
    private final String TABLE_NAME = "Tantalum_Table" + +priority;
    /**
     * Database id column tag
     */
    private static final String COL_ID = "id";
    /**
     * Database data column tag
     */
    private static final String COL_DIGEST = "dig";
    /**
     * Database key column tag
     */
    private static final String COL_KEY = "key";
    /**
     * Database data column tag
     */
    private static final String COL_DATA = "data";
    /**
     * Android object used for associated the database with an application
     */
    private static Context context = null;
    /**
     * SQL to create the database
     */
    private final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME + "(" + COL_ID + " INTEGER PRIMARY KEY, " + COL_DIGEST + " BIGINT NOT NULL" + COL_KEY
            + " TEXT NOT NULL, " + COL_DATA + " BLOB NOT NULL)";
    private final String CLEAR_TABLE = "DROP TABLE " + TABLE_NAME;
    private SQLiteDatabase db = null;
    private final Object MUTEX = new Object();

    /**
     * Create a new AndroidCache. You should not call this method directly, but
     * rather request the cache from
     * <code>PlatformUtils.getInstance().getFlashCache(priority)</code>
     *
     * @param priority
     */
    public AndroidCache(final char priority) {
        super(priority);

        helper = new SQLiteOpenHelper(context, databaseName, null, DB_VERSION);
        (new Task(Task.SHUTDOWN) {
            public Object exec(final Object in2) {
                if (db != null) {
                    db.close();
                }
                db = null;

                return in2;
            }
        }.setClassName("CloseOnShutdown")).fork();
    }

    /**
     * Execute SQL to create the database
     *
     * @param db
     */
    public void onCreate(final SQLiteDatabase db) {
        synchronized (MUTEX) {
            db.execSQL(CREATE_TABLE);
        }
    }

    /**
     * Execute SQL to update the database
     *
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        synchronized (MUTEX) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    /**
     * Get the stored byte[] associated with the specified key
     *
     * @param digest
     * @return
     * @throws FlashDatabaseException
     */
    public byte[] get(final long digest) throws FlashDatabaseException {
        synchronized (MUTEX) {
            Cursor cursor = null;

            final String[] fields = new String[]{COL_DATA};
            //#debug
            L.i(this, "get()", "digest=" + Long.toString(digest, 16));
            try {
                if (db == null) {
                    db = helper.getWritableDatabase();
                }
                cursor = db.query(TABLE_NAME, fields, COL_DIGEST + "=?",
                        new String[]{"" + digest}, null, null, null, null);

                if (cursor == null || cursor.getCount() == 0) {
                    return null;
                } else {
                    cursor.moveToFirst();

                    return cursor.getBlob(0);
                }
            } catch (NullPointerException e) {
                //#debug
                L.e("db not initialized, join() then try again", "getData", e);
                return null;
            } catch (Exception e) {
                //#debug
                L.e("db can not be initialized", "getData, key=" + Long.toString(digest, 16), e);
                throw new FlashDatabaseException("db init error on getData, key=" + Long.toString(digest, 16) + " : " + e);
            } finally {
                //#debug
                L.i("db getData", "end");
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    /**
     * Search the database for the String key associated with this Digest
     *
     * @param digest
     * @return the string form of the digest
     * @throws FlashDatabaseException 
     */
    public String getKey(final long digest) throws FlashDatabaseException {
        synchronized (MUTEX) {
            Cursor cursor = null;

            final String[] fields = new String[]{COL_KEY};
            //#debug
            L.i(this, "getKey()", "digest=" + Long.toString(digest, 16));
            try {
                if (db == null) {
                    db = helper.getWritableDatabase();
                }
                cursor = db.query(TABLE_NAME, fields, COL_DIGEST + "=?",
                        new String[]{"" + digest}, null, null, null, null);

                if (cursor == null || cursor.getCount() == 0) {
                    return null;
                } else {
                    cursor.moveToFirst();

                    return cursor.getString(0);
                }
            } catch (NullPointerException e) {
                //#debug
                L.e("db not initialized, join() then try again", "getData", e);
                return null;
            } catch (Exception e) {
                //#debug
                L.e("db can not be initialized", "getData, key=" + Long.toString(digest, 16), e);
                throw new FlashDatabaseException("db init error on getData, key=" + Long.toString(digest, 16) + " : " + e);
            } finally {
                //#debug
                L.i("db getData", "end");
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    /**
     * Store the specified byte[] to flash memory for later retrieval by key
     *
     * @param url
     * @param data
     * @throws FlashFullException
     * @throws FlashDatabaseException
     */
    public void put(final String url, final byte[] data) throws FlashFullException, FlashDatabaseException {
        if (url == null) {
            throw new IllegalArgumentException("You attempted to put a null key to the cache");
        }
        if (data == null) {
            throw new IllegalArgumentException("You attempted to put null data to the cache");
        }

        synchronized (MUTEX) {
            final ContentValues values = new ContentValues();

            values.put(COL_KEY, url);
            values.put(COL_DATA, data);

            try {
                if (db == null) {
                    db = helper.getWritableDatabase();
                }
                db.insert(TABLE_NAME, null, values);
            } catch (Exception e) {
                try {
                    if (Class.forName("android.database.sqlite.SQLiteFullException").isAssignableFrom(e.getClass())) {
                        throw new FlashFullException("Android database full, attempting cleanup of old..." + url + " : " + e);
                    }
                } catch (ClassNotFoundException e2) {
                    //#debug
                    L.e("Introspection error", "android.database.sqlite.SQLiteFullException", e2);
                }
                //#debug
                L.e("Android cache put database exception", "" + url, e);
                throw new FlashDatabaseException("key = " + url + " : " + e);
            }
        }
    }

    /**
     * Remove the byte[] associated with this key from the database
     *
     * @param digest 
     * @throws FlashDatabaseException
     */
    public void removeData(final long digest) throws FlashDatabaseException {
        synchronized (MUTEX) {
            final String where;
            where = COL_KEY + "==\"" + Long.toString(digest, 16) + "\"";

            try {
                if (db == null) {
                    db = helper.getWritableDatabase();
                }
                db.delete(TABLE_NAME, where, null);
            } catch (Exception e) {
                //#debug
                L.e("Can not access database on removeData()", Long.toString(digest, 16), e);
                throw new FlashDatabaseException("Can not remove data from database: " + e);
            }
        }
    }

    /**
     * Get a list of all the keys for data available in this database
     *
     * @return
     * @throws FlashDatabaseException
     */
    public long[] getDigests() throws FlashDatabaseException {
        synchronized (MUTEX) {
            long[] digests = null;
            Cursor cursor = null;

            try {
                if (db == null) {
                    db = helper.getWritableDatabase();
                }
                cursor = db.query(TABLE_NAME, new String[]{COL_KEY}, "*",
                        null, null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    digests = new long[cursor.getCount()];
                    cursor.moveToFirst();
                    for (int i = 0; i < digests.length; i++) {
                        if (i != 0) {
                            cursor.moveToNext();
                        }
                        final String s = new String(cursor.getBlob(0), "UTF-8");
                        digests[i] = CryptoUtils.getInstance().toDigest(s);
                    }
                }
            } catch (Exception e) {
                //#debug
                L.e("Can not access database on getKeys()", "", e);
                throw new FlashDatabaseException("Can not acccess database on getKeys() : " + e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return digests;
        }
    }

    /**
     * Remove all elements from this cache. Since there is a different table for
     * each cache, other caches still contain values.
     *
     */
    public void clear() {
        synchronized (MUTEX) {
            db.execSQL(CLEAR_TABLE);
        }
    }
}