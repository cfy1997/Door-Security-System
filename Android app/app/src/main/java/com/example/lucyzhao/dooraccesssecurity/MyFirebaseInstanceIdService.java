package com.example.lucyzhao.dooraccesssecurity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by LucyZhao on 2017/3/20.
 */

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    private static final String TAG = MyFirebaseInstanceIdService.class.toString();
    private static final String SENDER_ID = "1071319875822";
    private AtomicInteger msgId = new AtomicInteger(0);
    private FirebaseAuth mAuth;

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        System.out.println(TAG+refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        setRegistrationID(refreshedToken);
    }

    private void storeRegistrationIDInPhone(String refreshedToken){
        Log.v(TAG, "storing registrationID in phone");
        String filename = StringUtils.registrationIDFileName;
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(refreshedToken.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setRegistrationID(String refreshedToken){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        if (mDatabase != null && user != null) {
            Log.v(TAG, "user is signed in, directly update firebase storage of registration id");
            sendRegistrationIDToDatabase(mDatabase, user, refreshedToken);
        }
        storeRegistrationIDInPhone(refreshedToken);
    }

    /**
     * requires: the user belongs to a family
     * note that this method is asynchronous, it does not
     * happen in sequential order
     * @param mDatabase
     * @param user
     * @param token
     */
    public void sendRegistrationIDToDatabase(final DatabaseReference mDatabase, final FirebaseUser user, final String token){
        Log.v(TAG, "sending registrationID to database");
        DatabaseReference userRef = mDatabase.child(StringUtils.FirebaseUserEndpoint)
                                            .child(user.getUid());

        userRef.child(StringUtils.FirebaseUserRegistrationID).setValue(token);
        userRef.child(StringUtils.FirebaseUserFamilyBelongsTo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String houseName = (String) dataSnapshot.getValue();
                        mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                                .child(houseName)
                                .child(StringUtils.FirebaseFamilyRegistrationIDs)
                                .child(user.getUid())
                                .setValue(token);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    /**
     * requires: registration ID is stored in internal storage
     * @return
     */
    public static String getRegistrationID(Context context){
        String savedInfo;
        String tempToken = "";
        try {
            FileInputStream fis = context.openFileInput(StringUtils.registrationIDFileName);
            BufferedReader bufferedReader
                    = new BufferedReader(new InputStreamReader(fis));
            StringBuffer stringBuffer = new StringBuffer();
            while((savedInfo = bufferedReader.readLine()) != null ) {
                //stringBuffer.append(savedInfo + "\n");
                stringBuffer.append(savedInfo);
            }
            tempToken = stringBuffer.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "registration ID got from internal storage is: " + tempToken);
        return tempToken;
    }

}
