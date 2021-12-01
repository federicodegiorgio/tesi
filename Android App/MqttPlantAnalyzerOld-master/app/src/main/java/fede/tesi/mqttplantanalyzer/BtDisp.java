package fede.tesi.mqttplantanalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BtDisp {
        public String name;
        public String address;
        public UUID uid;
        public UUID characteristicUUIDsList ;
        public UUID descriptorUUIDsList;

    public BtDisp(String name,String address, String uid) {
        this.name=name;
        this.address=address;
        this.uid=UUID.fromString(uid);
    }
    public BtDisp(){

    }
}
