package insa.eda.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import insa.eda.R;
import insa.eda.database.models.DrivingRecord;

public class DrivingHistoryAdapter extends RecyclerView.Adapter<DrivingHistoryAdapter.ViewHolder> {

    private List<DrivingRecord> drivingRecords = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DrivingRecord record);
    }

    public DrivingHistoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driving_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DrivingRecord record = drivingRecords.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return drivingRecords.size();
    }

    public void setDrivingRecords(List<DrivingRecord> drivingRecords) {
        this.drivingRecords = drivingRecords;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final TextView distanceText;
        private final TextView durationText;
        private final TextView ecoScoreText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            distanceText = itemView.findViewById(R.id.distanceText);
            durationText = itemView.findViewById(R.id.durationText);
            ecoScoreText = itemView.findViewById(R.id.ecoScoreText);
        }

        public void bind(final DrivingRecord record, final OnItemClickListener listener) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateText.setText(dateFormat.format(record.getStartTime()));
            
            distanceText.setText(String.format(Locale.getDefault(), "%.1f km", record.getDistance()));
            
            long durationMillis = 0;
            if (record.getEndTime() != null) {
                durationMillis = record.getEndTime().getTime() - record.getStartTime().getTime();
            }
            int minutes = (int) (durationMillis / (1000 * 60));
            durationText.setText(String.format(Locale.getDefault(), "%d min", minutes));
            
            ecoScoreText.setText(String.format(Locale.getDefault(), "%d", record.getEcoScore()));
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(record);
                }
            });
        }
    }
}
