#!/usr/bin/python3
#from picamera import PiCamera
from tkinter import *
from datetime import *
#from serial import *
#from time import sleep
from functools import partial
from firebase import firebase
from firebase.firebase import FirebaseApplication, FirebaseAuthentication
#from pyfcm import FCMNotification
#from google.cloud import storage
#import time
import pyowm

owm = pyowm.OWM('41286752cfebfe333034f2aeb63c4458')
global city
city = "Vancouver,ca"
global observation
observation = owm.weather_at_place("Vancouver,ca")
global weather
weather = observation.get_weather()
#temperature read from Arduino
global inner_temp
global systemOn
systemOn = 1

global right_pass
entered_pass = 0
times = 0
oldPasswordCorrect = 0

global userClickTime
userClickTime = 0
global lightOn
lightOn = 0
global cameraClickTime
cameraClickTime = 0
global buzzerOn
buzzerOn = 0

locking = 0
takingPhoto = 0
changingPW = 0
needCheck = 1

global bgColor
bgColor = '#18191B'
global activeColor
activeColor = '#2C2C6F'
global powerOnColor
powerOnColor = '#00ffff'

global preSystemStatus
preSystemStatus = "off"

global dayBegin
dayBegin = 9
global dayEnd
dayEnd = 16
global nightBegin
nightBegin = 23
global nightEnd
nightEnd = 6

global manOn
manOn = 0
global manOff
manOff = 0

global angleValue
angleValue = 90
global fromGuiTakingPhoto
fromGuiTakingPhoto = 0

global indoorTemp
indoorTemp = "23.00 °C"
global hasHouseName
hasHouseName = 0
global hasEntered
hasEntered = 0

def wrongPasswordWarning():
    warningImage = PhotoImage(file = "warning.png")
    wpWarningBase = Label(topCanvas, image = warningImage, bg = bgColor)
    wpWarningBase.image = warningImage
    wpWarningBase.pack_propagate(0)
    wpWarningBase.place(x = 70, y = 85)
    wpWarningText = Label(wpWarningBase, font = 12, text = "The entered password is incorrect.", fg = "white", bg = bgColor)
    wpWarningText.place(x = 10, y = 50)
    wpWarningText2 = Label(wpWarningBase, font = 12, text = "Please try again...", fg = "white", bg = bgColor)
    wpWarningText2.place(x = 10, y = 70)
    okButton = Button(wpWarningBase, text = "OK", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor ,command = lambda:deleteWarning(wpWarningBase))
    okButton.place(x = 280, y = 90)

def exceedLengthWarning():
    warningImage = PhotoImage(file = "warning.png")
    elWarningBase = Label(topCanvas, image = warningImage, bg = bgColor)
    elWarningBase.image = warningImage
    elWarningBase.pack_propagate(0)
    elWarningBase.place(x = 70, y = 84)
    elWarningText = Label(elWarningBase, font = 12, text = "The entered password is too long.", fg = "white", bg = bgColor)
    elWarningText.place(x = 10, y = 50)
    elWarningText2 = Label(elWarningBase, font = 12, text = "The length of password should be 6.", fg = "white", bg = bgColor)
    elWarningText2.place(x = 10, y = 70)
    okButton = Button(elWarningBase, text = "OK", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor ,command = lambda:deleteWarning(elWarningBase))
    okButton.place(x = 280, y = 90)

def toShortWarning():
    warningImage = PhotoImage(file = "warning.png")
    tsWarningBase = Label(topCanvas, image = warningImage, bg = bgColor)
    tsWarningBase.image = warningImage
    tsWarningBase.pack_propagate(0)
    tsWarningBase.place(x = 70, y = 84)
    tsWarningText = Label(tsWarningBase, font = 12, text = "The entered password is too short.", fg = "white", bg = bgColor)
    tsWarningText.place(x = 10, y = 50)
    tsWarningText2 = Label(tsWarningBase, font = 12, text = "The length of password should be 6.", fg = "white", bg = bgColor)
    tsWarningText2.place(x = 10, y = 70)
    okButton = Button(tsWarningBase, text = "OK", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor ,command = lambda:deleteWarning(tsWarningBase))
    okButton.place(x = 280, y = 90)

def checkPassword(num):
    print (num)
    global entered_pass
    global times
    global needCheck
    global hasEntered
    print(entered_pass)

    entered_pass = entered_pass*10 + num
    times += 1
    text.set(str(entered_pass))

    if needCheck == 1:
        if(times == 6 and entered_pass == right_pass):
            print ("the password is correct")
            times = 0
            entered_pass = 0
            global locking
            if locking == 1:
                enableButtons()
                locking = 0
                hasEntered = 1
            else:
                enterNewPassword()

        elif(times == 6 and entered_pass != right_pass):
            print("the password is incoreect")
            entered_pass = 0
            times = 0
            text.set("")
            wrongPasswordWarning()

def clear():
    global entered_pass
    global times
    entered_pass = 0
    times = 0
    text.set("")

def delete():
    global entered_pass
    global times
    if times > 0:
        entered_pass = int(entered_pass/10)
        times -= 1
        if times == 0:
            text.set("")
        else:
            text.set(str(entered_pass))
    elif times == 0:
        text.set("")
        entered_pass = 0

def enterNewPassword():
    global needCheck
    needCheck = 0
    text.set("")
    cpFrame.changePWlabel.configure(text = "Please enter the new password")
    cpFrame.cencelButton.pack(side = LEFT, padx = 5)

    enterButton = Button(cpFrame, width = 8, text = "Enter", fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command = confirmNewPassword)
    enterButton.pack(side = RIGHT, padx = 5)

def confirmNewPassword():
    global times
    global entered_pass
    global right_pass
    global needCheck

    if times > 6:
            exceedLengthWarning()
    elif times < 6:
            toShortWarning()
    else:
            right_pass = entered_pass
            needCheck = 1
            cpFrame.destroy()
            firebase.put('/Families/'+ houseName,'password',str(entered_pass))

    text.set("")
    times = 0
    entered_pass = 0

