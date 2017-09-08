package com.example.lucyzhao.dooraccesssecurity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.toString();
    /*-------------Firebase fields--------------*/
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    private ValueEventListener photoIdListener;
    private ValueEventListener buzzerListener;

    /*-------------UI views-------------------*/
    private Button callButton;
    private Button buzzerButton;
    private ImageView buzzerImage;
    private LinearLayout imageLinearLayout;
    private ImageDialogFragment imageDialogFragment;

    /*----------constants--------------------*/
    private static float DENSITY;
    private static final int MY_PERMISSIONS_REQUEST_CALL = 1010;
    private static final int MY_PERMISSION_REQUEST_EXT_STORAGE = 2;
    private static final String IMAGE_TYPE = ".png";

    //flag that indicates photoIdListener is first attached
    private boolean firstAttach;
    //image loaded from Firebase storage
    //private Bitmap bitmap;
    //this int keeps track of the image fragments
    private int image_frag_id = 0;
    //the name of the house the logged in user currently belongs to
    //should be assigned a value in onCreate or shortly after
    private String houseName ="";


    /*----------------------------CALL BACKS-----------------------*/
    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "in onStart");
        Log.v(TAG, "auth state listener added");
        mAuth.addAuthStateListener(mAuthListener);
        Log.v(TAG, "in onStart firstAttach value is " + firstAttach);

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "in onStop");
        if (mAuthListener != null) {
            Log.v(TAG, "auth state listener removed");
            mAuth.removeAuthStateListener(mAuthListener);
        }

    }

    /**
     * Inflates the layout for toolbar menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "in onCreate");

        /*------------get the intent that started this activity-----------*/
        Intent intent = getIntent();
        firstAttach = intent.getBooleanExtra("firstAttach", true);
        Log.v(TAG, "in onCreate firstAttach value is " + firstAttach);


        /*-----------find the views needed in MainActivity-----------------*/
        ScrollView imageScrollView = (ScrollView) findViewById(R.id.image_scroll_view);
        imageScrollView.setBackgroundResource(R.drawable.dotted_shape);
        imageLinearLayout = (LinearLayout) findViewById(R.id.image_linear_layout);
        callButton = (Button) findViewById(R.id.call_button);
        buzzerButton = (Button) findViewById(R.id.alarm_button);
        buzzerImage = (ImageView) findViewById(R.id.bell_image);

        //get the phone screen density
        DENSITY = getApplicationContext().getResources().getDisplayMetrics().density;

        /*-------------------Firebase fields------------------------*/
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out, go to SignInActivity
                    Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                    startActivity(intent);
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        photoIdListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.v(TAG, "lastPhotoID data changed to " + dataSnapshot.getValue().toString());
                  if(firstAttach){
                      //since onDataChange will be called every time the listener is attached
                      //the program will try to load a picture even if there is no change to
                      //lastPhotoID field
                      Log.v(TAG, "photo id listener is first attached, photo might not have changed");
                      firstAttach = false;
                  }
                  else {
                      Log.v(TAG, "photo id listener is not first attached");
                      long lastPhotoID = (long) dataSnapshot.getValue();
                      loadPicture(lastPhotoID);
                  }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        buzzerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.v(TAG, "buzzer status changed to " + dataSnapshot.getValue());
                if (dataSnapshot.getValue().equals(StringUtils.ON)) {
                    buzzerButton.setText(R.string.buzzer_ringing);
                    buzzerButton.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.buzzerColor));
                    buzzerImage.setImageResource(R.drawable.orange_bell);
                } else {
                    buzzerButton.setText(R.string.sound_alarm);
                    buzzerButton.setTextColor(Color.WHITE);
                    buzzerImage.setImageResource(R.drawable.bell);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = mAuth.getCurrentUser();
        if (mDatabase != null && user != null) {
           /* get the user's house name */
            mDatabase.child(StringUtils.FirebaseUserEndpoint)
                    .child(user.getUid())
                    .child(StringUtils.FirebaseUserFamilyBelongsTo)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            houseName = (String) dataSnapshot.getValue();
                            Log.v(TAG, "house name is" + houseName);
                            /* read last photo id and buzzer state whenever they change in
                               database; attach a listener to these two objects in this step
                             */
                            //the listener should not be attached here
                            readLastPhotoId(houseName);
                            readBuzzerState(houseName);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        /*-------------configure toolbar-------------------------------*/
        mDatabase.child(StringUtils.FirebaseUserEndpoint)
                .child(user.getUid())
                .child(StringUtils.FirebaseUserEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
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


        //buttons are initially not clickable; set them to clickable after image has
        //arrived
        setButtonsClickable(false);

    }

    /**
     * user should not sign out when he/she destroys the app
     */
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    /*----------------------------------------------------*/


    /*-------------LISTENER MANAGEMENT-------------------*/
    /**
     * requires: the familyName passed exists and is the user's
     * current house name
     * attach a photoIdListener to the corresponding house's
     * lastPhotoID field
     *
     * @param familyName
     */
    private void readLastPhotoId(String familyName) {
        //user might not join a house so familyName might be empty string
        firstAttach = true;
        Log.v(TAG, "attaching read last photo id listener, firstAttach is" + firstAttach);
        if(!familyName.equals("") && photoIdListener != null) {
            mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                    .child(familyName)
                    .child(StringUtils.FirebaseFamilyLastPhotoID)
                    .addValueEventListener(photoIdListener);
        }
    }

    /**
     * remove the photoIdListener from the house
     * called when user is no longer registered in
     * this house
     * @param houseName
     */
    private void removePhotoIdListener(String houseName){
        if(!houseName.equals("") && photoIdListener != null){
            mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                    .child(houseName)
                    .child(StringUtils.FirebaseFamilyLastPhotoID)
                    .removeEventListener(photoIdListener);
        }
    }

    /**
     * attach a buzzer listener to the house's buzzer field
     * if the user is registered in a house
     * @param houseName
     */
    private void readBuzzerState(String houseName){
        if(!houseName.equals("") && buzzerListener != null) {
            mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                    .child(houseName)
                    .child(StringUtils.FirebaseFamilyStatus)
                    .child(StringUtils.FirebaseFamilyBuzzer)
                    .addValueEventListener(buzzerListener);
        }
    }

    /**
     * remove the buzzer listener attach to the user's house
     * field
     * @param houseName
     */
    private void removeBuzzerListener(String houseName){
        if(!houseName.equals("") && buzzerListener != null) {
            mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                    .child(houseName)
                    .child(StringUtils.FirebaseFamilyStatus)
                    .child(StringUtils.FirebaseFamilyBuzzer)
                    .removeEventListener(buzzerListener);
        }
    }

    /*----------------------------------------------------*/

    /**
     * controls whether buttons on UI are clickable or not
     * @param clickable
     */
    private void setButtonsClickable(boolean clickable){
        if(!clickable){
            buzzerButton.getBackground().setColorFilter(new LightingColorFilter(0xff888888, 0x000000));
            callButton.getBackground().setColorFilter(new LightingColorFilter(0xff888888, 0x000000));
        }
        else {
            buzzerButton.getBackground().clearColorFilter();
            callButton.getBackground().clearColorFilter();
        }
        callButton.setClickable(clickable);
        buzzerButton.setClickable(clickable);
    }

    /**
     * hide or show all buttons
     * @param visible
     */
    private void hideButtons(int visible){
        buzzerButton.setVisibility(visible);
        callButton.setVisibility(visible);
    }

    /*--------------IMAGE MANAGEMENT----------------------*/

    /**
     * remove all ImageViews from the UI's ScrollView
     * @param menuItem
     */
    public void clearAllPictures(MenuItem menuItem) {
        if (imageLinearLayout.getChildCount() > 0)
            imageLinearLayout.removeAllViews();
    }


    /**
     * Obtain the photo named "lastPhotoID.png" from FirebaseStorage
     * and display it on the top of the ScrollView
     * @param lastPhotoID the name (and index) of the image
     */
    private void loadPicture(final long lastPhotoID) {
        setButtonsClickable(true); //enable button click

        Log.v(TAG, "in load picture");
        try {
            //create a temporary file in internal storage
            final File localFile = File.createTempFile("images", "jpg");
            //the default path for storing an image is "housename/images/1.png"
            String firebaseImagePath = houseName + "/" + "images/" + lastPhotoID + IMAGE_TYPE;
            Log.v(TAG, "image got from " + firebaseImagePath);

            //obtain file from FirebaseStorage
            mStorageRef.child(firebaseImagePath).getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            // Successfully downloaded data to local file
                            Log.v(TAG, "local file successfully downloaded" + localFile.getPath());
                            ImageView image = new ImageView(MainActivity.this);
                            image.setLayoutParams(setImageLayoutParams());
                            final Bitmap bitmap = BitmapFactory.decodeFile(localFile.getPath());
                            image.setImageBitmap(bitmap);
                            final int imageId = (int) lastPhotoID;
                            Log.v(TAG, "new image's id is " + imageId);
                            image.setId(imageId);
                            image.setClickable(true);

                            //when the user long clicks the image, a dialog will pop up
                            //asking the user to save or delete this image
                            image.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    imageDialogFragment = ImageDialogFragment.newInstance(imageId, bitmap);
                                    imageDialogFragment.show(getSupportFragmentManager(),"imageDialogFragment");
                                    return true;
                                }
                            });

                            //when the user clicks on the image, a fragment will show up
                            //and display a larger view of the image
                            image.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ImageFragment fragment = ImageFragment.newInstance(imageId,bitmap);
                                    createFragment(fragment);
                                    hideButtons(View.INVISIBLE);
                                }
                            });
                            imageLinearLayout.addView(image,0);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle failed download
                    Toast.makeText(getApplicationContext(), "download failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * helper method that creates a set of layout parameters
     * that could be used by an ImageView to specify exactly
     * how it looks on the UI
     * @return
     */
    private LinearLayout.LayoutParams setImageLayoutParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
        lp.height = (int) (200 * DENSITY);
        lp.topMargin = (int) (20 * DENSITY);
        return lp;
    }

    /*----------------------------------------------------*/


   /*----------TOOLBAR ITEMS and BUTTONS-------------------------*/

    public void showInfo(MenuItem menuItem){
        Intent intent = new Intent(MainActivity.this, InfoActivity.class);
        startActivity(intent);
    }

    public void signOut(MenuItem menuItem) {
        Log.v(TAG, "sign out clicked");
        signOutDeleteRegID();
    }

    /**
     * Deletes registrationID every time the user explicitly signs out
     */
    private void signOutDeleteRegID(){
        final FirebaseUser user = mAuth.getCurrentUser();
        Log.v(TAG, "signing out-----house name is " + houseName);
        //the user should belong to a house
        //TODO user cannot sign out if doesn't have a house
        if( mDatabase != null && user != null && !houseName.equals("")) {
            DatabaseReference userRef = mDatabase.child(StringUtils.FirebaseUserEndpoint)
                                                 .child(user.getUid());
            Log.v(TAG, "setting user registration id to null");
            userRef.child(StringUtils.FirebaseUserRegistrationID).setValue(null);

            mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                    .child(houseName)
                    .child(StringUtils.FirebaseFamilyRegistrationIDs)
                    .child(user.getUid())
                    .setValue(null);
            Log.v(TAG, "removed user's registration id from user's house");
            mAuth.signOut();
        }
    }


    public void switchHouse(MenuItem menuItem) {
        Log.v(TAG, "switching to another house");
        SwitchHouseDialogFragment frag = new SwitchHouseDialogFragment();
        frag.show(getSupportFragmentManager(), "switch_house_frag");
    }

    public void call(View view) {
        Log.v(TAG, "calling");
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse(StringUtils.TargetPhoneNumber));
        //start calling
        if(requestUserPermission(Manifest.permission.CALL_PHONE,MY_PERMISSIONS_REQUEST_CALL)) {
            startActivity(callIntent);
            Log.v(TAG, "request granted");
        }
    }


    public void soundAlarm(View view){
        if( !houseName.equals("") ) {
            mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                                .child(houseName)
                                .child(StringUtils.FirebaseFamilyEncodeCmd)
                                .setValue(4);

            DatabaseReference buzzerRef
                    = mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                                .child(houseName)
                                .child(StringUtils.FirebaseFamilyStatus)
                                .child(StringUtils.FirebaseFamilyBuzzer);
            if(buzzerButton.getText().toString().equals(getString(R.string.sound_alarm))) {
                buzzerRef.setValue(StringUtils.ON);
            }
            else {
                buzzerRef.setValue(StringUtils.OFF);
            }
        }
    }

    /*--------------------------------------------------------*/

    /*---------------REQUEST FOR PERMISSIONS------------------*/

    private boolean requestUserPermission(String devicePermission, int requestCode){
        Log.v("tag","entering requesting user permission " + devicePermission);

        if (ActivityCompat.checkSelfPermission(this, devicePermission) != PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "not permitted");

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{devicePermission}, requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CALL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse(StringUtils.TargetPhoneNumber));
                    try {
                        startActivity(callIntent);
                    } catch (SecurityException e) {
                        Log.v(TAG, "security exception");
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Cannot use phone call", Toast.LENGTH_SHORT).show();
                    // permission denied. Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
            case MY_PERMISSION_REQUEST_EXT_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if(imageDialogFragment.bitmap != null ) {
                        Bitmap bitmap = imageDialogFragment.bitmap;
                        //imageDialogFragment.dismiss();
                        Log.v(TAG, "permission granted, saving image to phone");
                        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "incomingImage", "incomingImage");
                    }
                    else Log.v(TAG, "bitmap is null");

                } else {
                    Toast.makeText(getApplicationContext(), "Cannot use gallery", Toast.LENGTH_SHORT).show();
                    // permission denied. Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
        }
    }


    /*------------------------FRAGMENTS--------------------------------*/

    /**
     * create a fragment
     * @param fragment the fragment to be created
     */
    private void createFragment(Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack if needed
        transaction.addToBackStack(Integer.toString(image_frag_id));
        image_frag_id++;
        transaction.replace(R.id.activity_main, fragment);
        transaction.commit();
    }

    /**
     * A fragment that prompts the user to switch to a different house
     */
    public static class SwitchHouseDialogFragment extends DialogFragment {

        private void updateNumUsers(final DatabaseReference houseRef, final boolean increment){
            houseRef.child(StringUtils.FirebaseFamilyNumUsers)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            long numUsers = dataSnapshot.getValue(Long.class);
                            if(increment)
                                houseRef.child(StringUtils.FirebaseFamilyNumUsers)
                                    .setValue(numUsers + 1);
                            else if (numUsers > 0 )
                                houseRef.child(StringUtils.FirebaseFamilyNumUsers)
                                    .setValue(numUsers - 1);
                            Log.v(TAG, "updated num users from" + numUsers);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            final View fragmentView = inflater.inflate(R.layout.switch_house_dialog_fragment, null);
            builder.setView(fragmentView);

            final FirebaseAuth mAuth = ((MainActivity)getActivity()).mAuth;
            final DatabaseReference mDatabase = ((MainActivity)getActivity()).mDatabase;
            final String houseName = ((MainActivity)getActivity()).houseName;

            final TextView switchHouseHint = (TextView) fragmentView.findViewById(R.id.switch_house_hint);
            switchHouseHint.setText(getString(R.string.current_house_hint, houseName));

            final EditText houseNameEditText = (EditText) fragmentView.findViewById(R.id.switch_house_name);
            final EditText passwordEditText = (EditText) fragmentView.findViewById(R.id.switch_house_password);


            Button okButton = (Button) fragmentView.findViewById(R.id.ok_button);
            Button cancelButton = (Button) fragmentView.findViewById(R.id.cancel_button);

            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(TAG, "ok button clicked");
                    final String targetHouseName = houseNameEditText.getText().toString();

                    switchHouseHint.setText(getString(R.string.switching_to_house, targetHouseName));
                    final String password = passwordEditText.getText().toString();

                    final FirebaseUser user = mAuth.getCurrentUser();
                    Log.v(TAG, "getting user");
                    if(user != null && mDatabase != null) {
                        Log.v(TAG, "user and mdatabase not null");
                        final DatabaseReference houseRef = mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                                                              .child(targetHouseName);
                        houseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Log.v(TAG, "in ondata change");
                                //this house exists
                                if(dataSnapshot.exists()) {
                                    Log.v(TAG, "datasnapshot exists");
                                    String housePasswordRecord = (String) dataSnapshot.child(StringUtils.FirebaseFamilyPassword).getValue();
                                    if(StringUtils.joiningHouseInputCheck(password,housePasswordRecord,targetHouseName,switchHouseHint)) {
                                        //delete the user's registration id from the original family
                                        //so that he/she no longer receives notifications from the
                                        //original family
                                        mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                                                .child(houseName)
                                                .child(StringUtils.FirebaseFamilyRegistrationIDs)
                                                .child(user.getUid())
                                                .setValue(null);
                                        //decrement the original family's user count
                                        DatabaseReference oldHouseRef
                                                = mDatabase.child(StringUtils.FirebaseFamilyEndpoint)
                                                           .child(houseName);
                                        updateNumUsers(oldHouseRef, false);
                                        Log.v(TAG, "updated old house user numbers");

                                        //set the user's registration id to the new family
                                        houseRef
                                                .child(StringUtils.FirebaseFamilyRegistrationIDs)
                                                .child(user.getUid())
                                                .setValue(MyFirebaseInstanceIdService.getRegistrationID(getContext()));
                                        //update new house last login time
                                        SignInActivity.updateLastLoginTime(houseRef);
                                        //change the user's family to the target family
                                        mDatabase.child(StringUtils.FirebaseUserEndpoint)
                                                .child(user.getUid())
                                                .child(StringUtils.FirebaseUserFamilyBelongsTo)
                                                .setValue(targetHouseName);

                                        //increment the new family's user count
                                        updateNumUsers(houseRef, true);
                                        Log.v(TAG, "updated user numbers");
                                        //attach the state listeners to the new house
                                        ((MainActivity)getActivity()).removeBuzzerListener(houseName);
                                        ((MainActivity)getActivity()).removePhotoIdListener(houseName);
                                        ((MainActivity)getActivity()).readBuzzerState(targetHouseName);
                                        ((MainActivity)getActivity()).readLastPhotoId(targetHouseName);

                                        //update the class's record of current houseName;
                                        ((MainActivity)getActivity()).houseName = targetHouseName;
                                        dismiss();
                                    }
                                }
                                //target house does not exist
                                else{
                                    switchHouseHint.setText(R.string.joining_house_failed);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                String s = "Database error, operation cancelled";
                                switchHouseHint.setText(s);
                            }
                        });

                    }
                    else {
                        String s = "Database error, operation cancelled";
                        Log.v(TAG, "mdatabase or user is null");
                        switchHouseHint.setText(s);
                    }
                }
            });
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(TAG, "cancel button clicked");
                    dismiss();
                }
            });

            fragmentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "clicked on fragment, dismissing dialog");
                    dismiss();
                }
            });
            return builder.create();
        }

    }

    /**
     * a fragment that prompts the user to save a photo to gallery
     * or deletes a photo from the ScrollView
     */
    public static class ImageDialogFragment extends DialogFragment {
        Bitmap bitmap;

        public static ImageDialogFragment newInstance(int imageId, Bitmap bitmap) {
            ImageDialogFragment frag = new ImageDialogFragment();
            Bundle args = new Bundle();
            args.putParcelable("bitmap", bitmap);
            args.putInt("imageId", imageId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int imageId = getArguments().getInt("imageId");
            final Bitmap bitmap = getArguments().getParcelable("bitmap");
            this.bitmap = bitmap;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            final View fragmentView = inflater.inflate(R.layout.image_action_dialog_fragment, null);
            builder.setView(fragmentView);

            Button saveButton = (Button) fragmentView.findViewById(R.id.save);
            Button deleteButton = (Button) fragmentView.findViewById(R.id.delete);

            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(TAG, "save button clicked");
                    saveImage(bitmap);
                    dismiss();
                    Toast.makeText(getContext(),"image saved to gallery",Toast.LENGTH_SHORT).show();
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.v(TAG, "delete button clicked");
                    ImageView imageView = (ImageView) getActivity().findViewById(imageId);
                    ((MainActivity) getActivity()).imageLinearLayout.removeView(imageView);
                    dismiss();
                }
            });

            fragmentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "clicked on fragment, dismissing dialog");
                    dismiss();
                }
            });
            return builder.create();
        }

        private void saveImage(Bitmap bitmap){
            if( ((MainActivity)getActivity()).requestUserPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,MY_PERMISSION_REQUEST_EXT_STORAGE)) {
                MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "incomingImage", "incomingImage");
                Log.v(TAG, "request granted");
            }
        }

    }

    /**
     * Shows a magnified picture of the image clicked in ScrollView
     */
    public static class ImageFragment extends Fragment {

        /**
         * Create a new instance of DetailsFragment, initialized to
         * show the text at 'index'.
         */
        public static ImageFragment newInstance(int imageId, Bitmap bitmap) {
            ImageFragment f = new ImageFragment();
            Bundle args = new Bundle();
            args.putParcelable("bitmap", bitmap);
            args.putInt("imageId", imageId);
            f.setArguments(args);
            return f;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState){

            final int imageId = getArguments().getInt("imageId");
            final Bitmap bitmap = getArguments().getParcelable("bitmap");

            final View fragmentView = inflater.inflate(R.layout.image_fragment, null);
            ImageView imageView = (ImageView) fragmentView.findViewById(R.id.image_magnified);
            imageView.setImageBitmap(bitmap);
            Log.v(TAG, "setting image bitmap");
            Log.v(TAG, "image id is " + imageView.getId());

            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG, "popped backstack");
                    if(getActivity().getFragmentManager().getBackStackEntryCount()!= 0)
                        getActivity().getFragmentManager().popBackStack();
                    ((MainActivity)getActivity()).hideButtons(View.VISIBLE);
                }
            });

            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ((MainActivity)getActivity()).imageDialogFragment = ImageDialogFragment.newInstance(imageId, bitmap);
                    ((MainActivity)getActivity()).imageDialogFragment.show(((MainActivity)getActivity()).getSupportFragmentManager(),"imageDialogFragment");
                    return true;
                }
            });

            return fragmentView;

        }
    }


}
