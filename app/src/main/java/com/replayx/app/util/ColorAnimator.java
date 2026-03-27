package com.replayx.app.util;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;

public class ColorAnimator {
    
    private static final int COLOR_PURPLE = 0xB700FF;
    private static final int COLOR_GREEN = 0x00FF41;
    private static final long ANIMATION_DURATION = 2000; // 2 segundos
    
    private ValueAnimator animator;
    private View rootView;
    
    public ColorAnimator(View rootView) {
        this.rootView = rootView;
    }
    
    public void startColorAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            int color = interpolateColor(COLOR_PURPLE, COLOR_GREEN, progress);
            applyColorToView(rootView, color);
        });
        
        animator.start();
    }
    
    public void stopColorAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }
    
    private int interpolateColor(int colorStart, int colorEnd, float progress) {
        int a = (int) (Color.alpha(colorStart) + (Color.alpha(colorEnd) - Color.alpha(colorStart)) * progress);
        int r = (int) (Color.red(colorStart) + (Color.red(colorEnd) - Color.red(colorStart)) * progress);
        int g = (int) (Color.green(colorStart) + (Color.green(colorEnd) - Color.green(colorStart)) * progress);
        int b = (int) (Color.blue(colorStart) + (Color.blue(colorEnd) - Color.blue(colorStart)) * progress);
        return android.graphics.Color.argb(a, r, g, b);
    }
    
    private void applyColorToView(View view, int color) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            String text = tv.getText().toString();
            
            // Aplica cor a elementos específicos
            if (text.contains("Yguix") || text.contains("BYPASS") || 
                text.contains("HIDE STREAM") || text.contains("ACESSO")) {
                tv.setTextColor(color);
            }
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyColorToView(vg.getChildAt(i), color);
            }
        }
    }
    
    // Classe auxiliar para cores
    private static class Color {
        static int alpha(int color) {
            return (color >> 24) & 0xFF;
        }
        
        static int red(int color) {
            return (color >> 16) & 0xFF;
        }
        
        static int green(int color) {
            return (color >> 8) & 0xFF;
        }
        
        static int blue(int color) {
            return color & 0xFF;
        }
    }
}
