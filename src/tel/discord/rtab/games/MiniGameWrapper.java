package tel.discord.rtab.games;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.RtaBMath;
import tel.discord.rtab.Achievement;
import tel.discord.rtab.MoneyMultipliersToUse;

abstract class MiniGameWrapper implements MiniGame
{
	MessageChannel channel;
	boolean sendMessages;
	int baseNumerator;
	int baseDenominator;
	int gameMultiplier;
	List<Player> players;
	int player;
	Thread callWhenFinished;
	public ScheduledThreadPoolExecutor timer;
	boolean canSkip = false;
	boolean autoSkip = false;
	boolean enhanced = false;
	Thread interruptToSkip;
	
	//These seven methods must be implemented by all minigames

	/**
	 * This method will be called at the beginning of a minigame.
	 */
	abstract void startGame();
	
	/**
	 * This method will be called upon receiving input via getInput()
	 * @param input A message sent by the player
	 */
	abstract void playNextTurn(String input);
	
	/**
	 * This method will be called by getInput() if the minigame is being played by a bot.
	 * @return The message the bot wants to send to the game (this will be passed directly to playNextTurn())
	 */
	abstract String getBotPick();
	
	/**
	 * This method will be called if the player times out while the minigame is waiting for input.
	 * This should end the game immediately, stopping in a push-your-luck style game and awarding the worst possible result in other games.
	 */
	abstract void abortGame();
	
	/**
	 * This method should return the full name of the minigame.
	 */
	public abstract String getName();
	
	/**
	 * This method should return the short name of the minigame (what is displayed on the status line when it is collected)
	 */
	public abstract String getShortName();
	
	/**
	 * This method should return true if it is a bonus game (Supercash etc.), and false otherwise.
	 */
	public abstract boolean isBonus();

	//These methods are implemented by the wrapper class and available for your minigame to call as needed.
	
	/**
	 * This method will send a single message to the game channel.
	 * @param message A string to send to the game channel.
	 */
	void sendMessage(String message)
	{
		LinkedList<String> output = new LinkedList<>();
		output.add(message);
		sendMessages(output);
	}
	
	/**
	 * This method will send a list of messages to the game channel.
	 * @param messages A list of strings to send to the game channel.
	 */
	void sendMessages(LinkedList<String> messages)
	{
		//If there are no messages to send or we've been told not to send messages, immediately return
		if(!sendMessages || messages.size() == 0)
			return;
		//Send each message with a two-second delay (3 seconds for super bonus round)
		boolean firstMessage = true;
		int delay = getShortName().equals("sbr") ? 3000 : 2000;
		for(String nextMessage : messages)
		{
			if(firstMessage)
				firstMessage = false;
			else
			{
				try
				{
					Thread.sleep(delay);
				}
				catch(InterruptedException e)
				{
					//Immediately stop sending messages if we get interrupted, as it means the player has elected to skip
					return;
				}
			}
			channel.sendMessage(nextMessage).queue();
		}
	}
	
	/**
	 * This method will send a list of messages to the game channel and enable the !skip command so they can be skipped.
	 * This should be used for long strings of instructions that are the same every time the minigame is played.
	 * @param messages A list of strings to send to the game channel.
	 */
	void sendSkippableMessages(LinkedList<String> messages)
	{
		if(autoSkip)
			return;
		interruptToSkip = Thread.currentThread();
		canSkip = true;
		sendMessages(messages);
		canSkip = false;
	}
	
	/**
	 * This method will get the player class of the minigame's owner (the player who earned the minigame).
	 * @deprecated pvp minigames accidentally overrode this and broke some things, switch to getPlayer() instead
	 */
	@Deprecated
	Player getCurrentPlayer()
	{
		return players.get(player);
	}
	
	/**
	 * This method will get the player class of the minigame's owner (the player who earned the minigame), and cannot be overridden.
	 */
	final Player getPlayer()
	{
		return players.get(player);
	}
	
	/**
	 * This method will take an amount of money and apply the game's base multiplier to it.
	 * Any amounts of money the minigame uses should be passed through this method.
	 * @param amount The amount of money to multiply
	 * @return The multiplied amount of money
	 */
	int applyBaseMultiplier(int amount)
	{
		return RtaBMath.applyBaseMultiplier(amount, baseNumerator*gameMultiplier, baseDenominator);
	}
	
	/**
	 * This method will ask the minigame's owner for input and pass the collected message to playNextTurn().
	 */
	void getInput()
	{
		getInput(player);
	}
	
	/**
	 * This method will check whether or not a specified message is a number.
	 * @param message The string that might or might not be a number
	 * @return True if the message is a number, otherwise false
	 */
	boolean isNumber(String message)
	{
		try
		{
			//If this doesn't throw an exception we're good
			Integer.parseInt(message);
			return true;
		}
		catch(NumberFormatException e1)
		{
			return false;
		}
	}
	
