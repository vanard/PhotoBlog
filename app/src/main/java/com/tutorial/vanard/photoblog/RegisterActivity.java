package com.tutorial.vanard.photoblog;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText regEmail_field, regPass_field, regConfirm_field;
    private Button regBtn, regLoginBtn;
    private ProgressBar regProgress;

    FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        regEmail_field = findViewById(R.id.reg_email);
        regPass_field = findViewById(R.id.reg_pass);
        regConfirm_field = findViewById(R.id.reg_confirm_pass);
        regBtn = findViewById(R.id.reg_btn);
        regLoginBtn = findViewById(R.id.reg_login_btn);
        regProgress = findViewById(R.id.reg_progress);

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = regEmail_field.getText().toString();
                String pass = regPass_field.getText().toString();
                String confirm_pass = regConfirm_field.getText().toString();

                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass) && !TextUtils.isEmpty(confirm_pass)){
                    if (pass.equals(confirm_pass)){
                        regProgress.setVisibility(View.VISIBLE);
                        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    Intent i = new Intent(RegisterActivity.this, SetupActivity.class);
                                    startActivity(i);
                                    finish();
                                }else{
                                    String e = task.getException().getMessage();
                                    toastMessage("Error : " + e);
                                }
                                regProgress.setVisibility(View.INVISIBLE);
                            }
                        });
                    }else{
                        toastMessage("Confirm password and Password doesn't match");
                    }
                }else{
                    toastMessage("You must fill the form first");
                }
            }
        });


        regLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null){
            sendToMain();
        }
    }

    private void sendToMain(){
        Intent i = new Intent (RegisterActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }

    private void toastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
