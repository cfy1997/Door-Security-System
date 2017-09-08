package com.example.lucyzhao.dooraccesssecurity;

/**
 * Created by LucyZhao on 2017/3/22.
 * Simple class that represents a user
 */

public class User {
    private String UID;
    private String email;
    private String registrationID = "";
    private String houseName = "";
    public User(){}

    public User(String UID, String email){
        this.UID = UID;
        this.email = email;
    }

    public void setRegistrationID (String registrationID){
        this.registrationID = registrationID;
    }

    public String getUID(){
        return UID;
    }

    public String getEmail(){
        return email;
    }

    public String getRegistrationID(){
        return registrationID;
    }

    public String getHouseName(){ return houseName; }

    public void setHouseName(String houseName){
        this.houseName = houseName;
    }
}
