package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.RtaBMath;

public class SomethingForEveryone implements EventSpace
{
	@Override
	public String getName()
	{
		return "Something for Everyone";
	}

	@Override
	public void execute(GameController game, int player)
	{
		if(game.tiebreakMode)
		{
			game.channel.sendMessage("It's **Something for Everyone**!").queue();
			try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			game.channel.sendMessage("That \"Something\" is a chance to win the Season. Good luck!").queue();
			return;
		}	
		game.channel.sendMessage("It's **Something for Everyone**!").queue();

		for(Player nextPlayer : game.players)
		{
			if(nextPlayer.status == PlayerStatus.ALIVE)
			{
				switch (100 * RtaBMath.random())
				{
						
				//determine random chance here
					case 0 ->
					{
						//hidden command
					}
					case 1 ->
					{
						//peek
					}
					case 2 ->
					{
						//one buck behind
					}
					case 3 to 5 ->
					{
						//annuity
					}
					case 6 to 9 ->
					{
						//streak
					}
					case else ->
					{
						switch (100 * RtaBMath.random())
						{
							case 0 to 39 ->
							{
								//cash
							}
							case 40 to 75 ->
							{
								//boost
							}
							case 76 to 99 ->
							{
								//minigame
							}
						}	
					}
				}
			}
		}
	//nextPlayer.awardHiddenCommand();
        //int cashGiven = game.applyBaseMultiplier(50_000 + (int)(50_001 * RtaBMath.random())) * game.players.size() / game.playersAlive;
	//nextPlayer.addMoney(cashGiven, MoneyMultipliersToUse.BOOSTER_ONLY);
	}

}
