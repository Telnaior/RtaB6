package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import static tel.discord.rtab.RaceToABillionBot.waiter;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.BombType;
import tel.discord.rtab.board.Boost;
import tel.discord.rtab.board.Cash;
import tel.discord.rtab.board.EventType;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.HiddenCommand;
import tel.discord.rtab.board.SpaceType;
import tel.discord.rtab.commands.TestMinigameCommand;
import tel.discord.rtab.commands.channel.BooleanSetting;
import tel.discord.rtab.games.MiniGame;

public class GameController
{
	//Constants
	final static String[] VALID_ARC_RESPONSES = {"A","ABORT","R","RETRY","C","CONTINUE"};
	final static String[] NOTABLE_SPACES = {"$1,000,000","+500% Boost","+300% Boost","BLAMMO",
			"Jackpot","Starman","Split & Share","Minefield","Blammo Frenzy","Joker","Midas Touch","Bowser Event", "Lucky Space"};
	final static int THRESHOLD_PER_TURN_PENALTY = 100_000;
	static final int BOMB_PENALTY = -250_000;
	static final int NEWBIE_BOMB_PENALTY = -100_000;
	//Other useful technical things
	public ScheduledThreadPoolExecutor timer;
	public TextChannel channel, resultChannel;
	public ScheduledFuture<?> demoMode;
	private Message waitingMessage;
	public HashSet<String> pingList = new HashSet<>();
	public HashSet<String> lockoutList = new HashSet<>();
	ScheduledFuture<?> warnPlayer;
	Thread runAtGameEnd = null;
	//Settings that can be customised
	public int baseNumerator, baseDenominator, botCount, minPlayers, maxPlayers, maxLives, runDemo;
	int averagePlayers, nextGamePlayers, newbieProtection;
	public LifePenaltyType lifePenalty;
	boolean rankChannel, verboseBotGames, doBonusGames, playersLevelUp;
	public boolean playersCanJoin = true;
	//Game variables
	public GameStatus gameStatus = GameStatus.LOADING;
	public final List<Player> players = new ArrayList<>(16);
	private final List<Player> winners = new ArrayList<>();
	public Board gameboard;
	public boolean[] pickedSpaces;
	public int currentTurn;
	public int playersAlive, earlyWinners;
	int botsInGame;
	public int repeatTurn;
	public int boardSize, spacesLeft;
	public boolean firstPick;
	public boolean resolvingTurn, resolvingBomb;
	String coveredUp;
	public MiniGame currentGame;
	//Event variables
	public int boardMultiplier;
	public int fcTurnsLeft;
	int wagerPot;
	public boolean currentBlammo;
	public boolean futureBlammo;
	public boolean finalCountdown;
	public boolean reverse;
	public boolean starman;
	
