package com.example.inews;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.inews.databinding.ActivityPhotoViewerBinding;
import com.google.firebase.storage.FirebaseStorage;

public class PhotoViewer extends AppCompatActivity {

    ActivityPhotoViewerBinding b;

    FirebaseStorage mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityPhotoViewerBinding.inflate(getLayoutInflater());
        View v = b.getRoot();
        setContentView(v);

        b.layout.setOnClickListener(v1 -> finish());

        mStorage = FirebaseStorage.getInstance("gs://inews-b979d.appspot.com");

        mStorage.getReference("Users/").child(getIntent().getStringExtra("photo")).getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri uri = task.getResult();
                Glide.with(getApplicationContext()).load(uri).into(b.photo);
            }
        });

    }
}