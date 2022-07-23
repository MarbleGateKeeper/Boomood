package love.marblegate.boomood.mechanism.itemstackreversion;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import love.marblegate.boomood.mechanism.Situation;
import love.marblegate.boomood.mechanism.itemstackreversion.handler.ItemStackRevertHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ItemStackDropSituation extends Situation {
    private final ItemStackRevertHandler handler;
    private final List<ItemStack> items;

    ItemStackDropSituation(ItemStackRevertHandler handler, List<ItemStack> items) {
        this.handler = handler;
        this.items = items;
    }

    public static ItemStackDropSituation create(ItemStackRevertHandler handler, List<ItemStack> items) {
        return new ItemStackDropSituation(handler, items);
    }

    ItemStackRevertHandler getHandler() {
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


    public static class SituationSet extends ArrayList<ItemStackDropSituation> {

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
                Multimap<ItemStackRevertHandler, List<ItemStack>> mergeMap = MultimapBuilder.hashKeys().arrayListValues().build();
                Unordered ret = new Unordered();
                for (var situation : this) {
                    mergeMap.put(situation.getHandler(), situation.getItems());
                }
                for (var key : mergeMap.keySet()) {
                    var unmerged = mergeMap.get(key).stream().toList();
                    for (var merged : key.mergeItemStack(unmerged)) {
                        ret.add(new ItemStackDropSituation(key, merged));
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
