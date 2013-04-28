/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tantalum.util;

import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simplified cryptography routines
 *
 * @author phou
 */
public class CryptoUtils {

    /**
     * The length of each digest in bytes
     *
     * A digest is a byte[] of this length
     */
    public static final int DIGEST_LENGTH = 16;
    private MessageDigest messageDigest;

    private static class CryptoUtilsHolder {

        static final CryptoUtils instance = new CryptoUtils();
    }

    /**
     * Get the singleton
     *
     * @return
     */
    public static CryptoUtils getInstance() {
        return CryptoUtilsHolder.instance;
    }

    private CryptoUtils() {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            //#debug
            L.e("Can not init CryptoUtils", "", ex);
        }
    }

    /**
     * Convert from String form, which may take a lot of RAM, into a fixed size
     * cryptographic digest.
     *
     * @param key
     * @return 16 byte cryptographic has
     * @throws DigestException
     * @throws UnsupportedEncodingException
     */
    public synchronized long toDigest(final String key) throws DigestException, UnsupportedEncodingException {
        if (key == null) {
            throw new IllegalArgumentException("You attempted to convert a null string into a hash digest");
        }
        final byte[] bytes = key.getBytes("UTF-8");

        return toDigest(bytes);
    }

    /**
     * Generate a cryptographic MD5 digest from a byte array
     *
     * @param bytes
     * @return
     * @throws DigestException
     * @throws UnsupportedEncodingException
     */
    public synchronized long toDigest(final byte[] bytes) throws DigestException, UnsupportedEncodingException {
        if (bytes == null) {
            throw new IllegalArgumentException("You attempted to convert a null byte[] into a hash digest");
        }
        final byte[] hashKey = new byte[DIGEST_LENGTH];

        messageDigest.update(bytes, 0, bytes.length);
        messageDigest.digest(hashKey, 0, DIGEST_LENGTH);
        
        final byte[] l = new byte[8];
        for (int i = 0; i < l.length; i++) {
            l[i] = (byte)(hashKey[2*i] ^ hashKey[1 + (2*i)]);
        }

        return bytesToLong(l, 0);
    }

    /**
     * Encode 8 bytes into one Long
     *
     * @param bytes
     * @param start
     * @return
     */
    public long bytesToLong(final byte[] bytes, final int start) {
        if (bytes == null || bytes.length != 8) {
            throw new IllegalArgumentException("Bad byteLength != 8 or null: can not convert digest to Long");
        }
        long l = 0;

        for (int i = 0; i < 8; i++) {
            l |= ((long) (bytes[start + i] & 0xFF)) << (8 * i);
        }

        return l;
    }

    /**
     * Encode one Long to 8 bytes
     *
     * @param l
     * @return
     */
    public byte[] longToBytes(final long l) {
        final byte[] bytes = new byte[8];

        longToBytes(l, bytes, 0);
        
        return bytes;
    }

    /**
     * Encode one Long to 8 bytes inserted into an existing array
     *
     * @param l
     * @param bytes
     * @param start
     */
    public void longToBytes(final long l, final byte[] bytes, final int start) {
        for (int i = 0; i < 8; i++) {
            bytes[start + i] = (byte)(((int)(l >>> (8*i))) & 0xFF);
        }
    }
}
