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
package org.tantalum.jme;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Only one image processing routine will be active at a time. This is enforced
 * by internal synchronization, so image processing is thread safe with minimal
 * peak memory usage if you call these routines from multiple threads.
 *
 * @author phou
 */
public final class JMEImageUtils {

    /**
     * Although just slightly slower than ONE_POINT_PICK, this algorithm does a
     * log more by combing a weighted sampling 5 pixels from the source image to
     * generate each resized image pixel. The result is usually much more
     * detailed without much diagonal line step artifacts. Due to edge effects
     * of sampling the source image with 5 pixels in a X, the destination image
     * appears at a slightly different zoom than other images, and two
     * individually resized images tiled next to one another will not combine
     * seamlessly. In such cases, use a weighted average algorithm instead.
     * Generally this is very fast- the best performing scaling algorithm for
     * general use and a good default choice.
     */
    public static final int FIVE_POINT_BLEND = 0;
    /**
     * The fastest scaling algorithm, this pulls one pixel from the source image
     * to the destination image, thus decimating the image to make it smaller.
     * You can see visual artifacts such as diagonal lines displaying a
     * stair-step effect, and detail is not preserved as the image gets smaller.
     */
    public static final int BASIC_ONE_POINT_PICK = 1;
    /**
     * This is BASIC_ONE_POINT_PICK slightly modified to improve the legibility
     * when scaling down to very small sizes. The image is quickly halfed along
     * width and height until that is no longer possible, then ONE_POINT_PICK.
     * This is equally fast on larger images, and while slightly slower it
     * retains much of the image detail even on extreme down scaling. Because of
     * the image halfing which comgines ARGB values of 4 pixels into 1, the
     * exact time this takes can vary more than ONE_POINT_PICK
     */
    public static final int ONE_POINT_PICK = 2;
    /**
     * This algorithm combines the relative contributions of all source pixels
     * with appropriate weighting. The alpha channel is assumed to be opaque to
     * achieve a slight speed increase. This produces the best visual result,
     * but takes about 4 times as long as the FIVE_POINT_BLEND
     */
    public static final int WEIGHTED_AVERAGE_OPAQUE = 3;
    /**
     * This is the weighted average combination of pixels algorithm including
     * alpha channel blending to produce scaled translucent images for layering
     * effects. If your source image is opaque (all JPEG images are), or if you
     * will not visually blend the image with the background at render time, use
     * WEIGHTED_AVERAGE_OPAQUE instead.
     */
    public static final int WEIGHTED_AVERAGE_TRANSLUCENT = 4;
    private static final int MAX_SCALING_ALGORITHM = WEIGHTED_AVERAGE_TRANSLUCENT;
    private static final int M1 = 0x7F7F7F7F;
    private static final int M2 = 0x3F3F3F3F;
    private static final int M3 = 0x1F1F1F1F;
    private static final int FP_SHIFT = 12;
    private static final int FP_MASK = 0x00000FFF;
    private static final int ALPHA = 0xFF000000;
    private static final int RED = 0x00FF0000;
    private static final int GREEN = 0x0000FF00;
    private static final int BLUE = 0x000000FF;

    /**
     * Convenience class for scaling images. Images are only made smaller on one
     * axis or both. Upscaling is not supported.
     *
     * This handles all the buffer management for you. It has higher peak memory
     * usage and not buffer re-use which means the garbage collector will run
     * more frequently. So if you will scale images in a tight loop, better
     * performance can be achieved if you optimize your code and call the
     * buffered scaling methods directly.
     *
     * @param sourceImage
     * @param maxW - use Integer.MAX_VALUE if you do not want to scale by width
     * @param maxH - use Integer.MAX_VALUE if you do not want to scale by height
     * @param preserveAspectRatio - set false to shrink by different amounts on
     * each axis, but not more than needed to fit in the bounding box specified
     * by maxW and maxH
     * @param scalingAlgorithm
     * @return
     */
    public static Image scaleImage(final Image sourceImage, final int maxW,
            final int maxH, final boolean preserveAspectRatio, final int scalingAlgorithm) {
        if (maxW == sourceImage.getWidth() && maxH == sourceImage.getHeight()) {
            return sourceImage;
        }
        if (maxW < 1 || maxH < 1) {
            throw new IllegalArgumentException("scaleImage requires maxW and maxH be >= 1");
        }

        final int[] inputImageARGB = new int[sourceImage.getWidth()
                * sourceImage.getHeight()];

        sourceImage.getRGB(inputImageARGB, 0, sourceImage.getWidth(), 0, 0,
                sourceImage.getWidth(), sourceImage.getHeight());
        final int[] outputImageARGB;
        if (sourceImage.getWidth() >= maxW
                && sourceImage.getWidth() * sourceImage.getHeight() >= maxW
                * maxH) {
            outputImageARGB = inputImageARGB;
        } else {
            outputImageARGB = new int[maxW * maxH];
        }

        return scaleImage(inputImageARGB, outputImageARGB,
                sourceImage.getWidth(), sourceImage.getHeight(), maxW, maxH,
                preserveAspectRatio, scalingAlgorithm);
    }

