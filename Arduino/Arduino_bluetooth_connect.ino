//--------SONAR AND LM35------------
#define TRIG_PIN 12
#define ECHO_PIN 13
#define LM_PIN A2
#define TIMEOUT 30000
#define OPEN_DISTANCE_MIN 10
#define OPEN_DISTANCE_MAX 15
#define OPEN_DISTANCE 50    

/*initialize the HC-SR04 sensor and LM35*/
void setupSonar() { 
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  pinMode(LM_PIN, INPUT);
}

/*convert number from unit m/s to us/cm*/
float convertMStoUSCM(float number) {
  return 1.0 / number * 10000;
}

/*measure and return the current temperature*/
float readTemperature() {
  float readnumber = analogRead(LM_PIN); // read the value of the
  float millivolts = (readnumber / 1024.0) * 5000; // convert the readnumber to the millivolts
  float temperature = millivolts / 10; // sensitivity is 10mv per degree
  return temperature;
}

/*measure and return the current detected distance*/
float readDistance() {
  float readDistance;
  digitalWrite(TRIG_PIN, LOW);//wait a short period to make the system more stable
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH); //generate a 10us pulse to trig
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  int duration = pulseIn(ECHO_PIN, HIGH, TIMEOUT); // receive the duration of the pulse
  if (duration == 0) {// if no pulse is completed before the timeout
    readDistance = -1; // the distance is out of range or cannot be measured
  } else {
    float soundSpeed = 331.5 + 0.6 * readTemperature(); // calculate the speed of the sound by temperature
    readDistance = (float)duration / (convertMStoUSCM(soundSpeed) * 2.0) ; //distance = duration / (2 * speed(us/cm))
  }
  return readDistance;
}

#define BUZZER_PIN 11
/*detect whether the door is open*/
boolean doorOpen(){
//  if(readDistance() < OPEN_DISTANCE && readDistance() > 0)
//    digitalWrite(BUZZER_PIN, HIGH);
//  else if (readDistance() >= OPEN_DISTANCE && readDistance() > 0)
//    digitalWrite(BUZZER_PIN, LOW);
//  Serial.println(readDistance());
//  return (readDistance() > OPEN_DISTANCE_MIN && readDistance() < OPEN_DISTANCE_MAX);
  return ((readDistance() < OPEN_DISTANCE) && (readDistance() > 0)) ;
}
//-------------------------------------

//------PHTOTCELL AND LED-----------
#define photoCellPin A0
#define ledPin A1
#define lightThreshold 800
const int LED_ON = 1;
const int LED_OFF = 0;

void setupLED(){
  pinMode(photoCellPin, INPUT);
  pinMode(ledPin, OUTPUT);
}

void lighting(){
  if(analogRead(photoCellPin)> lightThreshold){
       digitalWrite(ledPin, LOW);
   }
  else {
      //the brightness of the led depends on the reading of the photocell
       analogWrite(ledPin, (1023-analogRead(photoCellPin))/4);
  } 
}

void controlLED(int LedMode){
  switch(LedMode){
  case LED_ON:
    digitalWrite(ledPin, HIGH);
    break;
  case LED_OFF:
    digitalWrite(ledPin, LOW);
    break;
  }
}
//--------------------------------------

//--------------BUZZER AND INFRARED-------
#define INFRARED_PIN 5
const int BUZZER_ON = 1;
const int BUZZER_OFF = 0;

void setupBuzzer(){
  pinMode(BUZZER_PIN, OUTPUT);
}

void setupInfrared(){
  pinMode(INFRARED_PIN, INPUT);      
}

int infrared(){
  //if the stranger is inside the area, returns 1, else returns 0
   if(digitalRead(INFRARED_PIN) == LOW)
      return 0;
   else
      return 1;
}

void controlBuzzer(int buzzerMode){
  switch(buzzerMode){
  case BUZZER_ON:
    analogWrite(BUZZER_PIN, 127);
    break;
  case BUZZER_OFF:
    analogWrite(BUZZER_PIN, 0);
    break;
  }
}
//--------------------------------------

//-------BLUETOOTH-------------------
int mode;
int preMode = 5; 
//byte Bxx: first byte buzzer, second byte led
void read(){
  if(Serial.available()){
    mode = Serial.parseInt();
   // Serial.println(mode);
  }
  if (mode == 0 || mode == 1){
    controlBuzzer(mode);
//    Serial.println(mode);
  }
  if (mode == 2 || mode == 3)
    controlLED(mode & B01);
  if (mode == 4 || (mode != 5 && preMode == 4))
    lighting();
  if (mode == 5)
    digitalWrite(ledPin, LOW);
  //Serial.flush();
  preMode = mode;
}
byte preBuf[3];
//preBuf[0] = 25;
//preBuf[1] = 0;
//preBuf[2] = 0;

void send(){
  byte buf[3];
  buf[0] = (int)readTemperature();//from LM35
  //buf[1] = infrared();//from infrared
  buf[1] = 0;
  buf[2] = doorOpen();//whether the buzzer is on 
  if((buf[0]-preBuf[0]<= 1 && preBuf[0]-buf[0]<= 1) && (buf[1] == 1 && preBuf[1] == 0 )|| (buf[2] == 1 && preBuf[2] == 0)){
    //Serial.println("pai");
    Serial.write(buf,3); 
//    Serial.print(buf[2]);
//    Serial.print(" prebuf");
//    Serial.print(preBuf[2]);
  } 
 // else{
  //  Serial.println("no pai");
  //}
  preBuf[0] = buf[0];
  preBuf[1] = buf[1];
  preBuf[2] = buf[2];
//    Serial.print(buf[2]);
//    Serial.print(" prebuf");
//    Serial.print(preBuf[2]);
//    Serial.print("\n");
}
//------------------------------------
void setup() {
  /*set up all sensors and motors*/
  setupSonar();
  setupLED();
  setupBuzzer();
  setupInfrared();
  Serial.begin(9600);
}

void loop(){
  send();
  read();
  //Serial.println(readDistance());
  //delay(100);
}
