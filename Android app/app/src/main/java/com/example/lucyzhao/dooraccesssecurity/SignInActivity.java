package com.example.lucyzhao.dooraccesssecurity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = SignInActivity.class.toString();
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private TextView signInHint;

    private void startMainActivityIntent(boolean firstAttach) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("firstAttach", firstAttach);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        progressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        signInHint = (TextView) findViewById(R.id.sign_in_hint);
    }

    @Override
    protected void onStart(){
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            //user is signed in
            Log.v(TAG, "user is signed in");
            //it's not the first time that the user signs in
            startMainActivityIntent(false);
            //TODO we might not need this for the code to work
        }
    }

    /**
     * checks whether the user's input for account and password
     * are valid
     *
     * @param account user's email
     * @param password user's password for this app
     * @return false if any field is an empty string or password
     * length is less than 6 characters
     */
    private boolean inputCheck(String account, String password){
        TextView hint = signInHint;
        if(account.equals("") || password.equals("")){
            hint.setText(R.string.fields_empty);
            return false;
        }
        else if( password.length() < 6){
            hint.setText(R.string.password_too_short);
            return false;
        }
        else return true;
    }


    public void signIn(View view) {
        Log.v(TAG, "signIn button clicked");

        EditText emailEditText = (EditText) findViewById(R.id.email_edit_text);
        EditText passwordEditText = (EditText) findViewById(R.id.password_edit_text);

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if(inputCheck(email,password)) {
            progressDialog.setMessage("logging in ...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                    System.out.println(TAG + "sign in with email complete");
                    if (!task.isSuccessful()) {
                        progressDialog.dismiss();

                        Log.w(TAG, "signInWithCredential", task.getException());
                        signInHint.setText(R.string.sign_in_failed);
                    } else {
                        //if sign in is successful, store the user's
                        //registration id to Firebase
                        sendRegistrationIdToFirebase();
                    }
                }

            });
        }
    }

    /**
     * Starts the MainActivity after updating registration id in firebase
     * This method is necessary if the user wants to login to the app
     * using someone else's phone. It ensures that push notifications are
     * always sent to correct devices
     */
    private void sendRegistrationIdToFirebase(){
        final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser user = mAuth.getCurrentUser();
        final String token = MyFirebaseInstanceIdService.getRegistrationID(getApplicationContext());

        final DatabaseReference userRef = mDatabase.child(StringUtils.FirebaseUserEndpoint)
                .child(user.getUid());

        userRef.child(StringUtils.FirebaseUserRegistrationID).setValue(token);
        userRef.child(StringUtils.FirebaseUserFamilyBelongsTo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //get user's house name
                        String houseName = (String) dataSnapshot.getValue();
                        //update the registration id field in the user's house
                        DatabaseReference houseRef
                                = mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                                .child(houseName);
                        houseRef
                                .child(StringUtils.FirebaseFamilyRegistrationIDs)
                                .child(user.getUid())
                                .setValue(token);

                        updateLastLoginTime(houseRef);

                        startMainActivityIntent(true);
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        progressDialog.dismiss();
                    }
                });
    }

    public static void updateLastLoginTime(DatabaseReference houseRef){
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => "+c.getTime());

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String formattedDate = df.format(c.getTime());
        Log.v(TAG, "current time is " + formattedDate);

        houseRef.child(StringUtils.FirebaseFamilySystemInfo)
                .child(StringUtils.FirebaseFamilyLastLoginTime)
                .setValue(formattedDate);
    }

    public void signUp(View view){
        //go to SignUp activity
        Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(intent);
    }
}



