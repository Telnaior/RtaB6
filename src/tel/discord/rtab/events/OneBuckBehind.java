package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

public class OneBuckBehind implements EventSpace
{
	private static final String ITS_ONE_BUCK_BEHIND = "It's **One Buck Behind the Leader**! ";

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
			if(nextPlayer.getRoundDelta() > highScore)
			{
				highScore = nextPlayer.getRoundDelta();
			}
		}
		if (highScore == 0)
		{
			int backupMoney = game.applyBaseMultiplier(100_000);
			game.channel.sendMessage(ITS_ONE_BUCK_BEHIND
					+ "But since no one has money this round, "
					+ String.format("we'll just give you **$%,d**!",backupMoney)).queue();
			game.players.get(player).addMoney(backupMoney, MoneyMultipliersToUse.NOTHING);
		}
		else if (highScore == game.players.get(player).getRoundDelta())
		{
			int playerChosen = 0;
			int lowScore = 1_000_000_001; //now we need a low scorer, this time in overall standings terms
			for(int i=0; i<game.players.size(); i++)
				if(game.players.get(i).money < lowScore && i != player && game.players.get(i).status == PlayerStatus.ALIVE)
				{
					playerChosen = i;
					lowScore = game.players.get(i).money;
				}
			game.channel.sendMessage(ITS_ONE_BUCK_BEHIND
					+ "But since *you're* the leader, we'll just place **"
					+ game.players.get(playerChosen).getName() + "** in front of you!").queue();
			game.players.get(playerChosen).resetRoundDelta();
			game.players.get(playerChosen).addMoney(highScore + 1, MoneyMultipliersToUse.NOTHING);			
		}
		else
		{
			game.channel.sendMessage(ITS_ONE_BUCK_BEHIND
					+ "Your round score is now one dollar behind the player with the most money!").queue();
			game.players.get(player).resetRoundDelta();
			game.players.get(player).addMoney(highScore - 1, MoneyMultipliersToUse.NOTHING);			
		}
	}

}