def cancelChangePassword():
    cpFrame.destroy()
    global times
    global entered_pass
    global needCheck

    times = 0
    entered_pass = 0
    needCheck = 1

#====================================LIGHT CONTROL=====================================
def lightControl():
    global lightOn
    global houseName
    if lightOn == 0:
        global bulbonImage
        bottomFrame.lightButton.configure(image = bulbonImage)
        lightOn = 1
        ser.write(b'3') # turn on light
        led = firebase.put('/Families/'+ houseName + '/status','led',"RUNNING")
    else:
        global bulboffImage
        bottomFrame.lightButton.configure(image = bulboffImage)
        lightOn = 0
        ser.write(b'2') # turn off light
        led = firebase.put('/Families/'+ houseName + '/status','led',"STOPPING")
#======================================================================================
    
#====================================CAMERA CONTROL====================================
def cameraControl():
    print("camera")
    global cameraClickTime
    global angleValue
    if cameraClickTime == 0:
        global cameraFrame
        cameraFrame = Frame(topCanvas, width = 120, bg = "white")
        cameraFrame.place(x = 120, y = 225)

        takePhoto = Button(cameraFrame, width = 12, text = "take a photo", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor, command = lambda:takeOnePhoto(1))
        takePhoto.pack(pady = 1)
        
        cameraClickTime = 1
    else:
        cameraFrame.destroy()
        cameraClickTime = 0

def closePhotoCommand():
    topCanvas.delete(topCanvas.onePhoto)
    topCanvas.closePhoto.destroy()
#======================================================================================
    
#====================================BUZZER CONTROL====================================
def buzzerControl():
    print("buzzer")
    global locking
    global buzzerOn
    global houseName
    if buzzerOn == 0:
        global buzzonImage
        bottomFrame.buzzButton.configure(image = buzzonImage)
        buzzerOn = 1
        #ser.write(b'1') # turn on buzzer
        buzz = firebase.put('/Families/'+ houseName + '/status','buzzer',"RUNNING")
    else:
        global buzzoffImage
        bottomFrame.buzzButton.configure(image = buzzoffImage)
        buzzerOn = 0
        #ser.write(b'0') # turn off buzzer
        buzz = firebase.put('/Families/'+ houseName + '/status','buzzer',"STOPPING")
#======================================================================================

#======================================USER CONTROL====================================
def userControl():
    global userClickTime
    if userClickTime == 0:
        global userFrame
        userFrame = Frame(topCanvas, width = 120, height = 87, bg = "white")
        userFrame.pack_propagate(0) 
        userFrame.place(x = 360, y = 169)

        cpButton = Button(userFrame, text = "Change Password", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor, command = changePassword)
        cpButton.pack(padx = 2)
        ctButton = Button(userFrame, text = "Change Duration", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor, command = changeActiveDuration)
        ctButton.pack(padx = 2)
        cfButton = Button(userFrame, text = "Change Family", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor, command = enterFamilyName)
        cfButton.pack(padx = 2)

        userClickTime = 1
    else:
        userFrame.destroy()
        userClickTime = 0

def changePassword():
    print("password")

    global cpFrame
    cpFrame = Frame(topCanvas, bg = bgColor)
    cpFrame.pack(pady = 10)

    cpFrame.changePWlabel = Label(cpFrame, text = "Please enter the old password", fg = "white", bg = bgColor)
    cpFrame.changePWlabel.pack(pady = 4)

    text.set("")
    entry = Entry(cpFrame, textvariable = text)
    entry.pack(pady = 4)

    kbFrame = Frame(cpFrame, width = 220, height = 135, bg = "white")
    kbFrame.pack_propagate(0) 
    kbFrame.pack(padx = 2, pady = 4)

    displayKeyboard(kbFrame)

    cpFrame.cencelButton = Button(cpFrame, width = 8, text = "Cancel", fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command = cancelChangePassword)
    cpFrame.cencelButton.pack(pady = 4)

def changeActiveDuration():
    global cdFrame
    global needCheck
    global entered_pass
    needCheck = 0
    cdFrame = Frame(topCanvas, bg = bgColor)
    cdFrame.pack(pady = 10)

    cdFrame.hintLabel = Label(cdFrame, text = "Please enter the new day begin time", fg = "white", bg = bgColor)
    cdFrame.hintLabel.grid(row = 0, pady = 2, columnspan = 2)
    cdFrame.hintLabel2 = Label(cdFrame, text = "(Use 24-hour system)", fg = "white", bg = bgColor)
    cdFrame.hintLabel2.grid(row = 1, pady = 2, columnspan = 2)

    text.set(str(dayBegin))
    entered_pass = dayBegin
    entry = Entry(cdFrame, textvariable = text, width = 3)
    entry.grid(row = 2, column = 0, pady = 4, sticky = E)

    cdFrame.timeLabel = Label(cdFrame, text = ":00", fg = "white", bg = bgColor)
    cdFrame.timeLabel.grid(row = 2, column = 1, sticky = W)
    
    kbFrame = Frame(cdFrame, width = 220, height = 135, bg = "white")
    kbFrame.pack_propagate(0) 
    kbFrame.grid(row = 3, columnspan = 2,padx = 2, pady = 3)

    displayKeyboard(kbFrame)
    
    cdFrame.cencelButton = Button(cdFrame, width = 8, text = "Done", fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command = cancelChangeDuration)
    cdFrame.cencelButton.grid(row = 4, column = 0, padx = 6, sticky = W)
    enterButton = Button(cdFrame, width = 8, text = "Enter", fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command = confirmNewDuration)
    enterButton.grid(row = 4, column = 1, padx = 6, sticky = E)

