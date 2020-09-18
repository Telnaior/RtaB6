package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

public class OneBuckBehind implements EventSpace
{

	@Override
	public void execute(GameController game, int player)
	{
		//Makes the player go one dollar behind whoever has the highest score so far this round.
		int highScore = 0;
		for(Player nextPlayer : game.players)
		{
			if(game.players.get(nextPlayer).money > highScore)
			{
				highScore = game.players.get(nextPlayer).money;
			}
		}
		if (highScore == 0)
		{
			game.channel.sendMessage("It's **One Buck Behind the Leader**! But since no one has money, we'll just give you **$100,000**!").queue();
			game.players.get(player).addMoney(100_000, MoneyMultipliersToUse.NONE);
		}
		else if (highScore == game.players.get(player).money)
		{
			game.channel.sendMessage("It's **One Buck Behind the Leader**! But since *you're* the leader, we'll just place you behind whoever was in second place!").queue();
			highScore = 0; //We have to find second place now
			for(Player nextPlayer : game.players)
			{
				if(game.players.get(nextPlayer).money != game.players.get(player).money)
				{
					highScore = game.players.get(nextPlayer).money;
				}
			}
			game.players.get(player).addMoney(-1 * (game.players.get(nextPlayer).money + 1 - highScore), MoneyMultipliersToUse.NONE);			
		}
		else
		{
			game.channel.sendMessage("It's **One Buck Behind the Leader**! Your round score is now one dollar behind the player with the most money!").queue();
			game.players.get(player).addMoney((highScore - game.players.get(player).money) - 1, MoneyMultipliersToUse.NONE);			
		}
	}
	
}
