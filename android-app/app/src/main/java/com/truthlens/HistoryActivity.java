package com.truthlens;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.truthlens.model.HistoryItem;
import com.truthlens.utils.HistoryManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HistoryActivity - History Screen
 * Displays a list of cached local analyses.
 */
public class HistoryActivity extends AppCompatActivity {

    private ListView listHistory;
    private TextView textEmpty;
    private List<HistoryItem> items;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Bind views
        ImageButton btnBack = findViewById(R.id.btnBack);
        AppCompatButton btnClearHistory = findViewById(R.id.btnClearHistory);
        listHistory = findViewById(R.id.listHistory);
        textEmpty = findViewById(R.id.textEmpty);

        // Load items from cache
        loadHistoryItems();

        // Back button action
        btnBack.setOnClickListener(v -> finish());

        // Clear history action
        btnClearHistory.setOnClickListener(v -> {
            if (items.isEmpty()) {
                Toast.makeText(this, "History is already empty", Toast.LENGTH_SHORT).show();
                return;
            }
            HistoryManager.clearHistory(this);
            loadHistoryItems();
            Toast.makeText(this, "History cleared successfully", Toast.LENGTH_SHORT).show();
        });

        // Click on list item opens ResultActivity with cached data
        listHistory.setOnItemClickListener((parent, view, position, id) -> {
            HistoryItem item = items.get(position);
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("ai_percentage", item.getAiPercentage());
            intent.putExtra("human_percentage", item.getHumanPercentage());
            intent.putExtra("verdict", item.getVerdict());
            intent.putExtra("explanation", item.getExplanations().toArray(new String[0]));
            startActivity(intent);
        });
    }

    private void loadHistoryItems() {
        items = HistoryManager.getHistory(this);
        if (items.isEmpty()) {
            listHistory.setVisibility(View.GONE);
            textEmpty.setVisibility(View.VISIBLE);
        } else {
            listHistory.setVisibility(View.VISIBLE);
            textEmpty.setVisibility(View.GONE);
        }

        adapter = new HistoryAdapter(this, items);
        listHistory.setAdapter(adapter);
    }

    /**
     * Custom Array Adapter to bind HistoryItem fields to row layout item_history.xml
     */
    private static class HistoryAdapter extends ArrayAdapter<HistoryItem> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public HistoryAdapter(AppCompatActivity context, List<HistoryItem> items) {
            super(context, R.layout.item_history, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_history, parent, false);
            }

            HistoryItem item = getItem(position);
            if (item != null) {
                TextView textSnippet = convertView.findViewById(R.id.textSnippet);
                TextView textDate = convertView.findViewById(R.id.textDate);
                TextView textAiVal = convertView.findViewById(R.id.textAiVal);
                TextView textHumanVal = convertView.findViewById(R.id.textHumanVal);

                textSnippet.setText(item.getText());
                textDate.setText(dateFormat.format(new Date(item.getTimestamp())));
                textAiVal.setText(String.format(Locale.US, "%.1f%%", item.getAiPercentage()));
                textHumanVal.setText(String.format(Locale.US, "%.1f%%", item.getHumanPercentage()));
            }

            return convertView;
        }
    }
}
