package com.example.lucyzhao.dooraccesssecurity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = SignUpActivity.class.toString();
    private String houseName;
    private String housePassword;
    private EditText houseNameEditText;
    private EditText housePasswordEditText;
    private EditText houseConfirmEditText;

    private TextView user_creation_hint;
    private TextView house_creation_hint;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private boolean createHouse = false;
    private String registrationID;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        user_creation_hint = (TextView) findViewById(R.id.sign_up_hint);
        house_creation_hint = (TextView) findViewById(R.id.house_creation_hint);

        houseNameEditText = (EditText) findViewById(R.id.house_name);
        housePasswordEditText = (EditText) findViewById(R.id.house_password);
        houseConfirmEditText = (EditText) findViewById(R.id.confirm_house_password);
        houseConfirmEditText.setVisibility(View.INVISIBLE);

        registrationID = MyFirebaseInstanceIdService.getRegistrationID(getApplicationContext());

        progressDialog = new ProgressDialog(this);
    }


    /**
     * Called when user clicks on the button "create house"
     * sets relevant parameters that indicate the user
     * wants to create a new houes rather than joining
     * an existing house
     * @param view
     */
    public void createHouse(View view){
        createHouse = true;
        String s = "House name should only be composed of capital letters. House password should be 6 digits";
        house_creation_hint.setText(s);
        houseNameEditText.setHint("Enter a house name");
        housePasswordEditText.setHint("Set a password");
        houseConfirmEditText.setVisibility(View.VISIBLE);
    }

    /**
     * Check whether user's input is valid and set appropriate
     * hints
     *
     * @param account either the house name or user's email
     * @param password for either the house or the user's own account
     * @param confirm confirm password
     * @param house whether this check if for house information or
     *              user information
     * @return false if any input is an empty string or password length
     * is less than 6 characters or confirm and password are not equal
     */
    private boolean inputCheck(String account, String password, String confirm, boolean house){
        TextView creation_hint;
        if(house){
            creation_hint = house_creation_hint;
            if(!StringUtils.isAlpha(account)){
                Log.v(TAG, "input check: not all characters are capital letters" );
                String s = "House name should only consist of capital letters";
                creation_hint.setText(s);
                return false;
            }
            else if( password.length() < 6){
                String s = "password should be exactly 6 digits";
                creation_hint.setText(s);
                return false;
            }
        }
        else creation_hint = user_creation_hint;

        if(account.equals("") || password.equals("") || confirm.equals("") ){
            creation_hint.setText(R.string.fields_empty);
            return false;
        }
        else if( password.length() < 6){
            creation_hint.setText(R.string.password_too_short);
            return false;
        }
        else if( !confirm.equals(password) ){
            creation_hint.setText(R.string.unequal_password);
            return false;
        }
        else return true;
    }


    public void signUp(View view){
        EditText emailEditText = (EditText) findViewById(R.id.user_email_edit_text);
        EditText passwordEditText = (EditText) findViewById(R.id.user_password_edit_text);
        EditText userConfirmEditText = (EditText) findViewById(R.id.confirm_user_password);

        String userEmail = emailEditText.getText().toString();
        String userPassword = passwordEditText.getText().toString();
        String userConfirm = userConfirmEditText.getText().toString();

        Log.v(TAG, "user password "+ userPassword);
        Log.v(TAG, "user confirm " + userConfirm);

        houseName = houseNameEditText.getText().toString();
        housePassword = housePasswordEditText.getText().toString();

        FirebaseUser user = mAuth.getCurrentUser();
        //if account has already been created, but house creation failed
        if( user_creation_hint.getText().toString().equals("Account successfully created") ){
            addUserToHouse(user);
        }
        else if( !inputCheck(userEmail,userPassword,userConfirm,false) ){
            //user_creation_hint.setText(R.string.unequal_password);
            Log.v(TAG, "input check for user failed");
        }
        //if user input seems fine (does not check for Firebase related info
        else {
            mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                            if (!task.isSuccessful()) {
                                Log.i(TAG, "Authentication failed.");

                                user_creation_hint.setText(R.string.user_creation_failed);
                            } else {
                                user_creation_hint.setText(R.string.user_creation_success);

                                FirebaseUser user = mAuth.getCurrentUser();
                                registerUserInDatabase(user);
                                addUserToHouse(user);
                            }
                        }
                    });
        }
    }

    /**
     * Store the current logged in user's information in firebase
     * @param user
     */
    private void registerUserInDatabase(FirebaseUser user){

        if (mDatabase != null && user != null) {
            User cur_user = new User(user.getUid(), user.getEmail());
            cur_user.setRegistrationID(registrationID);
            mDatabase.child(StringUtils.FirebaseUserEndpoint).child(user.getUid()).setValue(cur_user);
        }
    }

    /**
     * add the user's registration id to his/her house
     * @param houseRef
     * @param user
     */
    private void addRegistrationIDToHouse(DatabaseReference houseRef, FirebaseUser user){
        houseRef.child(StringUtils.FirebaseFamilyRegistrationIDs)
                .child(user.getUid())
                .setValue(registrationID);
    }

    private void updateNumUsers(final DatabaseReference houseRef){
        houseRef.child(StringUtils.FirebaseFamilyNumUsers)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        long numUsers = dataSnapshot.getValue(Long.class);
                        houseRef.child(StringUtils.FirebaseFamilyNumUsers)
                                .setValue(numUsers + 1);
                        Log.v(TAG, "incremented num users from" + numUsers);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    /**
     * tries to add a user to a house
     * the house is either non-existent or has already been created
     * called when firebase has confirmed that a valid email and
     * password has been entered
     * @param user
     */
    private void addUserToHouse(final FirebaseUser user) {
        Log.v(TAG, "in addUserToHouse");
        final DatabaseReference houseRef = mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                .child(houseName);
        Log.v(TAG, "house name is " + houseName);

        houseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                /*---------------------house exists-----------------*/
                if (dataSnapshot.exists() && !houseName.equals("")) {
                    Log.v(TAG, "house exists");
                    // user wants to create a house and house exists
                    if (createHouse) {
                        Log.v(TAG, "user wants to create a house but house exists");
                        house_creation_hint.setText(R.string.house_exists);
                    }
                    // user wants to join a house and house exists (good)
                    else {
                        Log.v(TAG, "user wants to join an existing house");
                        String housePasswordRecord = (String) dataSnapshot.child(StringUtils.FirebaseFamilyPassword).getValue();

                        if (!StringUtils.joiningHouseInputCheck(housePassword, housePasswordRecord, houseName, house_creation_hint)) {
                            Log.v(TAG, "joining house input check failed");
                        }
                        //password correct
                        else {
                            Log.v(TAG, "password correct");
                            //update the user's houseName field
                            mDatabase.child(StringUtils.FirebaseUserEndpoint)
                                    .child(user.getUid())
                                    .child(StringUtils.FirebaseUserFamilyBelongsTo)
                                    .setValue(houseName);

                            addRegistrationIDToHouse(houseRef, user);
                            updateNumUsers(houseRef);
                            Log.v(TAG, "updated number of users when user add to a house");
                            startMainActivityIntent();
                            finish();
                        }
                    }
                }
                /*-------------house does not exist-----------------*/
                else {
                    Log.v(TAG, "house doesn't exist");
                    if (createHouse) {
                        Log.v(TAG, "user wants to create a house");
                        String confirmPassword = houseConfirmEditText.getText().toString();

                        if (!inputCheck(houseName, housePassword, confirmPassword, true)) {
                            Log.v(TAG, "input check failed for house creation");
                        }
                        //user input seems correct
                        else {
                            progressDialog.setMessage("signing up ...");
                            progressDialog.setCancelable(false);
                            progressDialog.show();
                            //set the house's password and pictureArrived field
                            //set the house's all fields
                            initFirebaseFamilyFields(houseRef);

                            //update the user's houseName field
                            mDatabase.child(StringUtils.FirebaseUserEndpoint)
                                    .child(user.getUid())
                                    .child(StringUtils.FirebaseUserFamilyBelongsTo)
                                    .setValue(houseName);

                            addRegistrationIDToHouse(houseRef, user);
                            startMainActivityIntent();
                            progressDialog.dismiss();
                        }
                    }
                    //the user wants to join a house that does not exist
                    else {
                        Log.v(TAG, "user wants to join a house that does not exist");
                        house_creation_hint.setText(R.string.house_not_exist);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });
    }


    private void initFirebaseFamilyFields(DatabaseReference houseRef){

        /*---------------individual fields----------------------*/
        houseRef.child(StringUtils.FirebaseFamilyPassword).setValue(housePassword);
        houseRef.child(StringUtils.FirebaseFamilyLastPhotoID).setValue(0);

        houseRef.child(StringUtils.FirebaseFamilyDeletePhotoNumber)
                .setValue(0);
        houseRef.child(StringUtils.FirebaseFamilyEncodeCmd)
                .setValue(0);
        houseRef.child(StringUtils.FirebaseFamilyUrgent)
                .setValue(StringUtils.off);
        houseRef.child(StringUtils.FirebaseFamilyTakeInstantPhoto)
                .setValue(StringUtils.FALSE);
        houseRef.child(StringUtils.FirebaseFamilyNumUsers)
                .setValue(1);

        /*------------------------info field--------------------*/
        DatabaseReference sysRef = houseRef.child(StringUtils.FirebaseFamilySystemInfo);
        SignInActivity.updateLastLoginTime(houseRef);
        sysRef
                .child(StringUtils.FirebaseFamilySleepEnd)
                .setValue("6:00");
        sysRef.child(StringUtils.FirebaseFamilySleepStart)
                .setValue("23:00");
        sysRef.child(StringUtils.FirebaseFamilyWorkEnd)
                .setValue("16:00");
        sysRef.child(StringUtils.FirebaseFamilyWorkStart)
                .setValue("9:00");

        /*--------------------status field------------*/
        houseRef.child(StringUtils.FirebaseFamilyStatus)
                .child(StringUtils.FirebaseFamilyBuzzer)
                .setValue(StringUtils.OFF);
        houseRef.child(StringUtils.FirebaseFamilyStatus)
                .child(StringUtils.FirebaseFamilySysOnOff)
                .setValue(StringUtils.OFF);
        houseRef.child(StringUtils.FirebaseFamilyStatus)
                .child(StringUtils.FirebaseFamilyLED)
                .child(StringUtils.OFF);
    }

    /**
     * Helper method that alerts MainActivity that this
     * is the first time a user has entered the activity
     * and should probably not load a picture
     * Starts the MainActivity
     */
    private void startMainActivityIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("firstAttach", true);
        startActivity(intent);
    }

}
