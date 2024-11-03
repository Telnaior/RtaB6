package tel.discord.rtab.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Message;
import tel.discord.rtab.Achievement;
import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RtaBMath;
import tel.discord.rtab.board.EventType;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.SpaceType;
import tel.discord.rtab.games.objs.Jackpots;

public class Bowser implements EventSpace
{
	enum BowserEvent
	{
		COINS_FOR_BOWSER(false,"Cash for Bowser"),
		BOWSER_POTLUCK	(false,"Bowser's Cash Potluck"),
		COMMUNISM		(false,"Bowser Revolution"),
		REVERSE_CURSE	(false,"Bowser's Reverse Curse"),
		BLAMMO_FRENZY	(false,"Bowser's Multiplying Blammos"),
		MINIGAME_TTB	(false,"Bowser's Tic Tac Bomb"),
		CURSED_BOMBS	(false,"Bowser's Cursed Bombs"),
		RUNAWAY_1		(true, "Billion-Dollar Present"),
		RUNAWAY_2		(true, "+999% Boost Present"),
		RUNAWAY_3		(true, "Jokers - Packed to Go"),
		//RUNAWAY_4		(true, "99 Lives"),
		//Perhaps for later?
		JACKPOT			(true, "Bowser's Jackpot");

		final String name;
		final boolean hardToLandOn;
		BowserEvent(boolean hardToLandOn, String name)
		{
			this.hardToLandOn = hardToLandOn;
			this.name = name;
		}
		public String getName()
		{
			return name;
		}
	}
	
	private static final String[] INTRO_MESSAGES = {"Wah, hah, HAH! Welcome to the Bowser Event!",
								"Doom, doom, doom, DOOM! Well, well, well, %s! Welcome to Bowser's Event!",
								"Gwah, hah, hah! I was hoping I'd see you, %s."};
	private static final String[] EVENT_MESSAGES = {"We've plenty of fun events!",
								"I recommend you try one of my frightening events! I hope you enjoy it!",
								"So, what frighteningly fun event will we have this time? Gwah, hah, hah!"};
	private static final String[] ROULETTE_MESSAGES = {"Now then, choose your event by roulette!",
								"Step right up and let the roulette choose your fate!",
								"Step right up and let the roulette decide your fate!"};
	
	GameController game;
	int player;
	int bowserJackpot;
	boolean superSteal;
	private Player getCurrentPlayer()
	{
		return game.players.get(player);
	}
	
	@Override
	public String getName()
	{
		return "Bowser Event";
	}
	