def cancelChangeDuration():
    cdFrame.destroy()
    global times
    global entered_pass
    global needCheck
    global changeDurationClickTime

    text.set("")
    times = 0
    entered_pass = 0
    needCheck = 1
    changeDurationClickTime = 0

def wrongTimeWarning():
    warningImage = PhotoImage(file = "warning.png")
    wtWarningBase = Label(topCanvas, image = warningImage, bg = bgColor)
    wtWarningBase.image = warningImage
    wtWarningBase.pack_propagate(0)
    wtWarningBase.place(x = 70, y = 85)
    wtWarningText = Label(wtWarningBase, font = 12, text = "The entered time is invalid.", fg = "white", bg = bgColor)
    wtWarningText.place(x = 10, y = 50)
    wtWarningText2 = Label(wtWarningBase, font = 12, text = "Please try again...", fg = "white", bg = bgColor)
    wtWarningText2.place(x = 10, y = 70)
    okButton = Button(wtWarningBase, text = "OK", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor ,command = lambda:deleteWarning(wtWarningBase))
    okButton.place(x = 280, y = 90)

global changeDurationClickTime
changeDurationClickTime = 0
def confirmNewDuration():
    global dayBegin
    global dayEnd
    global nightBegin
    global nightEnd
    global entered_pass
    global times
    global changeDurationClickTime
    global houseName

    if entered_pass >= 24:
        wrongTimeWarning()
        entered_pass = 0
        text.set("")
        return
    if changeDurationClickTime == 0:
        dayBegin = entered_pass
        cdFrame.hintLabel.configure(text = "Please enter the new day end time")
        text.set(str(dayEnd))
        entered_pass = dayEnd
        changeDurationClickTime += 1
        firebase.put('/Families/'+ houseName + '/info','workstart', str(dayBegin) + ':00')
    elif changeDurationClickTime == 1:
        dayEnd = entered_pass
        cdFrame.hintLabel.configure(text = "Please enter the new night begin time")
        text.set(str(nightBegin))
        entered_pass = nightBegin
        changeDurationClickTime += 1
        firebase.put('/Families/'+ houseName + '/info','workend', str(dayEnd) + ':00')
    elif changeDurationClickTime == 2:
        nightBegin = entered_pass
        cdFrame.hintLabel.configure(text = "Please enter the new night end time")
        text.set(str(nightEnd))
        entered_pass = nightEnd
        changeDurationClickTime += 1
        firebase.put('/Families/'+ houseName + '/info','sleepstart', str(nightBegin) + ':00')
    elif changeDurationClickTime == 3:
        nightEnd = entered_pass
        cdFrame.destroy()
        changeDurationClickTime = 0
        text.set("")
        entered_pass = 0
        firebase.put('/Families/'+ houseName + '/info','sleepend', str(nightEnd) + ':00')
    times = 0
    topCanvas.itemconfigure(topCanvas.dayDurationLabel, text = "Day time working period from %s:00 to %s:00" %(dayBegin,dayEnd))
    topCanvas.itemconfigure(topCanvas.nightDurationLabel, text = "Night time working period from %s:00 to %s:00" %(nightBegin,nightEnd))
    
#======================================================================================
def displayKeyboard(frame):
    rowNum = 0
    buttonWidth = 5
    b1 = Button(frame, width = buttonWidth, text = "1", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(1))
    b1.grid(row = rowNum, column = 0, padx = 2, pady = 2)
    b2 = Button(frame, width = buttonWidth, text = "2", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(2))
    b2.grid(row = rowNum, column = 1)
    b3 = Button(frame, width = buttonWidth, text = "3", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(3))
    b3.grid(row = rowNum, column = 2, padx = 2)
    rowNum += 1
    
    b4 = Button(frame, width = buttonWidth, text = "4", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(4))
    b4.grid(row = rowNum, column = 0, padx = 2, pady = 2)
    b5 = Button(frame, width = buttonWidth, text = "5", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(5))
    b5.grid(row = rowNum, column = 1)
    b6 = Button(frame, width = buttonWidth, text = "6", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(6))
    b6.grid(row = rowNum, column = 2)
    rowNum += 1

    b7 = Button(frame, width = buttonWidth, text = "7", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(7))
    b7.grid(row = rowNum, column = 0, padx = 2, pady = 2)
    b8 = Button(frame, width = buttonWidth, text = "8", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(8))
    b8.grid(row = rowNum, column = 1)
    b9 = Button(frame, width = buttonWidth, text = "9", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(9))
    b9.grid(row = rowNum, column = 2)
    rowNum += 1
    
    bc = Button(frame, width = buttonWidth, text = "AC", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:clear())
    bc.grid(row = rowNum, column = 0, padx = 2, pady = 2)
    b0 = Button(frame, width = buttonWidth, text = "0", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:checkPassword(0))
    b0.grid(row = rowNum, column = 1)
    bd = Button(frame, width = buttonWidth, text = "DEL", relief = FLAT, highlightbackground = bgColor, fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command=lambda:delete())
    bd.grid(row = rowNum, column = 2)
    rowNum += 1

