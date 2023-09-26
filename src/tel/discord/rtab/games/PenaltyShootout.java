package tel.discord.rtab.games;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.games.PvPMiniGameWrapper.Status;

//It's unfinished, sorry
public class PenaltyShootout extends PvPMiniGameWrapper
{
	static final int WIN_BONUS = 1_000_000;
	//Each sub-array = prizes for one round, -1 = hidden command, -2 = +100% boost (there isn't enough non-cash to justify something more robust)
	static final int[][] GOAL_PRIZES = new int[][] {{50_000, 100_000, 10_000}, {75_000, 200_000, 100_000},
		{100_000, 300_000, -1},	{-2, 400_000, 0}, {0, 500_000, -250_000}};
	int roundNumber = 0;
	int playerGoals = 0;
	int opponentGoals = 0;
	GoalSection playerSaved, opponentSaved;
	
	private enum GoalSection
	{
		LEFT,
		MIDDLE,
		RIGHT
    }

	@Override
	LinkedList<String> getInstructions()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add("In Penalty Shootout, you'll each take five turns shooting at a goal filled with prizes.");
		output.add("The goal is divided into three sections - left, middle, and right. Each section will have a different prize in each round.");
		output.add("Whichever section you shoot for determines the prize you'll earn... *if* you manage to score the goal.");
		output.add("Your opponent will also get to secretly block one section, and if they block the section you shoot for, you get nothing.");
		output.add("After each player has had five shots at goal, "
				+ String.format("whoever has scored more goals will earn a bonus **$%,d**!",applyBaseMultiplier(WIN_BONUS)));
		return output;
	}

	@Override
	void startPvPGame()
	{
		//Figure out turn order
		playerTurn = true;
		if(players.get(player).isBot)
		{
			playerTurn = Math.random() < 0.2; //going second is an advantage so the AI favours it
			sendMessage(getPlayer().getName() + " elected to shoot " + (playerTurn ? "first." : "second."));
			startRound();
		}
		else
		{
			sendMessage(getPlayer().getSafeMention() + ", would you like to shoot FIRST or SECOND?");
			getInput();
		}
		startRound();
	}
	
	private void startRound()
	{
		LinkedList<String> output = new LinkedList<>();
		output.add("It is now Round "+(roundNumber+1)+". Here is the next goal:");
		output.add(generateGoal());
		output.add(generateBoard());
		sendMessages(output);
		askForBomb(player, true);
		askForBomb(opponent, false);
	}
	
	private void askForBomb(int playerID, boolean playerTurn)
	{
		if(players.get(playerID).isBot)
		{
			//TODO better AI logic for this
			GoalSection saved = GoalSection.values()[(int)(Math.random()*GoalSection.values().length)];
			if(playerTurn)
				playerSaved = saved;
			else
				opponentSaved = saved;
			checkReady();
		}
		else
		{
			players.get(playerID).user.openPrivateChannel().queue(
					(channel) -> channel.sendMessage("Please place your bomb within the next 60 seconds "
							+ "by sending a number 1-9").queue());
			//TODO display the goal they're saving
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Check if right player, and valid goal section choice
					e -> 
					{
						if(e.getAuthor().equals(players.get(playerID).user)	
							&& e.getChannel().getType() == ChannelType.PRIVATE)
						{
							String message = e.getMessage().getContentStripped().toUpperCase();
							for(GoalSection next : GoalSection.values())
								if(message.equals(next.toString()) ||
										(message.length() == 1 && next.toString().startsWith(message)))
									return true;
								return false;
						}
						else return false;
					},
					//Parse it and note down their choice
					e -> 
					{
						GoalSection section;
						String message = e.getMessage().getContentStripped().toUpperCase();
						for(GoalSection next : GoalSection.values())
							if(message.equals(next.toString()) ||
									(message.length() == 1 && next.toString().startsWith(message)))
								section = next;
						if(playerTurn)
							playerSaved = section;
						else
							opponentSaved = section;
						players.get(playerID).user.openPrivateChannel().queue(
								(channel) -> channel.sendMessage("Bomb placement confirmed.").queue());
						checkReady();
					},
					//Or timeout the prompt after a minute (nothing needs to be done here)
					playerTurn ? 60 : 61, TimeUnit.SECONDS, () -> timeoutBomb());
		}
	}
	
	private void checkReady()
	{
		if(playerSaved != null && opponentSaved != null)
			doFunThings();
	}

	@Override
	void playNextTurn(String input)
	{
		// TODO Auto-generated method stub

	}

	@Override
	String getBotPick()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void abortGame()
	{
		// TODO Auto-generated method stub

	}

	@Override public String getName() { return "Penalty Shootout"; }
	@Override public String getShortName() { return "Shootout"; }
	@Override public boolean isBonus() { return false; }
	@Override
	public String getEnhanceText()
	{
		return "you'll know when it happens"; //TODO
	}
}
