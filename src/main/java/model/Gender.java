package model;

// “学生性别”的枚举类型
public enum Gender {
    MALE("男"),
    FEMALE("女");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Gender fromLabel(String text) {
        return switch (text.trim()) {
            case "男", "MALE", "male", "Male" -> MALE;
            case "女", "FEMALE", "female", "Female" -> FEMALE;
            default -> throw new IllegalArgumentException("性别只支持 男/女");
        };
    }
}

