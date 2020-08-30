package tel.discord.rtab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import static tel.discord.rtab.RaceToABillionBot.waiter;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Bomb;
import tel.discord.rtab.board.Boost;
import tel.discord.rtab.board.Cash;
import tel.discord.rtab.board.Event;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.HiddenCommand;
import tel.discord.rtab.board.SpaceType;

public class GameController
{
	//Constants
	final static String[] VALID_ARC_RESPONSES = {"A","ABORT","R","RETRY","C","CONTINUE"};
	final static String[] NOTABLE_SPACES = {"$1,000,000","+500% Boost","+300% Boost","Grab Bag","BLAMMO",
			"Jackpot","Starman","Split & Share","Minefield","Blammo Frenzy","Joker","Midas Touch","Bowser Event"};
	final static int REQUIRED_STREAK_FOR_BONUS = 40;
	final static int THRESHOLD_PER_TURN_PENALTY = 100_000;
	static final int BOMB_PENALTY = -250_000;
	static final int NEWBIE_BOMB_PENALTY = -100_000;
	public ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
	public TextChannel channel, resultChannel;
	//Other useful technical things
	public ScheduledFuture<?> demoMode;
	private Message waitingMessage;
	//Settings that can be customised
	int baseNumerator, baseDenominator, botCount, runDemo, minPlayers, maxPlayers;
	boolean playersCanJoin = true;
	//Game variables
	public final List<Player> players = new ArrayList<>(16);
	Board gameboard;
	boolean[] pickedSpaces;
	int currentTurn, playersAlive, botsInGame, repeatTurn, boardSize, spacesLeft;
	boolean firstPick, resolvingTurn;
	String coveredUp;
	//Event variables
	int boardMultiplier, fcTurnsLeft;
	boolean currentBlammo, futureBlammo, finalCountdown, reverse, starman;
	GameStatus gameStatus = GameStatus.LOADING;
	
	public GameController(TextChannel gameChannel, String[] record, TextChannel resultChannel)
	{
		/*
		 * Guild settings file format:
		 * record[3] = base multiplier (expressed as fraction)
		 * record[4] = how many different bot players there are (0+)
		 * record[5] = how often to run demos (in minutes, 0 to disable)
		 * record[6] = the minimum number of players required for a game to start (2-16)
		 * record[7] = the maximum number of players that can participate in a single game (2-16)
		 */
		channel = gameChannel;
		this.resultChannel = resultChannel;
		//Let them know if anything goes wrong
		try
		{
			//Base multiplier is kinda complex
			String[] baseMultiplier = record[3].split("/");
			baseNumerator = Integer.parseInt(baseMultiplier[0]);
			//If no denominator supplied, treat it as 1
			if(baseMultiplier.length < 2)
				baseDenominator = 1;
			else
				baseDenominator = Integer.parseInt(baseMultiplier[1]);
			//Other settings just simple imports
			botCount = Integer.parseInt(record[4]);
			runDemo = Integer.parseInt(record[5]);
			minPlayers = Integer.parseInt(record[6]);
			maxPlayers = Integer.parseInt(record[7]);
			//Finally, create a game channel with all the settings as instructed
		}
		catch(Exception e1)
		{
			gameChannel.sendMessage("A fatal error has occurred.").queue();
			e1.printStackTrace();
			return;
		}
		reset();
	}
	
	public void reset()
	{
		players.clear();
		currentTurn = -1;
		playersAlive = 0;
		botsInGame = 0;
		if(gameStatus != GameStatus.SEASON_OVER)
			gameStatus = GameStatus.SIGNUPS_OPEN;
		boardSize = 0;
		repeatTurn = 0;
		firstPick = true;
		resolvingTurn = false;
		coveredUp = null;
		currentBlammo = false;
		futureBlammo = false;
		finalCountdown = false;
		reverse = false;
		starman = false;
		fcTurnsLeft = 99;
		boardMultiplier = 1;
		timer.shutdownNow();
		timer = new ScheduledThreadPoolExecutor(1);
		if(runDemo != 0 && botCount >= 4)
		{
			demoMode = timer.schedule(() -> 
			{
				for(int i=0; i<4; i++)
					addRandomBot();
				startTheGameAlready();
			},runDemo,TimeUnit.MINUTES);
		}
	}
	
	boolean initialised()
	{
		return gameStatus == GameStatus.SIGNUPS_OPEN;
	}

	public int findPlayerInGame(String playerID)
	{
		for(int i=0; i < players.size(); i++)
			if(players.get(i).uID.equals(playerID))
				return i;
		return -1;
	}

	/**
	 * Adds a player to the game, or updates their name if they're already in.
	 * 
	 * @param channelID - channel the request took place in (only used to know where to send game details to)
	 * @param playerID - ID of player to be added.
	 * @return true if the join attempt succeeded, or false if it failed.
	 */
	public boolean addPlayer(Member playerID)
	{
		//Are player joins even *allowed* here?
		if(!playersCanJoin)
		{
			channel.sendMessage("Cannot join game: Joining is not permitted in this channel.").queue();
			return false;
		}
		//Make sure game isn't already running
		if(gameStatus != GameStatus.SIGNUPS_OPEN)
		{
			channel.sendMessage("Cannot join game: "+
					(gameStatus == GameStatus.SEASON_OVER?"There is no season currently running.":"Game already running.")).queue();
			return false;
		}
		//Watch out for too many players
		if(players.size() >= maxPlayers)
		{
			channel.sendMessage("Cannot join game: Too many players.").queue();
			return false;
		}
		//Create player object
		Player newPlayer = new Player(playerID,this,null);
		if(newPlayer.name.contains(":") || newPlayer.name.contains("#") || newPlayer.name.startsWith("!"))
		{
			channel.sendMessage("Cannot join game: Illegal characters in name.").queue();
			return false;
		}
		//Dumb easter egg
		if(newPlayer.money <= -1000000000)
		{
			channel.sendMessage("Cannot join game: You have been eliminated from Race to a Billion.").queue();
			return false;
		}
		//If they're out of lives, charge them and let them know
		//Fee is 1% plus an extra 0.2% per additional life spent while already out
		if(newPlayer.lives <= 0 && newPlayer.newbieProtection <= 0)
		{
			int entryFee = calculateEntryFee(newPlayer.money, newPlayer.lives);
			newPlayer.addMoney(-1*entryFee,MoneyMultipliersToUse.NOTHING);
			newPlayer.oldMoney = newPlayer.money;
			channel.sendMessage(newPlayer.getSafeMention() + String.format(", you are out of lives. "
					+ "Playing this round will incur an entry fee of $%,d.",entryFee)).queue();
		}
		//Look for match already in player list
		int playerLocation = findPlayerInGame(newPlayer.uID);
		if(playerLocation != -1)
		{
			//Found them, check if we should update their name or just laugh at them
			if(players.get(playerLocation).name == newPlayer.name)
			{
				channel.sendMessage("Cannot join game: You have already joined the game.").queue();
				return false;
			}
			else
			{
				players.set(playerLocation,newPlayer);
				channel.sendMessage("Updated in-game name.");
				return false;
			}
		}
		//Haven't found one, add them to the list
		players.add(newPlayer);
		if(newPlayer.hiddenCommand != HiddenCommand.NONE)
		{
			StringBuilder commandHelp = new StringBuilder();
			switch(newPlayer.hiddenCommand)
			{
			case FOLD:
				commandHelp.append("You are carrying over a **FOLD** from a previous game.\n"
						+ "You may use it at any time by typing **!fold**.");
				break;
			case REPELLENT:
				commandHelp.append("You are carrying over **BLAMMO REPELLENT** from a previous game.\n"
						+ "You may use it when a blammo is in play by typing **!repel**.");
				break;
			case BLAMMO:
				commandHelp.append("You are carrying over a **BLAMMO SUMMONER** from a previous game.\n"
						+ "You may use it at any time by typing **!blammo**.");
				break;
			case DEFUSE:
				commandHelp.append("You are carryng over a **DEFUSER** from a previous game.\n"
						+ "You may use it at any time by typing **!defuse** _followed by the space you wish to defuse_.");
				break;
			case WAGER:
				commandHelp.append("You are carrying over a **WAGERER** from a previous game.\n"
						+ "You may use it at any time by typing **!wager**.");
				break;
			case BONUS:
				commandHelp.append("You are carrying over the **BONUS BAG** from a previous game.\n"
						+ "You may use it at any time by typing **!bonus** followed by 'cash', 'boost', 'game', or 'event'.");
				break;
			default:
				break;
			}
			newPlayer.user.openPrivateChannel().queue(
					(channel) -> channel.sendMessage(commandHelp.toString()).queueAfter(5,TimeUnit.SECONDS));
		}
		if(newPlayer.money > 900000000)
		{
			channel.sendMessage(String.format("%1$s needs only $%2$,d more to reach the goal!",
					newPlayer.name,(1000000000-newPlayer.money))).queue();
		}
		//If there's only one player right now, that means we're starting a new game so schedule the relevant things
		if(players.size() == 1)
		{
			if(runDemo != 0)
				demoMode.cancel(false);
			timer.schedule(() -> 
			{
			channel.sendMessage("Thirty seconds before game starts!").queue();
			channel.sendMessage(listPlayers(false)).queue();
			}, 90, TimeUnit.SECONDS);
			timer.schedule(() -> startTheGameAlready(), 120, TimeUnit.SECONDS);
			channel.sendMessage("Starting a game of Race to a Billion in two minutes. Type !join to sign up.").queue();
		}
		//Finally, wrap up by saying they actually managed to join
		channel.sendMessage(newPlayer.name + " successfully joined the game.").queue();
		return true;
	}
	