    /**
     * Return an image which is, if needed to fit inside a bounding box,
     * down-scaled and smaller then the original on one or both axes. The image
     * will not be up-scaled if smaller than the bounding box.
     *
     * Using this variant receiving an <source>int[]</source>
     * instead of <source>Image</source> allows you to reduce peak memory usage
     * during scaling. This reduces the likelihood you will see OutOfMemory
     * problems when scaling. A low-memory example usage pattern is below. The
     * <source>Task.LARGE_MEMORY_MUTEX</source> prevents multiple threads from
     * entering memory-spike sections simultaneously. The source and result data
     * arrays can be the same for maximum speed and minimal memory consumption.
     *
     * <source><pre>synchronized (Task.LARGE_MEMORY_MUTEX) {
     *    final int[] data = new int[w * h];
     *    image.getRGB(data, 0, w, 0, 0, w, h);
     *    image = null;
     *    image = JMEImageUtils.scaleImage(data, data, w, h, maxW, maxH, true,
     *       false, false);
     * }</pre></source>
     *
     * @param inputImageARGB - ARGB data for the original image
     * @param outputImageARGB - ARGB data buffer for the scaled image, can be
     * the same as inputImageARGB if downscaling (faster)
     * @param srcW - Source image width
     * @param srcH - Source image height
     * @param maxW - maximum (bounding box, will not upscale) width of scaled
     * image
     * @param maxH - maximum (bounding box, will not upscale) height of scaled
     * image
     * @param preserveAspectRatio - set true except for special effects
     * @param scalingAlgorithm - a constant from JMEImageUtils specifying how to
     * scale
     * @return
     */
    public static Image scaleImage(final int[] inputImageARGB,
            final int[] outputImageARGB, int srcW, int srcH, int maxW,
            int maxH, final boolean preserveAspectRatio,
            final int scalingAlgorithm) {
        if (scalingAlgorithm < 0 || scalingAlgorithm > MAX_SCALING_ALGORITHM) {
            throw new IllegalArgumentException("Unsupported scaling algorithm "
                    + scalingAlgorithm + ", should be [0-"
                    + MAX_SCALING_ALGORITHM + "]");
        }
        if (maxW < 1 || maxH < 1 || srcW < 1 || srcH < 1) {
            throw new IllegalArgumentException("scaleImage requires maxW, maxH, srcW and srcH be >= 1");
        }
        if (inputImageARGB == null || outputImageARGB == null) {
            throw new NullPointerException("scaleImage requires non-null input and output image buffers");
        }

        final float byWidth = maxW / (float) srcW;
        final float byHeight = maxH / (float) srcH;
        boolean widthIsMaxed = false;

        if (preserveAspectRatio) {
            if (byWidth <= byHeight) {
                maxW = (int) (srcW * byWidth);
                maxH = (int) (srcH * byWidth);
            } else {
                maxW = (int) (srcW * byHeight);
                maxH = (int) (srcH * byHeight);
            }
        }
        if (maxW >= srcW) {
            maxW = srcW;
            widthIsMaxed = true;
        }
        final boolean processAlpha = scalingAlgorithm != JMEImageUtils.WEIGHTED_AVERAGE_OPAQUE;
        if (maxH >= srcH) {
            if (widthIsMaxed) {
                // No resize needed
                maxH = srcH;
                return Image.createRGBImage(inputImageARGB, maxW, maxH,
                        processAlpha);
            }
            maxH = srcH;
        }
        switch (scalingAlgorithm) {
            default:
            case ONE_POINT_PICK:
                while (srcW >> 1 > maxW && srcH >> 1 > maxH) {
                    JMEImageUtils.half(inputImageARGB, inputImageARGB, srcW,
                            srcH);
                    srcW /= 2;
                    srcH /= 2;
                }
                if (srcW >> 1 == maxW && srcH >> 1 == maxH) {
                    JMEImageUtils.half(inputImageARGB, outputImageARGB, srcW,
                            srcH);
                    break;
                }
            case BASIC_ONE_POINT_PICK:
                JMEImageUtils.onePointPick(inputImageARGB, outputImageARGB, srcW,
                        srcH, maxW, maxH);
                break;
            case FIVE_POINT_BLEND:
                while (srcW / 2 > maxW && srcH / 2 > maxH) {
                    JMEImageUtils.half(inputImageARGB, inputImageARGB, srcW,
                            srcH);
                    srcH /= 2;
                    srcW /= 2;
                }
                if (srcW / 2 == maxW && srcH / 2 == maxH) {
                    JMEImageUtils.half(inputImageARGB, outputImageARGB, srcW,
                            srcH);
                    break;
                }
                JMEImageUtils.fivePointSampleDownscale(inputImageARGB,
                        outputImageARGB, srcW, srcH, maxW, maxH);
                break;
            case WEIGHTED_AVERAGE_TRANSLUCENT:
                if (srcW < maxW || srcH < maxH) {
                    JMEImageUtils.pureUpscale(inputImageARGB, outputImageARGB, srcW,
                            srcH, maxW, maxH, preserveAspectRatio);
                } else {
                    JMEImageUtils.pureDownscale(inputImageARGB, outputImageARGB, srcW,
                            srcH, maxW, maxH, preserveAspectRatio);
                }
                break;
            case WEIGHTED_AVERAGE_OPAQUE:
                JMEImageUtils.pureOpaqueDownscale(inputImageARGB, outputImageARGB,
                        srcW, srcH, maxW, maxH, preserveAspectRatio);
                break;
        }

        return Image.createRGBImage(outputImageARGB, maxW, maxH, processAlpha);
    }

