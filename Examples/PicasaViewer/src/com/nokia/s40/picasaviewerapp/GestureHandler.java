/*
 * Copyright (c) 2012-2013 Nokia Corporation. All rights reserved.
 * Nokia and Nokia Connecting People are registered trademarks of Nokia Corporation.
 * Oracle and Java are trademarks or registered trademarks of Oracle and/or its
 * affiliates. Other product and company names mentioned herein may be trademarks
 * or trade names of their respective owners. 
 * See LICENSE.TXT for license information.
 */
package com.nokia.s40.picasaviewerapp;

import org.tantalum.util.L;

import com.nokia.mid.ui.frameanimator.FrameAnimator;
import com.nokia.mid.ui.frameanimator.FrameAnimatorListener;
import com.nokia.mid.ui.gestures.GestureEvent;
import com.nokia.mid.ui.gestures.GestureInteractiveZone;
import com.nokia.mid.ui.gestures.GestureListener;
import com.nokia.mid.ui.gestures.GestureRegistrationManager;

/**
 *
 * @author phou
 */
public final class GestureHandler implements FrameAnimatorListener, GestureListener {

    public static final int FRAME_ANIMATOR_VERTICAL = FrameAnimator.FRAME_ANIMATOR_VERTICAL;
    public static final int FRAME_ANIMATOR_HORIZONTAL = FrameAnimator.FRAME_ANIMATOR_HORIZONTAL;
    public static final int FRAME_ANIMATOR_FREE_ANGLE = FrameAnimator.FRAME_ANIMATOR_FREE_ANGLE;
    public static final int FRAME_ANIMATOR_FRICTION_LOW = FrameAnimator.FRAME_ANIMATOR_FRICTION_LOW;
    public static final int FRAME_ANIMATOR_FRICTION_MEDIUM = FrameAnimator.FRAME_ANIMATOR_FRICTION_MEDIUM;
    public static final int FRAME_ANIMATOR_FRICTION_HIGH = FrameAnimator.FRAME_ANIMATOR_FRICTION_HIGH;
    private GestureCanvas canvas;
    private final FrameAnimator animator = new FrameAnimator();
    private GestureInteractiveZone giz = null;

    public GestureHandler() {
        try {
            giz = new GestureInteractiveZone(GestureInteractiveZone.GESTURE_ALL);
        } catch (IllegalArgumentException e) {
            //#debug
            L.e(L.class.getName(), "Can not register GESTURE_ALL, backwards compatibility fallback", e);
            giz = new GestureInteractiveZone(63); // GESTURE_ALL had a different value in SDK 1.1 and 1.0
        }
    }
    
    public void setCanvas(final GestureCanvas canvas) {
        this.canvas = canvas;
    }

    public void gestureAction(final Object container, final GestureInteractiveZone gestureInteractiveZone, final GestureEvent ge) {
        switch (ge.getType()) {
            // Pinch to force reload
            case GestureInteractiveZone.GESTURE_PINCH:
                canvas.gesturePinch(
                        ge.getPinchDistanceStarting(),
                        ge.getPinchDistanceCurrent(),
                        ge.getPinchDistanceChange(),
                        ge.getPinchCenterX(),
                        ge.getPinchCenterY(),
                        ge.getPinchCenterChangeX(),
                        ge.getPinchCenterChangeY());
                break;

            case GestureInteractiveZone.GESTURE_TAP:
                canvas.gestureTap(ge.getStartX(), ge.getStartY());
                break;

            case GestureInteractiveZone.GESTURE_LONG_PRESS:
                canvas.gestureLongPress(ge.getStartX(), ge.getStartY());
                break;

            case GestureInteractiveZone.GESTURE_LONG_PRESS_REPEATED:
                canvas.gestureLongPressRepeated(ge.getStartX(), ge.getStartY());
                break;

            case GestureInteractiveZone.GESTURE_DRAG:
                canvas.gestureDrag(ge.getStartX(), ge.getStartY(), ge.getDragDistanceX(), ge.getDragDistanceY());
                break;

            case GestureInteractiveZone.GESTURE_DROP:
                canvas.gestureDrop(ge.getStartX(), ge.getStartY(), ge.getDragDistanceX(), ge.getDragDistanceY());
                break;

            case GestureInteractiveZone.GESTURE_FLICK:
                canvas.gestureFlick(ge.getStartX(), canvas.getScrollY(), ge.getFlickDirection(), ge.getFlickSpeed(), ge.getFlickSpeedX(), ge.getFlickSpeedY());
                break;

            default:
                ;
        }
    }

    public void animate(final FrameAnimator animator, final int x, final int y, final short delta, final short deltaX, final short deltaY, final boolean lastFrame) {
        //#debug
        L.i(L.class.getName(), "animate, y=" + y + " deltaY=" + deltaY);
        canvas.animate(x, y, delta, deltaX, deltaY, lastFrame);
    }

    public void animateDrag(final int x, final int y) {
        //#debug
        L.i(L.class.getName(), "animate drag, y=" + y);
        animator.drag(x, y);
    }

    public void kineticScroll(final int startSpeed, final int direction, final int friction, final float angle) {
        //#debug
        L.i(L.class.getName(), "animate start kinetic scroll, startSpeed=" + startSpeed);
        animator.kineticScroll(startSpeed, direction, friction, angle);
    }

//    public void stopAnimation() {
//        //#debug
//        L.i(L.class.getName(), "stop animation");
//        try {
//            animator.stop();
//        } catch (Exception e) {
//            L.e("stopAnimation", "Can not stop", e);
//        }
//    }

    /**
     * Unregister gesture and animation events (there is a max # allowed at any
     * one time)
     *
     */
//    public void unregister() {
//        //#debug
//        L.i("Canvas " + canvas.getTitle(), "unregister");
////        stopAnimation();
//        animator.unregister();
//        GestureRegistrationManager.unregister(canvas, giz);
//    }
    
    public void updateCanvasSize() {
        giz.setRectangle(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Register for gesture and animation events when the parent becomes visible
     *
     */
    public void register(final int x, final int y) {
        //#debug
        L.i("Canvas " + canvas.getTitle(), "register");
        updateCanvasSize();
        GestureRegistrationManager.register(canvas, giz);
        GestureRegistrationManager.setListener(canvas, this);
        animator.register(x, y, (short) 0, (short) 0, this);
    }
}