	/**
	 * Removes a player from the game.
	 * 
	 * @param playerID - User ID of player to be removed.
	 * @return true if the quit attempt succeeded, or false if it failed.
	 */
	public boolean removePlayer(Member playerID)
	{
		//Make sure game isn't running, too late to quit now
		if(gameStatus != GameStatus.SIGNUPS_OPEN)
		{
			channel.sendMessage("The game cannot be left after it has started.").queue();
			return false;
		}
		//Search for player
		int playerLocation = findPlayerInGame(playerID.getId());
		if(playerLocation != -1)
		{
			players.remove(playerLocation);
			//Abort the game if everyone left
			if(players.size() == 0)
				reset();
			channel.sendMessage(playerID.getEffectiveName() + " left the game.").queue();
			return true;
		}
		//Didn't find them, why are they trying to quit in the first place?
		channel.sendMessage(playerID.getEffectiveName() + 
				" could not leave the game because they were never in the game. :thinking:").queue();
		return false;
	}
	
	public void addBot(int botNumber)
	{
		//Only do this if we're in signups!
		if(gameStatus != GameStatus.SIGNUPS_OPEN)
			return;
		GameBot chosenBot;
		try
		{
			chosenBot = new GameBot(channel.getGuild().getId(),botNumber);
		}
		catch (IOException e)
		{
			channel.sendMessage("Bot creation failed.").queue();
			e.printStackTrace();
			return;
		}
		Player newPlayer;
		newPlayer = new Player(chosenBot,this);
		players.add(newPlayer);
		botsInGame ++;
		if(newPlayer.money > 900_000_000)
		{
			channel.sendMessage(String.format("%1$s needs only $%2$,d more to reach the goal!",
					newPlayer.name,(1_000_000_000-newPlayer.money)));
		}
		//If they're the first player then don't bother with the timer, but do cancel the demo
		if(players.size() == 1 && runDemo != 0)
				demoMode.cancel(false);
		return;
	}
	
	public void addRandomBot()
	{
		//Only do this if the game hasn't started!
		if(gameStatus != GameStatus.SIGNUPS_OPEN && gameStatus != GameStatus.ADD_BOT_QUESTION
				&& gameStatus != GameStatus.BOMB_PLACEMENT)
			return;
		GameBot chosenBot;
		int nextBot = (int)(Math.random()*botCount);
		int triesLeft = botCount;
		//Start looping through until we find a valid bot (one that isn't already in the round)
		boolean goodPick;
		do
		{
			nextBot = (nextBot + 1) % botCount;
			triesLeft --;
			try
			{
				chosenBot = new GameBot(channel.getGuild().getId(),nextBot);
			}
			catch (IOException e)
			{
				channel.sendMessage("Bot generation failed.").queue();
				e.printStackTrace();
				return;
			}
			goodPick = (findPlayerInGame(chosenBot.botID) != -1);
		}
		while(!goodPick && triesLeft > 0);
		if(!goodPick)
		{
			//If we've checked EVERY bot...
			channel.sendMessage("Bot generation failed.").queue();
			return;
		}
		else
		{
			//But assuming we found one, pass them to the method that actually adds them
			addBot(nextBot);
		}
	}
	
	/**
	 * Close signups and run game initialisation stuff.
	 */
	public void startTheGameAlready()
	{
		//If the game's already running or no one's in it, just don't
		if((gameStatus != GameStatus.SIGNUPS_OPEN && gameStatus != GameStatus.ADD_BOT_QUESTION) || players.size() == 0)
		{
			return;
		}
		//Potentially ask to add bots
		if(gameStatus == GameStatus.SIGNUPS_OPEN && botCount - botsInGame > 0 &&
				(players.size() < minPlayers || (players.size() < 4 && Math.random() < 0.2) || (players.size() < 16 && Math.random() < 0.1)))
		{
			addBotQuestion();
			return;
		}
		//If we don't have enough players and for some reason didn't add bots, call the game off
		else if(players.size() < minPlayers)
		{
			channel.sendMessage("Not enough players. Game aborted.").queue();
			reset();
		}
		//Declare game in progress so we don't get latecomers
		channel.sendMessage("Starting game...").queue();
		gameStatus = GameStatus.BOMB_PLACEMENT;
		//Generate board
		boardSize = 5 + (5*players.size());
		spacesLeft = boardSize;
		gameboard = new Board(boardSize,players.size());
		pickedSpaces = new boolean[boardSize];
		//Then do bomb placement
		sendBombPlaceMessages();
	}
	
