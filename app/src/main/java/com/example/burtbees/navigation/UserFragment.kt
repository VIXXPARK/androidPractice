package com.example.burtbees.navigation

import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.burtbees.LoginActivity
import com.example.burtbees.MainActivity
import com.example.burtbees.R
import com.example.burtbees.navigation.model.AlarmDTO
import com.example.burtbees.navigation.model.ContentDTO
import com.example.burtbees.navigation.model.FollowDTO
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserFragment:Fragment(){
    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth: FirebaseAuth?= null
    var currentUserUid :String? = null
    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user,container,false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if(uid==currentUserUid){
            //MyPage
            fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.text=getString(R.string.signout)
            fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            //otherPage
            fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.text=getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainactivity?.findViewById<TextView>(R.id.toolbar_username)?.text=arguments?.getString("userId")
            mainactivity?.findViewById<ImageView>(R.id.toolbar_btn_back)?.setOnClickListener {
                mainactivity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId= R.id.action_home
            }
            mainactivity?.findViewById<ImageView>(R.id.toolbar_title_image)?.visibility=View.GONE
            mainactivity?.findViewById<TextView>(R.id.toolbar_username)?.visibility=View.VISIBLE
            mainactivity?.findViewById<ImageView>(R.id.toolbar_btn_back).visibility=View.VISIBLE
            fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.setOnClickListener {
                requestFollow()
            }

        }
        fragmentView?.findViewById<RecyclerView>(R.id.account_recyclerview)?.adapter=UserFragmentRecyclerViewAdapter()
        fragmentView?.findViewById<RecyclerView>(R.id.account_recyclerview)?.layoutManager = GridLayoutManager(activity!!,3)

        fragmentView?.findViewById<ImageView>(R.id.account_iv_profile)?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type="image/*"
            activity!!.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value==null)return@addSnapshotListener

            var followDTO = value.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount!=null){
                fragmentView?.findViewById<TextView>(R.id.account_tv_following_count)?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount!=null){
                fragmentView?.findViewById<TextView>(R.id.account_tv_follwer_count)?.text = followDTO?.followerCount?.toString()
                if (followDTO?.follwers?.containsKey(currentUserUid!!)){
                    fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.text = getString(R.string.follow_cancel)
                    fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.background?.setColorFilter(ContextCompat.getColor(activity!!,R.color.colorLightGray), Mode.MULTIPLY)
                }else{
                    if(uid!=currentUserUid){
                        fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.text = getString(R.string.follow)
                        fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)?.background?.colorFilter = null
                    }
                }
            }
        }
    }



    fun requestFollow(){
        //Save data to my account
        var toDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction{transaction->
            var followDTO = transaction.get(toDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount=1
                followDTO!!.follwers[uid!!]=true
                transaction.set(toDocFollowing,followDTO)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)){
                //It remove following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount-1
                followDTO?.followings?.remove(uid)
            }else{
                //It add following third person when a third person do not follow me
                followDTO?.followingCount = followDTO?.followingCount+1
                followDTO?.followings[uid!!]=true
            }
            transaction.set(toDocFollowing,followDTO)
            return@runTransaction
        }
        //Save data to third person
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO==null){
                followDTO= FollowDTO()
                followDTO!!.followerCount =1
                followDTO!!.follwers[currentUserUid!!]=true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.follwers.containsKey(currentUserUid)){
                //It cancel my follower when I follow a third person
                followDTO!!.followerCount=followDTO!!.followerCount-1
                followDTO!!.follwers.remove((currentUserUid!!))
            }else{
                //It add my follower when I follow a third person
                followDTO!!.followerCount=followDTO!!.followerCount+1
                followDTO!!.follwers[currentUserUid!!]=true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }

    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.detinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind=2
        alarmDTO.timestamp=System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
    }

    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { value, error ->
            if(value == null) return@addSnapshotListener
            if(value.data!=null){
                var url = value?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView!!.findViewById(R.id.account_iv_profile))
            }
        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init{
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { value, error ->
                //Sometimes, This code return null of value when it signout
                if(value==null) return@addSnapshotListener

                //Get data
                for(snapshot in value.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.findViewById<TextView>(R.id.account_tv_post_count)?.text=contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels/3
            var imageView = ImageView(parent.context)
            imageView.layoutParams=LinearLayoutCompat.LayoutParams(width,width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}

fun Drawable.setColorFilter(color: Int, mode: Mode = Mode.SRC_ATOP) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        colorFilter = BlendModeColorFilter(color, mode.getBlendMode())
    } else {
        @Suppress("DEPRECATION")
        setColorFilter(color, mode.getPorterDuffMode())
    }
}
enum class Mode {
    CLEAR,
    SRC,
    DST,
    SRC_OVER,
    DST_OVER,
    SRC_IN,
    DST_IN,
    SRC_OUT,
    DST_OUT,
    SRC_ATOP,
    DST_ATOP,
    XOR,
    DARKEN,
    LIGHTEN,
    MULTIPLY,
    SCREEN,
    ADD,
    OVERLAY;

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getBlendMode(): BlendMode =
            when (this) {
                CLEAR -> BlendMode.CLEAR
                SRC -> BlendMode.SRC
                DST -> BlendMode.DST
                SRC_OVER -> BlendMode.SRC_OVER
                DST_OVER -> BlendMode.DST_OVER
                SRC_IN -> BlendMode.SRC_IN
                DST_IN -> BlendMode.DST_IN
                SRC_OUT -> BlendMode.SRC_OUT
                DST_OUT -> BlendMode.DST_OUT
                SRC_ATOP -> BlendMode.SRC_ATOP
                DST_ATOP -> BlendMode.DST_ATOP
                XOR -> BlendMode.XOR
                DARKEN -> BlendMode.DARKEN
                LIGHTEN -> BlendMode.LIGHTEN
                MULTIPLY -> BlendMode.MULTIPLY
                SCREEN -> BlendMode.SCREEN
                ADD -> BlendMode.PLUS
                OVERLAY -> BlendMode.OVERLAY
            }

    fun getPorterDuffMode(): PorterDuff.Mode =
            when (this) {
                CLEAR -> PorterDuff.Mode.CLEAR
                SRC -> PorterDuff.Mode.SRC
                DST -> PorterDuff.Mode.DST
                SRC_OVER -> PorterDuff.Mode.SRC_OVER
                DST_OVER -> PorterDuff.Mode.DST_OVER
                SRC_IN -> PorterDuff.Mode.SRC_IN
                DST_IN -> PorterDuff.Mode.DST_IN
                SRC_OUT -> PorterDuff.Mode.SRC_OUT
                DST_OUT -> PorterDuff.Mode.DST_OUT
                SRC_ATOP -> PorterDuff.Mode.SRC_ATOP
                DST_ATOP -> PorterDuff.Mode.DST_ATOP
                XOR -> PorterDuff.Mode.XOR
                DARKEN -> PorterDuff.Mode.DARKEN
                LIGHTEN -> PorterDuff.Mode.LIGHTEN
                MULTIPLY -> PorterDuff.Mode.MULTIPLY
                SCREEN -> PorterDuff.Mode.SCREEN
                ADD -> PorterDuff.Mode.ADD
                OVERLAY -> PorterDuff.Mode.OVERLAY
            }
}
