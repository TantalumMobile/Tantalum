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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tantalum.MockedStaticInitializers;


/**
 *
 * @author phou
 */
public class StringUtilsTest extends MockedStaticInitializers {

    static final String EXAMPLE_FILE_NAME = "/rss.xml";

    int[] intTest1 = {0xFF, 0x00};
    int[] intTest2 = {0xCD, 0x1A};
    byte[] byteTest1 = new byte[intTest1.length];
    byte[] byteTest2 = new byte[intTest2.length];

    @Before
    public final void fixtureStringUtilsTest() {
        copyToBytes(intTest1, byteTest1);
        copyToBytes(intTest2, byteTest2);
    }
    
    private void copyToBytes(int[] ints, byte[] bytes) {
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) (ints[i] & 0xFF);
            System.out.print((bytes[i] & 0xFF) + " ");
        }
        System.out.println();
    }
    
    final String sampleAsHex = "FF00EC11";
    final byte[] sampleAsBytes = {(byte) 0xFF, (byte) 0x00, (byte) 0xEC, (byte) 0x11 };
    @Test
    public void sampleBytesToHexString() {
        assertArrayEquals(sampleAsBytes, StringUtils.hexStringToByteArray(sampleAsHex));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void hexStringChecksForWrongLengthInput() {
        StringUtils.hexStringToByteArray("0");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void illegalCharsInHexStringFirstPosition() {
        StringUtils.hexStringToByteArray("QF");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void illegalCharsInHexStringSecondPosition() {
        StringUtils.hexStringToByteArray("FZ");
    }
    
    @Test
    public void sampleHexStringAsBytes() {
        assertEquals(sampleAsHex, StringUtils.byteArrayToHexString(sampleAsBytes));
    }

    final String roundTripConversionTest = "  Goodies ¥ƏĲɞʬ ΔЩԈբא بठः ↯∰✈Ⱡあヅ ﷻﺺ 㿜";
    final String urlDecodeConversionTest = "%20%20Goodies%20%C2%A5%C6%8F%C4%B2%C9%9E%CA%AC%20%CE%94%D0%A9%D4%88%D5%A2%D7%90%20%D8%A8%E0%A4%A0%E0%A4%83%20%E2%86%AF%E2%88%B0%E2%9C%88%E2%B1%A0%E3%81%82%E3%83%85%20%EF%B7%BB%EF%BA%BA%20%E3%BF%9C";
    @Test
    public void toHexAndBack() throws UnsupportedEncodingException {
        System.out.println("toHexAndBack");
        System.out.println("before hexencode: " + roundTripConversionTest);
        System.out.println("as hexencode: " + StringUtils.stringToHexString(roundTripConversionTest));
        System.out.println("after hexencode-decode: " + StringUtils.hexStringToString(StringUtils.stringToHexString(roundTripConversionTest)));
        
        assertEquals(roundTripConversionTest, StringUtils.hexStringToString(StringUtils.stringToHexString(roundTripConversionTest)));
    }

    @Test
    public void uuencodeAndBack() throws UnsupportedEncodingException, IOException {
        System.out.println("toUUcodeAndBack");
        System.out.println("before uuencode: " + roundTripConversionTest);
        System.out.println("as uuencode: " + StringUtils.urlEncode(roundTripConversionTest));
        System.out.println("after urlencode-urldecode: " + StringUtils.urlDecode(StringUtils.urlEncode(roundTripConversionTest)));
        
        assertEquals(roundTripConversionTest, StringUtils.urlDecode(StringUtils.urlEncode(roundTripConversionTest)));
    }

    @Test
    public void uudecode() throws UnsupportedEncodingException, IOException {
        assertEquals(roundTripConversionTest, StringUtils.urlDecode(urlDecodeConversionTest));
    }

    final String oneChar = "㿜";
    final String oneCharEncoded = "%E3%BF%9C";
    @Test
    public void uudecodeOneChar() throws UnsupportedEncodingException, IOException {
        System.out.println("oneChar " + oneChar + " - length=" + oneChar.length() + " - " + Integer.toHexString(oneChar.charAt(0)) + " - " + Integer.toBinaryString(oneChar.charAt(0)));
        char decoded = StringUtils.urlDecode(oneCharEncoded).charAt(0);
        System.out.println("decoded - " + Integer.toHexString(decoded) + " - " + Integer.toBinaryString(decoded));
        assertEquals(oneChar, StringUtils.urlDecode(oneCharEncoded));
    }
    
    final byte[] fourByteArray = {(byte) 0xF0, (byte) 0xA4, (byte) 0xAD, (byte) 0xA2};
    final String fourByteEncoded = "%F0%A4%AD%A2";
    @Test
    public void fourByteDecode() throws UnsupportedEncodingException, IOException {
        String fourByteString = new String(fourByteArray, "UTF-8");
        assertEquals(fourByteString, StringUtils.urlDecode(fourByteEncoded));
    }

    // Java internally modifies 4-byte UTF-8 format away from the standard. Doing this would require some extra work.
    @Ignore
    @Test
    public void fourByteEncode() throws UnsupportedEncodingException, IOException {
        String fourByteString = new String(fourByteArray, "UTF-8");
        assertEquals(fourByteEncoded, StringUtils.urlEncode(fourByteString));
    }
    
    final byte[] threeByteArray = {(byte) 0xE2, (byte) 0x82, (byte) 0xAC};
    final String threeByteEncoded = "%E2%82%AC";
    @Test
    public void threeByteDecode() throws UnsupportedEncodingException, IOException {
        String fourByteString = new String(threeByteArray, "UTF-8");
        assertEquals(fourByteString, StringUtils.urlDecode(threeByteEncoded));
    }

    @Test
    public void threeByteEncode() throws UnsupportedEncodingException, IOException {
        String threeByteString = new String(threeByteArray, "UTF-8");
        assertEquals(threeByteEncoded, StringUtils.urlEncode(threeByteString));
    }
    
    final byte[] twoByteArray = {(byte) 0xC2, (byte) 0xA2};
    final String twoByteEncoded = "%C2%A2";
    @Test
    public void twoByteDecode() throws UnsupportedEncodingException, IOException {
        String twoByteString = new String(twoByteArray, "UTF-8");
        assertEquals(twoByteString, StringUtils.urlDecode(twoByteEncoded));
    }

    @Test
    public void twoByteEncode() throws UnsupportedEncodingException, IOException {
        String twoByteString = new String(twoByteArray, "UTF-8");
        assertEquals(twoByteEncoded, StringUtils.urlEncode(twoByteString));
    }
    
    final byte[] oneByteArray = {(byte) 0x10};
    final String oneByteEncoded = "%10";
    @Test
    public void oneByteDecode() throws UnsupportedEncodingException, IOException {
        String oneByteString = new String(oneByteArray, "UTF-8");
        assertEquals(oneByteString, StringUtils.urlDecode(oneByteEncoded));
    }

    @Test
    public void oneByteEncode() throws UnsupportedEncodingException, IOException {
        String oneByteString = new String(oneByteArray, "UTF-8");
        assertEquals(oneByteEncoded, StringUtils.urlEncode(oneByteString));
    }

    @Test
    public void toHexTestFF00() {
        assertEquals("FF00", StringUtils.byteArrayToHexString(byteTest1));
    }

    @Test
    public void toHexTestCD1A() {
        assertEquals("CD1A", StringUtils.byteArrayToHexString(byteTest2));
    }
    
    @Test
    public void urlEncodeSimple() throws IOException {
        assertEquals("This%2Bis+a+simple+%26+short+test.", StringUtils.urlEncode("This+is a simple & short test."));
    }
    
    @Test
    public void urlDecodeSimple() throws IOException {
        assertEquals("This+is a simple & short test.", StringUtils.urlDecode("This%2Bis+a+simple+%26+short+test."));
    }
    
    @Test
    public void urlDecodeSpaces() throws IOException {
        assertEquals("  ", StringUtils.urlDecode("+%20"));
    }
    
    @Test
    public void urlEncodeSymbols() throws IOException {
        assertEquals("%24+%26+%3C+%3E+%3F+%3B+%23+%3A+%3D+%2C+%22+%27+%7E+%2B+%25", StringUtils.urlEncode("$ & < > ? ; # : = , \" ' ~ + %"));
    }
    
    @Test
    public void urlDecodeSymbols() throws IOException {
        assertEquals("$ & < > ? ; # : = , \" ' ~ + %", StringUtils.urlDecode("%24+%26+%3C+%3E+%3F+%3B+%23+%3A+%3D+%2C+%22+%27+%7E+%2B+%25"));
    }

    /**
     * Test of testReadStringFromJAR method, of class StringUtils.
     *
     * @throws Exception
     */
    @Test
    public void testReadStringFromJAR() throws Exception {
        String name_1 = "/test.txt";
        String expResult_1 = "Hello, this is test text";
        String result_1 = StringUtils.readStringFromJAR(name_1);
        assertEquals(expResult_1, result_1);
    }

    /**
     * Test of testReadBytesFromJAR method, of class StringUtils.
     *
     * @throws Exception
     */
    @Test
    public void testReadBytesFromJAR() throws Exception {
        //ClassLoader classLoader = this.getClass()
        // JSE classloader to verify the length of the file, rather than using a magic number
        InputStream stream = getClass().getResourceAsStream(EXAMPLE_FILE_NAME);

        assertNotNull("Could not read example file from classpath", stream);

        byte[] result_1 = StringUtils.readBytesFromJAR(EXAMPLE_FILE_NAME);
        assertNotNull(result_1);
        assertEquals(stream.available(), result_1.length);
    }

    @Test
    public void urlEncodeTraditionalChinese() throws IOException {
        assertEquals("%E6%BC%A2%E5%AD%97", StringUtils.urlEncode("漢字"));
    }
    
    @Test
    public void urlEncodeSimplifiedChinese() throws IOException {
        assertEquals("%E6%B1%89%E5%AD%97", StringUtils.urlEncode("汉字"));
    }
    
    @Test
    public void urlEncodeVietnamese() throws IOException {
        assertEquals("ch%E1%BB%AF+H%C3%A1n", StringUtils.urlEncode("chữ Hán"));
    }
    
    @Test
    public void urlEncodeZhuang() throws IOException {
        assertEquals("%E5%80%B1", StringUtils.urlEncode("倱"));
    }
    
    @Test
    public void urlEncodeKoreanHangul() throws IOException {
        assertEquals("%ED%95%9C%EC%9E%90", StringUtils.urlEncode("한자"));
    }
    
    @Test
    public void urlEncodeKoreanHanja() throws IOException {
        assertEquals("%E6%BC%A2%E5%AD%97", StringUtils.urlEncode("漢字"));
    }
    
    @Test
    public void urlEncodeJapaneseHiragana() throws IOException {
        assertEquals("%E3%81%8B%E3%82%93%E3%81%98", StringUtils.urlEncode("かんじ"));
    }
    
    @Test
    public void urlEncodeHindi() throws IOException {
        assertEquals("%E0%A4%AE%E0%A5%81%E0%A4%9D%E0%A5%87+%E0%A4%A6%E0%A5%8B+%E0%A4%95%E0%A4%AE%E0%A4%B0%E0%A4%BE+%E0%A4%9A%E0%A4%BE%E0%A4%B9%E0%A4%BF%E0%A4%8F", StringUtils.urlEncode("मुझे दो कमरा चाहिए"));
    }
}
