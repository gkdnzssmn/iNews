package com.example.inews;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.bumptech.glide.Glide;
import com.example.inews.databinding.ActivityCreateNewsBinding;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateNews extends AppCompatActivity {

    public ActivityCreateNewsBinding b;
    Context context;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage mStorage;
    DocumentReference userRef;

    Map<String, Object> newMap;

    ArrayList<String> categorys, tags;

    String link = " ";
    String user;
    boolean trusted;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    boolean previewed;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCreateNewsBinding.inflate(getLayoutInflater());
        View v = b.getRoot();
        setContentView(v);

        b.shareNews.setOnClickListener(v1 -> startNews());

        b.newInput.setCustomSelectionActionModeCallback(new StyleCallback());

        b.textLeft.setOnClickListener(v1 -> b.newInput.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START));

        b.textCenter.setOnClickListener(v1 -> b.newInput.setTextAlignment(View.TEXT_ALIGNMENT_CENTER));

        b.textRight.setOnClickListener(v1 -> b.newInput.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END));

        b.textSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Integer.parseInt(s.toString()) >= 8) {
                    b.newInput.setTextSize(Float.parseFloat(s.toString()));
                }else{
                    b.newInput.setTextSize(8);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        b.link.setOnClickListener(v1 -> getLink());

        b.reEdit.setOnClickListener(v1 -> returnEdit());

        b.tags.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().endsWith(",")) {
                    addTag(s.toString().replace(",", ""));
                    b.tags.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        b.category.setOnClickListener(v1 -> getCategorys());

    }

    private void addTag(String replace) {
        Chip chip = new Chip(this);
        chip.setText(replace);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(this::onClick);
        b.tagsChips.addView(chip);
        tags.add(replace);
    }

    public void onClick(View v) {
        Chip chip = (Chip) v;
        b.tagsChips.removeView(chip);
    }

    private void returnEdit() {
        b.toolbar.setTitle("Oluştur");
        b.editPhase.setVisibility(View.VISIBLE);
        b.previewPhase.setVisibility(View.GONE);
        previewed = false;
        b.reEdit.setVisibility(View.GONE);
        b.shareNews.setCardBackgroundColor(AppCompatResources.getColorStateList(context, R.color.silver));
        b.shareText.setText(getString(R.string.preview));
    }

    public void startNews() {
        if (!previewed) {
            b.editPhase.setVisibility(View.GONE);
            b.previewPhase.setVisibility(View.VISIBLE);
            previewed = true;
            setPreview();
        }else{
            shareNews();
        }
    }

    private void shareNews() {
        newMap.put("title", b.titleNews.getText().toString());
        newMap.put("contain", b.newInput.getText().toString());
        newMap.put("link", link);
        newMap.put("category", Arrays.asList(b.category.getText().toString(), "Hepsi"));
        newMap.put("tags", tags);
        newMap.put("publisher", mUser.getUid());
        newMap.put("user", user);
        newMap.put("trusted", trusted);
        newMap.put("date", new SimpleDateFormat("dd-MM-yyyy, HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date()));
        newMap.put("rating", 0);
        db.collection("news").document(new SimpleDateFormat("dd-MM-yyyy, HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date())).set(newMap).addOnFailureListener(e -> Log.d("firestore", "onFailure: " + e));
        finish();
    }

    private void setPreview() {
        b.toolbar.setTitle("Önizleme");
        b.reEdit.setVisibility(View.VISIBLE);
        b.shareNews.setCardBackgroundColor(AppCompatResources.getColorStateList(context, R.color.purple));
        b.shareText.setText(getString(R.string.share));
        b.title.setText(b.titleNews.getText().toString());
        b.contain.setText(b.newInput.getText().toString());
        Glide.with(context).load(link).into(b.newImg);
    }

    private void getCategorys(){
        final ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, categorys));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Kategoriler")
                .setView(listView)
                .create();
        dialog.show();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            b.category.setText(categorys.get(position));
            dialog.hide();
        });
    }

    private void getLink() {
        final EditText taskEditText = new EditText(this);
        taskEditText.setBackgroundColor(getResources().getColor(R.color.transparent));
        taskEditText.setPadding(10, 0, 10, 0);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("İçerik Linki")
                .setView(taskEditText)
                .setPositiveButton("Ekle", (dialog1, which) -> link = taskEditText.getText().toString())
                .setNegativeButton("İptal", null)
                .create();
        dialog.show();
    }

    public class StyleCallback implements ActionMode.Callback {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.text_style, menu);
            menu.removeItem(android.R.id.selectAll);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, String.format("onActionItemClicked item=%s/%d", item.toString(), item.getItemId()));
            CharacterStyle cs;
            int start = b.newInput.getSelectionStart();
            int end = b.newInput.getSelectionEnd();
            SpannableStringBuilder ssb = new SpannableStringBuilder(b.newInput.getText());

            switch (item.getItemId()) {

                case R.id.normal:
                    cs = new StyleSpan(Typeface.NORMAL);
                    ssb.setSpan(cs, start, end, 1);
                    editor.putString("contain", ssb.toString()).commit();
                    b.newInput.setText(ssb);
                    return true;

                case R.id.bold:
                    cs = new StyleSpan(Typeface.BOLD);
                    ssb.setSpan(cs, start, end, 1);
                    editor.putString("contain", ssb.toString()).commit();
                    b.newInput.setText(ssb);
                    return true;

                case R.id.italic:
                    cs = new StyleSpan(Typeface.ITALIC);
                    ssb.setSpan(cs, start, end, 1);
                    editor.putString("contain", ssb.toString()).commit();
                    b.newInput.setText(ssb);
                    return true;

                case R.id.underline:
                    cs = new UnderlineSpan();
                    ssb.setSpan(cs, start, end, 1);
                    editor.putString("contain", ssb.toString()).commit();
                    b.newInput.setText(ssb);
                    return true;
            }
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        context = getApplicationContext();
        newMap = new HashMap<>();
        sharedPref = this.getSharedPreferences("myNews", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        b.shareNews.setCardBackgroundColor(AppCompatResources.getColorStateList(context, R.color.silver));
        b.shareText.setText(getString(R.string.preview));
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userRef = db.collection("Users").document(mUser.getUid());
        mStorage = FirebaseStorage.getInstance("gs://inews-b979d.appspot.com");

        getProfile();

        categorys = new ArrayList<>();
        tags = new ArrayList<>();
        categorys.add("Gündem");
        categorys.add("Teknoloji");
        categorys.add("Ekonomi");
        categorys.add("Sağlık");
        categorys.add("Yaşam");
        categorys.add("Spor");
        categorys.add("Siyaset");
        categorys.add("Eğitim");
        categorys.add("Dünya");

    }

    private void getProfile() {
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                user = snapshot.getString("name");
                trusted = snapshot.getBoolean("trusted");
            }
        });
    }

}