package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import static tel.discord.rtab.RaceToABillionBot.waiter;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.BombType;
import tel.discord.rtab.board.Boost;
import tel.discord.rtab.board.Cash;
import tel.discord.rtab.board.EventType;
import tel.discord.rtab.board.SpaceType;
import tel.discord.rtab.commands.channel.BooleanSetting;

public class GameController
{
	//Constants
	final static int THRESHOLD_PER_TURN_PENALTY = 100_000;
	static final int BOMB_PENALTY = -250_000;
	//Other useful technical things
	public ScheduledThreadPoolExecutor timer;
	public TextChannel channel, resultChannel;
	private Message waitingMessage;
	public HashSet<String> pingList = new HashSet<>();
	ScheduledFuture<?> warnPlayer;
	Thread runAtGameEnd = null;
	//Settings that can be customised
	public int baseNumerator, baseDenominator, botCount, minPlayers, maxPlayers;
	public int maxLives;
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
	public boolean resolvingTurn;
	String coveredUp;
	//Event variables
	public int boardMultiplier;
	public int fcTurnsLeft;
	int wagerPot;
	int peekStreak;
	
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
		//Look for match already in player list
		int playerLocation = findPlayerInGame(newPlayer.uID);
		if(playerLocation != -1)
		{
			//Found them, check if we should update their name or just laugh at them
			if(players.get(playerLocation).getName() == newPlayer.getName())
			{
				channel.sendMessage("Cannot join game: You have already joined the game.").queue();
				return false;
			}
			else
			{
				players.set(playerLocation,newPlayer);
				channel.sendMessage("Updated in-game name.").queue();
				return false;
			}
		}
		//Haven't found one, add them to the list
		players.add(newPlayer);
		//Remind everyone if they're close to the goal
		if(newPlayer.money > 900000000)
		{
			channel.sendMessage(String.format("%1$s needs only $%2$,d more to reach the goal!",
					newPlayer.getName(),(1000000000-newPlayer.money))).queue();
		}
		//If there's only one player right now, that means we're starting a new game so schedule the relevant things
		if(players.size() == 1)
		{
			timer.schedule(() -> 
			{
				if(gameStatus == GameStatus.SIGNUPS_OPEN)
				{
					channel.sendMessage("Thirty seconds before game starts!").queue();
					channel.sendMessage(listPlayers(false)).queue();
				}
			}, 90, TimeUnit.SECONDS);
			timer.schedule(() -> startTheGameAlready(), 120, TimeUnit.SECONDS);
			channel.sendMessage("Starting a game of Minesweeper to a Billion in two minutes. Type !join to sign up.").queue();
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
		boardSize = 5 + (5*players.size());
		spacesLeft = boardSize;
		gameboard = new Board(boardSize,players.size());
		pickedSpaces = new boolean[boardSize];
		awardEvent(-1,EventType.MINEFIELD);
		//Then do bomb placement
		sendBombPlaceMessages();
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
		timer.schedule(() -> abortRetryContinue(), playersCanJoin?60:90, TimeUnit.SECONDS);
		checkReady();
	}
	