    /**
     * Draw an image squeezed along the horizontal and/or vertical dimensions
     * (no preservation of aspect ratio) centered at (x, y).
     *
     * @param g - object to draw onto
     * @param x - horizontal center of where to draw the resulting image
     * @param y - vertical center of where to draw the resulting image
     * @param inputImageARGB
     * @param outputImageARGB - often this is the same as inputImageARGB for
     * speed
     * @param srcW - source image width
     * @param srcH - source image height
     * @param maxW - target width
     * @param maxH - target height
     * @param processAlpha - set false for speed if the image is opaque
     */
    public static void drawFlipshade(final Graphics g, final int x,
            final int y, final int[] inputImageARGB,
            final int[] outputImageARGB, int srcW, int srcH, int maxW,
            int maxH, final boolean processAlpha) {
        if (maxW < 1 || maxH < 1 || srcW < 1 || srcH < 1) {
            throw new IllegalArgumentException("drawFlipshade requires maxW, maxH, srcW and srcH be >= 1");
        }
        if (inputImageARGB == null || outputImageARGB == null) {
            throw new NullPointerException("drawFlipshade requires non-null input and output image buffers");
        }

        while (srcW >> 1 > maxW && srcH >> 1 > maxH) {
            JMEImageUtils.half(inputImageARGB, inputImageARGB, srcW, srcH >>= 1);
            srcW >>= 1;
        }
        if (srcW >> 1 == maxW && srcH >> 1 == maxH) {
            JMEImageUtils.half(inputImageARGB, outputImageARGB, maxW, maxH);
        } else {
            maxW = Math.min(srcW, maxW);
            maxH = Math.min(srcH, maxH);
            JMEImageUtils.fivePointSampleDownscale(inputImageARGB,
                    outputImageARGB, srcW, srcH, maxW, maxH);
        }
        g.drawRGB(outputImageARGB, 0, maxW, x - (maxW >> 1), y - (maxH >> 1),
                maxW, maxH, processAlpha);
    }