	/**
	 * This method will award an amount of money to the minigame's owner, and then end the minigame.
	 * @param moneyWon The amount of money the player won
	 */
	void awardMoneyWon(int moneyWon)
	{
		StringBuilder resultString = new StringBuilder();
		if(getPlayer().isBot)
			resultString.append(getPlayer().getName()).append(" won ");
		else
			resultString.append("Game Over. You won ");
		resultString.append(String.format("**$%,d** from ",moneyWon));
		if(gameMultiplier > 1)
			resultString.append(String.format("%d copies of ",gameMultiplier));
		resultString.append(getName()).append(".");
		StringBuilder extraResult = null;
		extraResult = getPlayer().addMoney(moneyWon,
				isBonus() ? MoneyMultipliersToUse.NOTHING : MoneyMultipliersToUse.BOOSTER_OR_BONUS);
		//We want the endgame result to show up unconditionally
		sendMessages = true;
		sendMessage(resultString.toString());
		if(extraResult != null)
			sendMessage(extraResult.toString());
		gameOver();
	}
	
	//These methods are used internally by the wrapper class, and most minigames don't need to worry about these.
	
	class MinigameThreadFactory implements ThreadFactory
	{
		@Override
		public Thread newThread(Runnable r)
		{
			Thread newThread = new Thread(r);
			newThread.setName(String.format("%s - %s", getName(), getPlayer().getName()));
			return newThread;
		}
	}
	
	/**
	 * This method is called by the game controller, and sets up minigame variables before passing to startGame().
	 */
	@Override
	public void initialiseGame(MessageChannel channel, boolean sendMessages, int baseNumerator, int baseDenominator,
			int gameMultiplier, List<Player> players, int player, Thread callWhenFinished, boolean enhanced)
	{
		//Initialise variables
		this.channel = channel;
		this.sendMessages = sendMessages;
		this.baseNumerator = baseNumerator;
		this.baseDenominator = baseDenominator;
		this.gameMultiplier = gameMultiplier;
		this.players = players;
		this.player = player;
		this.callWhenFinished = callWhenFinished;
		this.enhanced = enhanced;
		//Announce the minigame
		StringBuilder gameMessage = new StringBuilder();
		gameMessage.append(getPlayer().getSafeMention());
		if(isBonus())
			gameMessage.append(", you've unlocked a bonus game: ");
		else
			gameMessage.append(", time for your next minigame: ");
		gameMessage.append(getName()).append("!");
		sendMessage(gameMessage.toString());
		//Remind them if they have multiple copies
		if(gameMultiplier > 1)
			sendMessage(String.format("You have %d copies of this minigame, so the stakes have been multiplied!",gameMultiplier));
		if(gameMultiplier >= 3)
			Achievement.TRIPLE_MINIGAME.check(getPlayer());
		//Set up the threadpool
		timer = new ScheduledThreadPoolExecutor(1, new MinigameThreadFactory());
		//Then pass over to minigame-specific code
		timer.schedule(this::startGame, 1000, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * This method will ask the specified player for input and pass the collected message to playNextTurn().
	 * @param player The player to ask for input
	 */
	void getInput(int player)
	{
		//If they're a bot, just get the next bot pick
		if(players.get(player).isBot)
		{
			playNextTurn(getBotPick());
			return;
		}
		//Otherwise, ask for input
		ScheduledFuture<?> warnPlayer = timer.schedule(() ->
				channel.sendMessage(players.get(player).getSafeMention() +
						", are you still there? One minute left!").queue(), 120, TimeUnit.SECONDS);
		RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
				//Right player and channel
				e ->
						(e.getChannel().equals(channel) && e.getAuthor().equals(players.get(player).user)),
				//Parse it and call the method that does stuff
				e -> 
				{
					warnPlayer.cancel(false);
					timer.schedule(() -> playNextTurn(e.getMessage().getContentStripped()), 500, TimeUnit.MILLISECONDS);
				},
				180,TimeUnit.SECONDS, () ->
				{
					channel.sendMessage(players.get(player).getName() + 
							" has gone missing. Cancelling their minigames.").queue();
					players.get(player).games.clear();
					abortGame();
				});
	}
	
	/**
	 * This method is called by the !skip command, and works with sendSkippableMessages() to skip long strings of text.
	 */
	public void skipMessages()
	{
		if(canSkip)
			interruptToSkip.interrupt();
	}
	
	/**
	 * This method will end the minigame and notify the game controller.
	 * Most minigames will call this via awardMoneyWon().
	 */
	@Override
	public void gameOver()
	{
		timer.purge();
		timer.shutdownNow();
		callWhenFinished.start();
	}
}
