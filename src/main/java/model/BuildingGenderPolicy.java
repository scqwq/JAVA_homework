package model;

public enum BuildingGenderPolicy {
    MALE_ONLY("男"), //表示BuildingGenderPolicy 仅有的三个实例
    FEMALE_ONLY("女"),  //label指的是是调用枚举构造方法时传入的参数。
    MIXED_BY_FLOOR("男女分层"); //即BuildingGenderPolicy的参数

    private final String label;

    BuildingGenderPolicy(String label) {
        this.label = label;
    } //输入“男”实际执行等价于public static final BuildingGenderPolicy MALE_ONLY = new BuildingGenderPolicy("男");的表达

    public String label() {
        return label;
    }

    public boolean supports(Gender gender) {
        return this == MIXED_BY_FLOOR
                || (this == MALE_ONLY && gender == Gender.MALE)
                || (this == FEMALE_ONLY && gender == Gender.FEMALE);
    }

    public static BuildingGenderPolicy fromLabel(String text) {
        return switch (text.trim()) { //返回switch
            case "男", "MALE_ONLY" -> MALE_ONLY; //匹配后返回MALE_ONLY并结束switch
            case "女", "FEMALE_ONLY" -> FEMALE_ONLY;
            case "男女分层", "MIXED_BY_FLOOR" -> MIXED_BY_FLOOR;
            default -> throw new IllegalArgumentException("宿舍楼性别只支持 男/女/男女分层");
        };
    }
}