    /**
     * Return an ARGB image where width and height are half the original.
     *
     * 4 pixels are combined into 1 with 6 bit accuracy.
     *
     * @param in
     * @param srcW
     * @return
     */
    private static void half(final int[] in, final int[] out,
            final int srcW, final int srcH) {
        int z = 0;

        if (in == null) {
            throw new NullPointerException("Half a source image, but source array is null");
        }
        if (out == null) {
            throw new NullPointerException("Half a source image, but destination array is null");
        }
        if (in.length < srcW * srcH) {
            throw new IllegalArgumentException("Half a source image, but source array is less than data dimensions: " + in.length + " < " + srcW + "*" + srcH);
        }
        if (out.length < (srcW / 2) * (srcH / 2)) {
            throw new IllegalArgumentException("Half a source image, but destination array is less than data dimensions: " + out.length + " < " + srcW / 2 + "*" + srcH / 2);
        }

        for (int y = 0; y < srcH - 1; y += 2) {
            int sourceImagePixelIndex = y * srcW;
            for (int x = 0; x < srcW - 1; x += 2) {
                int e = (in[sourceImagePixelIndex++] >>> 2) & M2;
                e += (in[sourceImagePixelIndex--] >>> 2) & M2;
                sourceImagePixelIndex += srcW;
                e += (in[sourceImagePixelIndex++] >>> 2) & M2;
                out[z++] = e + ((in[sourceImagePixelIndex++] >>> 2) & M2);
                sourceImagePixelIndex -= srcW;
            }
        }
    }

    /**
     * Special thanks to Dr Teemu Korhonen for the original, very fast Matlab
     * algorithm and tests. A weighted "X" is slid across the source image to
     * generate destination pixels.
     *
     * @param in
     * @param srcW
     * @param srcH
     * @param w
     * @param h
     */
    private static void fivePointSampleDownscale(final int[] in,
            final int[] out, final int srcW, final int srcH,
            final int w, final int h) {
        if (in == null) {
            throw new NullPointerException("Five point downsample a source image, but source array is null");
        }
        if (out == null) {
            throw new NullPointerException("Five point downsample a source image, but destination array is null");
        }
        if (in.length < srcW * srcH) {
            throw new IllegalArgumentException("Five point downsample a source image, but source array is less than image dimensions: " + in.length + " < " + srcW + "*" + srcH);
        }
        if (out.length < w * h) {
            throw new IllegalArgumentException("Five point downsample a source image, but destination array is less than destination image dimensions: " + out.length + " < " + w + "*" + h);
        }

        final int dxFP = toFixedPoint(srcW / (float) (w + 2));
        final int dyFP = toFixedPoint(srcH / (float) (h + 2));
        int z = 0;

        for (int y = 0; y < h; y++) {
            final int rowstart = 1 + srcW + (srcW * fixedPointToInt(y * dyFP));
            for (int x = 0; x < w; x++) {
                int i = rowstart + fixedPointToInt(x * dxFP);
                int e = in[i--] >>> 1 & M1;
                i -= srcW;
                e += (in[i++] >>> 3 & M3);
                e += (in[++i] >>> 3 & M3);
                i += srcW << 1;
                e += in[i--] >>> 3 & M3;
                out[z++] = e + (in[--i] >>> 3 & M3);
            }
        }
    }

    /**
     * A single point selected from the source image to generate destination
     * pixels.
     *
     * @param in
     * @param srcW
     * @param srcH
     * @param w
     * @param h
     */
    private static void onePointPick(final int[] in, final int[] out,
            final int srcW, final int srcH, final int w, final int h) {
        final int dxFP = toFixedPoint(srcW / (float) w);
        final int dyFP = toFixedPoint(srcH / (float) h);
        int z = 0;

        for (int y = 0; y < h; y++) {
            final int rowstart = 1 + srcW + (srcW * fixedPointToInt(y * dyFP));
            for (int x = 0; x < w; x++) {
                out[z++] = in[rowstart + fixedPointToInt(x * dxFP)];
            }
        }
    }

    /**
     *
     * @param i
     * @return
     */
    // FIXME: Not being used
//    private static int toFixedPoint(final int i) {
//        return i << FP_SHIFT;
//    }

    /**
     *
     * @param f
     * @return
     */
    private static int toFixedPoint(final float f) {
        return (int) (f * (1 << FP_SHIFT));
    }

    /**
     *
     * @param i
     * @return
     */
    private static int fixedPointToInt(final int i) {
        return i >>> FP_SHIFT;
    }

    /**
     *
     * @param i
     * @return
     */
    private static float fixedPointToFloat(final int i) {
        return (i >>> FP_SHIFT) + ((float) (i & FP_MASK)) / (1 << FP_SHIFT);
    }

