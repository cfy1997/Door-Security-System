package com.example.lucyzhao.dooraccesssecurity;

import android.widget.TextView;

/**
 * Created by LucyZhao on 2017/3/22.
 * Helper class that provides useful strings
 */

public class StringUtils {
    //buzzer status
    public static final String ON = "RUNNING";
    public static final String OFF = "STOPPING";

    public static final String on = "on";
    public static final String off = "off";

    public static final String FALSE = "false";
    public static final String TRUE = "true";

    //Firebase user fields
    public static final String FirebaseUserEndpoint = "Users";
    public static final String FirebaseUserRegistrationID = "registrationID";
    public static final String FirebaseUserFamilyBelongsTo = "houseName";
    public static final String FirebaseUserEmail = "email";


    //Firebase family fields
    public static final String FirebaseFamilyEndpoint = "Families";
    public static final String FirebaseFamilyLastPhotoID = "lastPhotoID";
    public static final String FirebaseFamilyRegistrationIDs = "registrationIDs";
    public static final String FirebaseFamilyPassword = "password";
    public static final String FirebaseFamilyStatus = "status";
    public static final String FirebaseFamilyBuzzer = "buzzer";
    public static final String FirebaseFamilyEncodeCmd = "encodeCommand";


    public static final String FirebaseFamilySystemInfo = "info";
    public static final String FirebaseFamilyLastLoginTime = "lastLoginTime";
    public static final String FirebaseFamilySleepEnd = "sleepend";
    public static final String FirebaseFamilySleepStart = "sleepstart";
    public static final String FirebaseFamilyWorkEnd = "workend";
    public static final String FirebaseFamilyWorkStart = "workstart";
    public static final String FirebaseFamilySysOnOff = "system";
    public static final String FirebaseFamilyLED = "led";

    public static final String FirebaseFamilyDeletePhotoNumber = "deletePhotoNumber";
    public static final String FirebaseFamilyTakeInstantPhoto = "takeInstantPhoto";
    public static final String FirebaseFamilyUrgent = "urgent";
    public static final String FirebaseFamilyNumUsers = "numUsers";

    //TODO remove this
    public static final String FirebaseImagePath = "haus/images/";

    //the number to call when the user presses "call" button
    public static final String TargetPhoneNumber = "tel:7786810109";
    //indicates where the registration id for this device is stored
    public static final String registrationIDFileName = "registration_id";

    /**
     * Input check for joining a house
     * Checks whether input password matches the correct password stored
     * in Firebase and sets appropriate hints
     * @param enteredPassword user input
     * @param correctPassword the correct password stored in database
     * @param houseName the user's house's name
     * @param hint TextView to display hints
     * @return false if any field is null or empty string or the user
     * enters incorrect password
     */
    public static boolean joiningHouseInputCheck(String enteredPassword, String correctPassword, String houseName, TextView hint){

        if(enteredPassword == null || enteredPassword.equals("") || houseName == null || houseName.equals("")){
            hint.setText(R.string.fields_empty);
            return false;
        }
        else if(!enteredPassword.equals(correctPassword)) {
            hint.setText(R.string.joining_house_failed);
            return false;
        }
        return true;
    }

    public static boolean isAlpha(String name) {
        return name.matches("[A-Z]+");
    }

}
