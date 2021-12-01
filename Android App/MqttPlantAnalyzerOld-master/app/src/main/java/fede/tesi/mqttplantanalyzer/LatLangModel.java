package fede.tesi.mqttplantanalyzer;

public class LatLangModel {
    private double latitude;
    private double longitude;
    private String name;
    private String board;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBoard() {
        return this.board;
    }
    public void setBoard(String board) {
        this.board = board;
    }
}