    /**
     * Additive blending shrinkImage, 8 bit accuracy. For speed, integers are
     * used with fixed point accuracy instead of floats.
     *
     * Gets a source image along with new size for it and resizes it to fit
     * within max dimensions.
     *
     * @param in - ARGB image
     * @param out - ARGB image buffer, can be the same as
     * inputImageARGB and runs faster that way
     * @param srcW - source image width
     * @param srcH - source image height
     * @param w - final image width
     * @param h - final image height
     * @param preserveAspectRatio
     */
    private static void pureDownscale(final int[] in,
            final int[] out, final int srcW, final int srcH,
            final int w, final int h, final boolean preserveAspectRatio) {
        final int predictedCount = 1 + (srcW / w);
        final int[] lut = new int[predictedCount << 8];

        // Init division lookup table
        for (int i = 0; i < lut.length; i++) {
            lut[i] = i / predictedCount;
        }
        {
            // precalculate src/dest ratios
            final int ratioW = (srcW << FP_SHIFT) / w;

            // horizontal resampling (srcY = destY)
            for (int destY = 0; destY < srcH; ++destY) {
                final int srcRowStartIndex = destY * srcW;
                final int destRowStartIndex = destY * w;

                for (int destX = 0; destX < w; ++destX) {
                    int srcX = (destX * ratioW) >> FP_SHIFT; // calculate
                    // beginning of
                    // sample
                    final int initialSrcX = srcX;
                    final int srcX2 = ((destX + 1) * ratioW) >> FP_SHIFT; // calculate
                    // end
                    // of
                    // sample
                    int a = 0;
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    // now loop from srcX to srcX2 and add up the values for
                    // each channel
                    do {
                        final int argb = in[srcX + srcRowStartIndex];
                        a += (argb & ALPHA) >>> 24;
                        r += argb & RED;
                        g += argb & GREEN;
                        b += argb & BLUE;
                        ++srcX; // move on to the next pixel
                    } while (srcX <= srcX2
                            && srcX + srcRowStartIndex < in.length);

                    // average out the channel values
                    // recreate color from the averaged channels and place it
                    // into the destination buffer
                    r >>>= 16;
                    g >>>= 8;
                    final int count = srcX - initialSrcX;
                    if (count == predictedCount) {
                        in[destX + destRowStartIndex] = (lut[a] << 24)
                                | (lut[r] << 16) | (lut[g] << 8) | lut[b];
                    } else {
                        a /= count;
                        r /= count;
                        g /= count;
                        b /= count;
                        in[destX + destRowStartIndex] = ((a << 24)
                                | (r << 16) | (g << 8) | b);
                    }
                }
            }
        }

        // precalculate src/dest ratios
        final int predictedCount2;
        final int[] lut2;
        if (preserveAspectRatio) {
            predictedCount2 = predictedCount;
            lut2 = lut;
        } else {
            predictedCount2 = 1 + (srcH / h);
            lut2 = new int[predictedCount2 << 8];

            // Init division lookup table
            for (int i = 0; i < lut2.length; i++) {
                lut2[i] = i / predictedCount2;
            }
        }
        // vertical resampling (srcX = destX)
        final int ratioH = (srcH << FP_SHIFT) / h;
        for (int destX = 0; destX < w; ++destX) {
            for (int destY = 0; destY < h; ++destY) {
                int srcY = (destY * ratioH) >> FP_SHIFT; // calculate beginning
                // of sample
                final int initialSrcY = srcY;
                final int srcY2 = ((destY + 1) * ratioH) >> FP_SHIFT; // calculate
                // end
                // of
                // sample
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;

                // now loop from srcY to srcY2 and add up the values for each
                // channel
                do {
                    final int argb = in[destX + srcY * w];
                    a += (argb & ALPHA) >>> 24;
                    r += argb & RED;
                    g += argb & GREEN;
                    b += argb & BLUE;
                    ++srcY; // move on to the next pixel
                } while (srcY <= srcY2
                        && destX + srcY * w < in.length);

                // average out the channel values
                r >>>= 16;
                g >>>= 8;
                final int count = srcY - initialSrcY;
                if (count == predictedCount2) {
                    out[destX + destY * w] = (lut2[a] << 24)
                            | (lut2[r] << 16) | (lut2[g] << 8) | lut2[b];
                } else {
                    a /= count;
                    r /= count;
                    g /= count;
                    b /= count;
                    out[destX + destY * w] = (a << 24) | (r << 16)
                            | (g << 8) | b;
                }
            }
        }
    }

