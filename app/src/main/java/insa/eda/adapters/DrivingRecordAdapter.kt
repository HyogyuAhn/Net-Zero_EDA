package insa.eda.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import insa.eda.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrivingRecordAdapter : RecyclerView.Adapter<DrivingRecordAdapter.DrivingRecordViewHolder>() {
    
    private val records = mutableListOf<DrivingRecordItem>()
    
    fun updateRecords(newRecords: List<DrivingRecordItem>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrivingRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_driving_record, parent, false)
        return DrivingRecordViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DrivingRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }
    
    override fun getItemCount(): Int = records.size
    
    class DrivingRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvScore: TextView = itemView.findViewById(R.id.tv_score)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        private val tvSuddenAccel: TextView = itemView.findViewById(R.id.tv_sudden_accel)
        private val tvSuddenBrake: TextView = itemView.findViewById(R.id.tv_sudden_brake)
        private val tvIdling: TextView = itemView.findViewById(R.id.tv_idling)
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        fun bind(record: DrivingRecordItem) {
            tvDate.text = dateFormat.format(record.startTime)
            
            tvScore.text = "${record.ecoScore}점"
            
            when {
                record.ecoScore >= 80 -> tvScore.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                record.ecoScore >= 60 -> tvScore.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                else -> tvScore.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }
            
            tvDistance.text = String.format("%.1f km", record.distance)
            
            val hours = record.duration / 60
            val minutes = record.duration % 60
            tvDuration.text = if (hours > 0) {
                String.format("%d시간 %d분", hours, minutes)
            } else {
                String.format("%d분", minutes)
            }
            
            tvSuddenAccel.text = "급발진: ${record.suddenAccelCount}회"
            tvSuddenBrake.text = "급제동: ${record.suddenBrakeCount}회"
            tvIdling.text = "공회전: ${record.idlingCount}회"
        }
    }
    
    data class DrivingRecordItem(
        val id: String,
        val startTime: Date,
        val endTime: Date?,
        val duration: Int,
        val distance: Float,
        val ecoScore: Int,
        val suddenAccelCount: Int,
        val suddenBrakeCount: Int,
        val idlingCount: Int,
        val co2Emission: Float
    )
}