def displayImageKeyboard(frame,buttonHeight):
    buttonWidth = 65
    rowNum = 0

    num1 = PhotoImage(file = "1.png")
    num2 = PhotoImage(file = "2.png")
    num3 = PhotoImage(file = "3.png")
    num4 = PhotoImage(file = "4.png")
    num5 = PhotoImage(file = "5.png")
    num6 = PhotoImage(file = "6.png")
    num7 = PhotoImage(file = "7.png")
    num8 = PhotoImage(file = "8.png")
    num9 = PhotoImage(file = "9.png")
    num0 = PhotoImage(file = "0.png")
    ac = PhotoImage(file = "ac.png")
    d = PhotoImage(file = "del.png")

    b1 = Button(frame, height = buttonHeight, width = buttonWidth, image = num1, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(1))
    b1.image = num1
    b1.grid(row = rowNum, column = 0, padx = 2)
    b2 = Button(frame, height = buttonHeight, width = buttonWidth, image = num2, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(2))
    b2.image = num2
    b2.grid(row = rowNum, column = 1)
    b3 = Button(frame, height = buttonHeight, width = buttonWidth, image = num3, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(3))
    b3.image = num3
    b3.grid(row = rowNum, column = 2, padx = 2)
    rowNum += 1
    
    b4 = Button(frame, height = buttonHeight, width = buttonWidth, image = num4, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(4))
    b4.image = num4
    b4.grid(row = rowNum, column = 0, padx = 2)
    b5 = Button(frame, height = buttonHeight, width = buttonWidth, image = num5, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(5))
    b5.image = num5
    b5.grid(row = rowNum, column = 1)
    b6 = Button(frame, height = buttonHeight, width = buttonWidth, image = num6, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(6))
    b6.image = num6
    b6.grid(row = rowNum, column = 2)
    rowNum += 1

    b7 = Button(frame, height = buttonHeight, width = buttonWidth, image = num7, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(7))
    b7.image = num7
    b7.grid(row = rowNum, column = 0, padx = 2)
    b8 = Button(frame, height = buttonHeight, width = buttonWidth, image = num8, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(8))
    b8.image = num8
    b8.grid(row = rowNum, column = 1)
    b9 = Button(frame, height = buttonHeight, width = buttonWidth, image = num9, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(9))
    b9.image = num9
    b9.grid(row = rowNum, column = 2)
    rowNum += 1
    
    bc = Button(frame, height = buttonHeight, width = buttonWidth, image = ac, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:clear())
    bc.image = ac
    bc.grid(row = rowNum, column = 0, padx = 2)
    b0 = Button(frame, height = buttonHeight, width = buttonWidth, image = num0, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:checkPassword(0))
    b0.image = num0
    b0.grid(row = rowNum, column = 1)
    bd = Button(frame, height = buttonHeight, width = buttonWidth, image = d, relief = FLAT, highlightbackground = bgColor, bg = bgColor, activebackground = activeColor, fg='black', command=lambda:delete())
    bd.image = d
    bd.grid(row = rowNum, column = 2)
    rowNum += 1

def clearWindow():
    for child in mainw.winfo_children():
        child.destroy()

def enableButtons():
    kbFrame.destroy()
    entry.destroy()
    topCanvas.delete(topCanvas.passLabel)
    topCanvas.coords(topCanvas.timeLabel, 15, 10)
    topCanvas.coords(topCanvas.dateLabel, 15, 40)
    topCanvas.itemconfigure(topCanvas.timeLabel, font = ("Helvetica", 20))
    topCanvas.itemconfigure(topCanvas.dateLabel, font = ("Helvetica", 16))

    topCanvas.coords(topCanvas.outTempLabel, 255, 10)
    topCanvas.itemconfigure(topCanvas.outTempLabel, font = ("Helvetica", 16))
    topCanvas.outsideLabel = topCanvas.create_text(170, 10, text = "Outside:",font = ("Helvetica", 16), fill = "white", anchor = "nw")
    topCanvas.insideLabel = topCanvas.create_text(170, 40, text = "Inside:",font = ("Helvetica", 16), fill = "white", anchor = "nw")

    global indoorTemp
    topCanvas.indoorTempLabel = topCanvas.create_text(255, 40, text = indoorTemp, font = ("Helvetica", 16), fill = "white", anchor = "nw")

    global bottomFrame
    bottomFrame = Frame(topCanvas, height = 65, width = 480, bg = "white")
    bottomFrame.pack_propagate(0) 
    bottomFrame.pack(side = BOTTOM)

    global bulboffImage
    bulboffImage = PhotoImage(file = "bulb-off.png")
    global bulbonImage
    bulbonImage = PhotoImage(file = "bulb-on.png")
    global lightOn
    if lightOn == 0:
        bottomFrame.lightButton = Button(bottomFrame, height = 55, width = 111, image = bulboffImage, bg = bgColor, activebackground = activeColor, command = lightControl)
        bottomFrame.lightButton.image = bulboffImage
    else:
        bottomFrame.lightButton = Button(bottomFrame, height = 55, width = 111, image = bulbonImage, bg = bgColor, activebackground = activeColor, command = lightControl)
        bottomFrame.lightButton.image = bulbonImage
    bottomFrame.lightButton.pack(side = LEFT, padx = 2)

    camImage = PhotoImage(file = "camera.png")
    camButton = Button(bottomFrame, height = 55, width = 111, image = camImage, bg = bgColor, activebackground = activeColor, command = cameraControl)
    camButton.image = camImage
    camButton.pack(side = LEFT, padx = 2)

    global buzzoffImage
    buzzoffImage = PhotoImage(file = "buzz-off.png")
    global buzzonImage
    buzzonImage = PhotoImage(file = "buzz-on.png")
    global buzzerOn
    if buzzerOn == 0:
        bottomFrame.buzzButton = Button(bottomFrame, height = 55, width = 111, image = buzzoffImage, bg = bgColor, activebackground = activeColor, command = buzzerControl)
        bottomFrame.buzzButton.image = buzzoffImage
    else:
        bottomFrame.buzzButton = Button(bottomFrame, height = 55, width = 111, image = buzzonImage, bg = bgColor, activebackground = activeColor, command = buzzerControl)
        bottomFrame.buzzButton.image = buzzonImage
    bottomFrame.buzzButton.pack(side = LEFT, padx = 2)

    userImage = PhotoImage(file = "user.png")
    userButton = Button(bottomFrame, height = 55, width = 111, image = userImage, bg = bgColor, activebackground = activeColor, command = userControl)
    userButton.image = userImage
    userButton.pack(side = LEFT, padx = 2)

    powerImage = PhotoImage(file = "power.png")
    if systemOn == 0:
        topCanvas.powerButton = Button(topCanvas, height = 40, width = 40, image = powerImage, bg = bgColor, activebackground = activeColor, command = powerControl)
    else:
        topCanvas.powerButton = Button(topCanvas, height = 40, width = 40, image = powerImage, bg = powerOnColor, activebackground = activeColor, command = powerControl)
    topCanvas.powerButton.image = powerImage
    topCanvas.powerButton.place(x = 410, y = 10)

    lockImage = PhotoImage(file = "lock.png")
    topCanvas.lockButton = Button(topCanvas, height = 40, width = 40, image = lockImage, bg = bgColor, activebackground = activeColor, command = lock)
    topCanvas.lockButton.image = lockImage
    topCanvas.lockButton.place(x = 410, y = 60)

    topCanvas.dayDurationLabel = topCanvas.create_text(15,200, text = "Day time working period from %s:00 to %s:00" %(dayBegin,dayEnd), font = ("Helvetica", 14), fill = "white", anchor = "nw")
    topCanvas.nightDurationLabel = topCanvas.create_text(15,220, text = "Night time working period from %s:00 to %s:00" %(nightBegin,nightEnd), font = ("Helvetica", 14), fill = "white", anchor = "nw")

