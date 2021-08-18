package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

public class OneBuckBehind implements EventSpace
{
	@Override
	public String getName()
	{
		return "One Buck Behind the Leader";
	}

	@Override
	public void execute(GameController game, int player)
	{
		//Makes the player go one dollar behind whoever has the highest score so far this round.
		int highScore = 0;
		for(Player nextPlayer : game.players)
		{
			if(nextPlayer.money > highScore)
			{
				highScore = nextPlayer.money;
			}
		}
		if (highScore == game.players.get(player).money)
		{
			int playerChosen = 0;
			int lowScore = 1_000_000_001; //now we need a low scorer, this time in overall standings terms
			for(int i=0; i<game.players.size(); i++)
				if(game.players.get(i).money < lowScore && i != player && game.players.get(i).status == PlayerStatus.ALIVE)
				{
					playerChosen = i;
					lowScore = game.players.get(i).money;
				}
			game.channel.sendMessage("It's **One Buck Behind the Leader**! "
					+ "But since *you're* the leader, we'll just place **"
					+ game.players.get(playerChosen).getName() + "** in front of you!").queue();
			game.players.get(playerChosen).money = (highScore + 1);			
		}
		else
		{
			game.channel.sendMessage("It's **One Buck Behind the Leader**! "
					+ "Your score is now one dollar behind the player with the most money!").queue();
			game.players.get(player).money = (highScore - 1);			
		}
	}

}
