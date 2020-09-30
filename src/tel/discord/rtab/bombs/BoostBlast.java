package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

public class BoostBlast implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		game.channel.sendMessage("It goes **BOOM**...").queue();
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		if (game.players.get(victim).booster > 100)
		{
			int excessBoost = game.players.get(victim).booster - 100;
			int livingPlayers = 0;
			for(Player nextPlayer : game.players)
			{
				if(nextPlayer.status == PlayerStatus.ALIVE)
				{
					livingPlayers++;
				}
			}
			livingPlayers--; //account for about-to-be-blown-up player
			int boostPerPlayer = excessBoost / livingPlayers;
			if (boostPerPlayer < 1)
			{
				boostPerPlayer = 1; //give a minimum if there is /some/ boost
			}		
			game.channel.sendMessage(String.format("And blasts their boost between the players! $%,d% boost awarded to living players!",boostPerPlayer)).queue();
			for(Player nextPlayer : game.players)
			{
				if(nextPlayer.status == PlayerStatus.ALIVE && nextPlayer != victim)
				{
					game.players.get(nextPlayer).addBooster(boostPerPlayer);
				}
			}	
		}
			game.channel.sendMessage(String.format("$%,d lost as penalty.",Math.abs(penalty))).queue();
			StringBuilder extraResult = game.players.get(victim).blowUp(penalty,false);
			if(extraResult != null)
				game.channel.sendMessage(extraResult).queue();
	}
}
