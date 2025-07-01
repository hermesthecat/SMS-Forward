package com.keremgok.smsforward.ui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
// Removed import to avoid name conflict with our AnimationUtils class
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;

import com.keremgok.smsforward.R;

/**
 * Utility class for handling animations throughout the app
 * Provides smooth transitions and Material Design motion patterns
 */
public class AnimationUtils {
    private static final String TAG = "AnimationUtils";
    
    // Animation durations
    public static final int DURATION_SHORT = 150;
    public static final int DURATION_MEDIUM = 300;
    public static final int DURATION_LONG = 500;
    
    // Scale values
    private static final float SCALE_PRESSED = 0.95f;
    private static final float SCALE_NORMAL = 1.0f;
    
    /**
     * Apply a fade in animation to a view
     */
    public static void fadeIn(View view) {
        fadeIn(view, DURATION_MEDIUM, null);
    }
    
    public static void fadeIn(View view, int duration, Runnable onComplete) {
        if (view == null) return;
        
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        ViewCompat.animate(view)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(onComplete)
                .start();
    }
    
    /**
     * Apply a fade out animation to a view
     */
    public static void fadeOut(View view) {
        fadeOut(view, DURATION_MEDIUM, null);
    }
    
    public static void fadeOut(View view, int duration, Runnable onComplete) {
        if (view == null) return;
        
        ViewCompat.animate(view)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .start();
    }
    
    /**
     * Apply a scale animation to simulate button press
     */
    public static void animateButtonPress(View button) {
        if (button == null) return;
        
        ViewCompat.animate(button)
                .scaleX(SCALE_PRESSED)
                .scaleY(SCALE_PRESSED)
                .setDuration(DURATION_SHORT)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    ViewCompat.animate(button)
                            .scaleX(SCALE_NORMAL)
                            .scaleY(SCALE_NORMAL)
                            .setDuration(DURATION_SHORT)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                })
                .start();
    }
    
    /**
     * Apply a bounce animation for view entrance
     */
    public static void bounceIn(View view) {
        if (view == null) return;
        
        view.setScaleX(0.3f);
        view.setScaleY(0.3f);
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        ViewCompat.animate(view)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(DURATION_LONG)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
    }
    
    /**
     * Apply a slide up animation for cards or lists
     */
    public static void slideUp(View view) {
        slideUp(view, DURATION_MEDIUM, null);
    }
    
    public static void slideUp(View view, int duration, Runnable onComplete) {
        if (view == null) return;
        
        view.setTranslationY(view.getHeight());
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        
        ViewCompat.animate(view)
                .translationY(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(onComplete)
                .start();
    }
    
    /**
     * Apply a slide down animation for hiding views
     */
    public static void slideDown(View view) {
        slideDown(view, DURATION_MEDIUM, null);
    }
    
    public static void slideDown(View view, int duration, Runnable onComplete) {
        if (view == null) return;
        
        ViewCompat.animate(view)
                .translationY(view.getHeight())
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setTranslationY(0f);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .start();
    }
    
    /**
     * Apply a rotation animation (useful for refresh buttons)
     */
    public static void rotate(View view) {
        rotate(view, 360f, DURATION_LONG);
    }
    
    public static void rotate(View view, float degrees, int duration) {
        if (view == null) return;
        
        ViewCompat.animate(view)
                .rotationBy(degrees)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
    
    /**
     * Apply a pulse animation for loading indicators
     */
    public static void pulse(View view) {
        if (view == null) return;
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f, 1f);
        
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        alpha.setDuration(1000);
        
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        
        scaleX.start();
        scaleY.start();
        alpha.start();
    }
    
    /**
     * Stop pulse animation
     */
    public static void stopPulse(View view) {
        if (view == null) return;
        
        view.clearAnimation();
        ViewCompat.animate(view)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(DURATION_SHORT)
                .start();
    }
    
    /**
     * Apply Material Design card elevation animation
     */
    public static void elevateCard(View card, boolean elevate) {
        if (card == null) return;
        
        float targetElevation = elevate ? 16f : 4f;
        float targetScale = elevate ? 1.02f : 1f;
        
        ViewCompat.animate(card)
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(DURATION_MEDIUM)
                .setInterpolator(new DecelerateInterpolator())
                .start();
                
        // Animate elevation if supported
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ViewCompat.animate(card)
                    .translationZ(targetElevation)
                    .setDuration(DURATION_MEDIUM)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
    
    /**
     * Apply staggered animation for list items
     */
    public static void staggeredFadeIn(ViewGroup container, int delay) {
        if (container == null) return;
        
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(50f);
            
            ViewCompat.animate(child)
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * delay)
                    .setDuration(DURATION_MEDIUM)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
    
    /**
     * Apply loading state animation
     */
    public static void showLoadingState(View loadingView, View contentView) {
        if (loadingView != null) {
            fadeIn(loadingView);
            pulse(loadingView);
        }
        
        if (contentView != null) {
            fadeOut(contentView);
        }
    }
    
    /**
     * Hide loading state animation
     */
    public static void hideLoadingState(View loadingView, View contentView) {
        if (loadingView != null) {
            stopPulse(loadingView);
            fadeOut(loadingView);
        }
        
        if (contentView != null) {
            fadeIn(contentView);
        }
    }
    
    /**
     * Apply smooth height animation for expanding/collapsing views
     */
    public static void animateHeight(View view, int targetHeight, boolean expand) {
        if (view == null) return;
        
        int initialHeight = expand ? 0 : view.getMeasuredHeight();
        int finalHeight = expand ? targetHeight : 0;
        
        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, finalHeight);
        animator.setDuration(DURATION_MEDIUM);
        animator.setInterpolator(new DecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            view.setLayoutParams(params);
        });
        
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (expand) {
                    view.setVisibility(View.VISIBLE);
                }
            }
            
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!expand) {
                    view.setVisibility(View.GONE);
                }
            }
        });
        
        animator.start();
    }
    
    /**
     * Apply success animation (usually a checkmark or green flash)
     */
    public static void animateSuccess(View view) {
        if (view == null) return;
        
        ViewCompat.animate(view)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(DURATION_SHORT)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    ViewCompat.animate(view)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(DURATION_SHORT)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }
    
    /**
     * Apply error animation (shake effect)
     */
    public static void animateError(View view) {
        if (view == null) return;
        
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        animator.setDuration(DURATION_LONG);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }
} 