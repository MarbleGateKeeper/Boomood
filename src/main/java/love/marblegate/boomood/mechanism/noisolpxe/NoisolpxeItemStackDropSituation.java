package love.marblegate.boomood.mechanism.noisolpxe;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class NoisolpxeItemStackDropSituation implements NoisolpxeSituation {
    private final NoisolpxeItemStackDropSituationHandler handler;
    private final List<ItemStack> items;

    NoisolpxeItemStackDropSituation(NoisolpxeItemStackDropSituationHandler handler, List<ItemStack> items) {
        this.handler = handler;
        this.items = items;
    }

    static NoisolpxeItemStackDropSituation create(NoisolpxeItemStackDropSituationHandler handler, List<ItemStack> items) {
        return new NoisolpxeItemStackDropSituation(handler, items);
    }

    NoisolpxeItemStackDropSituationHandler getHandler() {
        return handler;
    }

    List<ItemStack> getItems() {
        return items;
    }

    @Override
    public void revert(Level level, BlockPos blockPos, Player manipulator) {
        handler.revert(level, blockPos, items, manipulator);
    }

    @Override
    public String toString() {
        return "Situation{" + "handler=" + handler + ", items=" + items + "}\n";
    }


    public static class SituationSet extends ArrayList<NoisolpxeItemStackDropSituation> {

        public void revert(Level level, BlockPos pos, Player player) {
            for (var situation : this) {
                situation.revert(level, pos, player);
            }
            // TODO this is for debug only, Removal is needed.
            System.out.println(this);
        }

        public static class Raw extends SituationSet {

            // Merge ItemStack for properly reverting
            public Unordered merge() {
                Multimap<NoisolpxeItemStackDropSituationHandler, List<ItemStack>> mergeMap = MultimapBuilder.hashKeys().arrayListValues().build();
                Unordered ret = new Unordered();
                for (var situation : this) {
                    mergeMap.put(situation.getHandler(), situation.getItems());
                }
                for (var key : mergeMap.keySet()) {
                    var unmerged = mergeMap.get(key).stream().toList();
                    for (var merged : key.mergeItemStack(unmerged)) {
                        ret.add(new NoisolpxeItemStackDropSituation(key, merged));
                    }
                }
                return ret;
            }
        }

        public static class Unordered extends SituationSet {

            // Sort NoisolpxeItemStackDropSituation for orderly reverting
            public SituationSet sort() {
                this.sort(Comparator.comparingInt(o -> -o.handler.priority()));
                var ret = new SituationSet();
                ret.addAll(this);
                return ret;
            }
        }
    }
}