	/**
	 * Ask the players if they want a bot (or more) in their game, and add them if agreed to.
	 * This method should only be called from startTheGameAlready, and will recurse back to it once it's done its work.
	 */
	private void addBotQuestion()
	{
		//Didn't get players? How about a bot?
		if(players.size() == 1)
			channel.sendMessage(players.get(0).getSafeMention()+", would you like to play against a bot? (Y/N)").queue();
		else
			channel.sendMessage("Would you like to play against a bot? (Y/N)").queue();
		gameStatus = GameStatus.ADD_BOT_QUESTION;
		waiter.waitForEvent(MessageReceivedEvent.class,
				//Accept if it's a player in the game, they're in the right channel, and they've given a valid response
				e ->
				{
					if(findPlayerInGame(e.getAuthor().getId()) != -1 && e.getChannel().equals(channel))
					{
						String firstLetter = e.getMessage().getContentRaw().toUpperCase().substring(0,1);
						return(firstLetter.startsWith("Y") || firstLetter.startsWith("N"));
					}
					return false;
				},
				//Parse it and call the method that does stuff
				e -> 
				{
					if(e.getMessage().getContentRaw().toUpperCase().startsWith("Y"))
					{
						do
						{
							addRandomBot();
						}
						while(players.size() < 4 && Math.random() < 0.2 && botCount > botsInGame);
						startTheGameAlready();
					}
					else
					{
						channel.sendMessage("Very well.").queue();
						startTheGameAlready();
					}
				},
				30,TimeUnit.SECONDS, () ->
				{
					startTheGameAlready();
				});
	}
	
	private void sendBombPlaceMessages()
	{
		//Get the "waiting on" message going
		waitingMessage = channel.sendMessage(listPlayers(true)).complete();
		//Request players send in bombs, and set up waiter for them to return
		for(int i=0; i<players.size(); i++)
		{
			//Skip anyone who's already placed their bomb
			if(players.get(i).status == PlayerStatus.ALIVE)
				continue;
			final int iInner = i;
			if(players.get(iInner).isBot)
			{
				int bombPosition = (int) (Math.random() * boardSize);
				players.get(iInner).knownBombs.add(bombPosition);
				checkForNotableCover(gameboard.addBomb(bombPosition));
				players.get(iInner).status = PlayerStatus.ALIVE;
				playersAlive ++;
			}
			else
			{
				players.get(iInner).user.openPrivateChannel().queue(
						(channel) -> channel.sendMessage("Please place your bomb within the next "+(playersCanJoin?60:90)+" seconds "
								+ "by sending a number 1-" + boardSize).queue());
				waiter.waitForEvent(MessageReceivedEvent.class,
						//Check if right player, and valid bomb pick
						e -> (e.getAuthor().equals(players.get(iInner).user)
								&& e.getChannel().getType() == ChannelType.PRIVATE
								&& checkValidNumber(e.getMessage().getContentRaw())),
						//Parse it and update the bomb board
						e -> 
						{
							if(players.get(iInner).status == PlayerStatus.OUT)
							{
								checkForNotableCover(gameboard.addBomb(Integer.parseInt(e.getMessage().getContentRaw())-1));
								players.get(iInner).knownBombs.add(Integer.parseInt(e.getMessage().getContentRaw())-1);
								players.get(iInner).user.openPrivateChannel().queue(
										(channel) -> channel.sendMessage("Bomb placement confirmed.").queue());
								players.get(iInner).status = PlayerStatus.ALIVE;
								playersAlive ++;
								checkReady();
							}
						},
						//Or timeout the prompt after a minute (nothing needs to be done here)
						90, TimeUnit.SECONDS, () -> {});
			}
		}
		timer.schedule(() -> abortRetryContinue(), playersCanJoin?60:90, TimeUnit.SECONDS);
		checkReady();
	}
	
	private void abortRetryContinue()
	{
		//We don't need to do this if we aren't still waiting for bombs
		if(gameStatus != GameStatus.BOMB_PLACEMENT)
			return;
		//If this is SBC, just turn over control to AI
		if(!playersCanJoin)
		{
			for(int i=0; i<players.size(); i++)
				if(players.get(i).status != PlayerStatus.ALIVE)
					players.get(i).isBot = true;
			sendBombPlaceMessages();
			return;
		}
		//If *no* humans placed their bomb, or if there aren't enough bots to add, abort automatically
		if(playersAlive == botsInGame || (botCount - botsInGame) < (players.size() - playersAlive))
		{
			channel.sendMessage("Bomb placement timed out. Game aborted.").queue();
			reset();
			return;
		}
		//Otherwise, prompt the players for what to do
		channel.sendMessage("Bomb placement timed out. (A)bort, (R)etry, (C)ontinue?").queue();
		waiter.waitForEvent(MessageReceivedEvent.class,
				//Waiting player and right channel
				e ->
				{
					int playerID = findPlayerInGame(e.getAuthor().getId());
					if(playerID == -1)
						return false;
					return (players.get(playerID).status == PlayerStatus.ALIVE && e.getChannel().equals(channel)
							&& gameStatus == GameStatus.BOMB_PLACEMENT
							&& Arrays.asList(VALID_ARC_RESPONSES).contains(e.getMessage().getContentRaw().toUpperCase()));
				},
				//Read their choice and handle things accordingly
				e -> 
				{
					switch(e.getMessage().getContentRaw().toUpperCase())
					{
					case "A":
					case "ABORT":
						channel.sendMessage("Very well. Game aborted.").queue();
						reset();
						break;
					case "C":
					case "CONTINUE":
						//For any players who haven't placed their bombs, replace them with bots
						int playersRemoved = 0;
						for(int i=0; i<players.size(); i++)
							if(players.get(i).status != PlayerStatus.ALIVE)
							{
								playersRemoved++;
								players.remove(i);
								i--;
							}
						//Now add that many random bots
						for(int i=0; i<playersRemoved; i++)
							addRandomBot();
						//No break here - it flows through to placing the new bots' bombs
					case "R":
					case "RETRY":
						sendBombPlaceMessages();
						break;
					}
				},
				30,TimeUnit.SECONDS, () ->
				{
					//If the game hasn't started automatically, abort
					if(gameStatus == GameStatus.BOMB_PLACEMENT)
					{
						channel.sendMessage("Game aborted.").queue();
						reset();
					}
				});
	}
	
	private void checkForNotableCover(String spaceCovered)
	{
		for(String notableCover : NOTABLE_SPACES)
		{
			if(notableCover.equalsIgnoreCase(spaceCovered))
			{
				coveredUp = spaceCovered;
				return;
			}
		}
	}
	
	private void checkReady()
	{
		//If everyone has sent in, what are we waiting for?
		if(playersAlive == players.size())
		{
			//Delete the "waiting on" message
			waitingMessage.delete().queue();
			//Determine player order
			Collections.shuffle(players);
			//Let's get things rolling!
			channel.sendMessage("Let's go!").queue();
			if(coveredUp != null)
			{
				StringBuilder snarkMessage = new StringBuilder();
				switch((int)(Math.random()*4))
				{
				case 0:
					snarkMessage.append("One of you covered up this: "+coveredUp+".");
					break;
				case 1:
					snarkMessage.append("The "+coveredUp+" space that was once on this board is now a bomb. Oops.");
					break;
				case 2:
					snarkMessage.append("One of you covered up this: "+coveredUp+". I'm not naming names, "
							+players.get((int)(Math.random()*players.size())).getName());
					break;
				case 3:
					snarkMessage.append("So much for the "+coveredUp+" space on this board. You covered it up with a bomb.");
				}
				channel.sendMessage(snarkMessage).queue();
			}
			gameStatus = GameStatus.IN_PROGRESS;
			//Always start with the first player
			currentTurn = 0;
			runTurn(0);
		}
		//If they haven't, update the message to tell us who we're still waiting on
		else
		{
			waitingMessage.editMessage(listPlayers(true)).queue();
		}
	}

