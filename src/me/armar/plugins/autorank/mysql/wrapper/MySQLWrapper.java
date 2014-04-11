package me.armar.plugins.autorank.mysql.wrapper;

import java.util.HashMap;
import java.util.UUID;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.data.SQLDataStorage;
import me.armar.plugins.autorank.data.SimpleYamlConfiguration;

import org.bukkit.configuration.ConfigurationSection;

/**
 * This class keeps all incoming and outgoing under control.
 * It sends MySQL queries and can locate the database.
 * Previously, {@link me.armar.plugins.autorank.playtimes.Playtimes} kept all
 * MySQL, but it wasn't neatly organised.
 * MySQLWrapper class is (hopefully) fail-prove and organised.
 * 
 * This also has a fail-safe when two queries are altering at the same time.
 * 
 * @author Staartvin
 * 
 */
public class MySQLWrapper {

	private final Autorank plugin;
	private SQLDataStorage mysql;
	String hostname, username, password, database, table;
	// Database time
	int databaseTime = 0;
	// This thread will be used to check if the database time has been retrieved.
	Thread timeThread;
	
	// Keeps track of when a call to the database was for this player
	private HashMap<String, Long> lastChecked = new HashMap<String, Long>();
	// Stores the last received global time for a player
	private HashMap<String, Integer> lastReceivedTime = new HashMap<String, Integer>();

	public MySQLWrapper(final Autorank instance) {
		plugin = instance;

		sqlSetup(plugin.getAdvancedConfig());
		if (mysql != null) {
			setupTable();
		}
	}

	public void setupTable() {
		// Check if connection is still alive
		if (mysql.isClosed()) {
			mysql.connect();
		}

		final String statement = "CREATE TABLE  IF NOT EXISTS " + table + " "
				+ "(uuid TINYTEXT not NULL, " + " time INTEGER not NULL, "
				+ " modified TIMESTAMP not NULL, " + " PRIMARY KEY ( uuid ))";

		// Run async to prevent load issues.
		plugin.getServer().getScheduler()
				.runTaskAsynchronously(plugin, new Runnable() {

					@Override
					public void run() {
						mysql.execute(statement);
					}
				});

	}

	/**
	 * Because the MySQL queries are done async, we need to wait for the result.
	 * Otherwise it would be cached and out of date.
	 * This waits for the thread to die and then it will continue
	 * Use this whenever you do an async MySQL thread.
	 */
	private void waitForThread(final Thread thread) {
		if (thread.isAlive()) {
			try {
				thread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void sqlSetup(final SimpleYamlConfiguration config) {
		final ConfigurationSection s = config.getConfigurationSection("sql");

		if (s == null) {
			plugin.getLogger().warning("MySQL options are missing in the advancedconfig.yml!");
			return;
		}
		
		final Boolean enabled = s.getBoolean("enabled");
		if (enabled != null && enabled) {

			hostname = s.getString("hostname");
			username = s.getString("username");
			password = s.getString("password");
			database = s.getString("database");
			table = s.getString("table");

			mysql = new SQLDataStorage(hostname, username, password, database);
			if (!mysql.connect()) {
				mysql = null;
				plugin.getLogger().severe("Could not connect to " + hostname);
			} else {
				plugin.getLogger().info(
						"Successfully established connection to " + hostname);
			}
		}
	}

	/**
	 * Gets the database time of player
	 * Run this ASYNC!
	 * <p>
	 * This will return an updated value every 5 minutes. Calling it every minute isn't smart,
	 * as it will only update every 5 minutes.
	 * 
	 * @param name Playername to get the time of
	 * @return time player has played across all servers
	 */
	public int getDatabaseTime(final String name) {
		
		// Do not make a call to the database every time.
		// Instead, only call once every 5 minutes.
		if (!isOutOfDate(name)) {
			return getCachedGlobalTime(name);
		}
		
		// Mysql is not enabled
		if (!isMySQLEnabled()) return -1;
		
		// Check if connection is still alive
		if (mysql.isClosed()) {
			mysql.connect();
		}
		// Retrieve database time
		timeThread = new Thread(new TimeRunnable(plugin, this, mysql, name, table));
		timeThread.start();

		// Wait for thread to finish
		waitForThread(timeThread);
		
		// Store last received time and last received value
		lastChecked.put(name, System.currentTimeMillis());
		lastReceivedTime.put(name, databaseTime);

		return databaseTime;
	}
	
	public boolean isOutOfDate(String playerName) {
		// Checks whether the last check was five minutes ago.
		// When the last check was more than five minutes ago,
		// the database time is 'outdated'
		
		// Never checked
		if (!lastChecked.containsKey(playerName)) {
			return true;
		}
		
		long currentTime = System.currentTimeMillis();
		
		long lastCheckedTime = lastChecked.get(playerName);
		
		// Weird time received.
		if (lastCheckedTime <= 0) {
			return true;
		}
		
		// Get the difference in minutes
		if ((currentTime - lastCheckedTime) / 60000 >= 5) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the cached value of the global time.
	 * @param playerName Name of the player
	 * @return cached global time or -1 if nothing was cached.
	 */
	public Integer getCachedGlobalTime(String playerName) {
		if (!lastReceivedTime.containsKey(playerName)) {
			return -1;
		}
		
		int cached = lastReceivedTime.get(playerName);
		
		// Weird cached
		if (cached <= 0) {
			return -1;
		}
		
		return cached;
	}

	/**
	 * Sets the time of a player
	 * 
	 * @param playerName Player to set the time of
	 * @param time Time to change to
	 */
	public void setGlobalTime(final String playerName, final int time) {
		
		if (!isMySQLEnabled()) return;
		
		// Check if connection is still alive
		if (mysql.isClosed()) {
			mysql.connect();
		}
		
		UUID uuid = plugin.getUUIDManager().getUUIDFromPlayer(playerName);

		final String statement = "INSERT INTO " + table + " VALUES ('"
				+ uuid.toString() + "', " + time + ", CURRENT_TIMESTAMP) "
				+ "ON DUPLICATE KEY UPDATE " + "time=" + time;

		// Run async to prevent load issues.
		plugin.getServer().getScheduler()
				.runTaskAsynchronously(plugin, new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						mysql.execute(statement);
					}
				});
	}

	public boolean isMySQLEnabled() {
		return mysql != null;
	}

	public String getDatabaseName() {
		return database;
	}
}
