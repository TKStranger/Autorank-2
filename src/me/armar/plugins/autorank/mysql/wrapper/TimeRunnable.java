package me.armar.plugins.autorank.mysql.wrapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.data.SQLDataStorage;

/**
 * This will get the database time.
 * 
 * We have to wait for the thread to finish before we can get the results
 * Every database lookup will have to have its own thread.
 * 
 * @author Staartvin
 * 
 */
public class TimeRunnable implements Runnable {

	private final MySQLWrapper wrapper;
	private final Autorank plugin;
	private final SQLDataStorage mysql;
	private final String name, table;
	private int time = 0;

	public TimeRunnable(Autorank plugin, final MySQLWrapper wrapper, final SQLDataStorage mysql,
			final String name, final String table) {
		this.wrapper = wrapper;
		this.mysql = mysql;
		this.name = name;
		this.table = table;
		this.plugin = plugin;
	}

	@Override
	public void run() {
		if (mysql != null) {

			UUID uuid = plugin.getUUIDManager().getUUIDFromPlayer(name);
			
			final String statement = "SELECT * FROM " + table + " WHERE uuid='"
					+ uuid.toString() + "'";
			final ResultSet rs = mysql.executeQuery(statement);

			if (rs == null)
				time = -1;

			try {
				if (rs.next()) {
					time = rs.getInt(2);
				} else {
					time = -1;
				}

			} catch (final SQLException e) {
				System.out.println("SQLException: " + e.getMessage());
				System.out.println("SQLState: " + e.getSQLState());
				System.out.println("VendorError: " + e.getErrorCode());
			}
		}

		wrapper.databaseTime = time;
	}

}