def powerControl():
    print("power")
    global systemOn
    global manOn
    global manOff
    global preSystemStatus
    global houseName

    if systemOn == 0:
        topCanvas.powerButton.configure(bg = powerOnColor)
        systemOn = 1
        manOn = 1
        sendLoginTime()
        system = firebase.put('/Families/'+ houseName + '/status','system',"on")
        ser.flushInput()
        preSystemStatus = 1
    else:
        topCanvas.powerButton.configure(bg = bgColor)
        systemOn = 0
        manOff = 1
        system = firebase.put('/Families/'+ houseName + '/status','system',"off")
        preSystemStatus = 0
    
def mainWindow():
    global mainw
    mainw = Tk()
    mainw.title("Welcome to this awesome secrity system")
    mainw.geometry("480x320")
    mainw.configure(bg = bgColor)
    #mainw.attributes("-fullscreen", True)

    global hasHouseName
    if(hasHouseName == 0):
        enterFamilyName()
    else:
        lock()
    print("out")
  
#======================== FAMILY NAME ===========================================
def wrongFamilyName():
    warningImage = PhotoImage(file = "warning.png")
    wfWarningBase = Label(topCanvas, image = warningImage, bg = bgColor)
    wfWarningBase.image = warningImage
    wfWarningBase.pack_propagate(0)
    wfWarningBase.place(x = 70, y = 84)
    wfWarningText = Label(wfWarningBase, font = 12, text = "The entered family does not exist.", fg = "white", bg = bgColor)
    wfWarningText.place(x = 10, y = 50)
    wfWarningText2 = Label(wfWarningBase, font = 12, text = "Please try again...", fg = "white", bg = bgColor)
    wfWarningText2.place(x = 10, y = 70)
    okButton = Button(wfWarningBase, text = "OK", fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor ,command = lambda:deleteWarning(wfWarningBase))
    okButton.place(x = 280, y = 90)

def checkHouseName():
    global right_pass
    global houseName

    houseName = HouseNameString.get()
    print(houseName)
    PATH = "/Families/" + houseName + "/password"
    result = firebase.get(PATH, None)
    print (result)
    if result == None:
        wrongFamilyName()
        HouseNameString.set("")
    else:
        right_pass = int(result)
        global hasHouseName
        hasHouseName = 1
    
def click(btn):
    if btn == 'DEL':
        s = HouseNameString.get()
        s = s[:-1] #substring s to delete one char
        HouseNameString.set(s)
    elif btn == 'OK':
        checkHouseName()
        print("ok")
    else:
        s = HouseNameString.get() + btn
        HouseNameString.set(s)
        HouseNameString.set(s)
    
def enterFamilyName():
    clearWindow()
    defaultVar()

    global hasHouseName
    hasHouseName = 0
    global hasEntered
    hasEntered = 0

    global topCanvas
    topCanvas = Canvas(mainw)
    topCanvas.pack_propagate(0) 
    topCanvas.pack(expand = YES, fill = BOTH)
    topCanvas.bg = PhotoImage(file = "b2-480x320.gif")
    topCanvas.create_image(0,0,image = topCanvas.bg, anchor = "nw")

    textFrame = Frame(topCanvas, bd=0, bg = bgColor)
    textFrame.pack(padx=20, pady=30)

    textFrame.hint = Label(textFrame, text = "Please enter the name of your house that you already created", fg = "white", bg = bgColor)
    textFrame.hint.grid(row = 0, pady = 2, columnspan = 2)

    global HouseNameString
    HouseNameString = StringVar()
    textFrame.houseNameLabel = Label(textFrame, textvariable = HouseNameString, fg = "black", bg = 'white')
    textFrame.houseNameLabel.grid(row = 1, pady = 2, columnspan = 2)
    HouseNameString.set("")


    # create a labeled frame for the keypad buttons
    lf = LabelFrame(topCanvas, bd=3, bg = "white")
    lf.pack(padx=15, pady=10)
        
    # typical calculator button layout
    btn_list = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',  
                'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'OK',
                'Z', 'X', 'C', 'V', 'B', 'N', 'M', 'DEL']
        

    # create and position all buttons with a for-loop
    # r, c used for row, column grid values
    r = 1
    c = 0
    n = 0
    # list(range()) needed for Python3
    btn = list(range(len(btn_list)))
    for label in btn_list:
        # partial takes care of function and argument
        cmd = partial(click, label)
        # create the button
        if label == 'DEL':
            btn[n] = Button(lf, text=label, command=cmd, fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor)
            btn[n].grid(row=r, column=c, columnspan = 2, sticky = W + E + N + S)
        elif label == 'OK':
            btn[n] = Button(lf, text=label, command=cmd, fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor)
            btn[n].grid(row=r, column=c, rowspan = 2, sticky = W + E + N + S)
        else:
            btn[n] = Button(lf, text=label, width=2, height=2, command=cmd, fg = "white", bg = bgColor, activeforeground = "white", activebackground = activeColor)
            # position the button
            btn[n].grid(row=r, column=c)
            # increment button index
        n += 1
        # update row/column position
        c += 1
        if c == 10:
            c = 0
            r += 1

    while hasHouseName == 0:
        mainw.update()
    #ser.flushInput()
    lock()
