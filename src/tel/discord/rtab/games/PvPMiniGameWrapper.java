package tel.discord.rtab.games;

import static tel.discord.rtab.RaceToABillionBot.waiter;

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
	boolean playerTurn;
	boolean dummyBot = false;
	Status gameStatus = Status.PRE_GAME;
	enum Status { PRE_GAME, MID_GAME, END_GAME}

	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add(String.format("Welcome to %s, a PvP minigame!",getName()));
		output.addAll(getInstructions());
		sendSkippableMessages(output);
		//Decide Opponent
		if(players.size() == 1)
		{
			//If there's only one player somehow, generate a dummy bot
			initialiseWithDummy();
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
	
	void initialiseWithDummy()
	{
		opponent = players.size(); //We immediately add a new one to the list so this lines up
		players.add(new Player());
		dummyBot = true;
		opponentEnhanced = enhanced; //The automatic AI doesn't get initialised properly so let's just set this here
		startPvPGame();
	}
	
	/**
	 * Special copy of getInput() that handles bot input itself and points us to the findOpponent() method instead
	 * it also gets raw input rather than stripped input so we can process mentions
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
					channel.sendMessage(getCurrentPlayer().getSafeMention() +
							", are you still there? One minute left!").queue(), 120, TimeUnit.SECONDS);
			RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
							(e.getChannel().equals(channel) && e.getAuthor().equals(players.get(player).user)),
					//Parse it and call the method that does stuff
					e -> 
					{
						warnPlayer.cancel(false);
						timer.schedule(() -> findOpponent(e.getMessage().getContentRaw()), 500, TimeUnit.MILLISECONDS);
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
	
	//Player turn methods are optional but if you want them they're here
	Player getCurrentPlayer()
	{
		return players.get(playerTurn ? player : opponent);
	}
	void getCurrentPlayerInput()
	{
		getInput(playerTurn ? player : opponent);
	}
	void advanceTurn()
	{
		playerTurn = !playerTurn;
	}

	private void findOpponent(String input)
	{
		boolean foundOpponent = false;
		//If it's a mention, parse it to the user ID
		if(input.contains("<@"))
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
			if(players.get(opponent).isBot)
				chooseOpponent(opponent);
			else
			{
				//Ask the player for confirmation, mostly just to make sure they're actually there
				waiter.waitForEvent(MessageReceivedEvent.class,
						//Accept if it's our opponent, they're in the right channel, and they've given a valid response
						e ->
						{
							if(findPlayerInGame(e.getAuthor().getId()) == opponent && e.getChannel().equals(channel))
							{
								String firstLetter = e.getMessage().getContentStripped().toUpperCase().substring(0,1);
								return(firstLetter.startsWith("Y") || firstLetter.startsWith("N"));
							}
							return false;
						},
						//Parse it and call the method that does stuff
						e -> 
						{
							if(e.getMessage().getContentStripped().toUpperCase().startsWith("Y"))
							{
								timer.schedule(() -> chooseOpponent(opponent), 500, TimeUnit.MILLISECONDS);
							}
							else
							{
								channel.sendMessage("Very well.").queue();
								timer.schedule(this::initialiseWithDummy, 500, TimeUnit.MILLISECONDS);
							}
						},
						30,TimeUnit.SECONDS, () ->
								timer.schedule(this::initialiseWithDummy, 500, TimeUnit.MILLISECONDS));
			}
		}
		else
			askForOpponent();
	}

	public int findPlayerInGame(String playerID)
	{
		for(int i=0; i < players.size(); i++)
			if(players.get(i).uID.equals(playerID))
				return i;
		return -1;
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
	
	void gameOver(int winner)
	{
		if(dummyBot)
			players.remove(opponent);
		gameOver();
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
