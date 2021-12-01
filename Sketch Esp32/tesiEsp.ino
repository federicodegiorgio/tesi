#include <WiFi.h>
#include <PubSubClient.h>
#include <SoftwareSerial.h>
#include "FS.h"
#include "SD.h"
#include "SPI.h"
#include <ArduinoJson.h>
#include "base64.h"
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Arduino.h>

#include <Adafruit_VC0706.h>

#include "DHT.h"



#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

#define DHTPIN 32     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)


#define VIN 5 // V power voltage photoresistor
#define R 10000 //ohm resistance value
#define LUX_CALC_SCALAR           12518931
#define LUX_CALC_EXPONENT         -1.405



DHT dht(DHTPIN, DHTTYPE);

Adafruit_VC0706 cam = Adafruit_VC0706(&Serial2);


/*
 * Bt settings for handshake
 */

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool namecheck=false;
bool ssidBool=false;
bool pwdBool=false; 
bool oldDeviceConnected = false;
uint8_t value = 0;
String nameid;
String ssid;
String pwd;
String uniq;
bool updateDataConnection=false;
bool conn=false;
int imagecountdown=8;


class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();
      String valuestr= "";
      if (rxValue.length() > 0) {
        if(conn){
          String s = "Connected";
          pCharacteristic->setValue(s.c_str());
          pCharacteristic->notify();
        }
        else{
          Serial.println("*********");
          Serial.print("Received Value: ");
          for (int i = 1; i < rxValue.length(); i++){
            Serial.print(rxValue[i]);
            valuestr=valuestr+rxValue[i];
          }
        
          Serial.println(valuestr);
          if(rxValue[0]=='$'){
            namecheck=true;
            nameid=valuestr;
          }
          
          if(rxValue[0]=='!'){
            ssidBool=true;
            ssid=valuestr;
          }
          if(rxValue[0]=='?'){
            pwdBool=true;
            pwd=valuestr;
            updateDataConnection=true;
          }
          Serial.println();
          Serial.println("*********");
        }

      }
    };
    void onRead(BLECharacteristic* pCharacteristic) {
            std::string rxValue = pCharacteristic->getValue();
            if (rxValue.length() > 0) {
        Serial.println("*********");
        Serial.print("Send Value: ");
        for (int i = 0; i < rxValue.length(); i++)
          Serial.print(rxValue[i]);

        Serial.println();
        Serial.println("*********");
      }
      Serial.println(rxValue[0]);
            Serial.println("Characteristic Read");
    }
};





const char* mqtt_server = "broker.hivemq.com";
#define mqtt_port 1883
#define MQTT_SERIAL_PUBLISH_CH "FedeTesiTopic"

WiFiClient wifiClient;

PubSubClient client(wifiClient);


void setup() {
  Serial.begin(115200);
  byte mac[6];
  WiFi.macAddress(mac);
  // uniq è ricavata dal mac address in modo da avere un id unico per ogni scheda
  uniq =  String(mac[0],HEX) +String(mac[1],HEX) +String(mac[2],HEX) +String(mac[3],HEX) + String(mac[4],HEX) + String(mac[5],HEX);

  Serial.println(uniq);

// Create the BLE Device
  BLEDevice::init("MyESP32");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );

  pCharacteristic->setCallbacks(new MyCallbacks());

  // https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
  // Create a BLE Descriptor
  pCharacteristic->addDescriptor(new BLE2902());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
  delay(100);
  while(!deviceConnected){
    delay(1000);  
  }
    while (pwdBool==false||conn==false) {
    
      // disconnecting
      if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        pServer->startAdvertising(); // restart advertising
        Serial.println("start advertising");
        oldDeviceConnected = deviceConnected;
      }
    // connecting
      if (deviceConnected && !oldDeviceConnected) {
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
      }
      if(updateDataConnection){
        Serial.println("reconnect");
        updateDataConnection=false;
        conn=setup_wifi_b(8);
        if(!conn)
          pServer->getAdvertising()->start();

      }
      delay(1000);
      Serial.println("reconnect");
    }
    Serial.println("Username is " + nameid);
    Serial.println("SSid is " + ssid);
    Serial.println("Password is " + pwd);



  dht.begin();
  //setup_wifi();

  // see if the card is present and can be initialized:
  if (!SD.begin(5)) {
    Serial.println("Card failed, or not present");
    // don't do anything more:
    return;
  }
}

