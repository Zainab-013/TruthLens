package com.truthlens.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.truthlens.model.HistoryItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to save, load, and clear history items using SharedPreferences and Gson.
 */
public class HistoryManager {

    private static final String PREF_NAME = "truthlens_history_pref";
    private static final String KEY_HISTORY = "history_items";
    private static final int MAX_HISTORY_LIMIT = 50; // Cap history to 50 items

    /**
     * Save a new history item to the cache (newest items first).
     */
    public static void saveHistoryItem(Context context, HistoryItem item) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            List<HistoryItem> historyList = getHistory(context);

            // Add the new item at index 0 (top of the list)
            historyList.add(0, item);

            // Enforce limit
            if (historyList.size() > MAX_HISTORY_LIMIT) {
                historyList = new ArrayList<>(historyList.subList(0, MAX_HISTORY_LIMIT));
            }

            Gson gson = new Gson();
            String json = gson.toJson(historyList);
            prefs.edit().putString(KEY_HISTORY, json).apply();
        } catch (Exception ignored) {
            // Silently ignore to avoid app crash on preference storage failure
        }
    }

    /**
     * Retrieve all saved history items.
     */
    public static List<HistoryItem> getHistory(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_HISTORY, null);
            if (json == null) {
                return new ArrayList<>();
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<List<HistoryItem>>() {}.getType();
            List<HistoryItem> items = gson.fromJson(json, listType);
            return items != null ? items : new ArrayList<HistoryItem>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Clear all cached history.
     */
    public static void clearHistory(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_HISTORY).apply();
        } catch (Exception ignored) {
        }
    }
}