	public GameController(TextChannel gameChannel, String[] record, TextChannel resultChannel)
	{
		/*
		 * Guild settings file format:
		 * record[3] = base multiplier (expressed as fraction)
		 * record[4] = how many different bot players there are (0+)
		 * record[5] = how often to run demos (in minutes, 0 to disable)
		 * record[6] = the minimum number of players required for a game to start (2-16)
		 * record[7] = the maximum number of players that can participate in a single game (2-16)
		 * record[8] = the basic life cap a player will be refilled to each day (1+)
		 * record[9] = the kind of life penalty (0 = none, 1 = flat $1m, 2 = 1% of total, 3 = increasing, 4 = hardcap
		 * record[10] = whether bot minigames should be displayed in full
		 * record[11] = whether bonus games (Supercash et al.) should be played
		 * record[12] = whether the player level should be updated (and achievements awarded)
		 */
		channel = gameChannel;
		rankChannel = channel.getId().equals("472266492528820226"); //Hardcoding this for now, easy to change later
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
			//"average" player count used for figuring out bots is 4 unless settings demand otherwise
			averagePlayers = Math.max(minPlayers, Math.min(maxPlayers, Math.min(minPlayers+botCount, 4)));
			nextGamePlayers = generateNextGamePlayerCount();
			maxLives = Integer.parseInt(record[8]);
			lifePenalty = LifePenaltyType.values()[Integer.parseInt(record[9])];
			verboseBotGames = BooleanSetting.parseSetting(record[10].toLowerCase(), false);
			doBonusGames = BooleanSetting.parseSetting(record[11].toLowerCase(), true);
			playersLevelUp = BooleanSetting.parseSetting(record[12].toLowerCase(), false);
			newbieProtection = Integer.parseInt(record[13]);
			//Finally, create a game channel with all the settings as instructed
		}
		catch(Exception e1)
		{
			channel.sendMessage("A fatal error has occurred.").queue();
			e1.printStackTrace();
			return;
		}
		reset();
		channel.sendMessage("Ready to play!").queue();
	}
	
	class ControllerThreadFactory implements ThreadFactory
	{
		@Override
		public Thread newThread(Runnable r)
		{
			Thread newThread = new Thread(r);
			newThread.setName(String.format("Game Controller - %s - %s", channel.getGuild().getName(), channel.getName()));
			return newThread;
		}
	}
	
	public void reset()
	{
		if(currentGame != null)
			currentGame.gameOver();
		players.clear();
		runAtGameEnd = null;
		currentTurn = -1;
		playersAlive = 0;
		earlyWinners = 0;
		botsInGame = 0;
		if(gameStatus != GameStatus.SEASON_OVER)
			gameStatus = GameStatus.SIGNUPS_OPEN;
		boardSize = 0;
		repeatTurn = 0;
		firstPick = true;
		resolvingTurn = false;
		resolvingBomb = false;
		coveredUp = null;
		currentBlammo = false;
		futureBlammo = false;
		finalCountdown = false;
		reverse = false;
		starman = false;
		fcTurnsLeft = 99;
		boardMultiplier = 1;
		wagerPot = 0;
		if(timer != null)
			timer.shutdownNow();
		timer = new ScheduledThreadPoolExecutor(1, new ControllerThreadFactory());
		if(runDemo != 0 && botCount >= minPlayers)
		{
			demoMode = timer.schedule(this::runDemo,runDemo,TimeUnit.MINUTES);
		}
	}
	
	int generateNextGamePlayerCount()
	{
		//We use this to decide how many bots we want in our next game
		//This is only called after a game is completed to prevent letting players reroll the rng
		int playerCount = minPlayers;
		while(playerCount < averagePlayers && Math.random() * 3 < 1)
			playerCount++;
		return playerCount;
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
	
	public void runDemo()
	{
		int demoSize = averagePlayers;
		int maxDemoSize = Math.min(maxPlayers, minPlayers+botCount);
		//Pick randomly whether to roll larger or smaller games if available, otherwise pick whichever one we can
		boolean lookUp;
		if(demoSize == minPlayers)
			lookUp = true;
		else if(demoSize == maxDemoSize)
			lookUp = false;
		else
			lookUp = Math.random() < 0.5;
		//With 50% chance, increase/decrease the size of the game by 1 and roll again
		while(Math.random() < 0.5 && ((lookUp && demoSize < maxDemoSize) || (!lookUp && demoSize > minPlayers)))
			demoSize += lookUp ? 1 : -1;
		for(int i=0; i<demoSize; i++)
			addRandomBot();
		startTheGameAlready();
	}

	/**
	 * Adds a player to the game, or updates their name if they're already in.
	 * 
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
		if(newPlayer.getName().contains(":") || newPlayer.getName().contains("#") || newPlayer.getName().startsWith("!"))
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
		//FLAT life penalty = $1,000,000
		//SCALED life penalty = 1% of the player's score, or $100,000 if it's greater
		//INCREASING life penalty = scaled penalty + 20% per additional life spent since running out
		if(newPlayer.lives <= 0 && newPlayer.newbieProtection <= 0 && lifePenalty != LifePenaltyType.NONE)
		{
			//If they've self-excluded, bypass all this and just lock them out
			if(lockoutList.contains(newPlayer.uID))
			{
				channel.sendMessage("Cannot join game: You have no lives remaining.").queue();
				return false;
			}
			int entryFee;
			switch(lifePenalty)
			{
			case NONE:
				entryFee = 0;
				break;
			case FLAT:
				entryFee = 1_000_000;
				break;
			case SCALED:
				entryFee = RtaBMath.calculateEntryFee(newPlayer.money, 0);
				break;
			case INCREASING:
				entryFee = RtaBMath.calculateEntryFee(newPlayer.money, newPlayer.lives);
				break;
			case HARDCAP:
			default:
				channel.sendMessage("Cannot join game: You have no lives remaining.").queue();
				return false;
			}
			newPlayer.addMoney(-1*entryFee,MoneyMultipliersToUse.NOTHING);
			newPlayer.oldMoney = newPlayer.money;
			channel.sendMessage(newPlayer.getSafeMention() + String.format(", you are out of lives. "
					+ "Playing this round will incur an entry fee of $%,d.",entryFee)).queue();
			newPlayer.paidLifePenalty = true;
		}
		//Look for match already in player list
		int playerLocation = findPlayerInGame(newPlayer.uID);
		if(playerLocation != -1)
		{
			//Found them, check if we should update their name or just laugh at them
			if(players.get(playerLocation).getName().equals(newPlayer.getName()))
			{
				channel.sendMessage("Cannot join game: You have already joined the game.").queue();
			}
			else
			{
				players.set(playerLocation,newPlayer);
				channel.sendMessage("Updated in-game name.").queue();
			}
			return false;
		}
		//Haven't found one, add them to the list
		players.add(newPlayer);
		//Remind them of their hidden command
		if(newPlayer.hiddenCommand != HiddenCommand.NONE)
			newPlayer.remindHiddenCommand();
		//Remind everyone if they're close to the goal
		if(newPlayer.money > 900000000)
		{
			channel.sendMessage(String.format("%1$s only needs $%2$,d more to reach the goal!",
					newPlayer.getName(),(1000000000-newPlayer.money))).queue();
		}
		//If there's only one player right now, that means we're starting a new game so schedule the relevant things
		if(players.size() == 1)
		{
			if(runDemo != 0)
				demoMode.cancel(false);
			timer.schedule(() -> 
			{
				if(gameStatus == GameStatus.SIGNUPS_OPEN)
				{
					channel.sendMessage("Thirty seconds before game starts!").queue();
					channel.sendMessage(listPlayers(false)).queue();
				}
			}, 90, TimeUnit.SECONDS);
			timer.schedule(this::startTheGameAlready, 120, TimeUnit.SECONDS);
			channel.sendMessage("Starting a game of Race to a Billion in two minutes. Type !join to sign up.").queue();
		}
		//Finally, wrap up by saying they actually managed to join
		channel.sendMessage(newPlayer.getName() + " joined the game.").queue();
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
		//Only do this if the game hasn't started and there's room in the game!
		if((gameStatus != GameStatus.SIGNUPS_OPEN && gameStatus != GameStatus.ADD_BOT_QUESTION && gameStatus != GameStatus.BOMB_PLACEMENT)
				|| players.size() >= maxPlayers || botNumber >= botCount)
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
		if(playersCanJoin || chosenBot.getHuman().equals("null"))
			newPlayer = new Player(chosenBot,this);
		else
		{
			newPlayer = new Player(
					channel.getGuild().retrieveMemberById(chosenBot.getHuman()).complete(),
					this, chosenBot.getName());
			//Hang on that's a HUMAN, remind them of their hidden command!
			if(newPlayer.hiddenCommand != HiddenCommand.NONE)
				newPlayer.remindHiddenCommand();
		}
		players.add(newPlayer);
		botsInGame ++;
		//Remind everyone if they're close to the goal
		if(newPlayer.money > 900_000_000)
		{
			channel.sendMessage(String.format("%1$s only needs $%2$,d more to reach the goal!",
					newPlayer.getName(),(1_000_000_000-newPlayer.money)));
		}
		channel.sendMessage(newPlayer.getName() + " joined the game.").queue();
		//If they're the first player then don't bother with the timer, but do cancel the demo
		if(players.size() == 1 && runDemo != 0)
			demoMode.cancel(false);
	}
	
	public void addRandomBot()
	{
		//Only do this if the game hasn't started and there's room in the game!
		if((gameStatus != GameStatus.SIGNUPS_OPEN && gameStatus != GameStatus.ADD_BOT_QUESTION && gameStatus != GameStatus.BOMB_PLACEMENT)
				|| players.size() >= maxPlayers)
		{
			channel.sendMessage("A bot can not be added at this time.").queue();
			return;
		}
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
			goodPick = (findPlayerInGame(chosenBot.getBotID()) == -1);
		}
		while(!goodPick && triesLeft > 0);
		if(!goodPick)
		{
			//If we've checked EVERY bot...
			channel.sendMessage("Bot generation failed.").queue();
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
		if(gameStatus == GameStatus.SIGNUPS_OPEN && botCount - botsInGame > 0 && players.size() > botsInGame && players.size() < maxPlayers &&
				//Either we're below the playercount we decided earlier we wanted, or it's a big game already and we're feeling nice
				(players.size() < nextGamePlayers || (players.size() >= averagePlayers && Math.random() < 0.1)))
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
		//Consider placing the season 12 event, with (players-2)/(players) chance
		if(Math.random() * players.size() > 2)
			gameboard.makeSeasonal((int)(Math.random()*boardSize));
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
						boolean allowCheatCodes = (players.size() == 1);
						do
						{
							addRandomBot();
						}
						//Always add 1, then add more depending on how many we decided on earlier
						while(players.size() < nextGamePlayers && botCount > botsInGame);
						//Cheat code - typing 'yeetpeeks' at the prompt starts a game without peeks
						if(allowCheatCodes && e.getMessage().getContentStripped().equalsIgnoreCase("yeetpeeks"))
							for(Player next : players)
								next.peeks = 0;
						timer.schedule(this::startTheGameAlready, 500, TimeUnit.MILLISECONDS);
					}
					else
					{
						channel.sendMessage("Very well.").queue();
						timer.schedule(this::startTheGameAlready, 500, TimeUnit.MILLISECONDS);
					}
				},
				30,TimeUnit.SECONDS, () ->
						timer.schedule(this::startTheGameAlready, 500, TimeUnit.MILLISECONDS));
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
				checkForNotableCover(gameboard.truesightSpace(bombPosition,baseNumerator,baseDenominator));
				gameboard.addBomb(bombPosition);
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
								&& checkValidNumber(e.getMessage().getContentStripped())),
						//Parse it and update the bomb board
						e -> 
						{
							if(players.get(iInner).status == PlayerStatus.OUT)
							{
								int bombLocation = Integer.parseInt(e.getMessage().getContentStripped())-1;
								checkForNotableCover(gameboard.truesightSpace(bombLocation,baseNumerator,baseDenominator));
								gameboard.addBomb(bombLocation);
								players.get(iInner).knownBombs.add(bombLocation);
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
		timer.schedule(this::abortRetryContinue, playersCanJoin?60:90, TimeUnit.SECONDS);
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
			for (Player player : players)
				if (player.status != PlayerStatus.ALIVE)
					player.isBot = true;
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
							&& Arrays.asList(VALID_ARC_RESPONSES).contains(e.getMessage().getContentStripped().toUpperCase()));
				},
				//Read their choice and handle things accordingly
				e -> 
				{
					switch(e.getMessage().getContentStripped().toUpperCase())
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
	
	private synchronized void checkReady()
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
				switch((int)(Math.random()*5))
				{
				case 0:
					snarkMessage.append("One of you covered up this: ").append(coveredUp).append(".");
					break;
				case 1:
					snarkMessage.append("The ").append(coveredUp).append(" space that was once on this board is now a bomb. Oops.");
					break;
				case 2:
					snarkMessage.append("A ").append(coveredUp).append(" space was covered by a bomb. Could there be another one?");
					break;
				case 3:
					snarkMessage.append("One of you covered up this: ").append(coveredUp).append(". I'm not naming names, ").append(players.get((int) (Math.random() * players.size())).getName()).append(".");
					break;
				case 4:
					snarkMessage.append("So much for the ").append(coveredUp).append(" space on this board. You covered it up with a bomb.");
				}
				channel.sendMessage(snarkMessage).queue();
			}
			gameStatus = GameStatus.IN_PROGRESS;
			//Always start with the first player
			currentTurn = 0;
			timer.schedule(() -> runTurn(0), 500, TimeUnit.MILLISECONDS);
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
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		//If someone has given our hapless player a blammo, they get that instead of their normal turn
		if(futureBlammo)
		{
			futureBlammo = false;
			resolvingTurn = true;
			if(repeatTurn > 0)
				repeatTurn --;
			channel.sendMessage(players.get(player).getSafeMention()
					+ ", someone has given you a **BLAMMO!**").queue();
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
				channel.sendMessage(players.get(player).getSafeMention() + ", pick again.").queue();
		}
		else
		{
			firstPick = false;
			if(!players.get(player).isBot)
				channel.sendMessage(players.get(player).getSafeMention() + 
						", your turn. Choose a space on the board.").queue();
		}
		if(repeatTurn > 0)
			repeatTurn --;
		displayBoardAndStatus(true, false, false);
		//Ready up the space picker depending on if it's a bot up next
		if(players.get(player).isBot)
		{
			//Sleep for a couple of seconds so they don't rush
			try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
			//and their logic is complicated so they get their own method
			runAITurn(player);
		}
		else
		{
			warnPlayer = timer.schedule(() -> 
			{
				//If they're out of the round somehow, why are we warning them?
				if(players.get(player).status == PlayerStatus.ALIVE && gameStatus == GameStatus.IN_PROGRESS && player == currentTurn && !resolvingTurn)
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
						if(players.get(player).status != PlayerStatus.ALIVE || gameStatus != GameStatus.IN_PROGRESS || player != currentTurn)
						{
							return true;
						}
						else if(e.getAuthor().equals(players.get(player).user) && e.getChannel().equals(channel)
								&& checkValidNumber(e.getMessage().getContentStripped()))
						{
								int location = Integer.parseInt(e.getMessage().getContentStripped());
								try
								{
									if(pickedSpaces[location-1])
									{
										channel.sendMessage("That space has already been picked.").queue();
										return false;
									}
									else
									{
										return true;
									}
								}
								catch(ArrayIndexOutOfBoundsException e1) { return false; }
						}
						return false;
					},
					//Parse it and call the method that does stuff
					e -> 
					{
						warnPlayer.cancel(false);
						//If they're somehow taking their turn when they're out of the round, just don't do anything
						if(players.get(player).status == PlayerStatus.ALIVE && gameStatus == GameStatus.IN_PROGRESS && player == currentTurn)
						{
							int location = Integer.parseInt(e.getMessage().getContentStripped())-1;
							//Anyway go play out their turn
							timer.schedule(() -> resolveTurn(player, location), 500, TimeUnit.MILLISECONDS);
						}
					},
					90,TimeUnit.SECONDS, () ->
					{
						//If they're somehow taking their turn when they shouldn't be, just don't do anything
						if(players.get(player).status == PlayerStatus.ALIVE && gameStatus == GameStatus.IN_PROGRESS && player == currentTurn && !resolvingTurn)
						{
							timer.schedule(() -> timeOutTurn(player), 500, TimeUnit.MILLISECONDS);
						}
					});
		}
	}
	
	private void runAITurn(int player)
	{
		//Get safe spaces, starting with all unpicked spaces
		ArrayList<Integer> openSpaces = new ArrayList<>(boardSize);
		for(int i=0; i<boardSize; i++)
			if(!pickedSpaces[i])
				openSpaces.add(i);
		//Remove all known bombs
		ArrayList<Integer> safeSpaces = new ArrayList<>(boardSize);
		safeSpaces.addAll(openSpaces);
		for(Integer bomb : players.get(player).knownBombs)
			safeSpaces.remove(bomb);
		//Test for hidden command stuff
		switch(players.get(player).hiddenCommand)
		{
		//Bonus bag under same condition as the fold, but more frequently because of its positive effect
		case BONUS:
			if(!starman && players.get(player).peeks < 1 && players.get(player).jokers == 0 && Math.random() * spacesLeft < 3)
			{
				//Let's just pick one randomly
				SpaceType desire = SpaceType.BOOSTER;
				if(Math.random()*2 < 1)
					desire = SpaceType.GAME;
				if(Math.random()*3 < 1)
					desire = SpaceType.CASH;
				if(Math.random()*4 < 1)
					desire = SpaceType.EVENT;
				useBonusBag(player,desire);
				return;
			}
			break;
		//Blammo under the same condition as the fold, but make sure they aren't repeating turns either
		//We really don't want them triggering a blammo if they have a joker, because it could eliminate them despite it
		//But do increase the chance for it compared to folding
		case BLAMMO:
			if(!starman && players.get(player).peeks < 1 && repeatTurn == 0 &&
					players.get(player).jokers == 0 && Math.random() * spacesLeft < players.size())
				useBlammoSummoner(player);
			break;
		//Wager should be used if it's early enough in the round that it should catch most/all players
		//Teeeeeechnically it isn't playing to win with this, but it is making the game more exciting for the real players.
		case WAGER:
			if(players.size() * 4 < spacesLeft)
				useWager(player);
			break;
		//Truesight under the same condition as a peek
		case TRUESIGHT:
			if(safeSpaces.size() > 1 && Math.random() < 0.5)
			{
				int truesightIndex = (int)(Math.random()*safeSpaces.size());
				int truesightSpace = safeSpaces.get(truesightIndex);
				if(!players.get(player).safePeeks.contains(truesightSpace))
				{
					safeSpaces.remove(truesightIndex); //We know there's another so this is fine
					String truesightIdentity = useTruesight(player,truesightSpace);
					boolean badPeek = false;
					if(truesightIdentity.startsWith("-") || truesightIdentity.contains("BOMB"))
						badPeek = true;
					else
						switch(truesightIdentity)
						{
						case "BLAMMO":
						case "Split & Share":
						case "Bowser Event":
						case "Reverse":
						case "Minefield":
							badPeek = true;
						}
					if(!badPeek)
					{
						resolveTurn(player, truesightSpace);
						return;
					}
				}
			}
			break;
		//With minesweeper, look for an opportunity every turn
		case MINESWEEP:
			if(safeSpaces.size() > 1)
			{
				//Look for a space with only one adjacent to it
				ArrayList<Integer> minesweepOpportunities = new ArrayList<>();
				for(int i = 0; i < boardSize; i++)
					if(!pickedSpaces[i])
					{
						int adjacentSpaces = 0;
						for(int j : RtaBMath.getAdjacentSpaces(i, players.size()))
							if(!pickedSpaces[j])
								adjacentSpaces++;
						if(adjacentSpaces == 1)
							minesweepOpportunities.add(i);
					}
				//If we found one, choose one at random to sweep
				if(minesweepOpportunities.size() > 0)
					useMinesweeper(player, minesweepOpportunities.get((int)(Math.random()*minesweepOpportunities.size())));
			}
		//Fold, Repel, Defuse, and Failsafe are more situational and aren't used at this time
		default:
			break;
		}
		//With chance depending on current board risk, look for a previous peek to use
		if(Math.random() * (spacesLeft - playersAlive) < playersAlive)
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
		/*
		 * Use a peek under the following conditions:
		 * - The bot has one to use
		 * - It hasn't already peeked the space selected
		 * - 50% chance (so it won't always fire immediately)
		 * Note that they never bluff peek their own bomb (it's just easier that way)
		 */
		if(players.get(player).peeks > 0 && safeSpaces.size() > 1 && Math.random() < 0.5)
		{
			int peekSpace = safeSpaces.get((int)(Math.random()*safeSpaces.size()));
			//If we've already seen this space, just take it instead of peeking it again
			if(players.get(player).safePeeks.contains(peekSpace))
			{
				resolveTurn(player, peekSpace);
			}
			else
			{
				//Let the players know what's going on
				channel.sendMessage("("+players.get(player).getName()+" peeks at space "+(peekSpace+1)+")").queue();
				//Then use the peek, and decide what to do based on whether it's safe or not
				if(!usePeek(player,peekSpace).isBomb())
				{
					//50% chance to consider bluffing a safe space (and never in 2p games), then decide based on board risk
					if(Math.random() < 0.5 || players.size() == 2 || Math.random() * (spacesLeft - playersAlive) < playersAlive)
						resolveTurn(player, peekSpace);
					else
						pickRandomSpaceForAITurn(player, openSpaces, safeSpaces);
				}
				else
				{
					//If it's a bomb, we'll just have to remember it and pick from the remaining spaces
					safeSpaces.remove(Integer.valueOf(peekSpace));
					pickRandomSpaceForAITurn(player, openSpaces, safeSpaces);
				}
			}
		}
		//Otherwise just pick a space
		else
			pickRandomSpaceForAITurn(player, openSpaces, safeSpaces);
	}
	
	private void pickRandomSpaceForAITurn(int player, ArrayList<Integer> openSpaces, ArrayList<Integer> safeSpaces)
	{
		//Start by getting a list of every space that opponents have peeked
		ArrayList<Integer> opponentPeeks = new ArrayList<>(players.size());
		for(int i=0; i<players.size(); i++)
			if(i != player)
				opponentPeeks.addAll(players.get(i).allPeeks);
		//Now we can check if every 'safe' space has been peeked
		boolean allSpacesPeeked = true;
		for(Integer next : safeSpaces)
			if(!opponentPeeks.contains(next))
			{
				allSpacesPeeked = false;
				break;
			}
		//Everything's been peeked? Let's try to escape
		if(allSpacesPeeked)
		{
			if(players.get(player).hiddenCommand == HiddenCommand.FAILSAFE)
			{
				useFailsafe(player);
				return;
			}
			if(players.get(player).hiddenCommand == HiddenCommand.DEFUSE)
			{
				int shuffledSpace;
				if(safeSpaces.size() > 0)
					shuffledSpace = safeSpaces.get((int)(Math.random()*safeSpaces.size()));
				else
					shuffledSpace = openSpaces.get((int)(Math.random()*openSpaces.size()));
				useShuffler(player, shuffledSpace);
				resolveTurn(player, shuffledSpace);
				return;
			}
			if(players.get(player).hiddenCommand == HiddenCommand.FOLD)
			{
				useFold(player);
				return;
			}
		}
		//If there's at least one space that might not be a bomb, pick from those
		if(safeSpaces.size() > 0)
		{
			int rollsLeft = 2;
			int chosenSpace;
			//If we're thinking of picking a space that someone else has peeked, let's think twice before we do it
			do
			{
				rollsLeft --;
				chosenSpace = safeSpaces.get((int)(Math.random()*safeSpaces.size()));
			}
			while(opponentPeeks.contains(chosenSpace) && rollsLeft > 0);
			resolveTurn(player, chosenSpace);
		}
		//No escape commands and everything is a bomb? I guess it's our loss.
		else
		{
			resolveTurn(player, openSpaces.get((int)(Math.random()*openSpaces.size())));
		}
	}
	
	public SpaceType usePeek(int playerID, int space)
	{
		Player peeker = players.get(playerID);
		peeker.peeks --;
		peeker.allPeeks.add(space);
		if(peeker.allPeeks.size() == 3)
			Achievement.EXTRA_PEEKS.check(peeker);
		SpaceType peekedSpace = gameboard.getType(space);
		//If it's a bomb, add it to their known bombs
		if(peekedSpace.isBomb())
			peeker.knownBombs.add(space);
		//Otherwise add it to their known safe spaces
		else	
			peeker.safePeeks.add(space);
		//If they're human, actually tell them what they won
		if(!peeker.isBot)
		{
			String peekClaim;
			switch(peekedSpace)
			{
			case CASH:
			case BLAMMO:
				peekClaim = "**CASH**";
				break;
			case GAME:
				peekClaim = "a **MINIGAME**";
				break;
			case BOOSTER:
				peekClaim = "a **BOOSTER**";
				break;
			case EVENT:
			case GRAB_BAG:
				peekClaim = "an **EVENT**";
				break;
			case BOMB:
			case GB_BOMB:
				peekClaim = "a **BOMB**";
				break;
			default:
				peekClaim = "an **ERROR**";
				break;
			}
			peeker.user.openPrivateChannel().queue(
					(channel) -> channel.sendMessage(String.format("Space %d is %s.",space+1,peekClaim)).queue());
		}
		return peekedSpace;
	}
	
	private void timeOutTurn(int player)
	{
		if(resolvingTurn)
			return;
		//If they haven't been warned, play nice and just pick a random space for them
		if(!players.get(player).warned)
		{
			players.get(player).warned = true;
			channel.sendMessage(players.get(player).getSafeMention() + 
					" is out of time. Wasting a random space.").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			//Get unpicked spaces
			ArrayList<Integer> spaceCandidates = new ArrayList<>(boardSize);
			for(int i=0; i<boardSize; i++)
				if(!pickedSpaces[i])
					spaceCandidates.add(i);
			//Pick one at random
			int spaceChosen = spaceCandidates.get((int) (Math.random() * spaceCandidates.size()));
			//If it's a bomb, it sucks to be them
			if(gameboard.getType(spaceChosen).isBomb())
			{
				resolveTurn(player, spaceChosen);
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
				channel.sendMessage("Space " + (spaceChosen+1) + " selected...").queue();
				//Don't forget the threshold
				if(players.get(player).threshold)
				{
					channel.sendMessage(String.format("(-$%,d)",applyBaseMultiplier(THRESHOLD_PER_TURN_PENALTY)))
						.queueAfter(1,TimeUnit.SECONDS);
					players.get(player).addMoney(applyBaseMultiplier(-1*THRESHOLD_PER_TURN_PENALTY),MoneyMultipliersToUse.NOTHING);
				}
				try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
				channel.sendMessage("It's not a bomb, so its contents are lost.").queue();
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
					if(gameboard.getType(i).isBomb() && !pickedSpaces[i])
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
					gameboard.addBomb(bombChosen);
				}
			}
			//NO DUDS ALLOWED
			gameboard.forceExplosiveBomb(bombChosen);
			//KABOOM KABOOM KABOOM KABOOM
			resolveTurn(player, bombChosen);
		}
	}
	
	private void resolveTurn(int player, int location)
	{
		//Try to detect double-turns and negate them before damage is done
		if(pickedSpaces[location] || player != currentTurn)
			return;
		//Check for a hold on the board, and hold it if there isn't
		if(resolvingTurn)
			return;
		else
			resolvingTurn = true;
		//Announce the picked space
		if(players.get(player).isBot)
		{
			channel.sendMessage(players.get(player).getName() + " selects space " + (location+1) + "...").queue();
		}
		else
		{
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("Space " + (location+1) + " selected...").queue();
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
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("...").queue();
		}
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
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
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			awardGame(player, gameboard.getGame(location));
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			awardBoost(player, gameboard.getBoost(location));
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			awardCash(player, gameboard.getCash(location));
			try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); } //mini-suspense lol
			awardEvent(player, gameboard.getEvent(location));
			break;
		case GB_BOMB:
			channel.sendMessage("It's a **Grab Bag**, you're winning some of everything!").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			awardGame(player, gameboard.getGame(location));
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			awardBoost(player, gameboard.getBoost(location));
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			awardCash(player, gameboard.getCash(location));
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); } //mega-mini-suspense lololol
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
		case BLAMMO:
			channel.sendMessage(players.get(player).getSafeMention() + ", it's a **BLAMMO!**").queue();
			startBlammo(player, false);
			return; //Blammos pass to end-turn-logic when they're done, and not before
		}
		runEndTurnLogic();
	}
	
	private void awardBomb(int player, BombType bombType)
	{
		//If player has a joker, change to a dud bomb
		if(players.get(player).jokers != 0)
		{
			channel.sendMessage("But you have a joker!").queueAfter(2,TimeUnit.SECONDS);
			channel.sendMessage("It goes _\\*fizzle*_.").queueAfter(5,TimeUnit.SECONDS);
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			//Don't deduct if negative, to allow for unlimited joker
			if(players.get(player).jokers > 0)
				players.get(player).jokers --;
			bombType = BombType.DUD;
		}
		else
		{
			resolvingBomb = true;
			int penalty = calculateBombPenalty(player);
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			//Pass control to the bomb itself to deal some damage
			bombType.getBomb().explode(this, player, penalty);
			resolvingBomb = false;
		}
	}
	
	public void awardCash(int player, Cash cashType)
	{
		int cashWon;
		String prizeWon = null;
		//Is it Mystery Money? Do that thing instead then
		if(cashType == Cash.MYSTERY)
		{
			channel.sendMessage("It's **Mystery Money**, and this time it awards you...").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
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
		cashWon = applyBaseMultiplier(cashWon*boardMultiplier);
		//On cash, update the player's score and tell them how much they won
		StringBuilder resultString = new StringBuilder();
		if(prizeWon != null)
		{
			resultString.append("It's **");
			if(boardMultiplier * baseNumerator > 1 || baseDenominator > 1)
			{
				resultString.append(boardMultiplier*baseNumerator);
				if(baseDenominator > 1)
					resultString.append("/").append(baseDenominator);
				resultString.append("x ");
			}
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
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage(extraResult.toString()).queue();
		}
		//Award hidden command with 40% chance if cash is negative and they don't have one already
		if(cashWon < 0 && Math.random() < 0.40 && players.get(player).hiddenCommand == HiddenCommand.NONE)
			players.get(player).awardHiddenCommand();
	}

	public void awardBoost(int player, Boost boostType)
	{
		int boostFound = boostType.getValue();
		String resultString = String.format("A **%+d%%** Booster", boostFound) +
				(boostFound > 0 ? "!" : ".");
		players.get(player).addBooster(boostFound);
		channel.sendMessage(resultString).queue();
		//Award hidden command with 40% chance if boost is negative and they don't have one already
		if(boostFound < 0 && Math.random() < 0.40 && players.get(player).hiddenCommand == HiddenCommand.NONE)
			players.get(player).awardHiddenCommand();
	}

	public void awardGame(int player, Game gameFound)
	{
		players.get(player).games.add(gameFound);
		players.get(player).games.sort(null);
		channel.sendMessage("It's a minigame: **" + gameFound.getName() + "**!").queue();
	}
	
	public void awardEvent(int player, EventType eventType)
	{
		//Pass control straight over to the event
		eventType.getEvent().execute(this, player);
	}
	
	private void startBlammo(int player, boolean mega)
	{
		channel.sendMessage("Quick, press a button!\n```" + (mega ? "\n MEGA " : "") + "\nBLAMMO\n 1  2 \n 3  4 \n```").queue();
		currentBlammo = true;
		resolvingTurn = false;
		List<BlammoChoices> buttons = Arrays.asList(BlammoChoices.values());
		Collections.shuffle(buttons);
		if(players.get(player).isBot)
		{
			//Use a relevant hidden command if the AI has one, or just press a button
			switch(players.get(player).hiddenCommand)
			{
			case REPEL:
				useRepel(player);
				break;
			case FOLD:
				useFold(player);
				break;
			case BLAMMO:
				//Revenge!! (then fall through to press a button anyway)
				useBlammoSummoner(player);
			default:
				timer.schedule(() -> runBlammo(player, buttons, (int) (Math.random() * 4), mega), 2, TimeUnit.SECONDS);
			}
		}
		else
		{
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
							(e.getAuthor().equals(players.get(player).user) && e.getChannel().equals(channel)
									&& checkValidNumber(e.getMessage().getContentStripped())
											&& Integer.parseInt(e.getMessage().getContentStripped()) <= 4),
					//Parse it and call the method that does stuff
					e -> 
					{
						//Don't resolve the blammo if there is no blammo right now
						if(currentBlammo)
						{
							int button = Integer.parseInt(e.getMessage().getContentStripped())-1;
							timer.schedule(() -> runBlammo(player, buttons, button, mega), 1, TimeUnit.SECONDS);
						}
					},
					30,TimeUnit.SECONDS, () ->
					{
						if(currentBlammo)
						{
							channel.sendMessage("Too slow, autopicking!").queue();
							int button = (int) (Math.random() * 4);
							timer.schedule(() -> runBlammo(player, buttons, button, mega), 1, TimeUnit.SECONDS);
						}
					});
		}
	}

	private void runBlammo(int player, List<BlammoChoices> buttons, int buttonPressed, boolean mega)
	{
		if(players.get(player).isBot)
		{
			channel.sendMessage(players.get(player).getName() + " presses button " + (buttonPressed+1) + "...").queue();
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
		{
			currentBlammo = false; //Too late to repel now
			resolvingTurn = true;
		}
		StringBuilder extraResult = null;
		int penalty = calculateBombPenalty(player);
		switch(buttons.get(buttonPressed))
		{
		case BLOCK:
			channel.sendMessage("You BLOCKED the BLAMMO!").completeAfter(3,TimeUnit.SECONDS);
			if(mega)
				Achievement.MEGA_DEFUSE.check(players.get(player));
			break;
		case ELIM_YOU:
			channel.sendMessage("You ELIMINATED YOURSELF!").completeAfter(3,TimeUnit.SECONDS);
			channel.sendMessage(String.format("$%,d"+(mega?" MEGA":"")+" penalty!",Math.abs(penalty*(mega?4:1)))).queue();
			extraResult = players.get(player).blowUp((mega?4:1)*penalty,false);
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
			penalty = calculateBombPenalty(playerToKill);
			channel.sendMessage("Goodbye, " + players.get(playerToKill).getSafeMention()
					+ String.format("! $%,d"+(mega?" MEGA":"")+" penalty!",Math.abs(penalty*(mega?4:1)))).queue();
			int tempRepeat = repeatTurn;
			extraResult = players.get(playerToKill).blowUp((mega?4:1)*penalty,false);
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
						if(nextPlayer.splitAndShare)
						{
							channel.sendMessage(String.format("Oh, %s had a split and share? Well there's no one to give your money to,"
									+ " so we'll just take it!", nextPlayer.getName()))
								.completeAfter(2, TimeUnit.SECONDS);
							nextPlayer.money *= 0.9;
							nextPlayer.splitAndShare = false;
						}
						//We don't use the typical penalty calculation method here because we're wiping out everyone in one go
						penalty = applyBaseMultiplier(nextPlayer.newbieProtection > 0 ? NEWBIE_BOMB_PENALTY : BOMB_PENALTY);
						channel.sendMessage(String.format("$%1$,d MEGA penalty for %2$s!",
								Math.abs(penalty*4),nextPlayer.getSafeMention())).completeAfter(2,TimeUnit.SECONDS);
						extraResult = nextPlayer.blowUp(penalty*4,false);
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
	
	private void runEndTurnLogic()
	{
		//Release the hold placed on the board
		resolvingTurn = false;
		//Make sure the game isn't already over
		if(gameStatus == GameStatus.END_GAME)
			return;
		//Test if game over - either all spaces gone and no blammo queued, or one player left alive
		if((spacesLeft <= 0 && !futureBlammo) || playersAlive <= 0 || (earlyWinners == 0 && playersAlive == 1)) 
		{
			gameOver();
		}
		else
		{
			//Advance turn to next player if there isn't a repeat going
			if(repeatTurn == 0)
				advanceTurn(false);
			timer.schedule(() -> runTurn(currentTurn), 1, TimeUnit.SECONDS);
		}
	}
	
	public void advanceTurn(boolean endGame)
	{
		//Keep spinning through until we've got someone who's still in the game, or until we've checked everyone
		int triesLeft = players.size();
		boolean isPlayerGood = false;
		do
		{
			//Subtract rather than add if we're reversed
			currentTurn += reverse ? -1 : 1;
			triesLeft --;
			currentTurn = Math.floorMod(currentTurn,players.size());
			//Is this player someone allowed to play now?
			switch(players.get(currentTurn).status)
			{
			case ALIVE:
				isPlayerGood = true;
				break;
			case FOLDED:
			case WINNER:
				if(endGame)
					isPlayerGood = true;
				break;
			default:
				break;
			}
		}
		while(!isPlayerGood && triesLeft > 0);
		//If we've checked everyone and no one is suitable anymore, whatever
		if(triesLeft == 0 && !isPlayerGood)
			currentTurn = -1;
	}
	
	void gameOver()
	{
		if(gameStatus == GameStatus.END_GAME)
			return;
		else
			gameStatus = GameStatus.END_GAME;
		if(spacesLeft < 0)
			channel.sendMessage("An error has occurred, ending the game, @Telna#2084 fix pls").queue();
		try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
		//Keep this one as complete since it's such an important spot
		channel.sendMessage("Game Over.").complete();
		currentBlammo = false;
		playersAlive += earlyWinners;
		if(spacesLeft > 0)
		{
			channel.sendMessage(gridList(true)).queue();
			detonateBombs(false);
		}
		timer.schedule(this::runNextEndGamePlayer, 1, TimeUnit.SECONDS);
	}

	public String gridList(boolean skipPickedSpaces)
	{
		StringBuilder output = new StringBuilder();
		for(int i=0; i<boardSize; i++)
			if(!skipPickedSpaces || !pickedSpaces[i])
				//Add the space number and contents to the list
				output.append(String.format("Space %d: %s\n", i+1, gameboard.truesightSpace(i,baseNumerator,baseDenominator)));
		return output.toString();
	}
	
	public int detonateBombs(boolean sendMessages)
	{
		int bombsDestroyed = 0;
		for(int i=0; i<boardSize; i++)
			if(!pickedSpaces[i] && (gameboard.getType(i).isBomb()))
			{
				if(sendMessages)
					channel.sendMessage("Bomb in space " + (i+1) + " destroyed.").queueAfter(1,TimeUnit.SECONDS);
				pickedSpaces[i] = true;
				spacesLeft --;
				bombsDestroyed ++;
			}
		return bombsDestroyed;
	}
	
	private void runNextEndGamePlayer()
	{
		//Are there any winners left to loop through?
		advanceTurn(true);
		//If we're out of people to run endgame stuff with, pass to the finish method
		if(currentTurn == -1)
		{
			runFinalEndGameTasks();
			return;
		}
		//No? Good. Let's get someone to reward!
		//If they won the round early, set them alive now so they're recognised
		if(players.get(currentTurn).status == PlayerStatus.WINNER)
			players.get(currentTurn).status = PlayerStatus.ALIVE;
		//If they're a winner, boost their winstreak (folded players don't get this)
		if(players.get(currentTurn).status == PlayerStatus.ALIVE)
		{
			channel.sendMessage(players.get(currentTurn).getSafeMention() + " Wins!")
				.completeAfter(1,TimeUnit.SECONDS);
			if(futureBlammo)
				Achievement.SUMMON_ESCAPE.check(players.get(currentTurn));
			//+0.5 per opponent defeated on a solo win, reduced on joint wins based on the ratio of surviving opponents
			players.get(currentTurn).addWinstreak((5 - (playersAlive-1)*5/(players.size()-1)) * (players.size() - playersAlive));
		}
		//Now the winstreak is right, we can display the board
		displayBoardAndStatus(false, false, false);
		int jokerCount = players.get(currentTurn).jokers;
		//If they're a winner and they weren't running diamond armour, give them a win bonus (folded players don't get this)
		if(players.get(currentTurn).status == PlayerStatus.ALIVE && jokerCount >= 0)
		{
			//Award $20k for each space picked, plus 5% extra per unused peek
			//Then double it if every space was picked, and split it with any other winners
			int winBonus = applyBaseMultiplier(20000 + 1000*players.get(currentTurn).peeks) * (boardSize - spacesLeft);
			if(spacesLeft <= 0)
				winBonus *= 2;
			winBonus /= playersAlive;
			if(spacesLeft <= 0 && playersAlive == 1)
			{
				channel.sendMessage("**SOLO BOARD CLEAR!**").queue();
				if(players.size() >= 14)
					Achievement.SOLO_BOARD_CLEAR.check(players.get(currentTurn));
			}
			channel.sendMessage(players.get(currentTurn).getName() + " receives a win bonus of **$"
					+ String.format("%,d",winBonus) + "**.").queue();
			StringBuilder extraResult = null;
			extraResult = players.get(currentTurn).addMoney(winBonus,MoneyMultipliersToUse.BONUS_ONLY);
			if(extraResult != null)
				channel.sendMessage(extraResult).queue();
		}
		//Now for other winner-only stuff (done separately to avoid conflicting with Midas Touch)
		if(players.get(currentTurn).status == PlayerStatus.ALIVE)
		{
			//If there's any wager pot, award their segment of it
			if(wagerPot > 0)
			{
				channel.sendMessage(String.format("You won $%,d from the wager!", wagerPot / playersAlive)).queue();
				players.get(currentTurn).addMoney(wagerPot / playersAlive, MoneyMultipliersToUse.NOTHING);
			}
			//Award the Jackpot if it's there
			if(players.get(currentTurn).jackpot > 0)
			{
				int jackpotAmount = applyBaseMultiplier(1_000_000*players.get(currentTurn).jackpot);
				channel.sendMessage(String.format("You won the $%,d **JACKPOT**!",jackpotAmount)).queue();
				players.get(currentTurn).addMoney(jackpotAmount,MoneyMultipliersToUse.NOTHING);
				if(players.get(currentTurn).jackpot > players.size()*4 + 5)
					Achievement.BIG_JACKPOT.check(players.get(currentTurn));
			}
		}
		//Cash in unused jokers, folded or not
		if(jokerCount > 0)
		{
			channel.sendMessage(String.format("You cash in your unused joker"+(jokerCount!=1?"s":"")+
					" for **$%,d**.", jokerCount * applyBaseMultiplier(250_000))).queue();
			StringBuilder extraResult = players.get(currentTurn).addMoney(applyBaseMultiplier(250_000)*jokerCount, 
					MoneyMultipliersToUse.BONUS_ONLY);
			if(extraResult != null)
				channel.sendMessage(extraResult).queue();
		}
		//Then, folded or not, play out any minigames they've won
		if(players.get(currentTurn).status == PlayerStatus.FOLDED)
			players.get(currentTurn).status = PlayerStatus.OUT;
		else
			players.get(currentTurn).status = PlayerStatus.DONE;
		timer.schedule(() -> prepareNextMiniGame(players.get(currentTurn).games.listIterator(0)), 1, TimeUnit.SECONDS);
	}
	
	private void prepareNextMiniGame(ListIterator<Game> gamesToPlay)
	{
		if(gamesToPlay.hasNext())
		{
			//Get the minigame
			Game nextGame = gamesToPlay.next();
			currentGame = nextGame.getGame();
			//Count the number of copies
			int multiplier = 1;
			while(gamesToPlay.hasNext())
			{
				//Move the iterator back one, to the first instance of the game
				gamesToPlay.previous();
				//If it matches (ie multiple copies), remove one and add it to the multiplier
				if(gamesToPlay.next() == gamesToPlay.next())
				{
					multiplier++;
					gamesToPlay.remove();
				}
				//Otherwise we'd better put it back where it belongs
				else
				{
					gamesToPlay.previous();
					break;
				}
			}
			//Pass to the game
			boolean sendMessages = !(players.get(currentTurn).isBot) || verboseBotGames;
			//Set up the thread we'll send to the game
			Thread postGame = new Thread(() -> {
				//Recurse to get to the next minigame
				currentGame = null;
				if(players.get(currentTurn).games.size() > 0)
					prepareNextMiniGame(players.get(currentTurn).games.listIterator(gamesToPlay.nextIndex()));
				else
					runNextEndGamePlayer();
			});
			postGame.setName(String.format("%s - %s - %s", 
					channel.getName(), players.get(currentTurn).getName(), currentGame.getName()));
			currentGame.initialiseGame(channel, sendMessages, baseNumerator, baseDenominator, multiplier,
					players, currentTurn, postGame, players.get(currentTurn).enhancedGames.contains(nextGame));
		}
		else
		{
			//Check for winning the game
			if(players.get(currentTurn).money >= 1000000000 && players.get(currentTurn).status == PlayerStatus.DONE)
			{
				winners.add(players.get(currentTurn));
			}
			runNextEndGamePlayer();
		}
	}
	
	public void runFinalEndGameTasks()
	{
		saveData();
		players.sort(new PlayerDescendingRoundDeltaSorter());
		displayBoardAndStatus(false, true, true);
		if(runAtGameEnd != null)
			runAtGameEnd.start();
		reset();
		timer.schedule(this::runPingList, 1, TimeUnit.SECONDS);
		nextGamePlayers = generateNextGamePlayerCount();
		if(winners.size() > 0)
		{
			//Got a single winner, crown them!
			if(winners.size() <= 1)
			{
				players.addAll(winners);
				currentTurn = 0;
				for(int i=0; i<3; i++)
				{
					channel.sendMessage("**" + players.get(0).getName().toUpperCase() + " WINS RACE TO A BILLION!**")
						.queueAfter(5+(5*i),TimeUnit.SECONDS);
				}
				if(runDemo != 0)
					demoMode.cancel(false); //Season's over no demo needed
				if(rankChannel)
					channel.sendMessage("@everyone").queueAfter(20,TimeUnit.SECONDS);
				gameStatus = GameStatus.SEASON_OVER;
				if(!players.get(0).isBot && rankChannel)
				{
					timer.schedule(() -> 
					{
						channel.sendMessage(players.get(0).getSafeMention() + "...").complete();
						channel.sendMessage("It is time to enter the Super Bonus Round.").completeAfter(5,TimeUnit.SECONDS);
						channel.sendMessage("...").completeAfter(10,TimeUnit.SECONDS);
						TestMinigameCommand.runGame(players.get(0).user,Game.SUPERBONUSROUND,channel, false);
					}, 90, TimeUnit.SECONDS);
				}
			}
			//Hold on, we have *multiple* winners? ULTIMATE SHOWDOWN HYPE
			else
			{
				//Tell them what's happening
				StringBuilder announcementText = new StringBuilder();
				for(Player next : winners)
				{
					next.initPlayer(this);
					next.peeks = 0; // No peeks in the final showdown :)
					announcementText.append(next.getSafeMention()).append(", ");
				}
				announcementText.append("you have reached the goal together.");
				channel.sendMessage(announcementText.toString()).completeAfter(5,TimeUnit.SECONDS);
				channel.sendMessage("BUT THERE CAN BE ONLY ONE.").completeAfter(5,TimeUnit.SECONDS);
				channel.sendMessage("**PREPARE FOR THE FINAL SHOWDOWN!**").completeAfter(5,TimeUnit.SECONDS);
				//Prepare the game
				players.addAll(winners);
				winners.clear();
				startTheGameAlready();
			}
		}
	}
	
	static class PlayerDescendingRoundDeltaSorter implements Comparator<Player>
	{
		@Override
		public int compare(Player arg0, Player arg1)
		{
			return arg1.getRoundDelta() - arg0.getRoundDelta();
		}
	}
	
	private void saveData()
	{
		try
		{
			List<String> list = Files.readAllLines(Paths.get("scores","scores"+channel.getId()+".csv"));
			//Go through each player in the game to update their stats
			for(int i=0; i<players.size(); i++)
			{
				/*
				 * Special case - if you lose the round with $1B you get bumped to $999,999,999
				 * so that an elimination without penalty (eg bribe) doesn't get you declared champion
				 * This is since you haven't won yet, after all (and it's *extremely* rare to win a round without turning a profit)
				 * Note that in the instance of a final showdown, both players are temporarily labelled champion
				 * But after the tie is resolved, one will be bumped back to $900M
				 */
				if(players.get(i).money == 1_000_000_000 && players.get(i).status != PlayerStatus.DONE)
					players.get(i).money --;
				//Send messages based on special status
				if(players.get(i).newbieProtection == 1) //Out of newbie protection
					channel.sendMessage(players.get(i).getSafeMention() + ", your newbie protection has expired. "
							+ "From now on, your base bomb penalty will be $250,000.").queue();
				if(players.get(i).totalLivesSpent % 5 == 0 && players.get(i).getEnhanceCap() > players.get(i).enhancedGames.size())
				{ //Just earned an enhancement (or spent 5 lives with an open slot - we don't want to remind them every game)
					if(players.get(i).isBot)
					{
						/* Bots need to pick a minigame to enhance on their own, so we do that now
						 * But first, check to make sure there is a minigame to put in that slot
						 * (In ultra-low base-multiplier seasons, this might actually be an issue)
						 * (It'd take roughly 3000 lives spent though)
						 */
						int enhanceableGames = 0;
						for(Game next : Game.values())
							if(next.getWeight(players.size()) > 0)
								enhanceableGames ++;
						if(players.get(i).enhancedGames.size() < enhanceableGames)
						{
							Game chosenGame;
							do
							{
								chosenGame = Board.generateSpaces(1,players.size(),Game.values()).get(0);
							}
							while(players.get(i).enhancedGames.contains(chosenGame)); //Reroll until we find one they haven't already done
							players.get(i).enhancedGames.add(chosenGame);
							channel.sendMessage(players.get(i).getName() + " earned an enhancement slot and chose to enhance "
									+ chosenGame.getName() + "!").queue();
						}
					}
					else
						channel.sendMessage(players.get(i).getSafeMention() + ", you have earned an enhancement slot! "
								+ "Use the !enhance command to pick a minigame to enhance.").queue();
				}
				//Find if they already exist in the savefile, and where
				String[] record;
				int location = -1;
				for(int j=0; j<list.size(); j++)
				{
					record = list.get(j).split("#");
					if(record[0].compareToIgnoreCase(players.get(i).uID) == 0)
					{
						location = j;
						break;
					}
				}
				//Then build their record and put it in the right place
				StringBuilder toPrint = new StringBuilder();
				toPrint.append(players.get(i).uID);
				toPrint.append("#").append(players.get(i).getName());
				toPrint.append("#").append(players.get(i).money);
				toPrint.append("#").append(players.get(i).booster);
				toPrint.append("#").append(players.get(i).winstreak);
				toPrint.append("#").append(Math.max(players.get(i).newbieProtection - 1, 0));
				toPrint.append("#").append(players.get(i).lives);
				toPrint.append("#").append(players.get(i).lifeRefillTime);
				toPrint.append("#").append(players.get(i).hiddenCommand);
				toPrint.append("#").append(players.get(i).boostCharge);
				toPrint.append("#").append(players.get(i).annuities);
				toPrint.append("#").append(players.get(i).totalLivesSpent);
				toPrint.append("#").append(players.get(i).enhancedGames);
				//If they already exist in the savefile then replace their record, otherwise add them
				if(location == -1)
					list.add(toPrint.toString());
				else
					list.set(location,toPrint.toString());
				//Update their player level if relevant
				if(playersLevelUp)
				{
					PlayerLevel playerLevelData = new PlayerLevel(channel.getGuild().getId(),players.get(i).uID,players.get(i).getName());
					boolean levelUp = playerLevelData.addXP(players.get(i).money - players.get(i).originalMoney);
					if(levelUp)
						channel.sendMessage(players.get(i).getSafeMention() + " has achieved Level " + playerLevelData.getTotalLevel() + "!").queue();
					playerLevelData.saveLevel();
				}
				//Update a player's role if it's the role channel, they're human, and have earned a new one
				if(players.get(i).money/100_000_000 != players.get(i).currentCashClub && !players.get(i).isBot && rankChannel)
				{
					//Get the mod controls
					Guild guild = channel.getGuild();
					List<Role> rolesToAdd = new LinkedList<>();
					List<Role> rolesToRemove = new LinkedList<>();
					//Remove their old score role if they had one
					if(players.get(i).originalMoney/100_000_000 > 0 && players.get(i).originalMoney/100_000_000 < 10)
						rolesToRemove.addAll(guild.getRolesByName(
										String.format("$%d00M",players.get(i).originalMoney/100_000_000),false));
					//Special case for removing Champion role in case of final showdown
					else if(players.get(i).originalMoney/100_000_000 == 10)
						rolesToRemove.addAll(guild.getRolesByName("Champion",false));
					//Add their new score role if they deserve one
					if(players.get(i).money/100_000_000 > 0 && players.get(i).money/100_000_000 < 10)
						rolesToAdd.addAll(guild.getRolesByName(
										String.format("$%d00M",players.get(i).money/100_000_000),false));
					//Or do fancy stuff for the Champion
					else if(players.get(i).money/100_000_000 == 10)
						rolesToAdd.addAll(guild.getRolesByName("Champion",false));
					//Then add/remove appropriately
					guild.modifyMemberRoles(players.get(i).member,rolesToAdd,rolesToRemove).queue();
				}
			}
			//Then sort and rewrite it
			list.sort(new DescendingScoreSorter());
			Path file = Paths.get("scores","scores"+channel.getId()+".csv");
			Path oldFile = Files.move(file, file.resolveSibling("scores"+channel.getId()+"old.csv"));
			Files.write(file, list);
			Files.delete(oldFile);
		}
		catch(IOException e)
		{
			System.err.println("Could not save data in "+channel.getName());
			e.printStackTrace();
		}
	}
	
	static class DescendingScoreSorter implements Comparator<String>
	{
		@Override
		public int compare(String arg0, String arg1) {
			String[] string0 = arg0.split("#");
			String[] string1 = arg1.split("#");
			int test0 = Integer.parseInt(string0[2]);
			int test1 = Integer.parseInt(string1[2]);
			//Deliberately invert it to get descending order
			return test1 - test0;
		}
	}

	private void runPingList()
	{
		//Don't do this if no one's actually there to ping
		if(pingList.size() == 0)
			return;
		StringBuilder output = new StringBuilder();
			output.append("The game is finished");
		for(String nextName : pingList)
		{
			output.append(" - ");
			output.append(nextName);
		}
		channel.sendMessage(output.toString()).complete(); //This needs to be complete() or the thread closes before the message sends
		pingList.clear();
	}
	
	public int applyBaseMultiplier(int amount)
	{
		return RtaBMath.applyBaseMultiplier(amount, baseNumerator, baseDenominator);
	}
	
	public int calculateBombPenalty(int victim)
	{
		//Start with the appropriate penalty for the player and apply the base multiplier
		int penalty = applyBaseMultiplier(players.get(victim).newbieProtection > 0 ? NEWBIE_BOMB_PENALTY : BOMB_PENALTY);
		//Reduce penalty by 10% for each opponent already out, up to five
		penalty /= 10;
		penalty *= (10 - Math.min(5,players.size()-playersAlive));
		return penalty;
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
				resultString.append(next.getName());
			}
		}
		return resultString.toString();
	}
	
	public boolean checkValidNumber(String message)
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
		int nameLength = players.get(0).getName()
				.codePointCount(0,players.get(0).getName().length());
		for(int i=1; i<players.size(); i++)
			nameLength = Math.max(nameLength,players.get(i).getName()
					.codePointCount(0,players.get(i).getName().length()));
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
			moneyLength = String.valueOf(Math.abs(players.get(0).getRoundDelta())).length();
			for(int i=1; i<players.size(); i++)
				moneyLength = Math.max(moneyLength,
						String.valueOf(Math.abs(players.get(i).getRoundDelta())).length());		
		}
		//Make a little extra room for the commas
		moneyLength += (moneyLength-1)/3;
		//Then start printing - including pointer if currently their turn
		for(int i=0; i<players.size(); i++)
		{
			board.append(currentTurn == i ? "> " : "  ");
			board.append(String.format("%-"+nameLength+"s",players.get(i).getName()));
			//Now figure out if we need a negative sign, a space, or neither
			int playerMoney = players.get(i).getRoundDelta();
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
				else if(players.get(i).peeks > 0)
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
			case WINNER:
				board.append("  [WIN] ");
			}
			//If they have any games, print them too
			if(players.get(i).games.size() > 0)
			{
				board.append(" {");
				for(Game minigame : players.get(i).games)
				{
					board.append(" ").append(minigame.getShortName());
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
	
	//Hidden Commands

	public void useFold(int player)
	{
		Player folder = players.get(player);
		channel.sendMessage(folder.getName() + " folded!").queue();
		folder.hiddenCommand = HiddenCommand.NONE;
		//Mark them as folded if they have minigames, or qualified for a bonus game
		if(folder.games.size() > 0)
		{
			channel.sendMessage("You'll still get to play your minigame"+(folder.games.size() != 1?"s":"")+", too.").queueAfter(1,TimeUnit.SECONDS);
			folder.status = PlayerStatus.FOLDED;
		}
		//Otherwise just mark them as out
		else folder.status = PlayerStatus.OUT;
		playersAlive --;
		folder.splitAndShare = false;
		//If it was the active player or there's only one left after this, shift things over to the next turn
		if(player == currentTurn || playersAlive <= 1)
			currentPlayerFoldedLogic();
	}
	private void currentPlayerFoldedLogic()
	{
		repeatTurn = 0;
		if(currentBlammo)
		{
			currentBlammo = false;
			futureBlammo = true; //Folding out of a blammo redirects it to the next player in line
		}
		//If they didn't fold out of a blammo, make sure the final countdown doesn't tick, as they didn't pick a space
		else if(finalCountdown)
			fcTurnsLeft ++;
		runEndTurnLogic();
	}
	public void useRepel(int player)
	{
		Player repeller = players.get(player);
		channel.sendMessage("But " + repeller.getName() + " repelled the blammo!").queue();
		repeller.hiddenCommand = HiddenCommand.NONE;
		currentBlammo = false;
		repeatTurn++;
		runEndTurnLogic();
	}
	public void useBlammoSummoner(int player)
	{
		Player summoner = players.get(player);
		channel.sendMessage(summoner.getName() + " summoned a blammo for the next player!").queue();
		summoner.hiddenCommand = HiddenCommand.NONE;
		futureBlammo = true;
	}
	public void useShuffler(int player, int space)
	{
		Player shuffler = players.get(player);
		channel.sendMessage(shuffler.getName() + " reshuffled space " + (space+1) + "!").queue();
		shuffler.hiddenCommand = HiddenCommand.NONE;
		gameboard.rerollSpace(space, players.size());
	}
	public void useWager(int player)
	{
		Player wagerer = players.get(player);
		channel.sendMessage(wagerer.getName() + " started a wager!").queue();
		wagerer.hiddenCommand = HiddenCommand.NONE;
		long totalBank = 0;
		for(Player next : players)
			totalBank += Math.max(0, next.money);
		//Wager amount is 1% of the average player bank
		int amountToWager = (int)(totalBank / players.size()) / 100;
		//Minimum wager of $1m x base multiplier, except for newbies
		amountToWager = Math.max(amountToWager, applyBaseMultiplier(1_000_000));
		int newbieWager = applyBaseMultiplier(100_000);
		channel.sendMessage(String.format("Everyone bets $%,d as a wager on the game!",amountToWager)).queue();
		wagerPot += amountToWager * playersAlive;
		for(Player next : players)
			if(next.status == PlayerStatus.ALIVE)
			{
				if(next.newbieProtection > 0) //newbies get subsidised
				{
					next.addMoney(-1*newbieWager, MoneyMultipliersToUse.NOTHING);
					channel.sendMessage(String.format("(%s only paid $%,d due to newbie protection)", next.getName(), newbieWager)).queue();
				}
				else
					next.addMoney(-1*amountToWager, MoneyMultipliersToUse.NOTHING);
			}
	}
	public void useBonusBag(int player, SpaceType desire)
	{
		Player bagger = players.get(player);
		channel.sendMessage(bagger.getName() + " dips into the bonus bag and finds...").queue();
		bagger.hiddenCommand = HiddenCommand.NONE;
		if(warnPlayer != null)
			warnPlayer.cancel(false);
		resolvingTurn = true;
		timer.schedule(() ->
		{
			switch(desire)
			{
			case BOMB:
				channel.sendMessage("It's a **BOMB**.").queue();
				awardBomb(player, BombType.NORMAL); //Never roll the bomb, so potential use in avoiding bankrupt
				break;
			case CASH:
				awardCash(player, Board.generateSpaces(1, players.size(), Cash.values()).get(0));
				break;
			case BOOSTER:
				awardBoost(player, Board.generateSpaces(1, players.size(), Boost.values()).get(0));
				break;
			case GAME:
				awardGame(player, players.get(player).generateEventMinigame());
				break;
			case EVENT:
				awardEvent(player, Board.generateSpaces(1, players.size(), EventType.values()).get(0));
				break;
			default:
				channel.sendMessage("Nothing. Did you do something weird?").queue();
			}
			if(bagger.hiddenCommand == HiddenCommand.BONUS)
				Achievement.BAGCEPTION.check(bagger); //Sorry I outed you, but it'll only happen once!
			runEndTurnLogic();
		}, 5, TimeUnit.SECONDS);
	}
	public String useTruesight(int player, int space)
	{
		Player eyeballer = players.get(player);
		channel.sendMessage(eyeballer.getName() + " used an Eye of Truth to look at space " + (space+1) + "!").queue();
		eyeballer.allPeeks.add(space);
		if(eyeballer.allPeeks.size() == 3)
			Achievement.EXTRA_PEEKS.check(eyeballer);
		eyeballer.hiddenCommand = HiddenCommand.NONE;
		String spaceIdentity = gameboard.truesightSpace(space,baseNumerator,baseDenominator);
		SpaceType peekedSpace = gameboard.getType(space);
		//Add the space to the internal list, the same as with a regular peek
		if(peekedSpace.isBomb())
			eyeballer.knownBombs.add(space);
		//Otherwise add it to their known safe spaces
		else	
			eyeballer.safePeeks.add(space);
		if(!eyeballer.isBot)
			eyeballer.user.openPrivateChannel().queue(
				(channel) -> channel.sendMessage(String.format("Space %d: **%s**.",space+1,spaceIdentity)).queue());
		return spaceIdentity;
	}
	public void useMinesweeper(int player, int space)
	{
		Player minesweeper = players.get(player);
		channel.sendMessage(minesweeper.getName() + " used a Minesweeper to sweep around space " + (space+1) + "!").queue();
		minesweeper.hiddenCommand = HiddenCommand.NONE;
		ArrayList<Integer> adjacentSpaces = new ArrayList<>(8);
		int adjacentBombs = 0;
		for(int i : RtaBMath.getAdjacentSpaces(space, players.size()))
		{
			if(!pickedSpaces[i])
			{
				adjacentSpaces.add(i);
				if(gameboard.getType(i).isBomb())
					adjacentBombs ++;
			}
		}
		if(adjacentBombs == 0)
			minesweeper.safePeeks.addAll(adjacentSpaces);
		else if(adjacentBombs == adjacentSpaces.size())
			minesweeper.knownBombs.addAll(adjacentSpaces);
		if(!minesweeper.isBot)
		{
			final int bombCount = adjacentBombs;
			if(bombCount == 1)
				minesweeper.user.openPrivateChannel().queue(
				(channel) -> channel.sendMessage(String.format("There is %d unpicked bomb adjacent to space %d.",
						bombCount,space+1)).queue());
			else
				minesweeper.user.openPrivateChannel().queue(
				(channel) -> channel.sendMessage(String.format("There are %d unpicked bombs adjacent to space %d.",
						bombCount,space+1)).queue());
		}
	}
	public void useFailsafe(int player)
	{
		Player failsafeUser = players.get(player);
		channel.sendMessage(failsafeUser.getName() + " has engaged the failsafe...").queue();
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		failsafeUser.hiddenCommand = HiddenCommand.NONE;
		//Search for any unpicked non-bomb spaces
		boolean success = true;
		for(int i=0; i<boardSize; i++)
			if(!pickedSpaces[i] && !gameboard.getType(i).isBomb()
					//If they have S&S, then a second S&S counts as a bomb too (TDTTOE)
					&& (!failsafeUser.splitAndShare || gameboard.getType(i) != SpaceType.EVENT || gameboard.getEvent(i) != EventType.SPLIT_SHARE))
			{
				success = false;
				break;
			}
		if(success)
		{
			//If it's all bombs, they win!
			channel.sendMessage("And successfully escaped the round!").queue();
			failsafeUser.status = PlayerStatus.WINNER;
			playersAlive --;
			earlyWinners ++;
			failsafeUser.splitAndShare = false;
			//If it was the active player or there's only one left after this, shift things over to the next turn
			if(player == currentTurn || playersAlive <= 1)
				currentPlayerFoldedLogic();
		}
		else
		{
			//If it's not all bombs, get owned
			channel.sendMessage("But there is still at least one safe space.").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			int fine = applyBaseMultiplier(1_000_000);
			channel.sendMessage(failsafeUser.getName() + String.format(" was fined $%,d.",fine)).queue();
			failsafeUser.addMoney(-1*fine, MoneyMultipliersToUse.NOTHING);
		}
	}
}
