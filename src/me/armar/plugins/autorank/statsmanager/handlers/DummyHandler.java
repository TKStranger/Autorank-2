package me.armar.plugins.autorank.statsmanager.handlers;

import java.util.HashMap;
import java.util.UUID;

import me.armar.plugins.autorank.statsmanager.StatsPlugin;

/**
 * This class is used when no stats plugin is found.
 * This does not do anything, but it allows the stats plugin to be gone so that
 * there are no errors.
 * 
 * @author Staartvin
 * 
 */
public class DummyHandler extends StatsPlugin {

	@Override
	public int getNormalStat(statTypes statType, final UUID uuid, HashMap<String, Object> arguments) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

}
