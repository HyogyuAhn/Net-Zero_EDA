package insa.eda.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import insa.eda.activities.AddressInfo
import insa.eda.R

class SearchResultAdapter(
    private val items: List<AddressInfo>,
    private val onItemClick: (AddressInfo) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {
    
    private var selectedPosition = -1
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvName.text = item.name
        holder.tvAddress.text = item.address
        
        holder.itemView.isSelected = selectedPosition == position
        
        holder.itemView.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = position
            
            if (previousSelectedPosition >= 0) {
                notifyItemChanged(previousSelectedPosition)
            }
            
            notifyItemChanged(selectedPosition)
            onItemClick(item)
        }
    }
    
    override fun getItemCount() = items.size
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_place_name)
        val tvAddress: TextView = view.findViewById(R.id.tv_address)
    }
}