void loop() {
    String s = "Connected";
    pCharacteristic->setValue(s.c_str());
    pCharacteristic->notify();
    String strUniq = "$"+uniq;
    pCharacteristic->setValue(strUniq.c_str());
    pCharacteristic->notify();
     client.setServer(mqtt_server, mqtt_port);
  reconnect();
  DynamicJsonDocument doc(1024);
  delay(300);
  client.loop();
  int analog_value = analogRead(34);
  float moisture = calc_moisture(analog_value);
  Serial.print("Moisture = ");
  Serial.print(moisture);
  Serial.print("%");
  Serial.print("\n");
  doc["Moisture"] = moisture;

  double intensity = light_intensity(analogRead(35));
  int lumcount=0;
  while (intensity<0 && lumcount<5){
    delay(2000);
    intensity = light_intensity(analogRead(35));
    lumcount++;
  }
  if(intensity<0){
    intensity=20;
  }
  int lum = analogRead(35);
  Serial.print("Luminosity = ");
  Serial.print(intensity);
  Serial.print(" lux");
  Serial.print("\n");
  doc["Luminosity"] = intensity;

  float temp_hum_val[2] = {0};
  // Reading temperature or humidity takes about 250 milliseconds!
  // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
  client.loop();


  if (!dht.readTempAndHumidity(temp_hum_val)) {
    Serial.print("Humidity: ");
    Serial.print(temp_hum_val[0]);
    Serial.print(" %\t");
    Serial.print("Temperature: ");
    Serial.print(temp_hum_val[1]);
    Serial.println(" *C");
    doc["Temperature"] = temp_hum_val[1];
    doc["Humidity"] = temp_hum_val[0];

  }
  else {
    Serial.println("Failed to get temprature and humidity value.");
    delay(3000);
    if (!dht.readTempAndHumidity(temp_hum_val)) {
       doc["Temperature"] = temp_hum_val[1];
       doc["Humidity"] = temp_hum_val[0];
    }
    else{
          Serial.println("Another Failed to get temprature and humidity value.");
    }
  }
    Serial.println(serializeJson(doc,Serial));
  Serial.print(" \n");
  client.loop();
  delay(200);
  client.loop();

  
  pCharacteristic->setValue(s.c_str());
    pCharacteristic->notify();
    
    pCharacteristic->setValue(strUniq.c_str());
    pCharacteristic->notify();
  if (cam.begin()) {
    Serial.println("Camera Found:");
  } else {
    Serial.println("No camera found?");
    return;
  }
  Serial.println("Snap in 3 secs...");
    client.loop();
  delay(3000);
    client.loop();
  if (! cam.takePicture())
    Serial.println("Failed to snap!");
  else
    Serial.println("Picture taken!");

  // set the risolution of the snapshot
  //cam.setImageSize(VC0706_640x480);        // biggest
  //cam.setImageSize(VC0706_320x240);        // medium
  cam.setImageSize(VC0706_160x120);          // small

  // Create an image with the name IMAGExx.JPG
  int sizeName=uniq.length()+6;
  char filename[sizeName];
  String fn="/"+uniq+".JPG";
  fn.toCharArray(filename,sizeName);  
  Serial.print(filename);
    client.loop();

  // Open the file for writing
  File imgFile = SD.open(filename, FILE_WRITE);

  // Get the size of the image (frame) taken
  uint16_t jpglen = cam.frameLength();
  Serial.print("Storing ");
  Serial.print(jpglen, DEC);
  Serial.print(" byte image.");
  int32_t time = millis();
  pinMode(8, OUTPUT);
  // Read all the data up to # bytes!
  byte wCount = 0; // For counting # of writes
  Serial.println("/n");
  while (jpglen > 0) {
    // read 32 bytes at a time;
    uint8_t *buffer;
    uint8_t bytesToRead = min(32, (int) jpglen); // change 32 to 64 for a speedup but may not work with all setups!
    buffer = cam.readPicture(bytesToRead);
    imgFile.write(buffer, bytesToRead);
    jpglen -= bytesToRead;
  }
  imgFile.close();
  delay(1000);

  if(imagecountdown>7){
  imagecountdown=0;
  File myimage=SD.open(filename,FILE_READ);
  Serial.println("step");
  delay(100);
  
  int imgBytes= myimage.size();
  Serial.println(myimage.size());
  int y=0;
  Serial.println(" ");
  DynamicJsonDocument imgJson (20480);
   while (myimage.available()){
    uint8_t buffer[64];
    uint8_t bytesToRead = min(64, imgBytes);
    myimage.read(buffer, bytesToRead);
    //bs64[y]=myimage.read();
    y++;
    if( (int) bytesToRead>63){
      imgBytes=imgBytes-64;
      char payl[512];
 //     rbase64.encode((char *)buffer);
      delay(20);
      //String res=rbase64.result();
      String res=base64::encode(buffer,bytesToRead);
      delay(20);
      imgJson["payload"] = res;
        if(imgBytes+64==myimage.size())
          imgJson["sez"] = "init";
        else
          imgJson["sez"] = "transfer";
      imgJson["name"]=filename;
      serializeJson(imgJson, payl,512);
      delay(20);
      if(client.publish(MQTT_SERIAL_PUBLISH_CH,payl)==false){
        reconnect();
        client.publish(MQTT_SERIAL_PUBLISH_CH,payl);
      }
      delay(30);
      Serial.print(res);
      Serial.println(payl);
    }
    if(((int) bytesToRead<64)||imgBytes==0){
        char payl[512];
//      rbase64.encode((char *)buffer);
      //String res=rbase64.result();
      String res=base64::encode(buffer,bytesToRead);
      delay(20);
      imgJson["payload"] = res;
      imgJson["sez"] = "eof";
      imgJson["name"]=filename;
      imgJson["fbUser"]=nameid;
      imgJson["codScheda"]=uniq;

      delay(20);
      serializeJson(imgJson, payl,512);
            delay(20);

      client.publish(MQTT_SERIAL_PUBLISH_CH,payl);
            delay(40);
      Serial.print(res);
      Serial.println("finitooo");
      delay(200);
    }
    
      client.loop();

    //rbase64.encode((char *)myimage.read());
  }
  myimage.close();
  pCharacteristic->setValue(s.c_str());
    pCharacteristic->notify();
    
    pCharacteristic->setValue(strUniq.c_str());
    pCharacteristic->notify();
  delay(200);

  
  deleteFile(SD,filename);


  
  Serial.println(y);
  time = millis() - time;
  Serial.println("done!");
  Serial.println(time); Serial.println(" ms elapsed");
  }
  Serial.println("Sending message to MQTT topic..");
  delay(20);
  doc["userId"]=nameid;
  doc["boardId"]=uniq;
  doc["sez"]="sensors";
  Serial.println(serializeJson(doc,Serial));
  char senspayl[512];
  serializeJson(doc,senspayl,512);
  delay(20);
  if((doc.containsKey("Temperature")) && (doc.containsKey("Humidity"))){
  if (client.publish(MQTT_SERIAL_PUBLISH_CH, senspayl) == true) {
    Serial.println("Success sending message");
  }
  else {
    Serial.println("Error sending message");
    Serial.println("\n");
    Serial.println("Try to reconnect");
    Serial.println("\n");
      reconnect();
      client.publish(MQTT_SERIAL_PUBLISH_CH, senspayl);
    }
}
  client.loop();
  delay(3000);


  client.loop();
  Serial.println("-------------");
  delay(1000000);
  client.loop();
  imagecountdown++;



}



