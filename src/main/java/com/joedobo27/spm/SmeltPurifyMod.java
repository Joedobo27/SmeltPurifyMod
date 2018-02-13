package com.joedobo27.spm;


import com.wurmonline.server.creatures.Communicator;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.Properties;
import java.util.logging.Logger;

public class SmeltPurifyMod implements WurmServerMod, ServerStartedListener, Configurable, PlayerMessageListener {

    static final Logger logger = Logger.getLogger(SmeltPurifyMod.class.getName());

    @Override public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        if (communicator.getPlayer().getPower() == 5 && message.startsWith("/SmeltPurifyMod properties")) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Reloading properties for SmeltPurifyMod."
            );
            ConfigureOptions.resetOptions();
            return MessagePolicy.DISCARD;
        }
        return MessagePolicy.PASS;
    }

    @Override
    public void configure(Properties properties) {
        ConfigureOptions.setOptions(properties);
    }

    @Override
    public void onServerStarted() {
        SmeltActionPerformer smeltActionPerformer = SmeltActionPerformer.getSmeltActionPerformer();
        ModActions.registerAction(smeltActionPerformer);
    }
}
