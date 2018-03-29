package com.joedobo27.spm;

import com.joedobo27.libs.LinearScalingFunction;
import com.joedobo27.libs.action.ActionMaster;
import com.wurmonline.math.TilePos;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.RuneUtilities;
import com.wurmonline.server.skills.NoSuchSkillException;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.shared.util.MaterialUtilities;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.WeakHashMap;

public class SmeltAction extends ActionMaster {

    private final Item targetItem;
    private int timeBlockLast;
    private int lumpUnitTotal;
    private float initialGrams;
    private static WeakHashMap<Action, SmeltAction> performers = new WeakHashMap<>();

    SmeltAction(Action action, Creature performer, @Nullable Item activeTool, @Nullable Integer usedSkill,
                          int minSkill, int maxSkill, int longestTime, int shortestTime, int minimumStamina,
                          Item targetItem) {
        super(action, performer, activeTool, usedSkill, minSkill, maxSkill, longestTime, shortestTime, minimumStamina);
        this.targetItem = targetItem;
        this.timeBlockLast = 0;
        this.lumpUnitTotal = (int) Math.floor(targetItem.getWeightGrams() /
                targetItem.getTemplate().getWeightGrams());
        this.initialGrams = targetItem.getWeightGrams();
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
        double lumpGramsLightestResult = this.targetItem.getTemplate().getWeightGrams();
        boolean lumpWeightInsufficient = lumpGrams < lumpGramsLightestResult;
        if (lumpWeightInsufficient) {
            String errorMessage = String.format("The lump needs to weight at least %.2f kg to smelt it.",
                    lumpGramsLightestResult / 1000);
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
    public void setInitialTime(ActionEntry actionEntry) {
        double MAX_WOA_EFFECT = 0.20;
        double TOOL_RARITY_EFFECT = 0.10;
        double ACTION_RARITY_EFFECT = 0.33;

        Skill toolSkill = null;
        double bonus = 0;
        if (this.activeTool != null && this.activeTool.hasPrimarySkill()) {
            try {
                toolSkill = this.performer.getSkills().getSkillOrLearn(this.activeTool.getPrimarySkill());
            } catch (NoSuchSkillException ignored) {}
        }
        if (toolSkill != null) {
            bonus = toolSkill.getKnowledge() / 10;
        }

        double modifiedKnowledge;
        if (this.usedSkill == null)
            modifiedKnowledge = 99;
        else
            modifiedKnowledge = this.performer.getSkills().getSkillOrLearn(this.usedSkill).getKnowledge(this.activeTool,
                    bonus);
        LinearScalingFunction linearScalingFunction = LinearScalingFunction.make(this.minSkill, this.maxSkill,
                this.longestTime, this.shortestTime);
        double time = linearScalingFunction.doFunctionOfX(modifiedKnowledge);

        if (this.activeTool != null && this.activeTool.getSpellSpeedBonus() != 0.0f)
            time = Math.max(this.shortestTime, time * (1 - (MAX_WOA_EFFECT *
                    this.activeTool.getSpellSpeedBonus() / 100.0)));

        if (this.activeTool != null && this.activeTool.getRarity() != MaterialUtilities.COMMON)
            time = Math.max(this.shortestTime, time * (1 - (this.activeTool.getRarity() *
                    TOOL_RARITY_EFFECT)));

        if (this.action != null && this.action.getRarity() != MaterialUtilities.COMMON)
            time = Math.max(this.shortestTime, time * (1 - (this.action.getRarity() * ACTION_RARITY_EFFECT)));

        if (this.activeTool != null && this.activeTool.getSpellEffects() != null &&
                this.activeTool.getSpellEffects().getRuneEffect(RuneUtilities.ModifierEffect.ENCH_USESPEED) != -10L)
            time = Math.max(this.shortestTime, time * (1 -
                    this.activeTool.getSpellEffects().getRuneEffect(RuneUtilities.ModifierEffect.ENCH_USESPEED)));

        int timeInt = (int)time;
        double combinedUnits = this.targetItem.getWeightGrams() / this.targetItem.getTemplate().getWeightGrams();
        int combinedTime = (int) (timeInt * combinedUnits);
        if (this.action != null)
            this.action.setTimeLeft(combinedTime);
        this.performer.sendActionControl(actionEntry.getVerbString(), true, combinedTime);

        synchronized (this){
            this.actionTimeTenthSecond = timeInt;
        }
    }

    void doSkillCheckAndGetPower() {
        if (this.usedSkill == null)
            return;
        double difficulty = this.targetItem.getTemplate().getDifficulty();
        this.performer.getSkills().getSkillOrLearn(this.usedSkill).skillCheck(difficulty, this.activeTool,
                        0, false, this.actionTimeTenthSecond/10);
    }

    boolean unitTimeIncremented() {
        int timeBlockNew = (int)((action.getCounterAsFloat() - 1) * 10) / this.actionTimeTenthSecond;
        if (timeBlockNew != this.timeBlockLast) {
            synchronized (this) {
                this.timeBlockLast = timeBlockNew;
            }
            return true;
        }
        else
            return false;
    }

    void modifyLump() {
        if (this.targetItem.getDamage() > 0F){
            this.targetItem.setQualityLevel(this.targetItem.getQualityLevel() * (1 - (this.targetItem.getDamage() / 100)));
            this.targetItem.setDamage(0F);
        }
        int totalWeightSmeltedGrams = (int)(this.initialGrams - (this.initialGrams / ConfigureOptions.getInstance().getWeightSmeltedRatio()));
        float qualityIncrease = (float)ConfigureOptions.getInstance().getQualityIncrease();
        int fractionalGramsChange = totalWeightSmeltedGrams / this.lumpUnitTotal;
        float fractionalQualityChange = qualityIncrease / this.lumpUnitTotal;

        int newWeightGrams = this.targetItem.getWeightGrams() - fractionalGramsChange;
        this.targetItem.setWeight(newWeightGrams, true);

        float newQuality = this.targetItem.getCurrentQualityLevel() + fractionalQualityChange;
        this.targetItem.setQualityLevel(newQuality);
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
