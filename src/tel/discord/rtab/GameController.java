package tel.discord.rtab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static tel.discord.rtab.RaceToABillionBot.waiter;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.HiddenCommand;
import tel.discord.rtab.board.SpaceType;

public class GameController
{
	//Basic stuff
	final static String[] VALID_ARC_RESPONSES = {"A","ABORT","R","RETRY","C","CONTINUE"};
	final static String[] NOTABLE_SPACES = {"$1,000,000","+500% Boost","+200% Boost","Grab Bag","BLAMMO",
			"Jackpot","Starman","Split & Share","Minefield","Blammo Frenzy","Joker","Midas Touch","Bowser Event"};
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
	boolean firstPick;
	String coveredUp;
	//Event variables
	int fcTurnsLeft;
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
		coveredUp = null;
		currentBlammo = false;
		futureBlammo = false;
		finalCountdown = false;
		reverse = false;
		starman = false;
		fcTurnsLeft = -1;
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
			if(repeatTurn > 0)
				repeatTurn --;
			channel.sendMessage(players.get(player).getSafeMention()
					+ ", someone from the gallery has given you a **BLAMMO!**").completeAfter(2, TimeUnit.SECONDS);
			startBlammo(false);
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
					resolveTurn(peekedSpaces.get((int)(Math.random()*peekedSpaces.size())));
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
							resolveTurn(peekSpace);
							break;
						//Cash or event can be risky, so roll the dice to pick it or not (unless it's 2p then there's no point)
						case CASH:
						case EVENT:
							if(Math.random()<0.5 || players.size() == 2)
								resolveTurn(peekSpace);
							else
								resolveTurn(safeSpaces.get((int)(Math.random()*safeSpaces.size())));
							break;
						//And obviously, don't pick it if it's a bomb!
						case BOMB:
							safeSpaces.remove(new Integer(peekSpace));
							//Make sure there's still a safe space left to pick, otherwise BAH
							if(safeSpaces.size()>0)
								resolveTurn(safeSpaces.get((int)(Math.random()*safeSpaces.size())));
							else
								resolveTurn(openSpaces.get((int)(Math.random()*openSpaces.size())));
							break;
						default:
							System.err.println("Bot made a bad peek!");
						}
					}
					//If we've already peeked the space we rolled, let's just take it
					else
					{
						resolveTurn(peekSpace);
					}
				}
				//Otherwise just pick a space
				else
					resolveTurn(safeSpaces.get((int)(Math.random()*safeSpaces.size())));
			}
			//Otherwise it sucks to be you, bot, eat bomb (or defuse bomb)!
			else
			{
				int bombToPick = openSpaces.get((int)(Math.random()*openSpaces.size()));
				/* TODO remove comment block when defuse works
				if(players.get(player).hiddenCommand == HiddenCommand.DEFUSE)
					defuseSpace(players.get(player),bombToPick);
					*/
				resolveTurn(bombToPick);
			}
		}
		else
		{
			displayBoardAndStatus(true, false, false);
			int thisPlayer = currentTurn;
			ScheduledFuture<?> warnPlayer = timer.schedule(() -> 
			{
				//If they're out of the round somehow, why are we warning them?
				if(players.get(thisPlayer).status == PlayerStatus.ALIVE && playersAlive > 1 && thisPlayer == currentTurn)
				{
					channel.sendMessage(players.get(thisPlayer).getSafeMention() + 
							", thirty seconds left to choose a space!").queue();
					displayBoardAndStatus(true,false,false);
				}
			}, 60, TimeUnit.SECONDS);
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
					{
						if(players.get(thisPlayer).status != PlayerStatus.ALIVE || playersAlive <= 1 || thisPlayer != currentTurn)
							return true;
						else if(e.getAuthor().equals(players.get(thisPlayer).user) && e.getChannel().equals(channel)
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
						if(players.get(thisPlayer).status == PlayerStatus.ALIVE && playersAlive > 1 && thisPlayer == currentTurn)
						{
							int location = Integer.parseInt(e.getMessage().getContentRaw())-1;
							//Anyway go play out their turn
							timer.schedule(() -> resolveTurn(location), 1, TimeUnit.SECONDS);
						}
					},
					90,TimeUnit.SECONDS, () ->
					{
						//If they're somehow taking their turn when they shouldn't be, just don't do anything
						if(players.get(thisPlayer).status == PlayerStatus.ALIVE && playersAlive > 1 && thisPlayer == currentTurn)
						{
							timeOutTurn();
						}
					});
		}
	}
	
	SpaceType usePeek(int playerID, int space)
	{
		players.get(playerID).peek --;
		SpaceType peekedSpace = gameboard.peekSpace(space);
		//If it's a bomb, add it to their known bombs
		if(peekedSpace == SpaceType.BOMB)
			players.get(playerID).knownBombs.add(space);
		//Otherwise add it to their known safe spaces
		else	
			players.get(playerID).safePeeks.add(space);
		return peekedSpace;
	}
	
	public int calculateEntryFee(int money, int lives)
	{
		int entryFee = Math.max(money/500,20000);
		entryFee *= 5 - lives;
		return entryFee;
	}
	
	public int baseMultiplier(int amount)
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
