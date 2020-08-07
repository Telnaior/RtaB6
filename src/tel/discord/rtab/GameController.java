package tel.discord.rtab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.TextChannel;
import tel.discord.rtab.board.HiddenCommand;
import net.dv8tion.jda.api.entities.Member;

public class GameController
{

	final int MAX_PLAYERS = 16;
	public ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
	public ScheduledFuture<?> demoMode;
	public TextChannel channel;
	int baseNumerator, baseDenominator, botCount, runDemo;
	public final List<Player> players = new ArrayList<>(16);
	int currentTurn, playersAlive, repeatTurn;
	boolean playersCanJoin = true;
	GameStatus gameStatus = GameStatus.SEASON_OVER;
	
	public GameController(TextChannel gameChannel, int baseNumerator, int baseDenominator, int botCount)
	{
		channel = gameChannel;
		this.baseNumerator = baseNumerator;
		this.baseDenominator = baseDenominator;
		this.botCount = botCount;
		reset();
	}
	
	public void reset()
	{
		players.clear();
		currentTurn = -1;
		gameStatus = GameStatus.SIGNUPS_OPEN;
	}

	public int findPlayerInGame(String playerID)
	{
		for(int i=0; i < players.size(); i++)
			if(players.get(i).uID.equals(playerID))
				return i;
		return -1;
	}

	/**
	 * addPlayer - adds a player to the game, or updates their name if they're already in.
	 * MessageChannel channelID - channel the request took place in (only used to know where to send game details to)
	 * String playerID - ID of player to be added.
	 * Returns true if the join attempt succeeded, or false if it failed.
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
		if(players.size() >= MAX_PLAYERS)
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
	 * removePlayer - removes a player from the game.
	 * String playerID - ID of player to be removed.
	 * Returns true if the quit attempt succeeded, or false if it failed.
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
	
	public void startTheGameAlready()
	{
		//TODO (gotcha)
		channel.sendMessage("PSYCHE game's not ready yet but thanks for testing").queue();
		reset();
	}
	
	public int calculateEntryFee(int money, int lives)
	{
		int entryFee = Math.max(money/500,20000);
		entryFee *= 5 - lives;
		return entryFee;
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
}