#================================================================================

def defaultVar():
    global entered_pass
    entered_pass = 0
    global needCheck
    needCheck = 1
    global times
    times = 0
    global locking
    locking = 1
    global changeDurationClickTime
    changeDurationClickTime = 0
    global userClickTime
    userClickTime = 0
    global cameraClickTime
    cameraClickTime = 0

def lock():
    clearWindow()
    defaultVar()

    global topCanvas
    topCanvas = Canvas(mainw)
    topCanvas.pack_propagate(0) 
    topCanvas.pack(expand = YES, fill = BOTH)
    topCanvas.bg = PhotoImage(file = "b2-480x320.gif")
    topCanvas.create_image(0,0,image = topCanvas.bg, anchor = "nw")

    global timeText
    timeText = StringVar()
    timeText.set(datetime.now().strftime('%H:%M'))
    topCanvas.timeLabel = topCanvas.create_text(50,90, font = ("Helvetica", 44), text = timeText.get(), fill = "white", anchor = "nw")

    global dateText
    dateText = StringVar()
    dateText.set(datetime.now().strftime('%Y-%m-%d'))
    topCanvas.dateLabel = topCanvas.create_text(50,160, font = ("Helvetica", 22), text = dateText.get(), fill = "white", anchor = "nw")

    global tempText
    tempText = StringVar()
    tempText.set(str(weather.get_temperature('celsius')['temp']) + " °C")
    topCanvas.outTempLabel = topCanvas.create_text(80,200, font = ("Helvetica", 18), text = tempText.get(), fill = "white", anchor = "nw")
    
    topCanvas.passLabel = topCanvas.create_text(250,37, text="Please enter the password", font = ("Helvetica"), fill = "white", anchor = "nw")

    global entry
    global text
    text = StringVar()
    text.set("")
    entry = Entry(topCanvas,textvariable = text)
    entry.place(x = 265, y = 65)
    
    global kbFrame
    kbFrame = Frame(topCanvas, height = 169, width = 220, bg = bgColor)
    kbFrame.pack_propagate(0)
    kbFrame.place(x = 237, y = 100)

    displayImageKeyboard(kbFrame, 39)

    if hasEntered == 0:
        swichFamilyButton = Button(topCanvas, text = "Switch Family", fg = "white", bg = bgColor, activebackground = activeColor, activeforeground = "white", command = enterFamilyName)
        swichFamilyButton.place(x = 75, y = 280)
        while hasEntered == 0:
            mainw.update()
            now = datetime.now()
            timeText.set(now.strftime('%H:%M'))
            topCanvas.itemconfigure(topCanvas.timeLabel, text = timeText.get())
            dateText.set(now.strftime('%Y-%m-%d'))
            topCanvas.itemconfigure(topCanvas.dateLabel, text = dateText.get())
            tempText.set(str(weather.get_temperature('celsius')['temp']) + " °C")
            topCanvas.itemconfigure(topCanvas.outTempLabel, text = tempText.get())
        #ser.flushInput()

def takePhotoWarning():
    global locking
    warningImage = PhotoImage(file = "warning.png")
    global tpWarningImage
    tpWarningImage = Label(topCanvas, image = warningImage, bg = bgColor)
    tpWarningImage.image = warningImage
    tpWarningImage.pack_propagate(0)
    tpWarningImage.place(x = 70, y = 80)
    tpWarningText = Label(tpWarningImage, font = 12, text = "Taking photos...", fg = "white", bg = bgColor)
    tpWarningText.place(x = 10, y = 50)
    tpWarningText2 = Label(tpWarningImage, font = 12, text = "Please do not push any buttons.", fg = "white", bg = bgColor)
    tpWarningText2.place(x = 10, y = 70)

    if locking == 0:
        for child in bottomFrame.winfo_children():
            child.configure(state = DISABLED)
        if(userClickTime == 1):
            for child in userFrame.winfo_children():
                child.configure(state = DISABLED)
        if(cameraClickTime == 1):
            for child in cameraFrame.winfo_children():
                child.configure(state = DISABLED)
        topCanvas.powerButton.configure(state = DISABLED)
        topCanvas.lockButton.configure(state = DISABLED)

def deleteWarning(base):
    global takingPhoto
    global locking
    base.destroy()
    if takingPhoto == 1 and locking == 0:
        for child in bottomFrame.winfo_children():
            child.configure(state = NORMAL)
        if(userClickTime == 1):
            for child in userFrame.winfo_children():
                child.configure(state = NORMAL)
        if(cameraClickTime == 1):
            for child in cameraFrame.winfo_children():
                child.configure(state = NORMAL)
        topCanvas.powerButton.configure(state = NORMAL)
        topCanvas.lockButton.configure(state = NORMAL)

    
def cameraInit():
    global camera
    camera=PiCamera()
    camera.resolution = (1296,972)
    camera.framerate = 15
    camera.annotate_text_size = 50

def serialInit():
    global ser
    subprocess.call(['sudo','rfcomm','bind','/dev/rfcomm1','00:21:13:00:47:4B','1'])
    ser = Serial('/dev/rfcomm1')

