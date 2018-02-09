package com.joedobo27.smeltpurifymod;

import com.joedobo27.libs.action.ActionMaster;
import com.wurmonline.math.TilePos;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.skills.SkillList;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.WeakHashMap;

public class SmeltAction extends ActionMaster {

    private final Item targetItem;
    private static WeakHashMap<Action, SmeltAction> performers = new WeakHashMap<>();

    SmeltAction(Action action, Creature performer, @Nullable Item activeTool, @Nullable Integer usedSkill,
                          int minSkill, int maxSkill, int longestTime, int shortestTime, int minimumStamina,
                          Item targetItem) {
        super(action, performer, activeTool, usedSkill, minSkill, maxSkill, longestTime, shortestTime, minimumStamina);
        this.targetItem = targetItem;
        performers.put(action, this);
    }

    @Nullable
    static SmeltAction getSmeltAction(Action action) {
        if (!performers.containsKey(action))
            return null;
        return performers.get(action);
    }

    boolean hasAFailureCondition() {
        if (this.activeTool == null || (this.activeTool.getTemplateId() != ItemList.smeltingPot)) {
            performer.getCommunicator().sendNormalServerMessage("You need to activate a smelting pot to smelt lumps.");
            return true;
        }
        if (this.targetItem == null) {
            performer.getCommunicator().sendNormalServerMessage("You need to target a lump to smelt it.");
            return true;
        }
        else {
            switch (this.targetItem.getTemplateId()) {
                case ItemList.goldBar:
                case ItemList.silverBar:
                case ItemList.ironBar:
                case ItemList.copperBar:
                case ItemList.zincBar:
                case ItemList.leadBar:
                case ItemList.steelBar:
                case ItemList.tinBar:
                case ItemList.brassBar:
                case ItemList.bronzeBar:
                case ItemList.adamantineBar:
                case ItemList.glimmerSteelBar:
                case ItemList.seryllBar:
                    break;
                default:
                    performer.getCommunicator().sendNormalServerMessage("You need to target a lump to smelt it.");
                    return true;
            }
        }
        Item forge = this.getTargetItem().getTopParentOrNull();
        boolean notInForge = Objects.equals(forge, null) || forge.getTemplateId() != ItemList.forge;
        if (notInForge){
            performer.getCommunicator().sendNormalServerMessage("You can only purify lumps inside a forge.");
            return true;
        }
        boolean notHotEnough = this.targetItem.getTemperature() < 6000;
        if (notHotEnough) {
            performer.getCommunicator().sendNormalServerMessage("The lump isn't hot enough.");
            return true;
        }
        int lumpGrams = this.targetItem.getWeightGrams();
        double lumpGramsSmelted = ConfigureOptions.getInstance().getWeightSmelted();
        double lumpGramsLightestResult = ConfigureOptions.getInstance().getLightestResult();
        boolean lumpWeightInsufficient = lumpGrams / (lumpGramsSmelted * 1000) < lumpGramsLightestResult;
        if (lumpWeightInsufficient) {
            String errorMessage = String.format("The smelted lump's weigh of %.2f kg would be less then the minimum of %.2f kg.",
                    this.targetItem.getWeightGrams() / (lumpGramsSmelted * 1000), lumpGramsLightestResult);
            performer.getCommunicator().sendNormalServerMessage(errorMessage);
            return true;
        }
        double modifiedKnowledge = Math.min(100, performer.getSkills().getSkillOrLearn(SkillList.SMITHING_METALLURGY)
                .getKnowledge(this.getActiveTool(), 0));
        double qualityIncrease = ConfigureOptions.getInstance().getQualityIncrease();
        boolean isNotSkilledEnough = modifiedKnowledge < (this.getTargetItem().getQualityLevel() + qualityIncrease);
        if (isNotSkilledEnough) {
            performer.getCommunicator().sendNormalServerMessage(
                    String.format("Smelting up to %.2f quality won't work. That smelting pot and your metallurgy skill will smelt up to %.2f quality.",
                            this.targetItem.getQualityLevel() + qualityIncrease, modifiedKnowledge));
            return true;
        }
        return false;
    }



    @Override
    public Item getActiveTool() {
        return activeTool;
    }

    @Override
    public Item getTargetItem() {
        return targetItem;
    }

    @Override
    public TilePos getTargetTile() {
        return null;
    }
}
