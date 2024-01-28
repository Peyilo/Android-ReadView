package org.peyilo.readview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.peyilo.readview.ui.ContentView

class TextAdapter(private val pages: List<PageData>) :
    RecyclerView.Adapter<TextAdapter.TextViewHolder>() {

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: View = itemView.findViewById(R.id.page_header)
        val process: TextView = itemView.findViewById(R.id.page_footer_process)
        val content: ContentView = itemView.findViewById(R.id.contentView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view_page, parent, false)
        return TextViewHolder(view)
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        val page = pages[position]
//        holder.titleView.text = page.title
//        holder.contentView.text = page.content
    }

    override fun getItemCount(): Int {
        return pages.size
    }
}
