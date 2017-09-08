package com.example.lucyzhao.dooraccesssecurity;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class InfoActivity extends AppCompatActivity {
    private static final String TAG = InfoActivity.class.toString();
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private DatabaseReference mDatabase;
    private DatabaseReference userRef;
    private DatabaseReference houseRef;

    private TextView lastLoginTime;
    private TextView workStart;
    private TextView workEnd;
    private TextView sleepStart;
    private TextView sleepEnd;
    private TextView lightStatus;
    private TextView sysStatus;
    private TextView numUsers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = mAuth.getCurrentUser();

        lastLoginTime = (TextView) findViewById(R.id.last_log_in_text);
        workEnd = (TextView) findViewById(R.id.work_end_text);
        workStart = (TextView) findViewById(R.id.work_start_text);
        sleepEnd = (TextView) findViewById(R.id.sleep_end_text);
        sleepStart = (TextView) findViewById(R.id.sleep_start_text);
        lightStatus = (TextView) findViewById(R.id.light_status_text);
        sysStatus = (TextView) findViewById(R.id.sys_status_text);
        numUsers = (TextView) findViewById(R.id.num_users_text);

        userRef = mDatabase.child(StringUtils.FirebaseUserEndpoint)
                                             .child(user.getUid());


        /*-------------configure toolbar-------------------------------*/

                userRef.child(StringUtils.FirebaseUserEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Toolbar myToolbar = (Toolbar) findViewById(R.id.info_toolbar);
                        setSupportActionBar(myToolbar);
                        /*---get user name----*/
                        String username = dataSnapshot.getValue(String.class);
                        getSupportActionBar().setTitle("signed in as: " + username);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.v(TAG, "get user name cancelled");
                    }
                });
        /*-------------------------------------*/

        userRef.child(StringUtils.FirebaseUserFamilyBelongsTo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String houseName = (String) dataSnapshot.getValue();
                        getHouseInfo(houseName);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


    }

    private void getHouseInfo(String houseName) {
        houseRef = mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                .child(houseName);
        houseRef.child(StringUtils.FirebaseFamilySystemInfo)
                .child(StringUtils.FirebaseFamilyLastLoginTime)
                .addValueEventListener(new MyValueEventListener(lastLoginTime));
        houseRef.child(StringUtils.FirebaseFamilySystemInfo)
                .child(StringUtils.FirebaseFamilySleepEnd)
                .addValueEventListener(new MyValueEventListener(sleepEnd));
        houseRef.child(StringUtils.FirebaseFamilySystemInfo)
                .child(StringUtils.FirebaseFamilySleepStart)
                .addValueEventListener(new MyValueEventListener(sleepStart));
        houseRef.child(StringUtils.FirebaseFamilySystemInfo)
                .child(StringUtils.FirebaseFamilyWorkEnd)
                .addValueEventListener(new MyValueEventListener(workEnd));
        houseRef.child(StringUtils.FirebaseFamilySystemInfo)
                .child(StringUtils.FirebaseFamilyWorkStart)
                .addValueEventListener(new MyValueEventListener(workStart));
        houseRef.child(StringUtils.FirebaseFamilyStatus)
                .child(StringUtils.FirebaseFamilyLED)
                .addValueEventListener(new MyValueEventListener(lightStatus));
        houseRef.child(StringUtils.FirebaseFamilyStatus)
                .child(StringUtils.FirebaseFamilySysOnOff)
                .addValueEventListener(new MyValueEventListener(sysStatus));
        houseRef.child(StringUtils.FirebaseFamilyNumUsers)
                .addValueEventListener(new MyLongValueEventListener(numUsers));
    }

    private class MyValueEventListener implements ValueEventListener {
        TextView textView;

        public MyValueEventListener(TextView textView){
            this.textView = textView;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.exists()){
                String value = dataSnapshot.getValue(String.class);
                textView.setText(value);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.v(TAG, "cancelled");
        }
    }

    private class MyLongValueEventListener implements ValueEventListener {
        TextView textView;

        public MyLongValueEventListener(TextView textView){
            this.textView = textView;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.exists()){
                long value = dataSnapshot.getValue(Long.class);
                String s = Long.toString(value);
                textView.setText(s);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.v(TAG, "cancelled");
        }
    }

}
