package fede.tesi.mqttplantanalyzer;


public class Chart_Type {
    private String val;
    private int imm;

    public Chart_Type( int imm, String val) {
        this.val = val;
        this.imm = imm;
    }

    public String getVal() {
        return val;
    }

    public int getImm() {
        return imm;
    }


    public void setVal(String val) {
        this.val = val;
    }

    public void setImm(int imm) {
        this.imm = imm;
    }

}
