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
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.inews.Models.News;
import com.example.inews.databinding.ActivityMainBinding;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding b;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage mStorage;
    DocumentReference userRef;

    Query query;
    FirestoreRecyclerOptions<News> options;
    FirestoreRecyclerAdapter adapter;

    String category = "Hepsi";
    boolean isPublisher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
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

        setProfile();

        b.userCard.setOnClickListener(v1 -> {
            Intent i = new Intent(MainActivity.this, ProfileViewer.class);
            i.putExtra("user", mUser.getUid());
            startActivity(i);
            overridePendingTransition(R.anim.zoomin, R.anim.zoomout);
        });

        b.swiperefresh.setOnRefreshListener(() -> new Handler().postDelayed(() -> b.swiperefresh.setRefreshing(false), 1000));

        b.createNews.setOnClickListener(v1 -> createPage());

        b.categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            Chip chip = b.categoryChips.findViewById(b.categoryChips.getCheckedChipId());
            updateAdapter(chip.getText().toString());
        });

        b.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    updateAdapter("Hepsi");
                }else{
                    searchAdapter(newText);
                }
                return false;
            }
        });

        b.searchBar.setOnCloseListener(() -> {
            updateAdapter("Hepsi");
            return false;
        });

        query = FirebaseFirestore.getInstance()
                .collection("news")
                .whereArrayContains("category", category)
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

                h.userImg.setOnClickListener(v1 -> {
                    Intent i = new Intent(MainActivity.this, ProfileViewer.class);
                    i.putExtra("user", m.getPublisher());
                    startActivity(i);
                    overridePendingTransition(R.anim.zoomin, R.anim.zoomout);
                });

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
                    Intent i = new Intent(MainActivity.this, NewViewer.class);
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

        b.newsRecycler.setHasFixedSize(true);
        b.newsRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        b.newsRecycler.setItemAnimator(null);
        b.newsRecycler.setAdapter(adapter);
        adapter.startListening();

    }

    private void updateAdapter(String category) {
        query = FirebaseFirestore.getInstance()
                .collection("news")
                .whereArrayContains("category", category)
                .limit(50);
        options =  new
                FirestoreRecyclerOptions.Builder<News>()
                .setQuery(query, News.class)
                .build();
        adapter.updateOptions(options);
        b.newsRecycler.scrollToPosition(0);
    }

    private void searchAdapter(String tag) {
        query = FirebaseFirestore.getInstance()
                .collection("news")
                .whereArrayContains("tags", tag)
                .limit(50);
        options =  new
                FirestoreRecyclerOptions.Builder<News>()
                .setQuery(query, News.class)
                .build();
        adapter.updateOptions(options);
        b.newsRecycler.scrollToPosition(0);
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

    private void setProfile() {
        mStorage.getReference("Users").child(mUser.getUid()).getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri uri = task.getResult();
                Glide.with(getApplicationContext()).load(uri).into(b.userPP);
            }
        });
        db.collection("Users").document(mUser.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                isPublisher = snapshot.getBoolean("publisher");
                if (isPublisher) {
                    b.createNews.setVisibility(View.VISIBLE);
                }else{
                    b.createNews.setVisibility(View.GONE);
                }
            }
        });
    }

    private void createPage() {
        startActivity(new Intent(this, CreateNews.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        setSupportActionBar(b.toolbar);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}