/*
    Funzioni per gestione rete/ mqtt
*/


bool setup_wifi_b(int sec) {
  int i=0;
  delay(100);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  char ssidC[ssid.length()+1];
  ssid.toCharArray(ssidC, ssid.length()+1);
  Serial.println(ssidC);
  char pwdC[pwd.length()+1];
  pwd.toCharArray(pwdC, pwd.length()+1);
  Serial.println(pwdC);
  WiFi.begin(ssidC, pwdC);
  while ((WiFi.status() != WL_CONNECTED)) {
    i++;
    delay(500);
    Serial.print(".");
    if(i/2>sec)
      return false;
    Serial.println(i);
  }
  randomSeed(micros());
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  return true;
}

void setup_wifi() {
  delay(100);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  char ssidC[ssid.length()+1];
  ssid.toCharArray(ssidC, ssid.length()+1);
  Serial.println(ssidC);
  char pwdC[pwd.length()+1];
  pwd.toCharArray(pwdC, pwd.length()+1);
  Serial.println(pwdC);
  WiFi.begin(ssidC, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  randomSeed(micros());
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void reconnect() {
  // Loop until we're reconnected
  int i = 3;
  while (!client.connected()) {
    i--;
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ESP32Client-";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (client.connect(clientId.c_str()/*, MQTT_USER, MQTT_PASSWORD*/)) {
      Serial.println("connected...");
      //Once connected, publish an announcement...
      if (client.publish("/provaTopic", "hello world"))
        Serial.print("Publish succes");
      else
        Serial.print("Errore publish mqtt");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      if (i == 0) {
        setup_wifi();
        i = 3;
      }
      delay(5000);
    }
  }
}

void publishSerialData(char *serialData) {
  if (!client.connected()) {
    reconnect();
  }
  client.publish(MQTT_SERIAL_PUBLISH_CH, serialData);

}

/*
   Funzioni di conversione valori sensori
*/

// converting luminosity in lux
double light_intensity (int raw) {
  if(raw==0)
    raw=1;
  // Conversion rule
  float resistorVoltage = (float)raw / 4095 * VIN;
  float ldrVoltage = VIN - resistorVoltage;
  
  // LDR_RESISTANCE_CONVERSION
  // resistance that the LDR would have for that voltage  
  float ldrResistance = ldrVoltage/resistorVoltage * R;
  
  // LDR_LUX
  // Change the code below to the proper conversion from ldrResistance to
  // ldrLux
  int ldrLux = LUX_CALC_SCALAR * pow(ldrResistance, LUX_CALC_EXPONENT);
  return ldrLux;
}

// converting moisture in percentage
float calc_moisture(int value) {
  value = 4095.0 - value;
  float moisture = map(value, 0, 4095, 0.0, 100.0);
  return moisture;
}

/*
   Funzioni per gestione file scheda sd
*/

void writeFile(fs::FS &fs, const char * path, const char * message) {
  Serial.printf("Writing file: %s\n", path);

  File file = fs.open(path, FILE_WRITE);
  if (!file) {
    Serial.println("Failed to open file for writing");
    return;
  }
  if (file.print(message)) {
    Serial.println("File written");
  } else {
    Serial.println("Write failed");
  }
  file.close();
}

void appendFile(fs::FS &fs, const char * path, const char * message) {
  Serial.printf("Appending to file: %s\n", path);

  File file = fs.open(path, FILE_APPEND);
  if (!file) {
    Serial.println("Failed to open file for appending");
    return;
  }
  if (file.print(message)) {
    Serial.println("Message appended");
  } else {
    Serial.println("Append failed");
  }
  file.close();
}

void deleteFile(fs::FS &fs, const char * path) {
  Serial.printf("Deleting file: %s\n", path);
  if (fs.remove(path)) {
    Serial.println("File deleted");
  } else {
    Serial.println("Delete failed");
  }
}
