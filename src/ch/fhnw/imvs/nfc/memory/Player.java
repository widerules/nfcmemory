/*
 * Copyright (c) 2012 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.imvs.nfc.memory;

public class Player implements Comparable<Player> {
	private String playerName;
	private int score;

	public Player(String playerName) {
		this.playerName = playerName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public void addPoints(int points) {
		score += points;
	}

	@Override
	public int compareTo(Player argument) {
		if (score < argument.score) {
			return -1;
		} else if (score > argument.score) {
			return 1;
		} else {
			return 0;
		}
	}
}
