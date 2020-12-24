package com.example.burtbees.navigation

import android.media.Image
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
import com.bumptech.glide.request.RequestOptions
import com.example.burtbees.R
import com.example.burtbees.navigation.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AlarmFragment:Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_alarm,container,false)
        view.findViewById<RecyclerView>(R.id.alarmfragment_recyclerview).adapter = AlarmRecyclerviewAdapter()
        view.findViewById<RecyclerView>(R.id.alarmfragment_recyclerview).layoutManager=LinearLayoutManager(activity)
        return view
    }
    inner class AlarmRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var alarmDTOList = ArrayList<AlarmDTO>()

        init{
            var uid = FirebaseAuth.getInstance().currentUser!!.uid

            FirebaseFirestore.getInstance()
                    .collection("alarms")
                    .whereEqualTo("destinationUid",uid)
                    .addSnapshotListener { value, error ->
                        alarmDTOList.clear()
                        if(value==null) return@addSnapshotListener
                        for(snapshot in value?.documents!!){
                            alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                        }
                        notifyDataSetChanged()
                    }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View): RecyclerView.ViewHolder(view)
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val profileImage = holder.itemView.findViewById<ImageView>(R.id.commentviewitem_imageview_profile)
            val commentTextView = holder.itemView.findViewById<TextView>(R.id.commentviewitem_textview_profile)

            FirebaseFirestore.getInstance().collection("profileImages")
                    .document(alarmDTOList[position].uid!!).get().addOnCompleteListener {
                        task ->
                        if(task.isSuccessful){
                            val url = task.result!!["image"]
                            Glide.with(activity!!)
                                    .load(url)
                                    .apply(RequestOptions().circleCrop())
                                    .into(profileImage)
                        }
                    }

            when (alarmDTOList[position].kind) {
                0 -> {
                    val str_0 = alarmDTOList[position].userId + getString(R.string.alarm_favorite)
                    commentTextView.text = str_0
                }

                1 -> {
                    val str_1 = alarmDTOList[position].userId +" " + alarmDTOList[position].message + getString(R.string.alarm_comment)
                    commentTextView.text = str_1
                }

                2 -> {
                    val str_2 = alarmDTOList[position].userId + getString(R.string.alarm_follow)
                    commentTextView.text = str_2
                }
            }
        }

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

    }
}