package club.frozed.tablist.entry;

import club.frozed.tablist.skin.Skin;

/**
 * Created by Ryzeon
 * Project: Hatsur TabAPI
 * Date: 12/10/2020 @ 08:36
 */
public class TabEntry {

	private final int column;
	private final int row;
	private final String text;

	private int ping = 1;
	private Skin skin = Skin.DEFAULT_SKIN;

	public TabEntry(int column, int row, String text, int ping, Skin skin) {
		this.column = column;
		this.row = row;
		this.text = text;
		this.ping = ping;
		this.skin = skin;
	}

	public TabEntry(int column, int row, String text) {
		this.column = column;
		this.row = row;
		this.text = text;
	}

	public int getColumn() {
		return column;
	}

	public int getRow() {
		return row;
	}

	public String getText() {
		return text;
	}

	public int getPing() {
		return ping;
	}

	public void setPing(int ping) {
		this.ping = ping;
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		this.skin = skin;
	}
}