def takeThreePhotos():
    global PhotoNum
    global camera
    global timeText
    global ser
    global bucket
    global firebase
    global takingPhoto
    takingPhoto = 1
    takePhotoWarning()
    ser.write(b'4') #turn on the light
    urgent = firebase.put('/Families/'+ houseName,'urgent','on')
    sendToPhone()
    for i in range(3):
        #PhotoNum += 1
        camera.annotate_text = datetime.now().strftime('%Y-%m-%d %H:%M')
        camera.capture('/home/pi/Desktop/photos/%s.png' %i, format = 'png')
    PhotoNum = firebase.get('/Families/' + houseName,'lastPhotoID')+3
    for i in range(3):
        string = houseName + "/images/"+str(PhotoNum-2+i)+".png"
        blob2 = bucket.blob(string)
        string2 = "/home/pi/Desktop/photos/"+str(i)+".png"
        blob2.upload_from_filename(filename=string2)
        result = firebase.put('/Families/'+ houseName,'lastPhotoID',PhotoNum-2+i)
    ser.write(b'5') #turn off the light
    ser.flushInput()
    sleep(1)
    deleteWarning(tpWarningImage)
    takingPhoto = 0

def takeOnePhoto(fromGui):
    global PhotoNum
    global camera
    global timeText
    global ser
    global bucket
    global firebase
    global houseName

    ser.write(b'4') #turn on the light
    camera.annotate_text = datetime.now().strftime('%Y-%m-%d %H:%M')
    PhotoNum = firebase.get('/Families/'+ houseName,'lastPhotoID')+1
    camera.capture('/home/pi/Desktop/photos/%s.png' %PhotoNum, format = 'png')
    string = houseName + "/images/"+str(PhotoNum)+".png"
    blob2 = bucket.blob(string)
    string2 = "/home/pi/Desktop/photos/"+str(PhotoNum)+".png"
    blob2.upload_from_filename(filename=string2)
    result = firebase.put('/Families/'+ houseName,'lastPhotoID',PhotoNum)
    ser.write(b'5') #turn off the light
    sleep(1)

    if fromGui == 1:
        cameraFrame.destroy()
        global cameraClickTime
        cameraClickTime = 0
        topCanvas.photo = PhotoImage(file = string2).subsample(4,4)
        topCanvas.onePhoto = topCanvas.create_image(15,10, image = topCanvas.photo, anchor = "nw")
        topCanvas.closePhoto = Button(topCanvas, text = "OK", command = closePhotoCommand)
        topCanvas.closePhoto.place(x = 380, y = 150)

    
def initFirebase():
    global bucket
    global firebase
    #client = storage.Client()
    #bucket = client.get_bucket('dooraccesssecurity-b83b8.appspot.com')
    EMAIL = 'lucyzhao0.0@gmail.com'
    UID = 'AYyAYh6G52WJ5zCWbBq5IUVAqXI3'
    FIREBASE_SECRET = 'zal5MbfNIzFLgeOvKLY2rQQCIRoU9kT7k0JCRtSr'
    DSN = 'https://dooraccesssecurity-b83b8.firebaseio.com'
    authentication = FirebaseAuthentication(FIREBASE_SECRET,EMAIL, extra={'id': UID})
    firebase.authentication = authentication
    user = authentication.get_user()
    firebase = firebase.FirebaseApplication(DSN,authentication)

def sendNotification( reg_list ):
    API_KEY = "AAAA-W-kAO4:APA91bGgi9H9H-NuEyQH9ER8wwAicGULVInaAoAZeco1zdV6TvY3WJhnjFjAUu-zSVpxBWXtBmcG-ZtA_7B9LSpDI4PHpmir4qp3jE0ADv79tg5P8VSaUtfkpXpYziZmeUmuSWAn8zQM"
    # Create a new FCM notification service associated with this project
    # Your api-key can be gotten from:  https://console.firebase.google.com/project/<project-name>/settings/cloudmessaging
    push_service = FCMNotification(api_key=API_KEY)

    # deivce registration ids 
    registration_ids = reg_list

    # Sending a notification with data message payload
    data_message = {
        "title" : "Door Access Security",
        "body" : "Someone entered your home!",
    }
    result = push_service.notify_multiple_devices(registration_ids=registration_ids, data_message=data_message)
    print (result)
    return;

def sendToPhone():
    global houseName

    HOUSE_REG_ID_PATH = '/Families/'+ houseName + '/registrationIDs'
    #GET returns a dictionary 
    reg_id_dict = firebase.get(HOUSE_REG_ID_PATH, None)
    #if this house currently has users logged in
    if reg_id_dict != None :
        #create a list of registration ids
        reg_id_list = list(reg_id_dict.values())
        #print all registration ids for this house 
        for id in reg_id_list:
            print (id)
        sendNotification( reg_id_list )
    #if this house currently has no user logged in
    #then no notification would be sent    
    else :
        print ("result is none")

