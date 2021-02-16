package tel.discord.rtab.bombs;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.GameController;
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
			//We deliberately give the blown-up player a share to be wasted, to make extreme outcomes less likely
			int livingPlayers = game.playersAlive;
			int streakPerPlayer = excessStreak / livingPlayers; 
			if (streakPerPlayer < 1)
			{
				streakPerPlayer = 1; //give a minimum if there is /some/ streak
			}
			game.channel.sendMessage("And it blasts their streak between the players! "
					+ String.format("+%1$d.%2$dx streak awarded to living players!", streakPerPlayer/10, streakPerPlayer%10)).queue();
			for(int i=0; i<game.players.size(); i++)
			{
				if(game.players.get(i).status == PlayerStatus.ALIVE && i != victim)
				{
					game.players.get(i).addWinstreak(streakPerPlayer);
					if(streakPerPlayer >= 40)
						Achievement.STREAK_BLAST.award(game.players.get(i));
				}
			}	
		}
			game.channel.sendMessage(String.format("$%,d lost as penalty.",Math.abs(penalty))).queue();
			StringBuilder extraResult = game.players.get(victim).blowUp(penalty,false);
			if(extraResult != null)
				game.channel.sendMessage(extraResult).queue();
	}
}