	@Override
	public void execute(GameController game, int player)
	{
		this.game = game;
		this.player = player;
		bowserJackpot = Jackpots.BOWSER.getJackpot(game.channel);
		if(RtaBMath.random() < 0.01 && getCurrentPlayer().getRoundDelta() > 0)
		{
			game.channel.sendMessage("It's ||B-B-B-**BOWSER**||!").queue();
			try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			game.channel.sendMessage("Wah, hah, HAH! Welcome to the **Bowser Event**! Aww, did I fool you?").queue();
		}
		else
		{
			game.channel.sendMessage("It's B-B-B-**BOWSER**!!").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			game.channel.sendMessage(String.format(INTRO_MESSAGES[(int)(RtaBMath.random()*INTRO_MESSAGES.length)],
					getCurrentPlayer().getName())).queue();
		}
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		//If they don't have any money yet, why not be kind and give them some?
		if(getCurrentPlayer().getRoundDelta() <= 0)
		{
			game.channel.sendMessage("Oh, but you don't have any money yet this round?").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			//100% chance of pity money at start, then 90% chance for $100M club, down to 10% chance in $900M club
			if(RtaBMath.random()*10 > getCurrentPlayer().money / 100_000_000)
			{
				//Only award the same percentage of the $1m "base" pity money
				int pityMoney = game.applyBaseMultiplier(100_000)*(10-(getCurrentPlayer().money/100_000_000));
				game.channel.sendMessage(String.format("Let no one say I am unkind. Here is **$%,d**!",pityMoney)).queue();
				getCurrentPlayer().addMoney(pityMoney,MoneyMultipliersToUse.NOTHING);
				return;
			}
			game.channel.sendMessage("Too bad!").queue();
		}
		else
		{
			game.channel.sendMessage(EVENT_MESSAGES[(int)(RtaBMath.random()*EVENT_MESSAGES.length)]).queue();
		}
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage(ROULETTE_MESSAGES[(int)(RtaBMath.random()*ROULETTE_MESSAGES.length)]).queue();
		//Build roulette wheel
		ArrayList<BowserEvent> bowserEvents = new ArrayList<>();
		//Always have a coins for bowser
		bowserEvents.add(BowserEvent.COINS_FOR_BOWSER);
		bowserEvents.add(switch((int) (RtaBMath.random() * 4)) {
			default -> BowserEvent.RUNAWAY_1;
			case 1 -> BowserEvent.RUNAWAY_2;
			case 2 -> BowserEvent.RUNAWAY_3;
			case 3 -> BowserEvent.JACKPOT;
		});
		//Then pick from the remainder until we fill up with five events
		ArrayList<BowserEvent> copy = new ArrayList<>(Arrays.asList(
				BowserEvent.COINS_FOR_BOWSER, BowserEvent.BOWSER_POTLUCK, BowserEvent.BLAMMO_FRENZY,
				BowserEvent.MINIGAME_TTB, BowserEvent.CURSED_BOMBS));
		//Fixed 50% chance to add bowser revolution, because we're like that
		if(RtaBMath.random() < 0.5)
			bowserEvents.add(BowserEvent.COMMUNISM);
		else
			copy.add(BowserEvent.COMMUNISM);
		if(game.playersAlive > 2) copy.add(BowserEvent.REVERSE_CURSE); //This one shouldn't show up in 2p
		Collections.shuffle(copy);
		bowserEvents.addAll(copy.subList(0,5-bowserEvents.size()));
		//Now give the list a shuffle and spin it!
		Collections.shuffle(bowserEvents);
		switch (spinWheel(bowserEvents)) {
			case COINS_FOR_BOWSER -> coinsForBowser();
			case BOWSER_POTLUCK -> bowserPotluck();
			case COMMUNISM -> communism();
			case MINIGAME_TTB -> minigame(Game.TIC_TAC_BOMB);
			case BLAMMO_FRENZY -> blammoFrenzy();
			case REVERSE_CURSE -> reverseCurse();
			case CURSED_BOMBS -> addCursedBombs();
			case JACKPOT -> {
				//If the player has too much money, they get the wrong kind of 'jackpot'
				if (RtaBMath.random() * 1_000_000_000 < (bowserJackpot + getCurrentPlayer().money)) {
					awardJackpot();
				} else {
					runaway();
					if (getCurrentPlayer().getRoundDelta() > 0) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						game.channel.sendMessage("...with all your money. Jackpot!").queue();
						bowserJackpot += getCurrentPlayer().resetRoundDelta();
					}
				}
			}
			case RUNAWAY_1, RUNAWAY_2, RUNAWAY_3 -> runaway();
		}
		//Update jackpot with whatever was added to it
		Jackpots.BOWSER.setJackpot(game.channel,bowserJackpot);
	}
	private BowserEvent spinWheel(ArrayList<BowserEvent> list)
	{
		int index = (int)(RtaBMath.random()*5);
		Message bowserMessage = game.channel.sendMessage(generateRouletteDisplay(list,index))
				.completeAfter(1,TimeUnit.SECONDS);
		int addon = (int)(RtaBMath.random()*5+1);
		//Make it spin
		for(int i=0; i<addon; i++)
		{
			index += 1;
			index %= 5;
			try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			bowserMessage.editMessage(generateRouletteDisplay(list,index)).queue();
		}
		//50% chance three times to give it an extra twist
		for(int i=0; i<3; i++)
			if(RtaBMath.random() < 0.5)
			{
				//Random direction
				index += RtaBMath.random() < 0.5 ? 1 : -1;
				index = (index+5) % 5;
				bowserMessage.editMessage(generateRouletteDisplay(list,index)).completeAfter(2,TimeUnit.SECONDS);
			}
		//Pause for a second
		try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		//Check if it's on the jackpot or runaway
		if(list.get(index).hardToLandOn)
		{
			//Usually give it an extra twist, but occasionally just stop
			if(RtaBMath.random() < 0.8)
			{
				addon = (int)(RtaBMath.random()*5+1);
				//Randomise direction for this one
				boolean direction = RtaBMath.random() < 0.5;
				//Make it spin
				for(int i=0; i<addon; i++)
				{
					index += direction ? 1 : -1;
					index = (index + 5) % 5;
					bowserMessage.editMessage(generateRouletteDisplay(list,index)).queue();
					try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
				}
			}
		}
		//Make the roulette vanish after a few seconds
		bowserMessage.delete().queueAfter(5, TimeUnit.SECONDS);
		return list.get(index);
	}
	
	private String generateRouletteDisplay(ArrayList<BowserEvent> list, int index)
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
	
	private void coinsForBowser()
	{
		game.channel.sendMessage("**Cash for Bowser** it is!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("In this FUN event, you give your money to ME!").queue();
		//Coins: Up to 100-200% of the base amount, determined by their round earnings and their total bank
		int coinFraction = (int)(RtaBMath.random()*51+50);
		//Use the greater of either their round earnings or 0.5% of their total bank
		int coins = Math.max(getCurrentPlayer().getRoundDelta(), game.applyBaseMultiplier(getCurrentPlayer().money) / 200);
		coins /= 100;
		coins *= (getCurrentPlayer().money / 5_000_000) + 1;
		coins /= 100;
		coins *= coinFraction;
		int minimumTake = game.applyBaseMultiplier(50_000);
		if(coins < minimumTake)
			coins = minimumTake;
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage(String.format("Ooh! I'm so excited! OK, that'll be **$%,d**! Wah, hah, hah, HAH!"
				,coins)).queue();
		getCurrentPlayer().addMoney(coins*-1,MoneyMultipliersToUse.NOTHING);
		bowserJackpot += coins;
	}
	private void bowserPotluck()
	{
		game.channel.sendMessage("It's **Bowser's Cash Potluck**!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("In this EXTRA FUN event, EVERY PLAYER gives me money!").queue();
		//Potluck: 0.01% - 1.00% of the average total bank of the living players in the round
		int potluckFraction = (int)(RtaBMath.random()*100+1);
		int potluck = 0;
		for(Player next : game.players)
			if(next.status == PlayerStatus.ALIVE)
				potluck += next.money/100;
		potluck /= game.playersAlive;
		potluck *= potluckFraction;
		potluck /= 100;
		if(potluck < 50000)
			potluck = 50000;
		potluck = game.applyBaseMultiplier(potluck);
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage(String.format("Let the event begin! That'll be **$%,d** each! Wah, hah, hah, HAH!"
				,potluck)).queue();
		for(Player next : game.players)
			if(next.status == PlayerStatus.ALIVE)
				next.addMoney(potluck * -1, MoneyMultipliersToUse.NOTHING);
		bowserJackpot += (potluck * game.playersAlive);
	}
	private void communism()
	{
		game.channel.sendMessage("I am not always thinking about money. Why can't we all be friends?").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("So, to make the world a more peaceful place, "
			+ "I've decided to *divide everyone's earnings evenly*!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("It's a **Bowser Revolution**!").queue();
		boolean superRevolution = RtaBMath.random() < 0.5;
		if(superRevolution)
		{
			try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			game.channel.sendMessage("And let's throw in 1% of your total banks as well!").queue();
		}
		//Get the total money added during the round
		int delta = 0;
		for(Player next : game.players)
		{
			//Add their delta to the pile
			delta += next.resetRoundDelta();
			//Take total banks as well if necessary
			if(superRevolution)
			{
				delta += (next.money / 100);
				next.money -= next.money / 100;
			}
			//If they're out with split and share, give them an achievement
			if(next.splitAndShare && next.status == PlayerStatus.OUT)
				Achievement.SPLIT_COMMUNISM.check(next);
		}
		//Add the remainder to the jackpot - Bowser keeps it!
		bowserJackpot += (delta % game.players.size());
		//Divide the total by the number of players
		delta /= game.players.size();
		//If the delta is negative, Bowser doesn't 'keep' the change!
		if(delta < 0)
			delta -= 1;
		//And give it to each of them
		for(Player next : game.players)
		{
			next.addMoney(delta,MoneyMultipliersToUse.NOTHING);
		}
	}
	private void blammoFrenzy()
	{
		game.channel.sendMessage("It's **Bowser's Multiplying Blammos**, we're using your cash to make more BLAMMOs! Good luck!").queue();
		for(int i=0; i<game.boardSize; i++)
		{
			//Determine blammo rate based on current cash totals
			int totalMillions = 0;
			for(Player next : game.players)
			{
				totalMillions += Math.pow(next.money/1_000_000,2);
			}
			int average = (int)Math.pow(totalMillions / game.players.size(), 0.5) / 100;
			average += 1; //So the range is 1-10 rather than 0-9
			//Switch cash to blammo with average/10 chance
			if(game.gameboard.getType(i) == SpaceType.CASH && RtaBMath.random()*10 < average)
				game.gameboard.changeType(i,SpaceType.BLAMMO);
		}
	}
	private void minigame(Game gameToAward)
	{
		game.channel.sendMessage(String.format("It's **%s**! You'd better not lose this minigame, HAH!",gameToAward.getName())).queue();
		getCurrentPlayer().addGame(gameToAward);
	}
	private void reverseCurse()
	{
		game.channel.sendMessage("It's **Bowser's Reverse Curse**!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("You've all been cursed to go in reverse... and I'm adding *lots* of Reverse!").queue();
		game.gameboard.eventCurse(EventType.REVERSE);
		game.reverse = !game.reverse;
	}
	private void addCursedBombs()
	{
		game.channel.sendMessage("It's **Bowser's Cursed Bombs**!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("You've been CURSED... and there are two new bombs on the board that only you can hit!").queue();
		getCurrentPlayer().cursed = true;
		//get a list of open spaces and pick two at random
		ArrayList<Integer> openSpaces = new ArrayList<>();
		for(int i=0; i<game.boardSize; i++)
			if(!game.pickedSpaces[i])
				openSpaces.add(i);
		Collections.shuffle(openSpaces);
		for(int i=0; i<2; i++)
			if(openSpaces.size() > i)
				game.gameboard.cursedBomb(openSpaces.get(i));
	}
	
	private void runaway()
	{
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("...").queue();
		try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("Bowser ran away!").queue();
	}
	private void awardJackpot()
	{
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("...").queue();
		try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("Bowser looks about to run away, but then gives you a pitiful look.").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("You're looking quite sad there, aren't you?").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		game.channel.sendMessage("Let no one say I am unkind. You can have this, but don't tell anyone...").queue();
		try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		//Final test: They need to be in last overall out of the players in the round
		boolean awardJP = true;
		int threshold = getCurrentPlayer().money;
		for(Player next : game.players)
			if(next.money < threshold)
			{
				awardJP = false;
				game.channel.sendMessage("Bowser left you **ABSOLUTELY NOTHING**! PSYCHE!").queue();
				return;
			}
		if(awardJP)
		{
			game.channel.sendMessage("Bowser left you **all the money he has collected**!!").queue();
			game.channel.sendMessage(String.format("**$%,d**!!",bowserJackpot)).queue();
			getCurrentPlayer().addMoney(bowserJackpot, MoneyMultipliersToUse.NOTHING);
			bowserJackpot = Jackpots.BOWSER.resetValue;
		}
	}
}
