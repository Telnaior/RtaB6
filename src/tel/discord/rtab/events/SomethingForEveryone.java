package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.RtaBMath;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.HiddenCommand;

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
				try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
				int rng = (int)(100 * RtaBMath.random());
				//determine random chance here
				//The percentages can change, and other stuff can be added
				if(rng <= 0)
				{
					//hidden command?
					if (nextPlayer.hiddenCommand == HiddenCommand.NONE)
					{
						game.channel.sendMessage(nextPlayer.getSafeMention() +
							" gets **a Hidden Command**!").queue();
						nextPlayer.awardHiddenCommand();
					}
					else
					{		
						//cash backup
						int cashGiven = game.applyBaseMultiplier(50_000 + (int)(50_001 * RtaBMath.random())) * game.players.size() / game.playersAlive;
						nextPlayer.addMoney(cashGiven, MoneyMultipliersToUse.BOOSTER_ONLY);
						game.channel.sendMessage(nextPlayer.getSafeMention() +
						" gets **" +
						String.format("$%,d",cashGiven) + "**!").queue();
					}
				}
				else if(rng <= 1)
				{
					//peek?
					game.channel.sendMessage(nextPlayer.getSafeMention() +
						" gets **an Extra Peek**!").queue();
					nextPlayer.peeks++;
				}
				else if(rng <= 2)
				{
					//Million
						game.channel.sendMessage(nextPlayer.getSafeMention() +
						" gets **$1,000,000**!").queue();
						nextPlayer.addMoney(1_000_000, MoneyMultipliersToUse.NOTHING);
				}
				else if(rng <= 5)
				{
					//annuity
					int annuityTurns = (int)(RtaBMath.random()*6 + 5);
					int annuityValue = (int)(RtaBMath.random()*5001 + 5000);
					game.channel.sendMessage(nextPlayer.getSafeMention() +
					" gets **" + annuityTurns + " of " +
					String.format("$%,d",annuityValue) + " annuity**!").queue();						
					nextPlayer.addAnnuity(annuityValue, annuityTurns);
				}
				else if(rng <= 9)
				{
					//streak
					int streakAwarded = (int)(RtaBMath.random()*6 + 2);
					game.channel.sendMessage(nextPlayer.getSafeMention() +
					" gets **a +0." + streakAwarded + " Streak Bonus**!").queue();
					nextPlayer.addWinstreak(streakAwarded);
				}
				else
				{
					rng = (int)(100 * RtaBMath.random());
					if(rng <= 39)
					{
						//cash
						int cashGiven = game.applyBaseMultiplier(50_000 + (int)(50_001 * RtaBMath.random())) * game.players.size() / game.playersAlive;
						nextPlayer.addMoney(cashGiven, MoneyMultipliersToUse.BOOSTER_ONLY);
						game.channel.sendMessage(nextPlayer.getSafeMention() +
						" gets **" +
						String.format("$%,d",cashGiven) + "**!").queue();
					}
					else if(rng <= 75)
					{
						//boost
						int boostGiven = 25 + (int)(26 * RtaBMath.random());
						game.channel.sendMessage(nextPlayer.getSafeMention() +
						" gets **a +" + boostGiven + "% Booster**!").queue();
						nextPlayer.addBooster(boostGiven);
					}
					else
					{
						//minigame
						Game chosenGame = nextPlayer.generateEventMinigame();
						nextPlayer.addGame(chosenGame);
						game.channel.sendMessage(nextPlayer.getSafeMention() +
								" get **a copy of " + chosenGame.getName() + "**!").queue();
					}
				}
			}
		}
	}
	//Hi there, lots of curly brackets! Whoopsie, bad coding
	//Oh well!
}
