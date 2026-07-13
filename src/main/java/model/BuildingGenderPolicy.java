package model;

/**
 * 宿舍楼性别策略枚举，定义宿舍楼可入住的性别规则。
 * <ul>
 *   <li>{@link #MALE_ONLY} — 仅男生入住</li>
 *   <li>{@link #FEMALE_ONLY} — 仅女生入住</li>
 *   <li>{@link #MIXED_BY_FLOOR} — 男女分层入住</li>
 * </ul>
 */
public enum BuildingGenderPolicy {
    MALE_ONLY("男"),          // 仅允许男生入住的宿舍楼策略。
    FEMALE_ONLY("女"),        // 仅允许女生入住的宿舍楼策略。
    MIXED_BY_FLOOR("男女分层"); // 男女可同楼但需分层的策略。

    private final String label;

    // 枚举构造器，传入该策略在 UI 中显示的中文标签。
    BuildingGenderPolicy(String label) {
        this.label = label;
    }

    // 返回策略的中文显示标签。
    public String label() {
        return label;
    }

    // 判断当前策略是否允许指定性别的学生入住。
    public boolean supports(Gender gender) {
        return this == MIXED_BY_FLOOR
                || (this == MALE_ONLY && gender == Gender.MALE)
                || (this == FEMALE_ONLY && gender == Gender.FEMALE);
    }

    // 根据用户输入的中文或英文名称解析为对应的策略枚举。
    public static BuildingGenderPolicy fromLabel(String text) {
        return switch (text.trim()) {
            case "男", "MALE_ONLY" -> MALE_ONLY;
            case "女", "FEMALE_ONLY" -> FEMALE_ONLY;
            case "男女分层", "MIXED_BY_FLOOR" -> MIXED_BY_FLOOR;
            default -> throw new IllegalArgumentException("宿舍楼性别只支持 男/女/男女分层");
        };
    }
}
