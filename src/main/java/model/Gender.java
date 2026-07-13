package model;

/**
 * 学生性别枚举，定义系统支持的性别取值。
 * <ul>
 *   <li>{@link #MALE} — 男</li>
 *   <li>{@link #FEMALE} — 女</li>
 * </ul>
 */
public enum Gender {
    MALE("男"),
    FEMALE("女");

    private final String label;

    // 枚举构造器，关联性别在界面中显示的中文标签。
    Gender(String label) {
        this.label = label;
    }

    // 返回性别对应的中文显示标签。
    public String label() {
        return label;
    }

    // 根据用户输入解析性别，支持中文和多种大小写英文写法。
    public static Gender fromLabel(String text) {
        return switch (text.trim()) {
            case "男", "MALE", "male", "Male" -> MALE;
            case "女", "FEMALE", "female", "Female" -> FEMALE;
            default -> throw new IllegalArgumentException("性别只支持 男/女");
        };
    }
}
