package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

public class LockdownBomb implements Bomb
{
	static final int MULTIPLIER = 4;
	public void explode(GameController game, int victim, int penalty)
	{
		game.channel.sendMessage(String.format("It goes **BOOM**. $%,d lockdown penalty.",Math.abs(penalty*MULTIPLIER))).queue();
		StringBuilder extraResult = game.players.get(victim).blowUp(penalty*MULTIPLIER,false);
		if(extraResult != null)
			game.channel.sendMessage(extraResult).queue();
	}
}