	private void abortRetryContinue()
	{
		//We don't need to do this if we aren't still waiting for bombs
		if(gameStatus != GameStatus.BOMB_PLACEMENT)
			return;
		//Place everyone's bombs randomly
		for(Player next : players)
		{
			if(next.status == PlayerStatus.OUT)
			{
				int bombLocation = (int)(Math.random()*boardSize);
				gameboard.addBomb(bombLocation);
				next.knownBombs.add(bombLocation);
				next.user.openPrivateChannel().queue(
						(channel) -> channel.sendMessage("Bomb placement randomised.").queue());
				next.status = PlayerStatus.ALIVE;
				playersAlive ++;
			}
		}
		checkReady();
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
							+players.get((int)(Math.random()*players.size())).getName()+".");
					break;
				case 3:
					snarkMessage.append("So much for the "+coveredUp+" space on this board. You covered it up with a bomb.");
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
		//Figure out who to ping and what to tell them
		if(repeatTurn > 0 && !firstPick)
		{
			channel.sendMessage(players.get(player).getSafeMention() + ", pick again.").queue();
		}
		else
		{
			firstPick = false;
			channel.sendMessage(players.get(player).getSafeMention() + 
					", your turn. Choose a space on the board.").queue();
		}
		if(repeatTurn > 0)
			repeatTurn --;
		displayBoardAndStatus(true, false, false);
		//Pick a space
		{
			warnPlayer = timer.schedule(() -> 
			{
				//If they're out of the round somehow, why are we warning them?
				if(players.get(player).status == PlayerStatus.ALIVE && playersAlive > 1 && player == currentTurn && !resolvingTurn)
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
					},
					90,TimeUnit.SECONDS, () ->
					{
						//If they're somehow taking their turn when they shouldn't be, just don't do anything
						if(players.get(player).status == PlayerStatus.ALIVE && playersAlive > 1 && player == currentTurn && !resolvingTurn)
						{
							timer.schedule(() -> timeOutTurn(player), 500, TimeUnit.MILLISECONDS);
						}
					});
		}
	}
	
	private void timeOutTurn(int player)
	{
		if(resolvingTurn)
			return;
		peekStreak = 0;
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
		peekStreak = 0;
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
		 * Always trigger on a bomb or blammo
		 * Otherwise, don't trigger if they have a joker or we've had a starman
		 * Otherwise trigger randomly, chance determined by spaces left and players in the game
		 */
		if((Math.random()*Math.min(spacesLeft,fcTurnsLeft)<players.size() && players.get(player).jokers == 0)
				|| gameboard.getType(location) == SpaceType.BOMB)
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
		}
		runEndTurnLogic();
	}
	
	private void awardBomb(int player, BombType bombType)
	{
		int penalty = calculateBombPenalty(player);
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		//Pass control to the bomb itself to deal some damage
		bombType.getBomb().explode(this, player, penalty);
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
		//Test if game over - either all spaces gone and no blammo queued, or one player left alive
		if((spacesLeft <= 0) || playersAlive <= 0 || (earlyWinners == 0 && playersAlive == 1)) 
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
			currentTurn += 1;
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
		timer.schedule(() -> runNextEndGamePlayer(), 1, TimeUnit.SECONDS);
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
		timer.schedule(() -> runNextEndGamePlayer(), 1000, TimeUnit.MILLISECONDS);
	}
	
	public void runFinalEndGameTasks()
	{
		//TODO
		saveData();
		players.sort(new PlayerDescendingRoundDeltaSorter());
		displayBoardAndStatus(false, true, true);
		if(runAtGameEnd != null)
			runAtGameEnd.start();
		reset();
		if(playersCanJoin)
			timer.schedule(() -> runPingList(), 1, TimeUnit.SECONDS);
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
				if(rankChannel)
					channel.sendMessage("@everyone").queueAfter(20,TimeUnit.SECONDS);
				gameStatus = GameStatus.SEASON_OVER;
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
					announcementText.append(next.getSafeMention() + ", ");
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
	
	class PlayerDescendingRoundDeltaSorter implements Comparator<Player>
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
				toPrint.append("#");
				toPrint.append("#");
				toPrint.append("#"+players.get(i).lifeRefillTime);
				toPrint.append("#");
				toPrint.append("#"+players.get(i).boostCharge);
				toPrint.append("#"+players.get(i).annuities);
				toPrint.append("#");
				toPrint.append("#");
				//If they already exist in the savefile then replace their record, otherwise add them
				if(location == -1)
					list.add(toPrint.toString());
				else
					list.set(location,toPrint.toString());
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
		int penalty = applyBaseMultiplier(BOMB_PENALTY);
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
		int nameLength = players.get(0).getName().length();
		for(int i=1; i<players.size(); i++)
			nameLength = Math.max(nameLength,players.get(i).getName().length());
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