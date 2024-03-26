package com.example.projectapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileEditActivity extends AppCompatActivity {
    private ImageView avatar;
    private String encodedImage;
    private ActivityResultLauncher<Intent> resultLauncher;
    private EditText userNameEditText, emailEditText, phoneEditText;
    private Button saveButton;
    private Attendee currentAttendee;
    private DataHandler dataHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);
        avatar = findViewById(R.id.avatarImage);
        Button avatarButton = findViewById(R.id.avatarImageButton);

        userNameEditText = findViewById(R.id.userNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.saveButton);
        registerResult();

        dataHandler = DataHandler.getInstance();
        loadAttendeeInfo();

        avatarButton.setOnClickListener(v -> pickImage());

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileChanges();
            }
        });
    }

    private void loadAttendeeInfo() {
        currentAttendee = dataHandler.getAttendee();
        if (currentAttendee != null) {
            userNameEditText.setText(currentAttendee.getName());
            emailEditText.setText(currentAttendee.getHomepage());
            phoneEditText.setText(currentAttendee.getContactInfo());

            String encodedImage = currentAttendee.getProfilePic();
            if (encodedImage != null && !encodedImage.isEmpty()) {
                Bitmap bitmap = stringToBitmap(encodedImage);
                if (bitmap != null) {
                    avatar.setImageBitmap(bitmap);
                }
            }
        }
    }

    private void saveProfileChanges() {
        String newName = userNameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String newPhone = phoneEditText.getText().toString().trim();

        currentAttendee.setName(newName);
        currentAttendee.setHomepage(newEmail);
        currentAttendee.setContactInfo(newPhone);

        if (encodedImage != null && !encodedImage.isEmpty()) {
            currentAttendee.setProfilePic(encodedImage);
        }

        updateAttendeeInFirestore(currentAttendee);

        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
    }



    private void updateAttendeeInFirestore(Attendee attendee) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference attendeeRef = db.collection("attendees").document(attendee.getAttendeeId());


        attendeeRef.set(attendee);
        attendeeRef.set(attendee) 

                .addOnSuccessListener(aVoid -> Log.d("ProfileEditActivity", "DocumentSnapshot successfully updated!"))
                .addOnFailureListener(e -> Log.w("ProfileEditActivity", "Error updating document", e));
    }

    private void pickImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        resultLauncher.launch(intent);
    }

    private void registerResult() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        avatar.setImageURI(imageUri);
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            encodedImage = bitmapToString(bitmap);
                        } catch (IOException e) {
                            Log.e("ProfileEditActivity", "Error converting image", e);
                        }
                    }
                }
        );
    }

    public String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public Bitmap stringToBitmap(String encodedString) {
        try {
            byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}