def readDatabase():
    global lightOn
    global buzzerOn
    global systemOn
    global preSystemStatus
    global locking
    global manOn
    global manOff
    global houseName
    encode = firebase.get('/Families/'+ houseName,'encodeCommand')

    if encode == 4:
        if buzzerOn == 0:
            ser.write(b'1') # turn on buzzer
            buzzerOn = 1
            buzz = firebase.put('/Families/'+ houseName + '/status','buzzer',"RUNNING")
            print("kai")
            if locking == 0:
                global buzzonImage
                bottomFrame.buzzButton.configure(image = buzzonImage)
        elif buzzerOn == 1:
            ser.write(b'0') # turn off buzzer
            buzzerOn = 0
            buzz = firebase.put('/Families/'+ houseName + '/status','buzzer',"STOPPING")
            print("guan")
            if locking == 0:
                global buzzoffImage
                bottomFrame.buzzButton.configure(image = buzzoffImage)
        update = firebase.put('/Families/'+ houseName,'encodeCommand',0)
    elif encode == 3:
        
        if lightOn == 0:
            ser.write(b'3') # turn on light
            lightOn = 1
            led = firebase.put('/Families/'+ houseName + '/status','led',"RUNNING")
            if locking == 0:
                global bulbonImage
                bottomFrame.lightButton.configure(image = bulbonImage)
        elif lightOn == 1:
            ser.write(b'2') # turn off light
            lightOn = 0
            led = firebase.put('/Families/'+ houseName + '/status','led',"STOPPING")
            if locking == 0:
                global bulboffImage
                bottomFrame.lightButton.configure(image = bulboffImage)
        update = firebase.put('/Families/'+ houseName,'encodeCommand',0)
    elif encode == 2:
        takeOnePhoto(0)
        takenphoto = firebase.put('/Families/'+ houseName,'takeInstantPhoto','pending')
        update = firebase.put('/Families/'+ houseName,'encodeCommand',0)
    elif encode == 1:
        if systemOn == 1:
            systemOn = 0
            manOff = 1
            system = firebase.put('/Families/'+ houseName + '/status','system',"off")
            preSystemStatus = 0
            if locking == 0:
                topCanvas.powerButton.configure(bg = bgColor)
        elif systemOn == 0:
            systemOn = 1
            manOn = 1
            sendLoginTime()
            system = firebase.put('/Families/'+ houseName + '/status','system',"on")
            ser.flushInput()
            preSystemStatus = 1
            if locking == 0:
                topCanvas.powerButton.configure(bg = powerOnColor)
        update = firebase.put('/Families/'+ houseName,'encodeCommand',0)

def sentActivePeriod():
    global nightBegin
    global nightEnd
    global dayBegin
    global dayEnd
    global houseName

    firebase.put('/Families/'+ houseName + '/info','sleepstart', str(nightBegin) + ':00')
    firebase.put('/Families/'+ houseName + '/info','sleepend', str(nightEnd) + ':00')
    firebase.put('/Families/'+ houseName + '/info','workstart', str(dayBegin) + ':00')
    firebase.put('/Families/'+ houseName + '/info','workend', str(dayEnd) + ':00')
    
def sendLoginTime():
    global houseName

    now = datetime.now()
    firebase.put('/Families/'+ houseName + '/info','lastLoginTime', now.strftime('%Y-%m-%d %H:%M'))
    
def main():
    global systemOn
    global dayBegin
    global dayEnd
    global nightBegin
    global nightEnd
    global manOn
    global manOff
    global preSystemStatus
    global houseName
    #cameraInit()
    #serialInit()
    initFirebase()
    mainWindow()
    #sentActivePeriod()
    #sendLoginTime()
    
    while True:
        #print(systemOn)
        mainw.update()
        now = datetime.now()
        timeText.set(now.strftime('%H:%M'))
        topCanvas.itemconfigure(topCanvas.timeLabel, text = timeText.get())
        dateText.set(now.strftime('%Y-%m-%d'))
        topCanvas.itemconfigure(topCanvas.dateLabel, text = dateText.get())
        tempText.set(str(weather.get_temperature('celsius')['temp']) + " °C")
        topCanvas.itemconfigure(topCanvas.outTempLabel, text = tempText.get())
        #readDatabase()
        
        #active duration
        dayBeginTime = now.replace(hour = dayBegin, minute = 0, second = 0, microsecond = 0)
        dayEndTime = now.replace(hour = dayEnd, minute = 0, second = 0, microsecond = 0)
        if now < dayBeginTime:
            nightBeginTime = now.replace(hour = nightBegin, minute = 0, second = 0, microsecond = 0) - timedelta(days = 1)
            nightEndTime = now.replace(hour = nightEnd, minute = 0, second = 0, microsecond = 0)
        else:
            nightBeginTime = now.replace(hour = nightBegin, minute = 0, second = 0, microsecond = 0)
            nightEndTime = now.replace(hour = nightEnd, minute = 0, second = 0, microsecond = 0) + timedelta(days = 1)

        if now.hour == dayBegin or now.hour == nightBegin and now.minute == 0:
            manOn = 0
            manOff = 0
        if now >= dayBeginTime and now <= dayEndTime and manOff == 0:
            #print("system auto day on")
            systemOn = 1
            if preSystemStatus == 0:
                sendLoginTime()
                system = firebase.put('/Families/'+ houseName + '/status','system',"on")
                ser.flushInput()
                if locking == 0:
                    topCanvas.powerButton.configure(bg = powerOnColor)
            preSystemStatus = 1
        elif now >= nightBeginTime and now <= nightEndTime and manOff == 0:
            #print("system auto night on")
            systemOn = 1
            if preSystemStatus == 0:
                sendLoginTime()
                system = firebase.put('/Families/'+ houseName + '/status','system',"on")
                ser.flushInput()
                if locking == 0:
                    topCanvas.powerButton.configure(bg = powerOnColor)
            preSystemStatus = 1
        elif manOn == 0:
            #print("system auto off")
            systemOn = 0
            if preSystemStatus == 1:
                system = firebase.put('/Families/'+ houseName + '/status','system',"off")
                if locking == 0:
                    topCanvas.powerButton.configure(bg = bgColor)
            preSystemStatus = 0
        """try:
            if ser.inWaiting() > 0 and systemOn == 1:
                Ard_data = ser.read(3)
                ser.flushInput()
                inner_temp = Ard_data[0]
                if (Ard_data[1]==1) or (Ard_data[2]==1):
                    takeThreePhotos()
                print (Ard_data[0])
                print (Ard_data[1])
                print (Ard_data[2])
                topCanvas.itemconfigure(topCanvas.inTempLabel, text = str(Ard_data[0]) + ".00 °C")
        except OSError:
            print("bluetooth")
            serialInit()
        if systemOn == 0:
            ser.flushInput()"""
    print("out true loop")
    mainw.mainloop()
    print("out gui loop")

if __name__== "__main__":
    main()
    print("out main")
