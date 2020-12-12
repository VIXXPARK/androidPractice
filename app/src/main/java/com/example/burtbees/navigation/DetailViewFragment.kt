package com.example.burtbees.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.burtbees.R
import com.example.burtbees.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.example.burtbees.MainActivity

class DetailViewFragment:Fragment(){
    var firestore : FirebaseFirestore? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container,false)
        firestore = FirebaseFirestore.getInstance()
        view.findViewById<RecyclerView>(R.id.detailviewfragment_recycleview).adapter=DetailRecyclerViewAdapter()
        view.findViewById<RecyclerView>(R.id.detailviewfragment_recycleview).layoutManager=LinearLayoutManager(activity)
        return view
    }
    inner class DetailRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO>
        var contentUidList : ArrayList<String>

        init{
            contentDTOs =ArrayList()
            contentUidList= ArrayList()

            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
                if(querySnapshot==null) return@addSnapshotListener
                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)!!
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)
            return CustomViewHolder(view)
        }



        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView

            //UserId
            viewholder.findViewById<TextView>(R.id.detailviewitem_profile_textview).text = contentDTOs!![position].userId

            //Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.findViewById(R.id.detailviewitem_imageview_content))

            //Explain of content
            viewholder.findViewById<TextView>(R.id.detailviewitem_explain_textview).text = contentDTOs!![position].explain

            //likes
            viewholder.findViewById<TextView>(R.id.detailviewitem_favoritecounter_textview).text = "Likes" + contentDTOs!![position].favoriteCount

            //ProfileImage
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.findViewById(R.id.detailviewitem_profile_image))


        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
    inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}