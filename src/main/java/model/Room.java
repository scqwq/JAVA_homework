package model;
/**
 * 
 * 房间类
 */
public class Room {
    private int id;
    private String roomNum;//房间号
    private int floor;//楼层
    private int buildingId;//宿舍楼ID
    private String buildingName;//宿舍楼名
    private int capacity = 4;//最大容量4人
    private int occupiedCount;//房间人数

    public Room() {
    }

    public Room(int id, String roomNum, int floor, int buildingId, int capacity) {
        this.id = id;
        this.roomNum = roomNum;
        this.floor = floor;
        this.buildingId = buildingId;
        this.capacity = capacity;
    }

    //Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomNum() {
        return roomNum;
    }

    public void setRoomNum(String roomNum) {
        this.roomNum = roomNum;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public void setOccupiedCount(int occupiedCount) {
        this.occupiedCount = occupiedCount;
    }
    
    //空闲床位
    public int getAvailableBeds() {
        return capacity - occupiedCount;
    }

    @Override
    public String toString(){
        return String.format("| %-4d | %-15s | %-12S | %-4d | %d/%d |",
                    id, buildingName == null ? "N/A" : buildingName, roomNum, floor, occupiedCount, capacity
        );
    }
}
