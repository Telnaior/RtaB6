package tel.discord.rtab.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Message;
import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.board.EventType;
import tel.discord.rtab.board.Game;

public class LuckySpace implements EventSpace
{
	GameController game;
	int player;
	enum LuckyEvent
	{
		BIG_BUCKS("Big Bucks"),
		JOKER("Joker"),
		MINIGAME("Minigame"),
		CASH_FOR_ALL("Cash for All"),
		DOUBLE_DEAL("Double Deal");

		final String name;
		LuckyEvent(String name)
		{
			this.name = name;
		}
		public String getName()
		{
			return name;
		}
	}
	
	@Override
	public String getName()
	{
		return "Lucky Space";
	}

	@Override
	public void execute(GameController game, int player)
	{
		this.game = game;
		this.player = player;
		//Generate the wheel
		ArrayList<LuckyEvent> wheel = new ArrayList<LuckyEvent>();
		wheel.addAll(Arrays.asList(LuckyEvent.values()));
		if(game.players.size() < 4)
			wheel.remove(LuckyEvent.JOKER);
		else
			wheel.remove(LuckyEvent.DOUBLE_DEAL);
		Collections.shuffle(wheel);
		game.channel.sendMessage("You found the **Lucky Space**! Step right up and claim your prize!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		switch(spinWheel(wheel))
		{
		case BIG_BUCKS:
			int cashWon = (int)Math.pow((Math.random()*14)+20,4); //Mystery money but with a much more limited range
			cashWon *= Math.sqrt(game.players.size()); //and boost it by the playercount
			cashWon -= cashWon % 10_000; //Round it off
			cashWon = game.applyBaseMultiplier(cashWon); //Then base multiplier
			game.channel.sendMessage(String.format("It's **Big Bucks**! You're taking home **$%,d**!",cashWon)).queue();
			StringBuilder extraResult = game.players.get(player).addMoney(game.applyBaseMultiplier(cashWon), MoneyMultipliersToUse.BOOSTER_ONLY);
			if(extraResult != null)
			{
				try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
				game.channel.sendMessage(extraResult.toString()).queue();
			}
			break;
		case CASH_FOR_ALL:
			game.awardEvent(player, EventType.CASH_FOR_ALL);
			break;
		case DOUBLE_DEAL:
			game.awardEvent(player, EventType.DOUBLE_DEAL);
			break;
		case JOKER:
			game.awardEvent(player, EventType.JOKER);
			break;
		case MINIGAME:
			Game minigame = game.generateEventMinigame(player);
			game.awardGame(player, minigame);
			if(game.players.size() >= 9)
			{
				game.channel.sendMessage("And you can have two copies of it!").queue();
				game.players.get(player).games.add(minigame);
			}
			game.players.get(player).games.sort(null);
			break;
		}
	}
	
	private LuckyEvent spinWheel(ArrayList<LuckyEvent> wheel)
	{
		int index = (int)(Math.random()*wheel.size());
		Message luckyMessage = game.channel.sendMessage(generateRouletteDisplay(wheel,index))
				.completeAfter(1,TimeUnit.SECONDS);
		int addon = (int)(Math.random()*wheel.size()+1);
		//Make it spin
		for(int i=0; i<addon; i++)
		{
			index += 1;
			index %= wheel.size();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			luckyMessage.editMessage(generateRouletteDisplay(wheel,index)).queue();
		}
		//50% chance to tick it over one more time
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		if(Math.random() < 0.5)
		{
			index += 1;
			index %= wheel.size();
			luckyMessage.editMessage(generateRouletteDisplay(wheel,index)).queue();
		}
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		//Delete the roulette message after a few seconds
		luckyMessage.delete().queueAfter(5, TimeUnit.SECONDS);
		return wheel.get(index);
	}
	
	private String generateRouletteDisplay(ArrayList<LuckyEvent> list, int index)
	{
		StringBuilder board = new StringBuilder().append("```\n");
		for(int i=0; i<list.size(); i++)
		{
			if(i == index)
				board.append("> ");
			else
				board.append("  ");
			board.append(list.get(i).getName());
			board.append("\n");
		}
		board.append("```");
		return board.toString();
	}
}