    private static void pureUpscale(final int[] inputImageARGB,
            final int[] outputImageARGB, final int srcW, final int srcH,
            final int w, final int h, final boolean preserveAspectRatio) {
        final int columnMultiplier = 1 + (w - 1) / srcW;
        final int predictedCount = (1 + (srcW / w)) * columnMultiplier;
        final int[] lut = new int[predictedCount << 8];

        // Init division lookup table
        for (int i = 0; i < lut.length; i++) {
            lut[i] = i / predictedCount;
        }
        {
            // precalculate src/dest ratios
            final int ratioW = (srcW << FP_SHIFT) / w;

            // horizontal resampling (srcY = destY)
            for (int destY = 0; destY < srcH; ++destY) {
                final int srcRowStartIndex = destY * srcW;
                final int destRowStartIndex = destY * w;

                for (int destX = 0; destX < w; ++destX) {
                    int srcX = (destX * ratioW) >> FP_SHIFT; // calculate
                    // beginning of
                    // sample
                    final int initialSrcX = srcX;
                    final int srcX2 = columnMultiplier * ((destX + 1) * ratioW) >> FP_SHIFT; // calculate
                    // end
                    // of
                    // sample
                    int a = 0;
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    // now loop from srcX to srcX2 and add up the values for
                    // each channel
                    do {
                        final int argb = inputImageARGB[srcX / columnMultiplier
                                + srcRowStartIndex];
                        a += (argb & ALPHA) >>> 24;
                        r += argb & RED;
                        g += argb & GREEN;
                        b += argb & BLUE;
                        ++srcX; // move on to the next pixel
                    } while (srcX <= srcX2
                            && srcX + srcRowStartIndex < inputImageARGB.length);

                    // average out the channel values
                    // recreate color from the averaged channels and place it
                    // into the destination buffer
                    r >>>= 16;
                    g >>>= 8;
                    final int count = columnMultiplier * (srcX - initialSrcX);
                    if (count == predictedCount) {
                        inputImageARGB[destX / columnMultiplier
                                + destRowStartIndex] = (lut[a] << 24)
                                | (lut[r] << 16) | (lut[g] << 8) | lut[b];
                    } else {
                        a /= count;
                        r /= count;
                        g /= count;
                        b /= count;
                        inputImageARGB[destX / columnMultiplier
                                + destRowStartIndex] = ((a << 24) | (r << 16)
                                | (g << 8) | b);
                    }
                }
            }
        }

        // precalculate src/dest ratios
        final int rowMultiplier = 1 + (h - 1) / srcH;
        final int predictedCount2;
        final int[] lut2;
        if (preserveAspectRatio) {
            predictedCount2 = predictedCount;
            lut2 = lut;
        } else {
            predictedCount2 = rowMultiplier * (1 + (srcH / h));
            lut2 = new int[predictedCount2 << 8];

            // Init division lookup table
            for (int i = 0; i < lut2.length; i++) {
                lut2[i] = i / predictedCount2;
            }
        }
        // vertical resampling (srcX = destX)
        final int ratioH = (srcH << FP_SHIFT) / h;
        for (int destX = 0; destX < w; ++destX) {
            for (int destY = 0; destY < h; ++destY) {
                int srcY = (destY * ratioH) >> FP_SHIFT; // calculate beginning
                // of sample
                final int initialSrcY = srcY;
                final int srcY2 = rowMultiplier * ((destY + 1) * ratioH) >> FP_SHIFT; // calculate
                // end
                // of
                // sample
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;

                // now loop from srcY to srcY2 and add up the values for each
                // channel
                do {
                    final int argb = inputImageARGB[destX + srcY * w
                            / rowMultiplier];
                    a += (argb & ALPHA) >>> 24;
                    r += argb & RED;
                    g += argb & GREEN;
                    b += argb & BLUE;
                    ++srcY; // move on to the next pixel
                } while (srcY <= srcY2
                        && destX + srcY * w < inputImageARGB.length);

                // average out the channel values
                r >>>= 16;
                g >>>= 8;
                final int count = srcY - initialSrcY;
                if (count == predictedCount2) {
                    outputImageARGB[destX + destY * w] = (lut2[a] << 24)
                            | (lut2[r] << 16) | (lut2[g] << 8) | lut2[b];
                } else {
                    a /= count;
                    r /= count;
                    g /= count;
                    b /= count;
                    outputImageARGB[destX + destY * w] = (a << 24) | (r << 16)
                            | (g << 8) | b;
                }
            }
        }
    }

