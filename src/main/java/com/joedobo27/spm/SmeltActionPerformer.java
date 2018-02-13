package com.joedobo27.spm;


import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
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
            smeltAction.setInitialTime(this.actionEntry);
            active.setDamage(active.getDamage() + 0.0015f * active.getDamageModifier());
            performer.getStatus().modifyStamina(-1000.0f);
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }


        if (!smeltAction.unitTimeIncremented())
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        performer.getStatus().modifyStamina(-10000.0f);
        if (smeltAction.isActionTimedOut(action, counter)) {
            smeltAction.doSkillCheckAndGetPower();
            smeltAction.modifyLump();
            smeltAction.doActionEndMessages();
            return true;
        }
        if (smeltAction.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        smeltAction.doSkillCheckAndGetPower();

        smeltAction.modifyLump();

        active.setDamage(active.getDamage() + active.getDamageModifier() * 0.002f);
        return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }

    static SmeltActionPerformer getSmeltActionPerformer() {
        return SingletonHelper._performer;
    }
}
