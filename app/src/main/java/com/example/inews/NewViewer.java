package com.example.inews;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.inews.Models.Comments;
import com.example.inews.databinding.ActivityNewViewerBinding;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NewViewer extends AppCompatActivity {

    ActivityNewViewerBinding b;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage mStorage;
    DocumentReference userRef;

    String title, contain, link, user, publisher, date;
    DocumentReference snapshot;
    Bundle bundle;

    String myInfo;

    Query query;
    FirestoreRecyclerOptions<Comments> options;
    FirestoreRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityNewViewerBinding.inflate(getLayoutInflater());
        View v = b.getRoot();
        setContentView(v);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userRef = db.collection("Users").document(mUser.getUid());
        mStorage = FirebaseStorage.getInstance("gs://inews-b979d.appspot.com");

        MobileAds.initialize(this, initializationStatus -> {

        });

        AdRequest adRequest = new AdRequest.Builder().build();
        b.adView.loadAd(adRequest);
        
        b.send.setOnClickListener(v1 -> {
            sendComment();
        });

        bundle = getIntent().getExtras();
        title = bundle.getString("title");
        contain = bundle.getString("contain");
        link = bundle.getString("link");
        user = bundle.getString("user");
        publisher = bundle.getString("publisher");
        date = bundle.getString("date");
        snapshot = db.collection("news").document(bundle.getString("snapshot").replace("news/", ""));
        getProfile();
        setLayout();

        query = snapshot
                .collection("comments")
                .limit(50);

        options = new
                FirestoreRecyclerOptions.Builder<Comments>()
                .setQuery(query, Comments.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Comments, CommentsHolder>(options) {
            @NonNull
            @Override
            public CommentsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.comments, parent, false);

                return new CommentsHolder(v);
            }

            @Override
            protected void onBindViewHolder(@NonNull CommentsHolder h, int p, @NonNull Comments m) {

                String publisherID = m.getPublisher();

                h.comment.setText(m.getComment());
                h.user.setText(m.getUser());

                mStorage.getReference("Users/").child(publisherID).getDownloadUrl().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri uri = task.getResult();
                        Log.d("userpp", "onBindViewHolder: " + uri);
                        Glide.with(getApplicationContext()).load(uri).into(h.userImg);
                    }
                }).addOnFailureListener(e -> Log.d("userpp", "onFailure: " + e));

            }
        };

        b.commentRecycler.setHasFixedSize(true);
        b.commentRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        b.commentRecycler.setAdapter(adapter);
        adapter.startListening();

    }

    private static class CommentsHolder extends RecyclerView.ViewHolder {

        TextView user, comment;
        ImageView userImg;

        public CommentsHolder(@NonNull View v) {
            super(v);

            user = v.findViewById(R.id.comUserName);
            comment = v.findViewById(R.id.comment);

            userImg = v.findViewById(R.id.comUserImg);

        }
    }

    private void sendComment() {
        Map<String, Object> cMap = new HashMap<>();
        cMap.put("comment", b.commentInput.getText().toString());
        cMap.put("publisher", mUser.getUid());
        cMap.put("date", new SimpleDateFormat("dd-MM-yyyy, HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date()));
        cMap.put("user", myInfo);
        snapshot.collection("comments").document(new SimpleDateFormat("dd-MM-yyyy, HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date())).set(cMap);
        b.commentInput.setText("");
    }

    private void getProfile(){
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                myInfo = snapshot.getString("name");
            }
        });
    }

    private void setLayout() {
        b.title.setText(title);
        b.contain.setText(contain);
        Glide.with(getApplicationContext()).load(link).into(b.newImg);
        b.dateTxt.setText(date);
        b.userName.setText(user);
        mStorage.getReference("Users/").child(publisher).getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri uri = task.getResult();
                Log.d("userpp", "onBindViewHolder: " + uri);
                Glide.with(getApplicationContext()).load(uri).into(b.userImg);
            }
        }).addOnFailureListener(e -> Log.d("userpp", "onFailure: " + e));
        new Handler().postDelayed(() -> b.scrollView.fullScroll(View.FOCUS_UP),500);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}