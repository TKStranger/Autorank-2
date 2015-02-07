package me.armar.plugins.autorank.playerchecker.requirement;

import me.armar.plugins.autorank.language.Lang;
import me.armar.plugins.autorank.statsmanager.handlers.StatsHandler;

import org.bukkit.entity.Player;

public class PlayerKillsRequirement extends Requirement {

	private int totalPlayersKilled = 0;

	@Override
	public String getDescription() {
		return Lang.PLAYER_KILLS_REQUIREMENT
				.getConfigValue(new String[] { totalPlayersKilled + "" });
	}

	@Override
	public String getProgress(final Player player) {
		String progress = "";
		progress = progress.concat(getStatsPlugin().getNormalStat(
				StatsHandler.statTypes.PLAYERS_KILLED.toString(),
				player.getUniqueId())
				+ "/" + totalPlayersKilled);
		return progress;
	}

	@Override
	public boolean meetsRequirement(final Player player) {
		return getStatsPlugin().isEnabled()
				&& getStatsPlugin().getNormalStat(
						StatsHandler.statTypes.PLAYERS_KILLED.toString(),
						player.getUniqueId()) >= totalPlayersKilled;
	}

	@Override
	public boolean setOptions(final String[] options) {
		try {
			totalPlayersKilled = Integer.parseInt(options[0]);
			return true;
		} catch (final Exception e) {
			totalPlayersKilled = 0;
			return false;
		}
	}
}
