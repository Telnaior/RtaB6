package tel.discord.rtab.games;

import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.Game;

abstract class PvPMiniGameWrapper extends MiniGameWrapper
{
	int opponent;
	boolean opponentEnhanced;

	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<String>();
		output.add(String.format("Welcome to %s, a PvP minigame!",getName()));
		output.addAll(getInstructions());
		sendSkippableMessages(output);
		//Decide Opponent
		if(players.size() == 1)
		{
			//If there's only one player somehow, automatically generate a bot for them
			players.add(new Player());
			opponent = 1;
			opponentEnhanced = enhanced; //The automatic AI doesn't get initialised properly so let's just set this here
			startPvPGame();
		}
		else if(!getCurrentPlayer().isBot && players.size() == 2)
		{
			//Bots don't use this so it falls through to the method that enables messages
			//If it's 2p, automatically designate the other player as the opponent
			opponent = 1-player;
			opponentEnhanced = isOpponentEnhanced();
			startPvPGame();
		}
		else
		{
			//Otherwise, the player gets to decide!
			sendMessage("First of all, name another player in this round to be your opponent for the game.");
			askForOpponent();
		}
	}
	
	/**
	 * Special copy of getInput() that handles bot input itself and points us to the findOpponent() method instead
	 * (maybe we should generalise getInput() more another time, but for now it's fine)
	 */
	void askForOpponent()
	{
		//If they're a bot, just get the next bot pick
		if(players.get(player).isBot)
		{
			//An AI will always choose the player other than itself with the lowest total cash bank
			int playerChosen = 0;
			int lowScore = 1_000_000_001;
			for(int i=0; i<players.size(); i++)
				if(players.get(i).money < lowScore && i != player)
				{
					playerChosen = i;
					lowScore = players.get(i).money;
				}
			chooseOpponent(playerChosen);
		}
		//Otherwise, ask for input
		else
		{
			ScheduledFuture<?> warnPlayer = timer.schedule(() -> 
			{
				channel.sendMessage(getCurrentPlayer().getSafeMention() + 
						", are you still there? One minute left!").queue();
			}, 120, TimeUnit.SECONDS);
			RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
					{
						return (e.getChannel().equals(channel) && e.getAuthor().equals(players.get(player).user));
					},
					//Parse it and call the method that does stuff
					e -> 
					{
						warnPlayer.cancel(false);
						timer.schedule(() -> findOpponent(e.getMessage().getContentStripped()), 500, TimeUnit.MILLISECONDS);
					},
					180,TimeUnit.SECONDS, () ->
					{
						channel.sendMessage(players.get(player).getName() + 
								" has gone missing. Cancelling their minigames.").queue();
						players.get(player).games.clear();
						awardMoneyWon(0);
					});
		}
	}
	
	private void findOpponent(String input)
	{
		boolean foundOpponent = false;
		//If it's a mention, parse it to the user ID
		if(event.getArgs().startsWith("<@"))
		{
			String opponentID = input.replaceAll("\\D","");
			for(int i=0; i<players.size(); i++)
				if(opponentID.equals(players.get(i).uID))
				{
					opponent = i;
					if(opponent != player)
						foundOpponent = true;
					break;
				}
		}
		//Otherwise, parse it by their name
		else
		{
			for(int i=0; i<players.size(); i++)
				if(input.equalsIgnoreCase(players.get(i).getName()))
				{
					opponent = i;
					if(opponent != player)
						foundOpponent = true;
					break;
				}
		}
		if(foundOpponent)
		{
			chooseOpponent(opponent);
		}
		else
			askForOpponent();
	}
	
	private void chooseOpponent(int opponent)
	{
		this.opponent = opponent;
		//Enable message sending if necessary
		if(!sendMessages && !players.get(opponent).isBot)
		{
			sendMessages = true;
			sendMessage(players.get(opponent).getSafeMention() + ", you've been chosen by " 
					+ players.get(player).getSafeMention() + String.format(" to play %s!",getName()));
			sendSkippableMessages(getInstructions());
		}
		opponentEnhanced = isOpponentEnhanced();
		startPvPGame();
	}
	
	private boolean isOpponentEnhanced()
	{
		if(opponent == -1)
			return false;
		return players.get(opponent).enhancedGames.contains(Game.TIC_TAC_BOMB); //This can break easily (but so can the entire minigame tbh)
	}
	
	/**
	 * This method will be run at the start of the minigame to tell the players what to do.
	 * @return linked list of strings containing minigame instructions
	 */
	abstract LinkedList<String> getInstructions();
	
	/**
	 * This method will be called once an opponent has been found, and should be used instead of startGame() to initialise PvP minigames.
	 */
	abstract void startPvPGame();
}
