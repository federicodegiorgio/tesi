// Import the functions you need from the SDKs you need
//var firebase = require('firebase/app');
import { initializeApp } from 'firebase/app';
import { getDatabase,ref,set } from "firebase/database";
const {Storage}= require('@google-cloud/storage');
var List = require("collections/list");
var http = require('http');
var fs = require ('fs');
var i=20;
var server = http.createServer(function (req, res) {
  res.writeHead(200, {'Content-Type': 'text/plain'});
  res.end('Hello Worldn');
})
server.listen(1337, '127.0.0.1');
console.log('Server running at http://127.0.0.1:1337/');

const firebaseConfig = {

  apiKey: "AIzaSyCOtcbUsltFgPAaJaELKnkYeDh7_yVMUzE",

  authDomain: "mqttplantanalyzer.firebaseapp.com",

  databaseURL: "https://mqttplantanalyzer-default-rtdb.europe-west1.firebasedatabase.app",

  projectId: "mqttplantanalyzer",

  storageBucket: "mqttplantanalyzer.appspot.com",

  messagingSenderId: "760999932027",

  appId: "1:760999932027:web:7c5082649bacfcfebe72de",

  measurementId: "G-VJZGZYKMWJ"

};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
// Get a reference to the database service

function writeUserData(userId, boardId , timestamp, lux, moisture, humidity, temperature) {

  const db = getDatabase(app);
  set(ref(db,userId + '/' + boardId + '/' + timestamp),{
    humidity : humidity,
    temperature: temperature,
    moisture : moisture,
    timestamp : timestamp,
    boardId : boardId,
    lux : lux,
    userId : userId});
}
var fbUser;
var codScheda;
const uploadFile = async() => {

        // Uploads a local file to the bucket
            await storage.bucket(bucketName).upload(filename.substring(1), {
                // Support for HTTP requests made with Accept-Encoding: gzip
                // By setting the option destination, you can change the name of the
                // object you are uploading to a bucket.
                destination:fbUser+"/"+codScheda+"/"+Date.now(),
                metadata: {
                    contentType: "image/jpg"
                    
                },
            });
        }

const storage= new Storage();

let bucketName = "gs://mqttplantanalyzer.appspot.com";

let filename;

var image;

var mqtt=require('mqtt');

const { decode } = require('punycode');
var client = mqtt.connect("mqtt://broker.hivemq.com",{clientId:"fedeTesiSserver"});
client.on("connect",function(){	
console.log("connected");
});
client.subscribe("provaTopic",{qos:2})
var listImage={};
client.on('message', function(topic, payload) {
    const obj = JSON.parse(payload.toString()) // payload is your JSON as string
  console.log(obj);
  if(obj.sez=="init"){
    var image=new Buffer(0);
    //imp=true;
    var temp = Buffer.from(obj.payload, 'base64');
    //var temp = obj.payload;
    console.log(temp);

    image = Buffer.concat([image,temp]);

    listImage[obj.name]=image;
    console.log(obj);


  }  
  if(obj.sez == "transfer"){
    console.log("transfer");
    var temp = Buffer.from(obj.payload, 'base64');
    //var temp = obj.payload;
    console.log(temp);

    image= listImage[obj.name];
    image = Buffer.concat([image,temp]);
    listImage[obj.name]=image;
    
  }
   if (obj.sez == "eof")
    {
        var temp = Buffer.from(obj.payload, 'base64');
        //var temp = obj.payload;
        image= listImage[obj.name];
        image = Buffer.concat([image,temp]);
        listImage[obj.name]=image;
        filename=obj.name;
        fs.writeFile(filename.substring(1), image, (err) => {
        if (err) throw err;
        console.log(image.size);
        console.log('It\'s saved!');
        console.log(listImage);
        
      });
        fs.readFile(filename.substring(1),(errore,data) =>{
            if(errore){
                throw errore;
            }
            fbUser=obj.fbUser;
            codScheda=obj.codScheda;
        uploadFile(filename.substring(1));
        });
        
    }
    if (obj.sez== "sensors"){
      writeUserData(obj.userId,obj.boardId,Date.now(),obj.Luminosity,obj.Moisture,obj.Humidity,obj.Temperature);
      i=0;
      
    }
 });
