package com.joedobo27.smeltpurifymod;


import com.wurmonline.server.Servers;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.RuneUtilities;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmeltPurifyMod implements WurmServerMod, ServerStartedListener, Configurable{

    private static final Logger logger = Logger.getLogger(SmeltPurifyMod.class.getName());
    private static final String[] STEAM_VERSION = new String[]{"1.3.1.3"};
    private static boolean versionCompliant = false;
    private static double minimumUnitActionTime;
    static double qualityIncrease;
    static double weightSmelted;
    static double lightestResult;

    @Override
    public void configure(Properties properties) {
        qualityIncrease = Double.parseDouble(properties.getProperty("qualityIncrease", Double.toString(qualityIncrease)));
        weightSmelted = Double.parseDouble(properties.getProperty("weightSmelted", Double.toString(weightSmelted)));
        minimumUnitActionTime = Double.parseDouble(properties.getProperty("minimumUnitActionTime", Double.toString(minimumUnitActionTime)));
        lightestResult = Double.parseDouble(properties.getProperty("lightestResult", Double.toString(lightestResult)));
        if (Arrays.stream(STEAM_VERSION)
                .filter(s -> Objects.equals(s, properties.getProperty("steamVersion", null)))
                .count() > 0)
            versionCompliant = true;
        else
            logger.log(Level.WARNING, "WU version mismatch. Your " + properties.getProperty(" steamVersion", null)
                    + "version doesn't match one of SmeltPurifyMod's required versions " + Arrays.toString(STEAM_VERSION));
    }

    @Override
    public void onServerStarted() {
        if (!versionCompliant)
            return;
        ModActions.registerAction(new SmeltAction());
    }


    /**
     * It shouldn't be necessary to have a fantastic, 104woa, speed rune, 99ql, 99 skill in order to get the fastest time.
     * Aim for just skill as getting close to shortest time and the other boosts help at lower levels but aren't needed to have
     * the best at end game.
     */
    static double getBaseUnitActionTime(Item activeTool, Creature performer, Action action){
        final double MAX_WOA_EFFECT = 0.20;
        final double TOOL_RARITY_EFFECT = 0.1;
        final double ACTION_RARITY_EFFECT = 0.33;
        final double MAX_SKILL = 100.0d;
        double time;
        double modifiedKnowledge = Math.min(MAX_SKILL, performer.getSkills().getSkillOrLearn(SkillList.SMITHING_METALLURGY)
                .getKnowledge(activeTool, 0));
        time = Math.max(minimumUnitActionTime, (160.0 - modifiedKnowledge) * 1.3f / Servers.localServer.getActionTimer());

        // woa
        if (activeTool != null && activeTool.getSpellSpeedBonus() > 0.0f)
            time = Math.max(minimumUnitActionTime, time * (1 - (MAX_WOA_EFFECT * activeTool.getSpellSpeedBonus() / 100.0)));
        //rare item, 10% speed reduction per rarity level.
        if (activeTool != null && activeTool.getRarity() > 0)
            time = Math.max(minimumUnitActionTime, time * (1 - (activeTool.getRarity() * TOOL_RARITY_EFFECT)));
        //rare action, 33% speed reduction per rarity level.
        if (action.getRarity() > 0)
            time = Math.max(minimumUnitActionTime, time * (1 - (action.getRarity() * ACTION_RARITY_EFFECT)));
        // rune effects
        if (activeTool != null && activeTool.getSpellEffects() != null && activeTool.getSpellEffects().getRuneEffect() != -10L)
            time = Math.max(minimumUnitActionTime, time * (1 - RuneUtilities.getModifier(activeTool.getSpellEffects().getRuneEffect(), RuneUtilities.ModifierEffect.ENCH_USESPEED)));
        return time;
    }
}
