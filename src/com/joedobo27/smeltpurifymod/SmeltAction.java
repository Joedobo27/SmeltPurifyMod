package com.joedobo27.smeltpurifymod;

import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;


@SuppressWarnings("unused")
public class SmeltAction implements ModAction, BehaviourProvider, ActionPerformer {

    private static final Logger logger = Logger.getLogger(SmeltPurifyMod.class.getName());
    private final short actionId;
    private final ActionEntry actionEntry;

    SmeltAction() {
        this.actionId = Actions.SMELT;
        this.actionEntry = Actions.actionEntrys[Actions.SMELT];
    }

    @Override
    public short getActionId(){
        return actionId;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item smeltPot, Item lump) {
        if (smeltPot.getTemplateId() == ItemList.smeltingPot && isTargetMetalLump(lump)) {
            return Collections.singletonList(this.actionEntry);
        }else {
            return BehaviourProvider.super.getBehavioursFor(performer, smeltPot, lump);
        }
    }

    @Override
    public boolean action(Action action, Creature performer, Item smeltPot, Item lump, short aActionId, float counter) {
        if (aActionId == actionId && smeltPot.getTemplateId() == ItemList.smeltingPot && isTargetMetalLump(lump)) {
            final float TIME_TO_COUNTER_DIVISOR = 10.0f;
            final float ACTION_START_TIME = 1.0f;
            String youMessage;
            String broadcastMessage;
            if (counter == ACTION_START_TIME) {
                if (hasAFailureCondition(action, performer, smeltPot, lump, aActionId)) {
                    return true;
                }
                youMessage = String.format("You start %s.", action.getActionEntry().getVerbString());
                broadcastMessage = String.format("%s starts to %s.", performer.getName(), action.getActionString());
                performer.getCommunicator().sendNormalServerMessage(youMessage);
                Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
                int time = (int) SmeltPurifyMod.getBaseUnitActionTime(smeltPot, performer, action);
                action.setTimeLeft(time);
                performer.sendActionControl(action.getActionEntry().getVerbString(), true, time);
                performer.getStatus().modifyStamina(-1000.0f);
                return false;
            }
            boolean hasTimeCompleted = counter - 1 > action.getTimeLeft() / TIME_TO_COUNTER_DIVISOR;
            if (hasTimeCompleted) {
                if (hasAFailureCondition(action, performer, smeltPot, lump, aActionId)) {
                    return true;
                }
                Skill metallurgy = performer.getSkills().getSkillOrLearn(SkillList.SMITHING_METALLURGY);
                metallurgy.skillCheck(lump.getTemplate().getDifficulty(), 0, false, counter);
                lump.setWeight((int)(lump.getWeightGrams() / (SmeltPurifyMod.weightSmelted * 1000) * 1000), true);
                lump.setQualityLevel((int)(lump.getQualityLevel() + SmeltPurifyMod.qualityIncrease));
                smeltPot.setDamage(smeltPot.getDamage() + smeltPot.getDamageModifier() * 0.002f);
                performer.getStatus().modifyStamina(-10000.0f);
                return true;
            }
            return false;
        }
        return ActionPerformer.super.action(action, performer, smeltPot, lump, aActionId, counter);
    }

    static private boolean hasAFailureCondition(Action action, Creature performer, Item smeltPot, Item lump, short aActionId) {

        Item forge = lump.getTopParentOrNull();
        boolean notInForge = Objects.equals(forge, null) || forge.getTemplateId() != ItemList.forge;
        if (notInForge){
            performer.getCommunicator().sendNormalServerMessage("You can only purify lumps inside a forge.");
            return true;
        }
        boolean notHotEnough = lump.getTemperature() < 6000;
        if (notHotEnough) {
            performer.getCommunicator().sendNormalServerMessage("The lump isn't hot enough.");
            return true;
        }
        boolean lumpWeightInsufficient = lump.getWeightGrams() / (SmeltPurifyMod.weightSmelted * 1000) < SmeltPurifyMod.lightestResult;
        if (lumpWeightInsufficient) {
            String errorMessage = String.format("The smelted lump's weigh of %.2f kg would be less then the minimum of %.2f kg.",
                    lump.getWeightGrams() / (SmeltPurifyMod.weightSmelted * 1000), SmeltPurifyMod.lightestResult);
            performer.getCommunicator().sendNormalServerMessage(errorMessage);
            return true;
        }
        double modifiedKnowledge = Math.min(100, performer.getSkills().getSkillOrLearn(SkillList.SMITHING_METALLURGY)
                .getKnowledge(smeltPot, 0));
        boolean isNotSkilledEnough = modifiedKnowledge < (lump.getQualityLevel() + SmeltPurifyMod.qualityIncrease);
        if (isNotSkilledEnough) {
            performer.getCommunicator().sendNormalServerMessage(
                    String.format("Smelting up to %.2f quality won't work. That smelting pot and your metallurgy skill will smelt up to %.2f quality.",
                            lump.getQualityLevel() + SmeltPurifyMod.qualityIncrease, modifiedKnowledge));
            return true;
        }
        return false;
    }

    static private boolean isTargetMetalLump(Item lump) {
        switch (lump.getTemplateId()){
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
                return true;
            default:
                return false;
        }
    }
}
