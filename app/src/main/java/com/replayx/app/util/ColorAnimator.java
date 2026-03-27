package com.replayx.app.util;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Animador de cores que alterna entre ROXO PURO e VERDE PURO
 * Sem cores intermediárias ou misturadas
 */
public class ColorAnimator {
    
    private static final int COLOR_PURPLE = 0xB700FF;  // Roxo Neon Puro
    private static final int COLOR_GREEN = 0x00FF41;   // Verde Neon Puro
    private static final long ANIMATION_DURATION = 3000; // 3 segundos por transição
    
    private ValueAnimator animator;
    private View rootView;
    
    public ColorAnimator(View rootView) {
        this.rootView = rootView;
    }
    
    /**
     * Inicia animação alternando entre roxo e verde
     */
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
            
            // Alterna entre roxo puro e verde puro
            int color = (progress < 0.5f) ? COLOR_PURPLE : COLOR_GREEN;
            
            // Se quiser transição suave, descomente:
            // int color = interpolateColor(COLOR_PURPLE, COLOR_GREEN, progress);
            
            applyColorToView(rootView, color);
        });
        
        animator.start();
    }
    
    /**
     * Para a animação
     */
    public void stopColorAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }
    
    /**
     * Interpola suavemente entre duas cores
     */
    private int interpolateColor(int colorStart, int colorEnd, float progress) {
        int a = (int) (Color.alpha(colorStart) + (Color.alpha(colorEnd) - Color.alpha(colorStart)) * progress);
        int r = (int) (Color.red(colorStart) + (Color.red(colorEnd) - Color.red(colorStart)) * progress);
        int g = (int) (Color.green(colorStart) + (Color.green(colorEnd) - Color.green(colorStart)) * progress);
        int b = (int) (Color.blue(colorStart) + (Color.blue(colorEnd) - Color.blue(colorStart)) * progress);
        return Color.argb(a, r, g, b);
    }
    
    /**
     * Aplica cor a todos os TextViews da view
     */
    private void applyColorToView(View view, int color) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            tv.setTextColor(color);
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyColorToView(vg.getChildAt(i), color);
            }
        }
    }
}