    /**
     * Additive blending shrinkImage, 8 bit accuracy. Slightly faster because
     * Alpha is not calculated.
     *
     * @param in - Opaque RGB image
     * @param out - Opaque RGB output image buffer, can be the same
     * as inputImageRGB and runs faster that way
     * @param srcW - source image width
     * @param srcH - source image height
     * @param w - final image width
     * @param h - final image height
     * @param preserveAspectRatio
     */
    private static void pureOpaqueDownscale(final int[] in,
            final int[] out, final int srcW, final int srcH,
            final int w, final int h, final boolean preserveAspectRatio) {
        final int predictedCount = 1 + (srcW / w);
        final int[] lut = new int[predictedCount << 8];

        // Init division lookup table
        for (int i = 0; i < lut.length; i++) {
            lut[i] = i / predictedCount;
        }
        {
            // precalculate src/dest ratios
            final int ratioW = (srcW << FP_SHIFT) / w;

            // horizontal resampling (srcY = destY)
            for (int destY = 0; destY < srcH; ++destY) {
                final int srcRowStartIndex = destY * srcW;
                final int destRowStartIndex = destY * w;

                for (int destX = 0; destX < w; ++destX) {
                    int srcX = (destX * ratioW) >> FP_SHIFT; // calculate
                    // beginning of
                    // sample
                    final int initialSrcX = srcX;
                    final int srcX2 = ((destX + 1) * ratioW) >> FP_SHIFT; // calculate
                    // end
                    // of
                    // sample
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    // now loop from srcX to srcX2 and add up the values for
                    // each channel
                    do {
                        final int rgb = in[srcRowStartIndex + srcX];
                        r += rgb & RED;
                        g += rgb & GREEN;
                        b += rgb & BLUE;
                        ++srcX; // move on to the next pixel
                    } while (srcX <= srcX2
                            && srcRowStartIndex + srcX < in.length);

                    // average out the channel values
                    // recreate color from the averaged channels and place it
                    // into the destination buffer
                    r >>>= 16;
                    g >>>= 8;
                    final int count = srcX - initialSrcX;
                    if (count == predictedCount) {
                        in[destX + destRowStartIndex] = (lut[r] << 16)
                                | (lut[g] << 8) | lut[b];
                    } else {
                        r /= count;
                        g /= count;
                        b /= count;
                        in[destX + destRowStartIndex] = (r << 16)
                                | (g << 8) | b;
                    }
                }
            }
        }

        // precalculate src/dest ratios
        final int predictedCount2;
        final int[] lut2;
        if (preserveAspectRatio) {
            predictedCount2 = predictedCount;
            lut2 = lut;
        } else {
            predictedCount2 = 1 + (srcH / h);
            lut2 = new int[predictedCount2 << 8];

            // Init division lookup table
            for (int i = 0; i < lut2.length; i++) {
                lut2[i] = i / predictedCount2;
            }
        }
        // vertical resampling (srcX = destX)
        final int ratioH = (srcH << FP_SHIFT) / h;
        for (int destX = 0; destX < w; ++destX) {
            for (int destY = 0; destY < h; ++destY) {
                int srcY = (destY * ratioH) >> FP_SHIFT; // calculate beginning
                // of sample
                final int initialSrcY = srcY;
                final int columnStart = srcY * w;
                final int srcY2 = ((destY + 1) * ratioH) >> FP_SHIFT; // calculate
                // end
                // of
                // sample
                int r = 0;
                int g = 0;
                int b = 0;

                // now loop from srcY to srcY2 and add up the values for each
                // channel
                do {
                    final int argb = in[columnStart + destX];
                    r += argb & RED;
                    g += argb & GREEN;
                    b += argb & BLUE;
                    ++srcY; // move on to the next pixel
                } while (srcY <= srcY2
                        && columnStart + destX < in.length);

                // average out the channel values
                r >>>= 16;
                g >>>= 8;
                final int count = srcY - initialSrcY;
                if (count == predictedCount2) {
                    out[destX + destY * w] = (lut2[r] << 16)
                            | (lut2[g] << 8) | lut2[b];
                } else {
                    r /= count;
                    g /= count;
                    b /= count;
                    out[destX + destY * w] = (r << 16) | (g << 8)
                            | b;
                }
            }
        }
    }
}
