package com.example.afinal;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class EditActivity extends AppCompatActivity {

    private EditText changename, changeemail, currentpass;
    private Button apply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_profile_information);

        // Ánh xạ các thành phần giao diện
        changename = findViewById(R.id.changename);
        changeemail = findViewById(R.id.changeemail);
        currentpass = findViewById(R.id.currentpass);
        apply = findViewById(R.id.apply);

        ImageView imageView = findViewById(R.id.imageView11);

        // Xử lý sự kiện click quay lại
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EditActivity.this, accoptionActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện nhấn nút Apply
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserProfile();
            }
        });
    }

    private void updateUserProfile() {
        String newName = changename.getText().toString().trim();
        String newEmail = changeemail.getText().toString().trim();
        String password = currentpass.getText().toString().trim();

        // Kiểm tra nếu không có trường nào được điền
        if ((newName.isEmpty() && newEmail.isEmpty())) {
            Toast.makeText(EditActivity.this, "Please fill in at least one field: Name or Email.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra mật khẩu (bắt buộc)
        if (password.isEmpty()) {
            currentpass.setError("Password is required");
            return;
        }

        // Kiểm tra định dạng email nếu có nhập email
        if (!newEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            changeemail.setError("Invalid email format");
            return;
        }

        // Kiểm tra mạng
        if (!isNetworkAvailable()) {
            Toast.makeText(EditActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        // Lấy người dùng hiện tại
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(EditActivity.this, "No user logged in. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Thực hiện re-authenticate với mật khẩu
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Cập nhật email hoặc tên nếu có nhập
                updateEmailIfNeeded(user, newEmail);
                updateNameIfNeeded(user, newName);
            } else {
                String error = task.getException().getMessage();
                if (error.contains("The password is invalid")) {
                    currentpass.setError("Invalid password");
                } else {
                    Toast.makeText(EditActivity.this, "Re-authentication failed: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateEmailIfNeeded(FirebaseUser user, String newEmail) {
        if (!newEmail.isEmpty()) {
            user.updateEmail(newEmail).addOnCompleteListener(updateEmailTask -> {
                if (updateEmailTask.isSuccessful()) {
                    Toast.makeText(EditActivity.this, "Email updated to: " + newEmail, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EditActivity.this, "Failed to update email: " + updateEmailTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateNameIfNeeded(FirebaseUser user, String newName) {
        if (!newName.isEmpty()) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();
            user.updateProfile(profileUpdates).addOnCompleteListener(updateNameTask -> {
                if (updateNameTask.isSuccessful()) {
                    Toast.makeText(EditActivity.this, "Name updated to: " + newName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EditActivity.this, "Failed to update name: " + updateNameTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}