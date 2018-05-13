package com.tutorial.vanard.photoblog;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.tutorial.vanard.photoblog.Adapter.BlogRecyclerAdapter;
import com.tutorial.vanard.photoblog.Model.BlogPost;
import com.tutorial.vanard.photoblog.Model.User;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private RecyclerView blogListView;
    private List<BlogPost> blogList;
    private List<User> userList;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth mAuth;
    private BlogRecyclerAdapter blogRecyclerAdapter;

    private DocumentSnapshot lastVisible;
    private Boolean isFirstPageFirstLoad = true;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        blogList = new ArrayList<>();
        userList = new ArrayList<>();
        blogListView = view.findViewById(R.id.blog_list_view);

        mAuth = FirebaseAuth.getInstance();

        blogListView.setHasFixedSize(true);
        blogListView.setLayoutManager(new LinearLayoutManager(getContext()));
        blogRecyclerAdapter = new BlogRecyclerAdapter(getContext(), blogList, userList);
        blogListView.setAdapter(blogRecyclerAdapter);

        if (mAuth.getCurrentUser() != null) {

            firebaseFirestore = FirebaseFirestore.getInstance();

            blogListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                    if (reachedBottom){

                        loadMorePost();
                    }
                }
            });

//            loadPost();

        }
        // Inflate the layout for this fragment
        return view;
    }

    private void loadPost(){
        Query firstQuery = firebaseFirestore.collection("Posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(3);
        firstQuery.addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (!documentSnapshots.isEmpty()) {

                    if (isFirstPageFirstLoad) {
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                        blogList.clear();
                        userList.clear();
                    }
                    for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            String blogPostId = doc.getDocument().getId();
                            final BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);

                            String blogUserId = doc.getDocument().getString("user_id");
                            firebaseFirestore.collection("Users").document(blogUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()){

                                        User user = task.getResult().toObject(User.class);

                                        if (isFirstPageFirstLoad) {
                                            userList.add(user);
                                            blogList.add(blogPost);
                                        } else {
                                            userList.add(0, user);
                                            blogList.add(0, blogPost);
                                        }
                                        blogRecyclerAdapter.notifyDataSetChanged();

                                    }
                                }
                            });
                        }
                    }
                    isFirstPageFirstLoad = false;
                }
            }
        });
    }

    public void loadMorePost(){
        if (mAuth.getCurrentUser() != null){

            Query nextQuery = firebaseFirestore.collection("Posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(3);

            nextQuery.addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    if (!documentSnapshots.isEmpty()) {
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                        for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                            if (doc.getType() == DocumentChange.Type.ADDED) {
                                String blogPostId = doc.getDocument().getId();
                                final BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);
                                String blogUserId = doc.getDocument().getString("user_id");

                                firebaseFirestore.collection("Users").document(blogUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()){

                                            User user = task.getResult().toObject(User.class);

                                            userList.add(user);
                                            blogList.add(blogPost);

                                            blogRecyclerAdapter.notifyDataSetChanged();

                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null) {
            blogList.clear();
            userList.clear();
            loadPost();
        }
    }
}
