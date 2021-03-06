package com.tutorial.vanard.photoblog.Adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.tutorial.vanard.photoblog.CommentsActivity;
import com.tutorial.vanard.photoblog.Model.BlogPost;
import com.tutorial.vanard.photoblog.Model.User;
import com.tutorial.vanard.photoblog.R;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    public final static String TAG = BlogRecyclerAdapter.class.getSimpleName();

    public List<BlogPost> blogList;
    public List<User> userList;
    public Context context;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public BlogRecyclerAdapter(Context context, List<BlogPost> blogList, List<User> userList) {
        this.context = context;
        this.blogList = blogList;
        this.userList = userList;
    }

    @Override
    public BlogRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item, parent, false);
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final BlogRecyclerAdapter.ViewHolder holder, final int position) {
        BlogPost blogPost = blogList.get(position);
        User user = userList.get(position);

        holder.setIsRecyclable(false);

        final String blogPostId = blogPost.BlogPostId;
        final String currentUserId = mAuth.getCurrentUser().getUid();

        String descData = blogPost.getDesc();
        holder.setDescText(descData);

        String imageUrl = blogPost.getImage_url();
        String thumbUri = blogPost.getImage_thumb();
        holder.setBlogImage(imageUrl, thumbUri);

        String blog_user_id = blogPost.getUser_id();

        if (blog_user_id.equals(currentUserId)){
            holder.blogDeleteBtn.setEnabled(true);
            holder.blogDeleteBtn.setVisibility(View.VISIBLE);
        }

        //user data will be retrieved
        String userName = user.getName();
        String userImage = user.getImage();

        holder.setUserData(userName, userImage);

        db.collection("Posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
        .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                for (DocumentChange doc : documentSnapshots.getDocumentChanges()){
                    BlogPost blogDoc = doc.getDocument().toObject(BlogPost.class);

                    try{
                        long milliseconds = blogDoc.getTimestamp().getTime();
                        String dateString = DateFormat.format("MM/dd/yyyy", new Date(milliseconds)).toString();
                        holder.setTime(dateString);
                    }catch (Exception ex){

                    }
                }
            }
        });

        //Get likes count
        db.collection("Posts/" + blogPostId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (!documentSnapshots.isEmpty()){
                    int count = documentSnapshots.size();
                    holder.updateLikesCount(count);
                }else{
                    holder.updateLikesCount(0);
                }
            }
        });
        //Get likes
        db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (documentSnapshot.exists()){
                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_accent));
                }else{
                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_gray));
                }
            }
        });
        //Likes feature
        holder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.getResult().exists()){
                            Map<String, Object> likesMap = new HashMap<>();
                            likesMap.put("timestamp", FieldValue.serverTimestamp());
                            db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).set(likesMap);
                        }else{
                            db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).delete();
                        }
                    }
                });
            }
        });

        holder.blogCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, CommentsActivity.class);
                i.putExtra("blog_post_id", blogPostId);
                context.startActivity(i);
            }
        });

        holder.blogDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    db.collection("Posts").document(blogPostId).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            blogList.remove(position);
                            userList.remove(position);
                        }
                    });
                }catch (Exception e){
                    Toast.makeText(context, "Exception : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return blogList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;

        private TextView descView, blogDate, blogUserName, blogLikeCount;
        private ImageView blogImageView, blogLikeBtn, blogCommentBtn;
        private CircleImageView blogUserImage;

        private Button blogDeleteBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            blogLikeBtn = mView.findViewById(R.id.blog_like_btn);
            blogCommentBtn = mView.findViewById(R.id.blog_comment_icon);
            blogDeleteBtn = mView.findViewById(R.id.blog_delete_btn);
        }

        public void setDescText(String descText){
            descView = mView.findViewById(R.id.blog_desc);
            descView.setText(descText);
        }

        public void setBlogImage(String downloadUri, String thumbUri){
            blogImageView = mView.findViewById(R.id.blog_image);
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.demo_image_default);
            Glide.with(context).load(downloadUri)
                    .thumbnail(Glide.with(context).load(thumbUri))
                    .into(blogImageView);
        }

        public void setTime(String date){
            blogDate = mView.findViewById(R.id.blog_date);
            blogDate.setText(date);
        }

        public void setUserData(String name, String image){
            blogUserImage = mView.findViewById(R.id.blog_user_image);
            blogUserName = mView.findViewById(R.id.blog_username);

            blogUserName.setText(name);

            RequestOptions placeholderOption = new RequestOptions();
            placeholderOption.placeholder(R.drawable.user_image);

            Glide.with(context).applyDefaultRequestOptions(placeholderOption).load(image).into(blogUserImage);
        }

        public void updateLikesCount(int count){
            blogLikeCount = mView.findViewById(R.id.blog_like_count);
            blogLikeCount.setText(count + " Likes");
        }
    }
}
