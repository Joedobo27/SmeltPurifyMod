package com.joedobo27.smeltpurifymod;

import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.*;


public class SmeltActionPerformer implements ModAction, ActionPerformer {

    private final short actionId;
    private final ActionEntry actionEntry;

    SmeltActionPerformer(short actionId, ActionEntry actionEntry) {
        this.actionId = actionId;
        this.actionEntry = actionEntry;
    }

    private static class SingletonHelper {
        private static final SmeltActionPerformer _performer;
        static {
            _performer = new SmeltActionPerformer( Actions.SMELT, Actions.actionEntrys[Actions.SMELT]);
        }
    }

    @Override
    public short getActionId(){
        return actionId;
    }

    ActionEntry getActionEntry() {
        return actionEntry;
    }

    @Override
    public boolean action(Action action, Creature performer, Item active, Item target, short aActionId, float counter) {

        SmeltAction smeltAction = SmeltAction.getSmeltAction(action);
        if (smeltAction == null) {
            ConfigureOptions.ActionOptions options = ConfigureOptions.getInstance().getSmeltAction();
            smeltAction = new SmeltAction(action, performer, active, SkillList.SMITHING_METALLURGY, options.getMinSkill(),
                    options.getMaxSkill(), options.getLongestTime(), options.getShortestTime(), options.getMinimumStamina(),
                    target);
        }

        if (smeltAction.isActionStartTime(counter) && smeltAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        if (smeltAction.isActionStartTime(counter)) {
            smeltAction.doActionStartMessages();
            smeltAction.setInitialTime(Actions.actionEntrys[Actions.HARVEST]);
            active.setDamage(active.getDamage() + 0.0015f * active.getDamageModifier());
            performer.getStatus().modifyStamina(-1000.0f);
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        if (!smeltAction.isActionTimedOut(action, counter)) {
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }
        if (smeltAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        Skill metallurgy = performer.getSkills().getSkillOrLearn(SkillList.SMITHING_METALLURGY);
        metallurgy.skillCheck(target.getTemplate().getDifficulty(), 0, false, counter);
        target.setWeight((int)(target.getWeightGrams() / (SmeltPurifyMod.weightSmelted * 1000) * 1000), true);
        target.setQualityLevel((int)(target.getQualityLevel() + SmeltPurifyMod.qualityIncrease));
        active.setDamage(active.getDamage() + active.getDamageModifier() * 0.002f);
        performer.getStatus().modifyStamina(-10000.0f);
        youMessage = String.format("You finish %s.", action.getActionString());
        performer.getCommunicator().sendNormalServerMessage(youMessage);
        broadcastMessage = String.format("%s finishes %s.", performer.getName(), action.getActionString());
        Server.getInstance().broadCastAction(broadcastMessage, performer, 5);
        return true;
    }

    static private int scaleTimeProportionalWeight(int time, Item lump) {
        int unitWeight = lump.getTemplate().getWeightGrams();
        double unitCount = lump.getWeightGrams() / unitWeight;
        return (int) (time * unitCount);
    }

    static SmeltActionPerformer getSmeltActionPerformer() {
        return SingletonHelper._performer;
    }
}
