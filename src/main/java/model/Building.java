package model;

/**
 * 
 * 宿舍楼类
 */
public class Building {
    private int id;
    private String name;// 楼名
    private String address;// 地址
    private int floorCount;// 楼层数

    public Building() {
    }

    public Building(int id, String name, String address, int floorCount) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.floorCount = floorCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(int floorCount) {
        this.floorCount = floorCount;
    }

    @Override
    public String toString() {
        return String.format("| %-4d | %-15s | %-20s | %-4d |",
                id, name, address == null ? address : "N/A", floorCount);
    }
}
