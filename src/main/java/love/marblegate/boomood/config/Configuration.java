package love.marblegate.boomood.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class Configuration {
    public static final ForgeConfigSpec MOD_CONFIG;

    public static ForgeConfigSpec.IntValue NOISOLPXE_HORIZONTAL_RADIUS;
    public static ForgeConfigSpec.IntValue NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT;
    public static ForgeConfigSpec.IntValue NOISOLPXE_DEPTH;
    public static ForgeConfigSpec.BooleanValue NOISOLPXE_CREATE_TNT;

    public static ForgeConfigSpec.BooleanValue NOISOLPXE_ARMOR_STAND_POSE_RANDOMIZE;

    public static ForgeConfigSpec.DoubleValue NOISOLPXE_GLOW_ITEM_FRAME_POSSIBILITY;

    public static ForgeConfigSpec.BooleanValue NOISOLPXE_ITEM_FRAME_SITUATION_REMEDY_IS_SUPPORT_BLOCK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("noisolpxe");
        NOISOLPXE_HORIZONTAL_RADIUS = builder.comment("This radius will be used to calculate revert area and reverting process will also follow it.")
                .defineInRange("NOISOLPXE_HORIZONTAL_RADIUS", 3, 2, 16);
        NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT = builder.comment("This height will be used to calculate revert area height. Reverting process will try to follow this height as limit but it is not guaranteed!")
                .defineInRange("NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT", 3, 2, 16);
        NOISOLPXE_DEPTH = builder.comment("This depth will be used to calculate revert area depth. Reverting process will not revert things to height below event center.")
                .defineInRange("NOISOLPXE_DEPTH", 1, 1, 16);
        NOISOLPXE_CREATE_TNT = builder.comment("Will the reverting process put tnt block back?")
                .define("NOISOLPXE_CREATE_TNT", true);
        NOISOLPXE_ARMOR_STAND_POSE_RANDOMIZE = builder.comment("Will the reverting process put armor stand with randomized pose?")
                .define("NOISOLPXE_ARMOR_STAND_POSE_RANDOMIZE", true);
        NOISOLPXE_GLOW_ITEM_FRAME_POSSIBILITY = builder.comment("The possibility of the reverting process putting glow item frame?")
                .defineInRange("NOISOLPXE_GLOW_ITEM_FRAME_POSSIBILITY", 0.2,0,1);
        NOISOLPXE_ITEM_FRAME_SITUATION_REMEDY_IS_SUPPORT_BLOCK = builder.comment("If ture, when item frame destruction situation is reverted and there is no suitable place,",
                        "Support Block will be created,",
                        "Otherwisew item will be put into chest.")
                .define("NOISOLPXE_ITEM_FRAME_SITUATION_REMEDY_IS_SUPPORT_BLOCK", true);
        builder.pop();

        MOD_CONFIG = builder.build();
    }
}
