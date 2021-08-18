package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import static tel.discord.rtab.RaceToABillionBot.waiter;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Boost;
import tel.discord.rtab.board.Cash;
import tel.discord.rtab.board.EventType;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.SpaceType;
import tel.discord.rtab.commands.channel.BooleanSetting;
import tel.discord.rtab.games.MiniGame;

public class GameController
{
	//Constants
	final static String[] VALID_ARC_RESPONSES = {"A","ABORT","R","RETRY","C","CONTINUE"};
	final static String[] NOTABLE_SPACES = {"$1,000,000","+500% Boost","+300% Boost","BLAMMO",
			"Jackpot","Starman","Split & Share","Minefield","Blammo Frenzy","Joker","Midas Touch","Bowser Event"};
	final static int THRESHOLD_PER_TURN_PENALTY = 100_000;
	static final int BOMB_PENALTY = -250_000;
	static final int NEWBIE_BOMB_PENALTY = -100_000;
	//Other useful technical things
	public ScheduledThreadPoolExecutor timer;
	public TextChannel channel, resultChannel;
	public ScheduledFuture<?> demoMode;
	private Message waitingMessage;
	public HashSet<String> pingList = new HashSet<>();
	ScheduledFuture<?> warnPlayer;
	Thread runAtGameEnd = null;
	//Settings that can be customised
	public int baseNumerator, baseDenominator, botCount, minPlayers, maxPlayers;
	public int maxLives;
	public int runDemo;
	public LifePenaltyType lifePenalty;
	boolean rankChannel, verboseBotGames, doBonusGames, playersLevelUp;
	public boolean playersCanJoin = true;
	//Game variables
	public GameStatus gameStatus = GameStatus.LOADING;
	public final List<Player> players = new ArrayList<>(16);
	public Board gameboard;
	public boolean[] pickedSpaces;
	public int currentTurn;
	public int playersAlive;
	int botsInGame;
	public int repeatTurn;
	public int boardSize, spacesLeft;
	public boolean firstPick;
	public boolean resolvingTurn;
	String coveredUp;
	public MiniGame currentGame;
	//Event variables
	public int boardMultiplier;
	public int fcTurnsLeft;
	int wagerPot;
	int peekStreak;
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
			maxLives = Integer.parseInt(record[8]);
			lifePenalty = LifePenaltyType.values()[Integer.parseInt(record[9])];
			verboseBotGames = BooleanSetting.parseSetting(record[10].toLowerCase(), false);
			doBonusGames = BooleanSetting.parseSetting(record[11].toLowerCase(), true);
			playersLevelUp = BooleanSetting.parseSetting(record[12].toLowerCase(), false);
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
		wagerPot = 0;
		peekStreak = 0;
		if(timer != null)
			timer.shutdownNow();
		timer = new ScheduledThreadPoolExecutor(1, new ControllerThreadFactory());
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
			newPlayer = new Player(
					channel.getGuild().retrieveMemberById(chosenBot.getHuman()).complete(),
					this, chosenBot.getName());
		players.add(newPlayer);
		botsInGame ++;
		if(newPlayer.money > 900_000_000)
		{
			channel.sendMessage(String.format("%1$s needs only $%2$,d more to reach the goal!",
					newPlayer.getName(),(1_000_000_000-newPlayer.money)));
		}
		channel.sendMessage(newPlayer.getName() + " joined the game.").queue();
		//If they're the first player then don't bother with the timer, but do cancel the demo
		if(players.size() == 1 && runDemo != 0)
			demoMode.cancel(false);
		return;
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
		boardSize = 100;
		spacesLeft = boardSize;
		gameboard = new Board(boardSize,players.size());
		pickedSpaces = new boolean[boardSize];
		//Add ONE (1) Instant Billion
		int instantBillion = (int)(Math.random()*100);
		gameboard.setGoalSpace(instantBillion);
		System.out.println(String.format("Goal Space: %02d",(instantBillion+1)%100));
		//NOBOMZEER
		for(Player next : players)
		{
			next.status = PlayerStatus.ALIVE;
			playersAlive++;
		}
		checkReady();
	}
	
	private void checkReady()
	{
		//If everyone has sent in, what are we waiting for?
		if(playersAlive == players.size())
		{
			//Player order - most money goes first
			players.sort(new PlayerDescendingRoundDeltaSorter());
			//Let's get things rolling... with instructions~!
			channel.sendMessage("Welcome to the Grand Final!").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("Things are going to play out a little differently here.").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("On this hundred-space board, there are no bombs, no blammos, not even negative cash.").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("The winner is the first player to reach ONE BILLION DOLLARS!").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("We have some special events to help you out along the way...").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("And if you find a minigame, you will get to play it immediately!").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("Oh, and in case that wasn't enough...").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("One space on the board will instantly award you a billion dollars and the championship!").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("Play begins with the player who has the most money. Best of luck in this Race to a Billion!").queue();
			//Always start with the first player
			gameStatus = GameStatus.IN_PROGRESS;
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
		displayBoardAndStatus(true, false);
		//Ready up the space picker depending on if it's a bot up next
		if(players.get(player).isBot)
		{
			//Sleep for a couple of seconds so they don't rush
			try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
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
			//If there's any spaces that aren't known bombs, pick one and resolve it
			if(safeSpaces.size() > 0)
			{
				resolveTurn(player, safeSpaces.get((int)(Math.random()*safeSpaces.size())));
			}
			//Otherwise it sucks to be you, bot, eat bomb (or defuse bomb)!
			else
			{
				int bombToPick = openSpaces.get((int)(Math.random()*openSpaces.size()));
				resolveTurn(player, bombToPick);
			}
		}
		else
		{
			warnPlayer = timer.schedule(() -> 
			{
				//If they're out of the round somehow, why are we warning them?
				if(players.get(player).status == PlayerStatus.ALIVE && playersAlive > 1 && player == currentTurn && !resolvingTurn)
				{
					channel.sendMessage(players.get(player).getSafeMention() + 
							", thirty seconds left to choose a space!").queue();
					displayBoardAndStatus(true,false);
				}
			}, 60, TimeUnit.SECONDS);
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
					{
						if(players.get(player).status != PlayerStatus.ALIVE || playersAlive <= 1 || player != currentTurn)
						{
							return true;
						}
						else if(e.getAuthor().equals(players.get(player).user) && e.getChannel().equals(channel)
								&& checkValidNumber(e.getMessage().getContentStripped()))
						{
								int location = Integer.parseInt(e.getMessage().getContentStripped());
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
						return false;
					},
					//Parse it and call the method that does stuff
					e -> 
					{
						warnPlayer.cancel(false);
						//If they're somehow taking their turn when they're out of the round, just don't do anything
						if(players.get(player).status == PlayerStatus.ALIVE && playersAlive > 1 && player == currentTurn)
						{
							int location = Integer.parseInt(e.getMessage().getContentStripped())-1;
							//Anyway go play out their turn
							timer.schedule(() -> resolveTurn(player, location), 500, TimeUnit.MILLISECONDS);
						}
					});
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
		peekStreak = 0;
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
		/*
		 * Suspense rules:
		 * Always on the $1b
		 * Otherwise 10/(spacesLeft) chance
		 */
		if(Math.random()*spacesLeft < 10 || 
				(gameboard.getType(location) == SpaceType.EVENT && gameboard.getEvent(location) == EventType.INSTANT_BILLION))
		{
			try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
			channel.sendMessage("...").queue();
		}
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		switch(gameboard.getType(location))
		{
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
		}
		runEndTurnLogic();
	}
	
	public void awardCash(int player, Cash cashType)
	{
		int cashWon;
		String prizeWon = null;
		//Is it Mystery Money? Do that thing instead then
		if(cashType == Cash.MYSTERY)
		{
			channel.sendMessage("It's **Mystery Money**, which today awards you...").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
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
					resultString.append("/"+baseDenominator);
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
	}

	public void awardBoost(int player, Boost boostType)
	{
		int boostFound = boostType.getValue();
		StringBuilder resultString = new StringBuilder();
		resultString.append(String.format("A **%+d%%** Booster",boostFound));
		resultString.append(boostFound > 0 ? "!" : ".");
		players.get(player).addBooster(boostFound);
		channel.sendMessage(resultString.toString()).queue();
	}

	public void awardGame(int player, Game gameFound)
	{
		players.get(player).games.add(gameFound);
		players.get(player).games.sort(null);
		channel.sendMessage("It's a minigame, **" + gameFound + "**!").queue();
	}
	
	public void awardEvent(int player, EventType eventType)
	{
		//Pass control straight over to the event
		eventType.getEvent().execute(this, player);
	}
	
	private void runEndTurnLogic()
	{
		//Release the hold placed on the board
		resolvingTurn = false;
		//Make sure the game isn't already over
		if(gameStatus == GameStatus.END_GAME)
			return;
		//If the current player has a minigame, play it immediately and come back here later
		if(players.get(currentTurn).games.size() > 0)
		{
			displayBoardAndStatus(true, false);
			try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
			prepareNextMiniGame(players.get(currentTurn).games.listIterator());
			return;
		}
		//If the current player has $1,000,000,000, they immediately win (so eliminate everyone else)
		if(players.get(currentTurn).money >= 1_000_000_000)
			for(int i=0; i<players.size(); i++)
				if(i != currentTurn)
					players.get(i).blowUp(0,false);
		//Test if game over - either all spaces gone and no blammo queued, or one player left alive
		if((spacesLeft <= 0 && !futureBlammo) || playersAlive <= 1) 
		{
			displayBoardAndStatus(true, false);
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
			channel.sendMessage("An error has occurred, ending the game, @Atia#2084 fix pls").queue();
		try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
		//Keep this one as complete since it's such an important spot
		channel.sendMessage("Game Over.").complete();
		currentBlammo = false;
		timer.schedule(() -> runFinalEndGameTasks(), 1, TimeUnit.SECONDS);
	}
	
	private void prepareNextMiniGame(ListIterator<Game> gamesToPlay)
	{
		if(gamesToPlay.hasNext())
		{
			//Get the minigame
			Game nextGame = gamesToPlay.next();
			currentGame = nextGame.getGame();
			//Get rid of the minigame so we don't replay it
			gamesToPlay.remove();
			//Pass to the game
			boolean sendMessages = !(players.get(currentTurn).isBot) || verboseBotGames;
			//Set up the thread we'll send to the game
			Thread postGame = new Thread()
			{
				public void run()
				{
					currentGame = null;
					runEndTurnLogic();
				}
			};
			postGame.setName(String.format("%s - %s - %s", 
					channel.getName(), players.get(currentTurn).getName(), currentGame.getName()));
			currentGame.initialiseGame(channel, sendMessages, baseNumerator, baseDenominator, 1,
					players, currentTurn, postGame);
		}
		else
		{
			runEndTurnLogic();
		}
	}
	
	public void runFinalEndGameTasks()
	{
		saveData();
		players.sort(new PlayerDescendingRoundDeltaSorter());
		for(int i=0; i<3; i++) //we just sorted them the first player is the winner
		{
			channel.sendMessage("**" + players.get(0).getName().toUpperCase() + " WINS RACE TO A BILLION!**")
				.queueAfter(5+(5*i),TimeUnit.SECONDS);
		}
		if(runDemo != 0)
			demoMode.cancel(false); //Season's over no demo needed
		reset();
		if(runAtGameEnd != null)
			runAtGameEnd.start();
		if(playersCanJoin)
			timer.schedule(() -> runPingList(), 1, TimeUnit.SECONDS);
	}
	
	class PlayerDescendingRoundDeltaSorter implements Comparator<Player>
	{
		@Override
		public int compare(Player arg0, Player arg1)
		{
			return arg1.money - arg0.money; //Round delta is actually just total cash here
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
				//Replace the records of the players if they're there, otherwise add them
				if(players.get(i).newbieProtection == 1)
					channel.sendMessage(players.get(i).getSafeMention() + ", your newbie protection has expired. "
							+ "From now on, your base bomb penalty will be $250,000.").queue();
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
				toPrint.append("#"+players.get(i).getName());
				toPrint.append("#"+players.get(i).money);
				toPrint.append("#"+players.get(i).booster);
				toPrint.append("#"+players.get(i).winstreak);
				toPrint.append("#"+Math.max(players.get(i).newbieProtection-1,0));
				toPrint.append("#"+players.get(i).lives);
				toPrint.append("#"+players.get(i).lifeRefillTime);
				toPrint.append("#NONE"); // no hidden commands
				toPrint.append("#"+players.get(i).boostCharge);
				toPrint.append("#"+players.get(i).annuities);
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
	
	class DescendingScoreSorter implements Comparator<String>
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
	
	public void displayBoardAndStatus(boolean printBoard, boolean copyToResultChannel)
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
			int boardWidth = 10;
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
				board.append(pickedSpaces[i] ? "  " : String.format("%02d",(i+1)%100));
				board.append((i%boardWidth) == (boardWidth-1) ? "\n" : " ");
			}
			board.append("\n");
		}
		//Next the status line
		//Start by getting the lengths so we can pad the status bars appropriately
		//Add one extra to name length because we want one extra space between name and cash
		int nameLength = players.get(0).getName().length();
		for(int i=1; i<players.size(); i++)
			nameLength = Math.max(nameLength,players.get(i).getName().length());
		nameLength ++;
		//And ignore the negative sign if there is one
		int moneyLength;
		moneyLength = String.valueOf(Math.abs(players.get(0).money)).length();
		for(int i=1; i<players.size(); i++)
			moneyLength = Math.max(moneyLength, String.valueOf(Math.abs(players.get(i).money)).length());
		//Make a little extra room for the commas
		moneyLength += (moneyLength-1)/3;
		//Then start printing - including pointer if currently their turn
		for(int i=0; i<players.size(); i++)
		{
			board.append(currentTurn == i ? "> " : "  ");
			board.append(String.format("%-"+nameLength+"s",players.get(i).getName()));
			//Now figure out if we need a negative sign, a space, or neither
			int playerMoney = players.get(i).money;
			//What sign to print?
			board.append(playerMoney<0 ? "-" : " ");
			//Then print the money itself
			board.append(String.format("$%,"+moneyLength+"d",Math.abs(playerMoney)));
			//Now the booster display
			switch(players.get(i).status)
			{
			case ALIVE:
			case DONE:
				//If they're alive, display their booster
				board.append(String.format(" [%3d%%",players.get(i).booster));
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
		}
		//Close it off and print it out
		board.append("```");
		channel.sendMessage(board.toString()).queue();
		if(copyToResultChannel && resultChannel != null)
			resultChannel.sendMessage(board.toString()).queue();
	}
}
