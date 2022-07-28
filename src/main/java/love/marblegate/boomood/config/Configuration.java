package love.marblegate.boomood.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class Configuration {
    public static final ForgeConfigSpec MOD_CONFIG;

    public static class Common{
        public static ForgeConfigSpec.IntValue RADIUS;

        public static ForgeConfigSpec.EnumValue<AreaType> AREA_SHAPE;
        public static ForgeConfigSpec.EnumValue<RemedyType> REMEDY_TYPE;

        public enum AreaType{
            SPHERE,
            CUBE;

            public boolean isSphere(){
                return this==SPHERE;
            }
        }

        public enum RemedyType{
            UPWARD,
            SHATTER_IN_DOUBLE_RADIUS;

            public boolean goUpward(){
                return this==UPWARD;
            }
        }
    }

    public static class ItemStackReversion{

        public static ForgeConfigSpec.BooleanValue CREATE_TNT;
        public static ForgeConfigSpec.BooleanValue ARMOR_STAND_POSE_RANDOMIZE;
        public static ForgeConfigSpec.DoubleValue GLOW_ITEM_FRAME_POSSIBILITY;
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("common");
        Common.RADIUS = builder.comment("The reversion area radius.")
                .defineInRange("RADIUS", 5, 2, 16);
        Common.AREA_SHAPE = builder.comment("The reversion area shape").defineEnum("AREA_SHAPE", Common.AreaType.SPHERE);
        Common.REMEDY_TYPE = builder.comment("If there is no enough space in reversion area, it decides how are is reverting process will be extend.").defineEnum("REMEDY_TYPE", Common.RemedyType.UPWARD);
        builder.pop();

        builder.push("itemstack_reversion");
        ItemStackReversion.CREATE_TNT = builder.comment("Will the reversion process put tnt block back?")
                .define("CREATE_TNT", true);
        ItemStackReversion.ARMOR_STAND_POSE_RANDOMIZE = builder.comment("Will the reversion process put armor stand with randomized pose?")
                .define("ARMOR_STAND_POSE_RANDOMIZE", true);
        ItemStackReversion.GLOW_ITEM_FRAME_POSSIBILITY = builder.comment("The possibility of the reversion process putting glow item frame?")
                .defineInRange("GLOW_ITEM_FRAME_POSSIBILITY", 0.2,0,1);
        builder.pop();

        MOD_CONFIG = builder.build();
    }
}
