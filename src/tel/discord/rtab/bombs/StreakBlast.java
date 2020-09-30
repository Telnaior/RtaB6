package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

public class StreakBlast implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		game.channel.sendMessage("It goes **BOOM**...").queue();
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		if (game.players.get(victim).winstreak > 10)
		{
			int excessStreak = game.players.get(victim).winstreak - 10;
			int livingPlayers = 0;
			for(Player nextPlayer : game.players)
			{
				if(nextPlayer.status == PlayerStatus.ALIVE)
				{
					livingPlayers++;
				}
			}
			//We deliberately give the blown-up player a share to be wasted, to make extreme outcomes less likely
			int streakPerPlayer = excessStreak / livingPlayers; 
			if (streakPerPlayer < 1)
			{
				streakPerPlayer = 1; //give a minimum if there is /some/ streak
			}
			game.channel.sendMessage(String.format("And it blasts their streak between the players! +%1d%.%2d%x streak awarded to living players!",streakPerPlayer/10, streakPerPlayer%10)).queue();
			for(int i=0; i<game.players.size(); i++)
			{
				if(game.players.get(i).status == PlayerStatus.ALIVE && i != victim)
				{
					game.players.get(i).winstreak += streakPerPlayer;
				}
			}	
		}
			game.channel.sendMessage(String.format("$%,d lost as penalty.",Math.abs(penalty))).queue();
			StringBuilder extraResult = game.players.get(victim).blowUp(penalty,false);
			if(extraResult != null)
				game.channel.sendMessage(extraResult).queue();
	}
}
