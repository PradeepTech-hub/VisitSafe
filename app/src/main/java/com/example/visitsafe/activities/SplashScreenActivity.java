package com.example.visitsafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visitsafe.R;
import com.example.visitsafe.utils.RoleNavigator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView logo = findViewById(R.id.ivSplashLogo);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        new Handler().postDelayed(this::checkSessionAndNavigate, 2000);
    }

    private void checkSessionAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getPhoneNumber() != null) {
            String phone = currentUser.getPhoneNumber();
            FirebaseFirestore.getInstance().collection("users").document(phone).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String role = doc.getString("role");
                            Intent intent = RoleNavigator.dashboardIntent(this, role);
                            if (intent != null) {
                                intent.putExtra("phone", phone);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                        // If user document doesn't exist or role is missing, go to selection
                        goToSelection();
                    })
                    .addOnFailureListener(e -> goToSelection());
        } else {
            goToSelection();
        }
    }

    private void goToSelection() {
        startActivity(new Intent(SplashScreenActivity.this, ApartmentSelectionActivity.class));
        finish();
    }
}