package com.replayx.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import com.replayx.app.R;

public class ThemeManager {
    
    private static final String PREFS_THEME = "yguix_theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    
    public enum Theme {
        ORANGE(0xFF6B00, 0x5A2000, "orange"),
        GREEN(0x00FF41, 0x00AA22, "green"),
        PURPLE(0xB700FF, 0x7700BB, "purple"),
        BLUE(0x00AAFF, 0x0066BB, "blue"),
        RED(0xFF0055, 0xBB0033, "red");
        
        public final int primaryColor;
        public final int darkColor;
        public final String name;
        
        Theme(int primary, int dark, String name) {
            this.primaryColor = primary;
            this.darkColor = dark;
            this.name = name;
        }
    }
    
    public static void saveTheme(Context context, Theme theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, theme.name).apply();
    }
    
    public static Theme getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE);
        String themeName = prefs.getString(KEY_THEME, "orange");
        
        for (Theme theme : Theme.values()) {
            if (theme.name.equals(themeName)) {
                return theme;
            }
        }
        return Theme.ORANGE;
    }
    
    public static void applyTheme(View view, Theme theme) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(theme.primaryColor);
        } else if (view instanceof Button) {
            ((Button) view).setTextColor(theme.primaryColor);
        }
    }
    
    public static void applyThemeToAll(View rootView, Theme theme) {
        applyThemeRecursive(rootView, theme);
    }
    
    private static void applyThemeRecursive(View view, Theme theme) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            String currentText = tv.getText().toString();
            
            // Aplica cor ao texto
            if (currentText.contains("BYPASS") || 
                currentText.contains("Yguix") || 
                currentText.contains("SCRIPT") ||
                currentText.contains("yguix")) {
                tv.setTextColor(theme.primaryColor);
            }
        }
        
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyThemeRecursive(vg.getChildAt(i), theme);
            }
        }
    }
}
