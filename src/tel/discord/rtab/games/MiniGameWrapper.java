package tel.discord.rtab.games;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.RtaBMath;
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
	public ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
	boolean canSkip = false;
	boolean autoSkip = false;
	Thread interruptToSkip;

	@Override
	public void initialiseGame(MessageChannel channel, boolean sendMessages, int baseNumerator, int baseDenominator,
			int gameMultiplier, List<Player> players, int player, Thread callWhenFinished)
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
		//Announce the minigame
		StringBuilder gameMessage = new StringBuilder();
		gameMessage.append(getCurrentPlayer().getSafeMention());
		if(isBonus())
			gameMessage.append(", you've unlocked a bonus game: ");
		else
			gameMessage.append(", time for your next minigame: ");
		gameMessage.append(getName() + "!");
		sendMessage(gameMessage.toString());
		//Then pass over to minigame-specific code
		timer.schedule(() -> startGame(), 1000, TimeUnit.MILLISECONDS);
	}

	abstract void startGame();
	
	abstract void playNextTurn(String input);
	
	abstract String getBotPick();
	
	abstract void abortGame();
	
	void sendSkippableMessages(LinkedList<String> messages)
	{
		if(autoSkip)
			return;
		interruptToSkip = Thread.currentThread();
		canSkip = true;
		sendMessages(messages);
		canSkip = false;
	}
	
	void sendMessage(String message)
	{
		LinkedList<String> output = new LinkedList<String>();
		output.add(message);
		sendMessages(output);
	}
	
	void sendMessages(LinkedList<String> messages)
	{
		//If we aren't sending messages this minigame, just abort immediately
		if(!sendMessages)
			return;
		//Send each message with a two-second delay
		for(String nextMessage : messages)
		{
			channel.sendMessage(nextMessage).queue();
			try
			{
				Thread.sleep(2000);
			}
			catch(InterruptedException e)
			{
				//Immediately stop sending messages if we get interrupted, as it means the player has elected to skip
				return;
			}
		}
	}
	
	public void skipMessages()
	{
		if(canSkip)
			interruptToSkip.interrupt();
	}
	
	public abstract String getName();
	
	public abstract String getShortName();
	
	public abstract boolean isBonus();
	
	Player getCurrentPlayer()
	{
		return players.get(player);
	}
	
	int applyBaseMultiplier(int amount)
	{
		return RtaBMath.applyBaseMultiplier(amount, baseNumerator*gameMultiplier, baseDenominator);
	}
	
	void getInput(int player)
	{
		//If they're a bot, just get the next bot pick
		if(players.get(player).isBot)
			playNextTurn(getBotPick());
		//Otherwise, ask for input
		ScheduledFuture<?> warnPlayer = timer.schedule(() -> 
		{
			channel.sendMessage(getCurrentPlayer().getSafeMention() + 
					", are you still there? One minute left!").queue();
		}, 120, TimeUnit.SECONDS);
		RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
				//Right player and channel
				e ->
				{
					return (e.getChannel().equals(channel) && e.getAuthor().equals(getCurrentPlayer().user));
				},
				//Parse it and call the method that does stuff
				e -> 
				{
					warnPlayer.cancel(false);
					playNextTurn(e.getMessage().getContentRaw());
				},
				180,TimeUnit.SECONDS, () ->
				{
					channel.sendMessage(getCurrentPlayer().name + 
							" has gone missing. Cancelling their minigames.").queue();
					abortGame();
					getCurrentPlayer().games.clear();
					gameOver();
				});
	}
	
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
	
	void awardMoneyWon(int moneyWon)
	{
		StringBuilder resultString = new StringBuilder();
		if(getCurrentPlayer().isBot)
			resultString.append(getCurrentPlayer().name + " won ");
		else
			resultString.append("Game Over. You won ");
		resultString.append(String.format("**$%,d** from ",moneyWon));
		if(gameMultiplier > 1)
			resultString.append(String.format("%d copies of ",gameMultiplier));
		resultString.append(getName() + ".");
		StringBuilder extraResult = null;
		extraResult = getCurrentPlayer().addMoney(moneyWon,
				isBonus() ? MoneyMultipliersToUse.NOTHING : MoneyMultipliersToUse.BOOSTER_OR_BONUS);
		sendMessage(resultString.toString());
		if(extraResult != null)
			sendMessage(extraResult.toString());
		gameOver();
	}
	
	//All minigames should call this when they are finished
	@Override
	public void gameOver()
	{
		timer.shutdownNow();
		callWhenFinished.interrupt();
	}
}
