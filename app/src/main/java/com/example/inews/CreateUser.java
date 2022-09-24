package com.example.inews;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inews.databinding.ActivityCreateUserBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateUser extends AppCompatActivity {

    ActivityCreateUserBinding b;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore db;
    FirebaseStorage mStorage;
    DocumentReference userRef;

    Map<String, Object> userMap;

    DatePickerDialog datePickerDialog;
    Calendar calendar;
    int month, year, dayOfMonth;

    boolean dateT, genT, imgT;

    private Uri filePath;

    private final int PICK_IMAGE_REQUEST = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCreateUserBinding.inflate(getLayoutInflater());
        View v = b.getRoot();
        setContentView(v);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        userRef = db.collection("Users").document(mUser.getUid());
        userMap = new HashMap<>();
        mStorage = FirebaseStorage.getInstance("gs://inews-b979d.appspot.com");

        b.saveInfos.setOnClickListener(v1 -> saveUser());

        b.dateInput.setOnClickListener(v1 -> pickDate());

        b.genderInput.setOnClickListener(v1 -> pickGen());

        b.imgCard.setOnClickListener(v1 -> selectImage());

    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Profil fotoğrafınızı seçin"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                b.imgView.setImageBitmap(bitmap);
                imgT = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void pickGen() {
        final CharSequence[] gender = {"Erkek","Kadın","Belirsiz"};
        AlertDialog.Builder alert = new AlertDialog.Builder(CreateUser.this);
        alert.setTitle("Select Gender");
        alert.setSingleChoiceItems(gender,-1, (dialog, which) -> {
            if(gender[which]=="Erkek")
            {
                b.genderInput.setText(gender[which]);
                genT = true;
            }
            else if (gender[which]=="Kadın")
            {
                b.genderInput.setText(gender[which]);
                genT = true;
            }
            else if (gender[which]=="Belirsiz")
            {
                b.genderInput.setText(gender[which]);
                genT = true;
            }
        });
        alert.show();
    }

    private void pickDate() {
        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        datePickerDialog = new DatePickerDialog(CreateUser.this,
                (datePicker, year, month, day) -> {
            month++;
                    if (year > 1930 & year < 2010) {
                        b.dateInput.setText(day + "/" + month + "/" + year);
                        dateT = true;
                    }else{
                        b.ageLay.setError("Tarihi kontrol ediniz.");
                        dateT = false;
                    }
                }, year, month, dayOfMonth);
        datePickerDialog.show();
    }

    private void saveUser() {
        if (b.nameInput.getText().length() > 3 & b.bioInput.getText().length() > 16 & dateT & genT & imgT) {
            userMap.put("name", b.nameInput.getText().toString());
            userMap.put("bio", b.bioInput.getText().toString());
            userMap.put("date", b.dateInput.getText().toString());
            userMap.put("gender", b.genderInput.getText().toString());
            userMap.put("trusted", false);
            userRef.set(userMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    uploadImg();
                }else{
                    Toast.makeText(CreateUser.this, getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            if (!genT) {
                b.genderLay.setError("");
            }

            if (b.nameInput.getText().length() < 3) {
                b.nameLay.setError("En az 3 karakter.");
            }

            if (b.bioInput.getText().length() < 16) {
                b.bioLay.setError("En az 16 karakter.");
            }
        }
    }

    private void uploadImg() {
        mStorage.getReference("Users/" + mUser.getUid()).putFile(filePath).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                finish();
                startActivity(new Intent(CreateUser.this, MainActivity.class));
            }else{
                Toast.makeText(CreateUser.this, getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        });
    }
}