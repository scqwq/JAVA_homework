package model;

public class Student {
    private int id;
    private String studentNo;//学号
    private String name;//姓名
    private String gender;//性别
    private String major;//专业

    public Student() {
    }
    
    public Student(int id, String studentNo, String name, String gender, String major) {
        this.id = id;
        this.studentNo = studentNo;
        this.name = name;
        this.gender = gender;
        this.major = major;
    }


    //Getter and Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    @Override
    public String toString() {
        return String.format("| %-4d | %-12s | %-10s | %-6s | %-15s |",
                    id, studentNo, name, gender, major
        );
    }
}
