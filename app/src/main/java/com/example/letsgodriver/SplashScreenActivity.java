package com.example.letsgodriver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SplashScreenActivity extends AppCompatActivity {
    private final static int LOGIN_REQUEST_CODE = 7171;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private String fName, lName, phone;


    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @Override
    protected void onStart() {
        super.onStart();
        delaySplashScreen();

    }

    @Override
    protected void onStop() {
        if (firebaseAuth!=null && listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        init();


    }

    private void init() {
        ButterKnife.bind(this);
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference(Common.RIDER_INFO_REF);
        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );
        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth ->{
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user!=null){
                {
                    //update Token
                    FirebaseInstanceId.getInstance()
                            .getInstanceId()
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(SplashScreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }).addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            Log.d("TOKEN", instanceIdResult.getToken());
                            UserUtils.updateToken(SplashScreenActivity.this, instanceIdResult.getToken());

                        }
                    });
                    checkUserFromFirebase();
                }

            }else {
                showLoginLayout();
            }
        };

    }

    private void checkUserFromFirebase() {

        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
//                            Toast.makeText(SplashScreenActivity.this, "User Already Exist", Toast.LENGTH_SHORT).show();
                            RiderModel riderModel = snapshot.getValue(RiderModel.class);
                            gotoHomeActivity(riderModel);

                        }else {
                            showRegisterLayout();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        Toast.makeText(SplashScreenActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void gotoHomeActivity(RiderModel riderModel) {
        Common.currentUser = riderModel;
        startActivity(new Intent(SplashScreenActivity.this, RiderModel.class));
        finish();
    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.DialogTheme);
        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);

        TextInputEditText ed_first_name = itemView.findViewById(R.id.ed_first_name);
        TextInputEditText ed_last_name = itemView.findViewById(R.id.ed_last_name);
        TextInputEditText ed_phone_number = itemView.findViewById(R.id.edit_phone_number);

        Button btn_continue = itemView.findViewById(R.id.btnRegister);

        if (FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()!=null && !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
            ed_phone_number.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        builder.setView(itemView);
        AlertDialog dialog =  builder.create();
        dialog.show();

        btn_continue.setOnClickListener(view -> {
            if (TextUtils.isEmpty(ed_first_name.getText().toString())){
                ed_first_name.setError("First Name Required!");
            }
            else if (TextUtils.isEmpty(ed_last_name.getText().toString())){
                ed_last_name.setError("Last Name Required!");
            }
            else if (TextUtils.isEmpty(ed_first_name.getText().toString())){
                ed_phone_number.setError("Phone Number Required!");
            }
            else {
//                fName = ed_first_name.getText().toString();
//                lName = ed_last_name.getText().toString();
//                phone = ed_phone_number.getText().toString();

                RiderModel riderModel = new RiderModel();
                riderModel.setFirst_name(ed_first_name.getText().toString());
                riderModel.setLast_name(ed_last_name.getText().toString());
                riderModel.setPhone_number(ed_phone_number.getText().toString());
                //riderModel.setRatting(0.0);



                userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(riderModel)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.dismiss();
                                Toast.makeText(SplashScreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        dialog.dismiss();
                        Toast.makeText(SplashScreenActivity.this, "Register successfully....", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        gotoHomeActivity(riderModel);
                    }
                });
            }
        });

    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new  AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phn_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .build(),LOGIN_REQUEST_CODE);
    }
    private void delaySplashScreen() {
        progressBar.setVisibility(View.VISIBLE);
        Completable.timer(3, TimeUnit.SECONDS,
                AndroidSchedulers.mainThread())
                .subscribe(() ->
                        firebaseAuth.addAuthStateListener(listener));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOGIN_REQUEST_CODE){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }else {
                Toast.makeText(this, "[ERROR]: " + response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}