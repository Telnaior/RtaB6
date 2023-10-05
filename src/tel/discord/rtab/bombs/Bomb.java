package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

import java.security.SecureRandom;

public interface Bomb
{
	default void explode(GameController game, int victim, int penalty)
	{
		SecureRandom r = new SecureRandom();
		//Small chance of making them think something exciting is gonna happen
		if(r.nextDouble() < 0.05)
		{
			game.channel.sendMessage("It goes **BOOM**...").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			game.channel.sendMessage(String.format("$%,d lost as penalty.",Math.abs(penalty))).queue();
		}
		//But most of the time, just blow them up
		else
		{
			game.channel.sendMessage(String.format("It goes **BOOM**. $%,d lost as penalty.",Math.abs(penalty))).queue();
		}
		StringBuilder extraResult = game.players.get(victim).blowUp(penalty,false);
		if(extraResult != null)
			game.channel.sendMessage(extraResult).queue();
	}
}
