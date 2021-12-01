package fede.tesi.mqttplantanalyzer;

public class MqttValue {
    private long timestamp;
    private int lux;
    private int moisture;
    private int humidity;
    private int temperature;
    private String boardId;
    private String userId;
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLux() {
        return lux;
    }

    public void setLux(int lux) {
        this.lux = lux;
    }

    public int getMoisture() {
        return moisture;
    }

    public void setMoisture(int moisture) {
        this.moisture = moisture;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
