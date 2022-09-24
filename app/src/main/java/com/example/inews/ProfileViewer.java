package com.example.inews;

import android.content.Intent;
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
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.inews.Models.News;
import com.example.inews.databinding.ActivityProfileViewerBinding;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

public class ProfileViewer extends AppCompatActivity {

    ActivityProfileViewerBinding b;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage mStorage;
    DocumentReference userRef;

    Bundle bundle;

    String user;

    Query query;
    FirestoreRecyclerOptions<News> options;
    FirestoreRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityProfileViewerBinding.inflate(getLayoutInflater());
        View v = b.getRoot();
        setContentView(v);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userRef = db.collection("Users").document(mUser.getUid());
        mStorage = FirebaseStorage.getInstance("gs://inews-b979d.appspot.com");

        bundle = getIntent().getExtras();

        user = bundle.getString("user");
        setProfile();

        b.avatar.setDrawingCacheEnabled(true);

        b.avatar.setOnClickListener(v1 -> {
            b.avatar.setAnimating(true);
            new Handler().postDelayed(() -> {
                Intent i = new Intent(ProfileViewer.this, PhotoViewer.class);
                i.putExtra("photo", user);
                startActivity(i);
                overridePendingTransition(R.anim.zoomin, R.anim.zoomout);
                b.avatar.setAnimating(false);
            }, 2000);
        });

        query = FirebaseFirestore.getInstance()
                .collection("news")
                .whereEqualTo("publisher", user)
                .limit(50);

        options = new
                FirestoreRecyclerOptions.Builder<News>()
                .setQuery(query, News.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<News, NewsHolder>(options) {

            @NonNull
            @Override
            public NewsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.news, parent, false);

                return new NewsHolder(v);
            }

            @Override
            protected void onBindViewHolder(@NonNull NewsHolder h, int p, @NonNull News m) {

                String publisherID = m.getPublisher();

                h.title.setText(m.getTitle());
                h.contain.setText(m.getContain());
                h.user.setText(m.getUser());
                Glide.with(getApplicationContext()).load(m.getLink()).into(h.containImg);
                mStorage.getReference("Users/").child(publisherID).getDownloadUrl().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri uri = task.getResult();
                        Log.d("userpp", "onBindViewHolder: " + uri);
                        Glide.with(getApplicationContext()).load(uri).into(h.userImg);
                    }
                }).addOnFailureListener(e -> Log.d("userpp", "onFailure: " + e));

                if (m.isTrusted()) {
                    h.user.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.check_circle_fill,0);
                    h.user.setCompoundDrawablePadding(10);
                }else{
                    h.user.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,0);
                }

                h.rating.setText(m.getRating() + " " + getString(R.string.liked));

                if (h.liked) {
                    h.like.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.silver));
                }

                h.like.setOnClickListener(v12 -> {
                    if (!h.liked) {
                        h.liked = true;
                        h.like.setImageResource(R.drawable.heart_fill);
                        h.like.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.red));
                        if (!m.getPublisher().equals(mUser.getUid())) {
                            getSnapshots().getSnapshot(h.getBindingAdapterPosition()).getReference().update("rating", FieldValue.increment(1));
                        }
                    }else{
                        h.liked = false;
                        h.like.setImageResource(R.drawable.heart);
                        h.like.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.silver));
                        if (!m.getPublisher().equals(mUser.getUid())) {
                            getSnapshots().getSnapshot(h.getBindingAdapterPosition()).getReference().update("rating", FieldValue.increment(-1));
                        }
                    }
                });

                h.newCard.setOnClickListener(v1 -> {
                    Intent i = new Intent(ProfileViewer.this, NewViewer.class);
                    i.putExtra("title", m.getTitle());
                    i.putExtra("contain", m.getContain());
                    i.putExtra("user", m.getUser());
                    i.putExtra("date", m.getDate());
                    i.putExtra("link", m.getLink());
                    i.putExtra("publisher", m.getPublisher());
                    i.putExtra("snapshot", getSnapshots().getSnapshot(p).getReference().getPath());
                    i.putExtra("rating", m.getRating());
                    startActivity(i);
                    overridePendingTransition(R.anim.zoomin, R.anim.zoomout);
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
            }
        };

        b.profileRecycler.setHasFixedSize(true);
        b.profileRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        b.profileRecycler.setItemAnimator(null);
        b.profileRecycler.setAdapter(adapter);
        adapter.startListening();

    }

    private void setProfile(){
        mStorage.getReference("Users").child(user).getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri uri = task.getResult();
                Glide.with(getApplicationContext()).load(uri).into(b.avatar);
            }
        });
        db.collection("Users").document(user).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                boolean trusted = snapshot.getBoolean("trusted");
                String name = snapshot.getString("name");
                String bio = snapshot.getString("bio");
                setLayout(name, bio, trusted);
            }
        });
    }

    private void setLayout(String name, String bio, boolean trusted) {
        b.userNM.setText(name);
        b.userBio.setText(bio);
        b.avatar.setText(name);
        if (trusted) {
            b.userNM.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.check_circle_fill, 0);
        }else{
            b.userNM.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private static class NewsHolder extends RecyclerView.ViewHolder {

        boolean liked;
        TextView title, contain, user, rating;
        ImageView userImg, containImg, like;
        CardView newCard;

        public NewsHolder(@NonNull View v) {
            super(v);

            title = v.findViewById(R.id.title);
            contain = v.findViewById(R.id.contain);
            user = v.findViewById(R.id.userName);
            rating = v.findViewById(R.id.rating);

            userImg = v.findViewById(R.id.userImg);
            containImg = v.findViewById(R.id.newImg);
            like = v.findViewById(R.id.heart);

            newCard = v.findViewById(R.id.newBg);

        }
    }

}