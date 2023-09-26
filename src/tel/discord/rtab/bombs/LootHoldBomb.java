package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

public class LootHoldBomb implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		//If they don't have anything to keep, fall back to normal bomb code
		if(game.players.get(victim).booster == 100 && game.players.get(victim).boostCharge == 0 
				&& game.players.get(victim).games.isEmpty())
			Bomb.super.explode(game, victim, penalty);
		else
		{
			game.channel.sendMessage("It holds your boost and minigames, then goes **BOOM**. "
					+ String.format("$%,d lost as penalty.",Math.abs(penalty))).queue();
			StringBuilder extraResult = game.players.get(victim).blowUp(penalty,true);
			if(extraResult != null)
				game.channel.sendMessage(extraResult).queue();
		}
	}
}