	private void runTurn(int player)
	{
		//There is NO reason why we should be running a turn for anyone other than the current player
		if(player != currentTurn)
			return;
		//If someone from the Gallery has given our hapless player a blammo, they get that instead of their normal turn
		if(futureBlammo)
		{
			futureBlammo = false;
			resolvingTurn = true;
			if(repeatTurn > 0)
				repeatTurn --;
			channel.sendMessage(players.get(player).getSafeMention()
					+ ", someone from the gallery has given you a **BLAMMO!**").completeAfter(2, TimeUnit.SECONDS);
			startBlammo(player, false);
			return;
		}
		//Count down if necessary
		if(finalCountdown)
		{
			//End the game if we're out of turns
			if(fcTurnsLeft == 0)
			{
				gameOver();
				return;
			}
			//Otherwise display the appropriate message
			else if(fcTurnsLeft == 1)
				channel.sendMessage("The round will end **after this pick!**").queue();
			else
				channel.sendMessage(String.format("The round will end in **%d picks**.",fcTurnsLeft)).queue();
			//And then subtract one
			fcTurnsLeft --;
		}
		//Figure out who to ping and what to tell them
		if(repeatTurn > 0 && !firstPick)
		{
			if(!(players.get(player).isBot))
				channel.sendMessage(players.get(player).getSafeMention() + ", pick again.")
					.completeAfter(2,TimeUnit.SECONDS);
		}
		else
		{
			firstPick = false;
			if(!players.get(player).isBot)
				channel.sendMessage(players.get(player).getSafeMention() + ", your turn. Choose a space on the board.")
					.completeAfter(2,TimeUnit.SECONDS);
		}
		if(repeatTurn > 0)
			repeatTurn --;
		displayBoardAndStatus(true, false, false);
		//Ready up the space picker depending on if it's a bot up next
		if(players.get(player).isBot)
		{
			//Sleep for a couple of seconds so they don't rush
			Thread.sleep(2000);
			/* TODO remove the comment notation when hidden commands work
			//Test for hidden command stuff first
			switch(players.get(player).hiddenCommand)
			{
			//Fold if they have no peeks, jokers, there's been no starman, and a random chance is hit
			case FOLD:
				if(!starman && players.get(player).peek < 1 && 
						players.get(player).jokers == 0 && Math.random() * spacesLeft < 1)
				{
					channel.sendMessage(players.get(player).name + " folded!").queue();
					players.get(player).hiddenCommand = HiddenCommand.NONE;
					foldPlayer(players.get(player));
					currentPlayerFoldedLogic();
					return;
				}
				break;
			//Bonus bag under same condition as the fold, but more frequently because of its positive effect
			case BONUS:
				if(!starman && players.get(player).peek < 1 && players.get(player).jokers == 0 && Math.random() * spacesLeft < 3)
				{
					channel.sendMessage(players.get(player).name + " dips into the bonus bag and finds..").queue();
					players.get(player).hiddenCommand = HiddenCommand.NONE;
					//Let's just pick one randomly
					SpaceType desire = SpaceType.BOOSTER;
					if(Math.random()*2 < 1)
						desire = SpaceType.GAME;
					if(Math.random()*3 < 1)
						desire = SpaceType.CASH;
					if(Math.random()*4 < 1)
						desire = SpaceType.EVENT;
					dipIntoBonusBag(desire);
				}
			//Blammo under the same condition as the fold, but make sure they aren't repeating turns either
			//We really don't want them triggering a blammo if they have a joker, because it could eliminate them despite it
			//But do increase the chance for it compared to folding
			case BLAMMO:
				if(!starman && players.get(player).peek < 1 && repeatTurn == 0 &&
						players.get(player).jokers == 0 && Math.random() * spacesLeft < players.size())
				{
					summonBlammo(players.get(player));
				}
				break;
			//Wager should be used if it's early enough in the round that it should catch most/all players
			//Teeeeeechnically it isn't playing to win with this, but it is making the game more exciting for the real players.
			case WAGER:
				if(players.size() * 4 < spacesLeft)
				{
					wagerGame(players.get(player));
				}
			//Repel and Defuse are more situational and aren't used at this time
			default:
				break;
			}
			*/
			//Get safe spaces, starting with all unpicked spaces
			ArrayList<Integer> openSpaces = new ArrayList<>(boardSize);
			for(int i=0; i<boardSize; i++)
				if(!pickedSpaces[i])
					openSpaces.add(i);
			//With chance depending on current board risk, look for a previous peek to use
			if(Math.random() * (spacesLeft - 1) < playersAlive)
			{
				//Check for known peeked spaces that are still available
				ArrayList<Integer> peekedSpaces = new ArrayList<>(boardSize);
				for(Integer peek : players.get(player).safePeeks)
				{
					if(openSpaces.contains(peek))
						peekedSpaces.add(peek);
				}
				//If there's any, pick one and end our logic
				if(peekedSpaces.size() > 0)
				{
					resolveTurn(player, peekedSpaces.get((int)(Math.random()*peekedSpaces.size())));
					return;
				}
			}
			//Remove all known bombs
			ArrayList<Integer> safeSpaces = new ArrayList<>(boardSize);
			safeSpaces.addAll(openSpaces);
			for(Integer bomb : players.get(player).knownBombs)
				safeSpaces.remove(bomb);
			//If there's any pick one at random and resolve it
			if(safeSpaces.size() > 0)
			{
				/*
				 * Use a peek under the following conditions:
				 * - The bot has one to use
				 * - It hasn't already peeked the space selected
				 * - 50% chance (so it won't always fire immediately)
				 * Note that they never bluff peek their own bomb (it's just easier that way)
				 */
				if(players.get(player).peek > 0 && Math.random() < 0.5)
				{
					int peekSpace = safeSpaces.get((int)(Math.random()*safeSpaces.size()));
					//Don't use the peek if we've already seen this space
					if(!players.get(player).safePeeks.contains(peekSpace))
					{
						//Let the players know what's going on
						channel.sendMessage("("+players.get(player).getName()+" peeks space "+(peekSpace+1)+")").queue();
						//Then use the peek, and decide what to do from there
						switch(usePeek(currentTurn,peekSpace))
						{
						//If it's a minigame or booster, take it immediately - it's guaranteed safe
						case BOOSTER:
						case GAME:
							resolveTurn(player, peekSpace);
							break;
						//Cash or event can be risky, so roll the dice to pick it or not (unless it's 2p then there's no point)
						case CASH:
						case EVENT:
							if(Math.random()<0.5 || players.size() == 2)
								resolveTurn(player, peekSpace);
							else
								resolveTurn(player, safeSpaces.get((int)(Math.random()*safeSpaces.size())));
							break;
						//And obviously, don't pick it if it's a bomb!
						case BOMB:
							safeSpaces.remove(new Integer(peekSpace));
							//Make sure there's still a safe space left to pick, otherwise BAH
							if(safeSpaces.size()>0)
								resolveTurn(player, safeSpaces.get((int)(Math.random()*safeSpaces.size())));
							else
								resolveTurn(player, openSpaces.get((int)(Math.random()*openSpaces.size())));
							break;
						default:
							System.err.println("Bot made a bad peek!");
						}
					}
					//If we've already peeked the space we rolled, let's just take it
					else
					{
						resolveTurn(player, peekSpace);
					}
				}
				//Otherwise just pick a space
				else
					resolveTurn(player, safeSpaces.get((int)(Math.random()*safeSpaces.size())));
			}
			//Otherwise it sucks to be you, bot, eat bomb (or defuse bomb)!
			else
			{
				int bombToPick = openSpaces.get((int)(Math.random()*openSpaces.size()));
				/* TODO remove comment block when defuse works
				if(players.get(player).hiddenCommand == HiddenCommand.DEFUSE)
					defuseSpace(players.get(player),bombToPick);
					*/
				resolveTurn(player, bombToPick);
			}
		}
		else
		{
			displayBoardAndStatus(true, false, false);
			ScheduledFuture<?> warnPlayer = timer.schedule(() -> 
			{
				//If they're out of the round somehow, why are we warning them?
				if(players.get(player).status == PlayerStatus.ALIVE && playersAlive > 1 && player == currentTurn)
				{
					channel.sendMessage(players.get(player).getSafeMention() + 
							", thirty seconds left to choose a space!").queue();
					displayBoardAndStatus(true,false,false);
				}
			}, 60, TimeUnit.SECONDS);
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
					{
						if(players.get(player).status != PlayerStatus.ALIVE || playersAlive <= 1 || player != currentTurn)
							return true;
						else if(e.getAuthor().equals(players.get(player).user) && e.getChannel().equals(channel)
								&& checkValidNumber(e.getMessage().getContentRaw()))
						{
								int location = Integer.parseInt(e.getMessage().getContentRaw());
								if(pickedSpaces[location-1])
								{
									channel.sendMessage("That space has already been picked.").queue();
									return false;
								}
								else
									return true;
						}
						return false;
					},
					//Parse it and call the method that does stuff
					e -> 
					{
						warnPlayer.cancel(false);
						//If they're somehow taking their turn when they're out of the round, just don't do anything
						if(players.get(player).status == PlayerStatus.ALIVE && playersAlive > 1 && player == currentTurn)
						{
							int location = Integer.parseInt(e.getMessage().getContentRaw())-1;
							//Anyway go play out their turn
							timer.schedule(() -> resolveTurn(player, location), 1, TimeUnit.SECONDS);
						}
					},
					90,TimeUnit.SECONDS, () ->
					{
						//If they're somehow taking their turn when they shouldn't be, just don't do anything
						if(players.get(player).status == PlayerStatus.ALIVE && playersAlive > 1 && player == currentTurn)
						{
							timeOutTurn(player);
						}
					});
		}
	}
	
	SpaceType usePeek(int playerID, int space)
	{
		players.get(playerID).peek --;
		SpaceType peekedSpace = gameboard.getType(space);
		//If it's a bomb, add it to their known bombs
		if(peekedSpace == SpaceType.BOMB)
			players.get(playerID).knownBombs.add(space);
		//Otherwise add it to their known safe spaces
		else	
			players.get(playerID).safePeeks.add(space);
		return peekedSpace;
	}
	
	private void timeOutTurn(int player)
	{
		//If they haven't been warned, play nice and just pick a random space for them
		if(!players.get(player).warned)
		{
			players.get(player).warned = true;
			channel.sendMessage(players.get(player).getSafeMention() + 
					" is out of time. Wasting a random space.").queue();
			//Get unpicked spaces
			ArrayList<Integer> spaceCandidates = new ArrayList<>(boardSize);
			for(int i=0; i<boardSize; i++)
				if(!pickedSpaces[i])
					spaceCandidates.add(i);
			//Pick one at random
			int spaceChosen = spaceCandidates.get((int) (Math.random() * spaceCandidates.size()));
			//If it's a bomb, it sucks to be them
			if(gameboard.getType(spaceChosen) == SpaceType.BOMB)
			{
				resolveTurn(spaceChosen, player);
			}
			//If it isn't, throw out the space and let the players know what's up
			else
			{
				if(resolvingTurn)
					return;
				else
					resolvingTurn = true;
				pickedSpaces[spaceChosen] = true;
				spacesLeft --;
				channel.sendMessage("Space " + (spaceChosen+1) + " selected...").completeAfter(1,TimeUnit.SECONDS);
				//Don't forget the threshold
				if(players.get(player).threshold)
				{
					channel.sendMessage(String.format("(-$%,d)",applyBaseMultiplier(THRESHOLD_PER_TURN_PENALTY)))
						.queueAfter(1,TimeUnit.SECONDS);
					players.get(player).addMoney(applyBaseMultiplier(-1*THRESHOLD_PER_TURN_PENALTY),MoneyMultipliersToUse.NOTHING);
				}
				channel.sendMessage("It's not a bomb, so its contents are lost.").completeAfter(5,TimeUnit.SECONDS);
				runEndTurnLogic();
			}
		}
		//If they've been warned, it's time to BLOW STUFF UP!
		else
		{
			channel.sendMessage(players.get(player).getSafeMention() + 
					" is out of time. Eliminating them.").queue();
			//Jokers? GET OUT OF HERE!
			if(players.get(player).jokers > 0)
				channel.sendMessage("Joker"+(players.get(player).jokers!=1?"s":"")+" deleted.").queue();
			players.get(player).jokers = 0;
			//Find a bomb to destroy them with
			int bombChosen;
			//If their own bomb is still out there, let's just use that one
			if(!pickedSpaces[players.get(player).knownBombs.get(0)])
			{
				bombChosen = players.get(player).knownBombs.get(0);
			}
			//Otherwise look for someone else's bomb
			else
			{
				ArrayList<Integer> bombCandidates = new ArrayList<>(boardSize);
				for(int i=0; i<boardSize; i++)
					if(gameboard.getType(i) == SpaceType.BOMB && !pickedSpaces[i])
						bombCandidates.add(i);
				//Got bomb? Pick one to detonate
				if(bombCandidates.size() > 0)
				{
					bombChosen = bombCandidates.get((int) (Math.random() * bombCandidates.size()));
				}
				//No bomb? WHO CARES, THIS IS RACE TO A BILLION, WE'RE BLOWING THEM UP ANYWAY!
				else
				{
					//Get unpicked spaces
					ArrayList<Integer> spaceCandidates = new ArrayList<>(boardSize);
					for(int i=0; i<boardSize; i++)
						if(!pickedSpaces[i])
							spaceCandidates.add(i);
					//Pick one and turn it into a BOMB
					bombChosen = spaceCandidates.get((int) (Math.random() * spaceCandidates.size()));
					gameboard.changeType(bombChosen,SpaceType.BOMB);
				}
			}
			//NO DUDS ALLOWED
			gameboard.forceExplosiveBomb(bombChosen);
			//KABOOM KABOOM KABOOM KABOOM
			resolveTurn(bombChosen, player);
		}
	}
	
	private void resolveTurn(int location, int player)
	{
		//Check for a hold on the board, and hold it if there isn't
		if(resolvingTurn)
			return;
		else
			resolvingTurn = true;
		//Try to detect double-turns and negate them before damage is done
		if(pickedSpaces[location])
		{
			resolvingTurn = false;
			return;
		}
		//Announce the picked space
		if(players.get(player).isBot)
		{
			channel.sendMessage(players.get(player).name + " selects space " + (location+1) + "...")
				.complete();
		}
		else
		{
			channel.sendMessage("Space " + (location+1) + " selected...").completeAfter(1,TimeUnit.SECONDS);
		}
		pickedSpaces[location] = true;
		spacesLeft--;
		//Now run through stuff that happens on every turn this player takes
		//Check annuities (threshold situation counts as one too)
		int annuityPayout = players.get(player).giveAnnuities();
		if(players.get(player).threshold)
			annuityPayout -= applyBaseMultiplier(THRESHOLD_PER_TURN_PENALTY);
		if(annuityPayout != 0)
		{
			players.get(player).addMoney(annuityPayout,MoneyMultipliersToUse.NOTHING);
			channel.sendMessage(String.format("("+(annuityPayout<0?"-":"+")+"$%,d)",Math.abs(annuityPayout)))
					.queueAfter(1,TimeUnit.SECONDS);
		}
		//Check boost charger
		if(players.get(player).boostCharge != 0)
		{
			players.get(player).addBooster(players.get(player).boostCharge);
			channel.sendMessage(String.format("(%+d%%)",players.get(player).boostCharge))
				.queueAfter(1,TimeUnit.SECONDS);
		}
		//Now look at the space they actually picked
		//Midas Touch check
		if(players.get(player).jokers == -1)
		{
			//Blammos are still immune :P
			if(gameboard.getType(location) != SpaceType.BLAMMO)
				gameboard.changeType(location,SpaceType.CASH);
		}
		/*
		 * Suspense rules:
		 * Always trigger on a bomb or blammo
		 * Otherwise, don't trigger if they have a joker or we've had a starman
		 * Otherwise trigger randomly, chance determined by spaces left and players in the game
		 */
		if((Math.random()*Math.min(spacesLeft,fcTurnsLeft)<players.size() && players.get(player).jokers == 0 && !starman)
				|| gameboard.getType(location) == SpaceType.BLAMMO || gameboard.getType(location) == SpaceType.BOMB)
		{
			Thread.sleep(5000);
			channel.sendMessage("...").queue();
		}
		Thread.sleep(5000);
		switch(gameboard.getType(location))
		{
		case BOMB:
			//Start off by sending the appropriate message
			if(players.get(player).knownBombs.contains(location))
			{
				//Mock them appropriately if they self-bombed
				if(players.get(player).knownBombs.get(0) == location)
					channel.sendMessage("It's your own **BOMB**.").queue();
				//Also mock them if they saw the bomb in a peek
				else
					channel.sendMessage("As you know, it's a **BOMB**.").queue();
			}
			//Otherwise, just give them the dreaded words...
			else
				channel.sendMessage("It's a **BOMB**.").queue();
			awardBomb(player, gameboard.getBomb(location));
			break;
		case CASH:
			awardCash(player, gameboard.getCash(location));
			break;
		case BOOSTER:
			awardBoost(player, gameboard.getBoost(location));
			break;
		case GAME:
			awardGame(player, gameboard.getGame(location));
			break;
		case EVENT:
			awardEvent(player, gameboard.getEvent(location));
			break;
		case GRAB_BAG:
			channel.sendMessage("It's a **Grab Bag**, you're winning some of everything!").queue();
			Thread.sleep(1000);
			awardGame(player, gameboard.getGame(location));
			Thread.sleep(1000);
			awardBoost(player, gameboard.getBoost(location));
			Thread.sleep(1000);
			awardCash(player, gameboard.getCash(location));
			Thread.sleep(2000); //mini-suspense lol
			awardEvent(player, gameboard.getEvent(location));
			break;
		case BLAMMO:
			channel.sendMessage(players.get(player).getSafeMention() + ", it's a **BLAMMO!**").queue();
			startBlammo(player, false);
			return; //Blammos pass to end-turn-logic when they're done, and not before
		}
		runEndTurnLogic();
	}
	
	private void awardBomb(int player, Bomb bombType)
	{
		//TODO hook up to bomb classes
	}
	
	private void awardCash(int player, Cash cashType) throws InterruptedException
	{
		int cashWon;
		String prizeWon = null;
		//Is it Mystery Money? Do that thing instead then
		if(cashType == Cash.MYSTERY)
		{
			channel.sendMessage("It's **Mystery Money**, which today awards you...").queue();
			Thread.sleep(1000);
			if(Math.random() < 0.1)
				cashWon = -1*(int)Math.pow((Math.random()*39)+1,3);
			else
				cashWon = (int)Math.pow((Math.random()*39)+1,4);
		}
		else
		{
			Pair<Integer,String> data = cashType.getValue();
			cashWon = data.getLeft();
			prizeWon = data.getRight();
		}
		//Boost by board multiplier
		cashWon = applyBaseMultiplier(cashWon);
		//On cash, update the player's score and tell them how much they won
		StringBuilder resultString = new StringBuilder();
		if(prizeWon != null)
		{
			resultString.append("It's **");
			if(boardMultiplier > 1)
				resultString.append(String.format("%dx ",boardMultiplier));
			resultString.append(prizeWon);
			resultString.append("**, worth ");
		}
		resultString.append("**");
		if(cashWon<0)
			resultString.append("-");
		resultString.append(String.format("$%,d**!",Math.abs(cashWon)));
		channel.sendMessage(resultString.toString()).queue();
		StringBuilder extraResult = players.get(player).addMoney(cashWon, MoneyMultipliersToUse.BOOSTER_ONLY);
		if(extraResult != null)
		{
			Thread.sleep(1000);
			channel.sendMessage(extraResult.toString()).queue();
		}
		//Award hidden command with 25% chance if cash is negative and they don't have one already
		if(cashWon < 0 && Math.random() < 0.25 && players.get(player).hiddenCommand == HiddenCommand.NONE)
			awardHiddenCommand(player);
	}
	
	private void awardHiddenCommand(int player)
	{
		HiddenCommand[] possibleCommands = HiddenCommand.values();
		//Never pick "none", which is at the start of the list
		int commandNumber = (int) (Math.random() * (possibleCommands.length - 1) + 1);
		HiddenCommand chosenCommand = possibleCommands[commandNumber];
		StringBuilder commandHelp = new StringBuilder();
		if(players.get(player).hiddenCommand != HiddenCommand.NONE)
			commandHelp.append("Your Hidden Command has been replaced with...\n");
		else
			commandHelp.append("You found a Hidden Command...\n");
		players.get(player).hiddenCommand = chosenCommand;
		//Send them the PM telling them they have it
		if(!players.get(player).isBot)
		{
			switch(chosenCommand)
			{
			case FOLD:
				commandHelp.append("A **FOLD**!\n"
						+ "The fold allows you to drop out of the round at any time by typing **!fold**.\n"
						+ "If you use it, you will keep your mulipliers and minigames, "
						+ "so consider it a free escape from a dangerous board!\n");
				break;
			case REPELLENT:
				commandHelp.append("A **BLAMMO REPELLENT**!\n"
						+ "You may use this by typing **!repel** whenever any player is facing a blammo to automatically block it.\n"
						+ "The person affected will then need to choose a different space from the board.\n");
				break;
			case BLAMMO:
				commandHelp.append("A **BLAMMO SUMMONER**!\n"
						+ "You may use this by typing **!blammo** at any time to give the next player a blammo!\n"
						+ "This will activate on the NEXT turn (not the current one), and will replace that player's normal turn.\n");
				break;
			case DEFUSE:
				commandHelp.append("A **DEFUSER**!\n"
						+ "You may use this at any time by typing **!defuse 13**, replacing '13' with the space you wish to defuse.\n"
						+ "Any bomb placed on the defused space will fail to explode. Use this wisely!\n");
				break;
			case WAGER:
				commandHelp.append("A **WAGERER**!\n"
						+ "The wager allows you to force all living players to add a portion of their total bank to a prize pool, "
						+ "which the winner(s) of the round will claim.\n"
						+ "The amount is equal to 1% of the last-place player's total bank, "
						+ "and you can activate this at any time by typing **!wager**.\n"); 
				break;
			case BONUS:
				commandHelp.append("The **BONUS BAG**!\n"
						+ "The bonus bag contains many things, "
						+ "and you can use this command to pass your turn and draw from the bag instead.\n"
						+ "To do so, type !bonus followed by either 'cash', 'boost', 'game', or 'event', depending on what you want.\n"
						+ "WARNING: The bag is not limitless, and misuse of the bonus bag is likely to end explosively.\n"
						+ "It is suggested that you do not wish for  something that has already been wished for this game.\n");
				break;
			default:
				commandHelp.append("An **ERROR**. Report this to @Atia#2084 to get it fixed.");
				break;
			}
			commandHelp.append("You may only have one Hidden Command at a time, and you will keep it even across rounds "
					+ "until you either use it or hit a bomb and lose it.\n"
					+ "Hidden commands must be used in the game channel, not in private.");
			players.get(player).user.openPrivateChannel().queue(
					(channel) -> channel.sendMessage(commandHelp.toString()).queueAfter(5,TimeUnit.SECONDS));
		}
		return;
	}

	private void awardBoost(int player, Boost boostType)
	{
		int boostFound = boostType.getValue();
		StringBuilder resultString = new StringBuilder();
		resultString.append(String.format("A **%+d%%** Booster",boostFound));
		resultString.append(boostFound > 0 ? "!" : ".");
		players.get(player).addBooster(boostFound);
		channel.sendMessage(resultString.toString()).queue();
	}

	private void awardGame(int player, Game gameFound)
	{
		players.get(player).games.add(gameFound);
		players.get(player).games.sort(null);
		channel.sendMessage("It's a minigame, **" + gameFound + "**!").queue();
	}
	
	private void awardEvent(int player, Event eventType)
	{
		//TODO hook up to event classes
	}
	
	private void startBlammo(int player, boolean mega)
	{
		channel.sendMessage("Quick, press a button!\n```" + (mega ? "\n MEGA " : "") + "\nBLAMMO\n 1  2 \n 3  4 \n```").queue();
		currentBlammo = true;
		List<BlammoChoices> buttons = Arrays.asList(BlammoChoices.values());
		Collections.shuffle(buttons);
		if(players.get(player).isBot)
		{
			/* TODO remove when hidden commands are in
			//Repel it if they have a repel to use
			if(players.get(player).hiddenCommand == HiddenCommand.REPELLENT)
			{
				channel.sendMessage("But " + players.get(player).name + " repelled it!").queue();
				players.get(player).hiddenCommand = HiddenCommand.NONE;
				repelBlammo();
			}
			//Otherwise wait a few seconds for someone else to potentially repel it before pressing a button
			else
			*/
				timer.schedule(() -> runBlammo(player, buttons, (int) (Math.random() * 4), mega), 5, TimeUnit.SECONDS);
		}
		else
		{
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
					{
						return (e.getAuthor().equals(players.get(player).user) && e.getChannel().equals(channel)
								&& checkValidNumber(e.getMessage().getContentRaw()) 
										&& Integer.parseInt(e.getMessage().getContentRaw()) <= 4);
					},
					//Parse it and call the method that does stuff
					e -> 
					{
						//Don't resolve the blammo if there is no blammo right now
						if(currentBlammo)
						{
							int button = Integer.parseInt(e.getMessage().getContentRaw())-1;
							timer.schedule(() -> runBlammo(player, buttons, button, mega), 1, TimeUnit.SECONDS);
						}
					},
					30,TimeUnit.SECONDS, () ->
					{
						if(currentBlammo)
						{
							channel.sendMessage("Too slow, autopicking!").queue();
							int button = (int) Math.random() * 4;
							runBlammo(player, buttons, button, mega);
						}
					});
		}
	}

	private void runBlammo(int player, List<BlammoChoices> buttons, int buttonPressed, boolean mega)
	{
		if(players.get(player).isBot)
		{
			channel.sendMessage(players.get(player).name + " presses button " + (buttonPressed+1) + "...").queue();
		}
		else
		{
			channel.sendMessage("Button " + (buttonPressed+1) + " pressed...").queue();
		}
		channel.sendMessage("...").completeAfter(3,TimeUnit.SECONDS);
		//Double-check that there is actually a blammo
		if(!currentBlammo)
			return;
		else
			currentBlammo = false; //Too late to repel now
		StringBuilder extraResult = null;
		int penalty = applyBaseMultiplier(BOMB_PENALTY);
		switch(buttons.get(buttonPressed))
		{
		case BLOCK:
			channel.sendMessage("You BLOCKED the BLAMMO!").completeAfter(3,TimeUnit.SECONDS);
			break;
		case ELIM_YOU:
			channel.sendMessage("You ELIMINATED YOURSELF!").completeAfter(3,TimeUnit.SECONDS);
			if(players.get(player).newbieProtection > 0)
				penalty = applyBaseMultiplier(NEWBIE_BOMB_PENALTY);
			penalty /= 10;
			penalty *= (10 - Math.min(9,players.size()-playersAlive));
			channel.sendMessage(String.format("$%,d"+(mega?" MEGA":"")+" penalty!",Math.abs(penalty*4*(mega?4:1)))).queue();
			players.get(player).threshold = true;
			extraResult = players.get(player).blowUp((mega?4:1)*penalty,false,(players.size()-playersAlive));
			break;
		case ELIM_OPP:
			channel.sendMessage("You ELIMINATED YOUR OPPONENT!").completeAfter(3,TimeUnit.SECONDS);
			//Pick a random living player
			int playerToKill = (int) ((Math.random() * (playersAlive-1)));
			//Bypass dead players and the button presser
			for(int i=0; i<=playerToKill; i++)
				if(players.get(i).status != PlayerStatus.ALIVE || i == player)
					playerToKill++;
			//Kill them dead
			if(players.get(playerToKill).newbieProtection > 0)
				penalty = applyBaseMultiplier(NEWBIE_BOMB_PENALTY);
			penalty /= 10;
			penalty *= (10 - Math.min(9,players.size()-playersAlive));
			channel.sendMessage("Goodbye, " + players.get(playerToKill).getSafeMention()
					+ String.format("! $%,d"+(mega?" MEGA":"")+" penalty!",Math.abs(penalty*4*(mega?4:1)))).queue();
			players.get(playerToKill).threshold = true;
			int tempRepeat = repeatTurn;
			extraResult = players.get(playerToKill).blowUp((mega?4:1)*penalty,false,(players.size()-playersAlive));
			repeatTurn = tempRepeat;
			break;
		case THRESHOLD:
			if(mega)
			{
				//They actually did it hahahahahahahaha
				channel.sendMessage("Oh no, you **ELIMINATED EVERYONE**!!").completeAfter(3,TimeUnit.SECONDS);
				for(Player nextPlayer : players)
				{
					if(nextPlayer.status == PlayerStatus.ALIVE)
					{
						//Check for special events to bring extra pain
						if(players.get(player).splitAndShare)
						{
							channel.sendMessage(String.format("Oh, %s had a split and share? Well there's no one to give your money to,"
									+ " so we'll just take it!", players.get(player).name))
								.completeAfter(2, TimeUnit.SECONDS);
							players.get(player).money *= 0.9;
							players.get(player).splitAndShare = false;
						}
						if(players.get(player).jackpot > 0)
						{
							channel.sendMessage(String.format("Oh, %1$s had a jackpot? More like an ANTI-JACKPOT! "+
									"**MINUS $%2$,d,000,000** for you!", players.get(player).name, players.get(player).jackpot))
								.completeAfter(2, TimeUnit.SECONDS);
							players.get(player).addMoney(players.get(player).jackpot*-1_000_000, MoneyMultipliersToUse.NOTHING);
							players.get(player).jackpot = 0;
						}
						penalty = applyBaseMultiplier(players.get(player).newbieProtection > 0 ? NEWBIE_BOMB_PENALTY : BOMB_PENALTY);
						channel.sendMessage(String.format("$%1$,d MEGA penalty for %2$s!",
								Math.abs(penalty*16),players.get(player).getSafeMention())).completeAfter(2,TimeUnit.SECONDS);
						players.get(player).threshold = true;
						extraResult = players.get(player).blowUp(penalty*4,false,0);
						if(extraResult != null)
							channel.sendMessage(extraResult).queue();
					}
				}
				//Re-null this so we don't get an extra quote of it
				extraResult = null;
				break;
			}
			else if(players.get(player).threshold)
			{
				//You already have a threshold situation? Time for some fun!
				channel.sendMessage(players.get(player).getSafeMention() + ", you **UPGRADED the BLAMMO!** "
						+ "Don't panic, it can still be stopped...").completeAfter(5,TimeUnit.SECONDS);
				startBlammo(player, true);
				return;
			}
			else
			{
				channel.sendMessage("You're entering a THRESHOLD SITUATION!").completeAfter(3,TimeUnit.SECONDS);
				channel.sendMessage(String.format("You'll lose $%,d for every pick you make, ",
						applyBaseMultiplier(THRESHOLD_PER_TURN_PENALTY))
						+ "and if you lose the penalty will be four times as large!").queue();
				players.get(player).threshold = true;
				break;
			}
		}
		if(extraResult != null)
			channel.sendMessage(extraResult).queue();
		runEndTurnLogic();
	}
	
	public int calculateEntryFee(int money, int lives)
	{
		int entryFee = Math.max(money/500,20000);
		entryFee *= 5 - lives;
		return entryFee;
	}
	
	public int applyBaseMultiplier(int amount)
	{
		long midStep = amount * baseNumerator;
		long endStep = midStep / baseDenominator;
		if(endStep > 1_000_000_000)
			endStep = 1_000_000_000;
		if(endStep < -1_000_000_000)
			endStep = -1_000_000_000;
		return (int)endStep;
	}

	public String listPlayers(boolean waitingOn)
	{
		StringBuilder resultString = new StringBuilder();
		if(waitingOn)
			resultString.append("**WAITING ON**");
		else
			resultString.append("**PLAYERS**");
		for(Player next : players)
		{
			if(!waitingOn || (waitingOn && next.status == PlayerStatus.OUT))
			{
				resultString.append(" | ");
				resultString.append(next.name);
			}
		}
		return resultString.toString();
	}
	
	boolean checkValidNumber(String message)
	{
		try
		{
			int location = Integer.parseInt(message);
			return (location > 0 && location <= boardSize);
		}
		catch(NumberFormatException e1)
		{
			return false;
		}
	}
	
	public void displayBoardAndStatus(boolean printBoard, boolean totals, boolean copyToResultChannel)
	{
		if(gameStatus == GameStatus.SIGNUPS_OPEN)
		{
			//No board to display if the game isn't running!
			return;
		}
		StringBuilder board = new StringBuilder().append("```\n");
		//Board doesn't need to be displayed if game is over
		if(printBoard)
		{
			//Do we need a complex header, or should we use the simple one?
			int boardWidth = Math.max(5,players.size()+1);
			if(boardWidth < 6)
				board.append("     RtaB     \n");
			else
			{
				for(int i=7; i<=boardWidth; i++)
				{
					//One space for odd numbers, two spaces for even numbers
					board.append(i%2==0 ? "  " : " ");
				}
				//Then print the first part
				board.append("Race to ");
				//Extra space if it's odd
				if(boardWidth%2 == 1) board.append(" ");
				//Then the rest of the header
				board.append("a Billion\n");
			}
			for(int i=0; i<boardSize; i++)
			{
				board.append(pickedSpaces[i] ? "  " : String.format("%02d",(i+1)));
				board.append((i%boardWidth) == (boardWidth-1) ? "\n" : " ");
			}
			board.append("\n");
		}
		//Next the status line
		//Start by getting the lengths so we can pad the status bars appropriately
		//Add one extra to name length because we want one extra space between name and cash
		int nameLength = players.get(0).name.length();
		for(int i=1; i<players.size(); i++)
			nameLength = Math.max(nameLength,players.get(i).name.length());
		nameLength ++;
		//And ignore the negative sign if there is one
		int moneyLength;
		if(totals)
		{
			moneyLength = String.valueOf(Math.abs(players.get(0).money)).length();
			for(int i=1; i<players.size(); i++)
				moneyLength = Math.max(moneyLength, String.valueOf(Math.abs(players.get(i).money)).length());
		}
		else
		{
			moneyLength = String.valueOf(Math.abs(players.get(0).money-players.get(0).oldMoney)).length();
			for(int i=1; i<players.size(); i++)
				moneyLength = Math.max(moneyLength,
						String.valueOf(Math.abs(players.get(i).money-players.get(i).oldMoney)).length());		
		}
		//Make a little extra room for the commas
		moneyLength += (moneyLength-1)/3;
		//Then start printing - including pointer if currently their turn
		for(int i=0; i<players.size(); i++)
		{
			board.append(currentTurn == i ? "> " : "  ");
			board.append(String.format("%-"+nameLength+"s",players.get(i).name));
			//Now figure out if we need a negative sign, a space, or neither
			int playerMoney = (players.get(i).money - players.get(i).oldMoney);
			//What sign to print?
			board.append(playerMoney<0 ? "-" : "+");
			//Then print the money itself
			board.append(String.format("$%,"+moneyLength+"d",Math.abs(playerMoney)));
			//Now the booster display
			switch(players.get(i).status)
			{
			case ALIVE:
			case DONE:
				//If they're alive, display their booster
				board.append(String.format(" [%3d%%",players.get(i).booster));
				//If it's endgame, show their winstreak afterward
				if(players.get(i).status == PlayerStatus.DONE || (gameStatus == GameStatus.END_GAME && currentTurn == i))
					board.append(String.format("x%1$d.%2$d",players.get(i).winstreak/10,players.get(i).winstreak%10));
				//Otherwise, display whether or not they have a peek
				else if(players.get(i).peek > 0)
					board.append("P");
				else
					board.append(" ");
				//Then close off the bracket
				board.append("]");
				break;
			case OUT:
			case FOLDED:
				board.append("  [OUT] ");
				break;
			}
			//If they have any games, print them too
			if(players.get(i).games.size() > 0)
			{
				board.append(" {");
				for(Game minigame : players.get(i).games)
				{
					board.append(" " + minigame.getShortName());
				}
				board.append(" }");
			}
			board.append("\n");
			//If we want the totals as well, do them on a second line
			if(totals)
			{
				//Get to the right spot in the line
				for(int j=0; j<(nameLength-4); j++) board.append(" ");
				board.append("Total:");
				//Print sign
				board.append(players.get(i).money<0 ? "-" : " ");
				//Then print the money itself
				board.append("$");
				board.append(String.format("%,"+moneyLength+"d\n\n",Math.abs(players.get(i).money)));
			}
		}
		//Close it off and print it out
		board.append("```");
		channel.sendMessage(board.toString()).queue();
		if(copyToResultChannel && resultChannel != null)
			resultChannel.sendMessage(board.toString()).queue();
	}
}
