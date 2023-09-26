package tel.discord.rtab.events;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Boost;
import tel.discord.rtab.board.Cash;
import tel.discord.rtab.board.EventType;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.HiddenCommand;
import tel.discord.rtab.board.SpaceType;

public class Market implements EventSpace
{
	static final int BUY_BOOST_PRICE = 10_000;
	static final int SELL_BOOST_PRICE = 10_000;
	static final int GAME_PRICE = 720_720; //divided by living players
	static final int BUY_PEEK_PRICE = 1_000_000;
	static final int SELL_PEEK_PRICE = 250_000;
	static final int BUY_COMMAND_PRICE = 100_000;
	static final int BUY_INFO_PRICE = 100_000;

	int buyBoostAmount, sellBoostAmount, effectiveGamePrice;
	Game minigameOffered = null;
	boolean hasInfo = true;
	int commandPrice = 10;
	int itemsBought = 0;
	LinkedList<String> validOptions;
	RPSOption shopWeapon, backupWeapon;
	EventStatus status = EventStatus.PREPARING;
	ChaosOption chaosOption = null;
	
	private enum RPSOption
	{
		ROCK, PAPER, SCISSORS
	}
	private enum EventStatus
	{
		PREPARING, WAITING, RESOLVING, FINISHED
	}
	private enum ChaosOption
	{
		BANKRUPT_CASH("$%,d", "All bombs become Bankrupt")
		{
			String getReward(GameController game, int player)
			{
				return String.format(reward, game.applyBaseMultiplier(1_000_000*game.playersAlive));
			}
			boolean checkCondition(GameController game, int player)
			{
				return !game.starman && game.spacesLeft > 0;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Here's your money, good luck keeping it!").queue();
				game.players.get(player).addMoney(game.applyBaseMultiplier(1_000_000*game.playersAlive), MoneyMultipliersToUse.NOTHING);
				game.gameboard.bankruptCurse();
			}
		},
		SPLIT_AND_SHARE_ALL("Inflict Split and Share on All Opponents", "$%,d")
		{
			String getRisk(GameController game, int player)
			{
				return String.format(risk, (game.players.get(player).money/200)*game.playersAlive);
			}
			boolean checkCondition(GameController game, int player)
			{
				return game.players.get(player).money >= 100_000_000;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Good luck cashing in~").queue();
				game.players.get(player).addMoney(-1*(game.players.get(player).money/200)*game.playersAlive, MoneyMultipliersToUse.NOTHING);
				for(int i=0; i<game.players.size(); i++)
					if(i != player && game.players.get(i).status == PlayerStatus.ALIVE && !game.players.get(i).splitAndShare)
					{
						game.players.get(i).splitAndShare = true;
						game.channel.sendMessage("Split and Share applied to "+game.players.get(i).getSafeMention()+".").queue();
					}
			}
		},
		RISKY_JOKER("1 Joker", "Split and Share (if you lose)")
		{
			boolean checkCondition(GameController game, int player)
			{
				return game.players.get(player).jokers >= 0 && !game.players.get(player).splitAndShare;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Best of luck to you!").queue();
				game.awardEvent(player, EventType.JOKER);
				game.awardEvent(player, EventType.SPLIT_SHARE);
			}
		},
		ELAVIA_BRIBE("$%,d", "Fold out of the round immediately")
		{
			String getReward(GameController game, int player)
			{
				return String.format(reward, game.applyBaseMultiplier(20_000*game.spacesLeft));
			}
			boolean checkCondition(GameController game, int player)
			{
				return game.spacesLeft > 0;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Thanks for playing!").queue();
				game.players.get(player).addMoney(game.applyBaseMultiplier(20_000*game.spacesLeft), MoneyMultipliersToUse.NOTHING);
				game.players.get(player).status = !game.players.get(player).games.isEmpty() ? PlayerStatus.FOLDED : PlayerStatus.OUT;
				game.playersAlive--;
				game.players.get(player).splitAndShare = false;
			}
		},
		BUY_WINSTREAK("+%d.%d Streak", "$%,d")
		{
			private int getPrice(int currentStreak)
			{
				if(currentStreak < 40)
					return 5_000_000;
				else if(currentStreak < 80)
					return 15_000_000;
				else if(currentStreak < 120)
					return 50_000_000;
				else
					return 250_000_000; //This should never happen but just in case
			}
			String getReward(GameController game, int player)
			{
				int streakToGive = 40 - (game.players.get(player).winstreak % 40);
				return String.format(reward, streakToGive/10, streakToGive%10);
			}
			String getRisk(GameController game, int player)
			{
				return String.format(risk, game.applyBaseMultiplier(getPrice(game.players.get(player).winstreak)));
			}
			boolean checkCondition(GameController game, int player)
			{
				//The player limitation is to make sure a solo win won't give them the next bonus game after the one they buy
				return game.players.get(player).winstreak < 120 && game.players.size() <= 8;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Good luck in the bonus game!").queue();
				game.players.get(player).addMoney(
						game.applyBaseMultiplier(-1*getPrice(game.players.get(player).winstreak)), MoneyMultipliersToUse.NOTHING);
				game.players.get(player).addWinstreak(40 - (game.players.get(player).winstreak % 40));
			}
		},
		SELL_WINSTREAK("$%,d", "Reset Winstreak to x1")
		{
			private int getPrice(int currentStreak)
			{
				if(currentStreak >= 200)
					return 100_000_000;
				else if(currentStreak >= 160)
					return 50_000_000;
				else if(currentStreak >= 120)
					return 20_000_000;
				else if(currentStreak >= 80)
					return 5_000_000;
				else if(currentStreak >= 40)
					return 2_000_000;
				else
					return 250_000; //This should never happen but just in case
			}
			String getReward(GameController game, int player)
			{
				return String.format(reward, game.applyBaseMultiplier(getPrice(game.players.get(player).winstreak)));
			}
			boolean checkCondition(GameController game, int player)
			{
				return game.players.get(player).winstreak >= 40;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Good luck rebuilding your streak!").queue();
				game.players.get(player).addMoney(
						game.applyBaseMultiplier(getPrice(game.players.get(player).winstreak)), MoneyMultipliersToUse.NOTHING);
				game.players.get(player).winstreak = 0;
			}
		},
		NEGATIVE_ANNUITY("$%,d 'on loan'", "-$%,d per space for 100 spaces")
		{
			String getReward(GameController game, int player)
			{
				return String.format(reward, game.players.get(player).calculateBoostedAmount(
						game.applyBaseMultiplier(2_500_000), MoneyMultipliersToUse.BOOSTER_OR_BONUS));
			}
			String getRisk(GameController game, int player)
			{
				return String.format(risk, game.players.get(player).calculateBoostedAmount(
						game.applyBaseMultiplier(25_000), MoneyMultipliersToUse.BOOSTER_OR_BONUS));
			}
			boolean checkCondition(GameController game, int player)
			{
				return true;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Enjoy your loan!").queue();
				game.players.get(player).addMoney(
						game.applyBaseMultiplier(2_500_000), MoneyMultipliersToUse.BOOSTER_OR_BONUS);
				game.players.get(player).addAnnuity(game.applyBaseMultiplier(-25_000),100);
			}
		},
		FAKE_STARMAN("Starman", "Minefield immediately after")
		{
			boolean checkCondition(GameController game, int player)
			{
				return !game.starman && game.spacesLeft > 0;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Good luck!").queue();
				game.awardEvent(player, EventType.STARMAN);
				game.awardEvent(player, EventType.MINEFIELD);
			}
		},
		GRAB_BAG_FRENZY("All Events become Grab Bags", "Halve everyone's Booster (150% -> 75%)")
		{
			boolean checkCondition(GameController game, int player)
			{
				//Condition: There is at least one event still on the board
				for(int i=0; i<game.boardSize; i++)
					if(!game.pickedSpaces[i] && game.gameboard.getType(i) == SpaceType.EVENT)
						return true;
				return false;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Enjoy your Grab Bags!").queue();
				for(int i=0; i<game.boardSize; i++)
					if(!game.pickedSpaces[i] && game.gameboard.getType(i) == SpaceType.EVENT)
						game.gameboard.changeType(i, SpaceType.GRAB_BAG);
				for(Player next : game.players)
					next.addBooster(-1*next.booster/2);
			}
		},
		EXTRA_BOMB("Place an extra BOMB", "All opponents receive an Extra Peek")
		{
			boolean checkCondition(GameController game, int player)
			{
				return game.spacesLeft > 0;
			}
			void applyResult(GameController game, int player)
			{
				for(int i=0; i<game.players.size(); i++)
					if(i != player)
						game.players.get(i).peeks ++;
				if(game.players.get(player).isBot)
				{
					game.channel.sendMessage("Chaos Option Selected. The new bomb has been placed!").queue();
					//Get unknown spaces
					ArrayList<Integer> openSpaces = new ArrayList<>(game.boardSize);
					for(int i=0; i<game.boardSize; i++)
						if(!game.pickedSpaces[i] && !game.players.get(player).knownBombs.contains(i)
								&& !game.players.get(player).safePeeks.contains(i))
							openSpaces.add(i);
					//If there were any, place the bomb in one of them (otherwise don't place the bomb at all)
					if(!openSpaces.isEmpty())
					{
						int bombPosition = openSpaces.get((int)(Math.random()*openSpaces.size()));
						game.players.get(player).knownBombs.add(bombPosition);
						game.gameboard.addBomb(bombPosition);
					}
				}
				else
				{
					game.channel.sendMessage("Chaos Option Selected. **The next player may want to wait until the bomb has been placed.**").queue();
					game.players.get(player).user.openPrivateChannel().queue(
							(channel) -> channel.sendMessage("Please place your bomb within the next 30 seconds "
									+ "by sending a number 1-" + game.boardSize + " (make sure the space hasn't been picked)").queue());
					waiter.waitForEvent(MessageReceivedEvent.class,
							//Check if right player, and valid bomb pick
							e -> (e.getAuthor().equals(game.players.get(player).user) && e.getChannel().getType() == ChannelType.PRIVATE
									&& game.checkValidNumber(e.getMessage().getContentStripped())
									&& !game.pickedSpaces[Integer.parseInt(e.getMessage().getContentStripped())-1]),
							//Parse it and update the bomb board
							e -> 
							{
								int bombLocation = Integer.parseInt(e.getMessage().getContentStripped())-1;
								game.gameboard.addBomb(bombLocation);
								game.players.get(player).knownBombs.add(bombLocation);
								game.players.get(player).user.openPrivateChannel().queue(
										(channel) -> channel.sendMessage("Bomb placement confirmed.").queue());
								game.channel.sendMessage("The new bomb has been placed!").queue();
							},
							//Or timeout the prompt without adding a bomb (but tell them it was added anyway)
							45, TimeUnit.SECONDS, () ->
                                    game.channel.sendMessage("The new bomb has been placed!").queue());
				}
			}
		},
		DEATH_OR_GLORY("Quintuple Deal", "Half of cash spaces become BLAMMOs")
		{
			boolean checkCondition(GameController game, int player)
			{
				return game.spacesLeft > 0;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Hope you win big!").queue();
				game.boardMultiplier *= 5;
				for(int i=0; i<game.boardSize; i++)
					if(!game.pickedSpaces[i] && game.gameboard.getType(i) == SpaceType.CASH && Math.random() < 0.5)
						game.gameboard.changeType(i, SpaceType.BLAMMO);
			}
		},
		MARKET_INVESTMENT("'Invest in the Market' and add %d Market events to the board", "$%,d")
		{
			final static int PER_MARKET_PRICE = 250_000;
			int countMarkets(GameController game)
			{
				int markets = 0;
				for(int i=0; i<game.boardSize; i++)
					if(!game.pickedSpaces[i] && (game.gameboard.getType(i) == SpaceType.EVENT || game.gameboard.getType(i) == SpaceType.GRAB_BAG))
						markets ++;
				return markets;
			}
			String getReward(GameController game, int player)
			{
				return String.format(reward, countMarkets(game));
			}
			String getRisk(GameController game, int player)
			{
				return String.format(risk, game.applyBaseMultiplier(PER_MARKET_PRICE*countMarkets(game)));
			}
			boolean checkCondition(GameController game, int player)
			{
				return countMarkets(game) > 1;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Time for a shopping spree!").queue();
				game.players.get(player).addMoney(-1*game.applyBaseMultiplier(PER_MARKET_PRICE*countMarkets(game)), MoneyMultipliersToUse.NOTHING);
				game.gameboard.eventCurse(EventType.RTAB_MARKET);
			}
		},
		DRAW_FOUR_YOURSELF("+100% Boost", "Draw 4 on Yourself")
		{
			boolean checkCondition(GameController game, int player)
			{
				return game.spacesLeft >= 4;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option selected. Have fun capitalising on it!").queue();
				game.players.get(player).addBooster(100);
				game.repeatTurn += 4;
			}
		};
		
		
		final String reward;
		final String risk;
		ChaosOption(String reward, String risk)
		{
			this.reward = reward;
			this.risk = risk;
		}
		String getReward(GameController game, int player)
		{
			return reward;
		}
		String getRisk(GameController game, int player)
		{
			return risk;
		}
		abstract boolean checkCondition(GameController game, int player);
		abstract void applyResult(GameController game, int player);
	}
	
	GameController game;
	int player;
	private Player getCurrentPlayer()
	{
		return game.players.get(player);
	}
	
	@Override
	public String getName()
	{
		return "RtaB Market";
	}

	@Override
	public void execute(GameController game, int player)
	{
		//Initialise stuff
		this.game = game;
		this.player = player;
		game.channel.sendMessage("It's the **RtaB Market**!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		//Decide on basic offerings
		validOptions = new LinkedList<>();
		int boostBuyable = getCurrentPlayer().getRoundDelta() / game.applyBaseMultiplier(BUY_BOOST_PRICE);
		buyBoostAmount = Math.max(0, Math.min(999-getCurrentPlayer().booster,(int)((Math.random()*.8+.2)*boostBuyable)));
		if(buyBoostAmount > 0)
			validOptions.add("BUY BOOST");
		int boostAvailable = getCurrentPlayer().booster / 2;
		sellBoostAmount = boostAvailable > 50 ? (int)((Math.random()*.4+.1)*boostAvailable)+1 : 0;
		if(sellBoostAmount > 0)
			validOptions.add("SELL BOOST");
		effectiveGamePrice = game.applyBaseMultiplier(GAME_PRICE) / game.playersAlive;
		minigameOffered = game.players.get(player).generateEventMinigame();
		validOptions.add("BUY GAME");
		if(!getCurrentPlayer().games.isEmpty())
			validOptions.add("SELL GAME");
		validOptions.add("BUY PEEK");
		if(getCurrentPlayer().peeks > 0)
			validOptions.add("SELL PEEK");
		if(Math.random() < 0.01)
			validOptions.add("BUY LIFE");
		if(Math.random() < -1)
			validOptions.add("BUY TRIFORCE"); //Neener neener
		if(!game.tiebreakMode)
			validOptions.addAll(Arrays.asList("BUY COMMAND", "BUY INFO")); //No commands or info in a tiebreak
		//25% chance of chaos option
		if(Math.random() < 0.25)
		{
			chaosOption = ChaosOption.values()[(int)(Math.random()*ChaosOption.values().length)];
			if(chaosOption.checkCondition(game, player))
				validOptions.add("CHAOS");
		}
		//Prepare for robbery
		validOptions.addAll(Arrays.asList("ROB ROCK","ROB PAPER","ROB SCISSORS"));
		//and let them go
		validOptions.add("LEAVE");
		//Open the market!
		openMarket(true);
		//Wait for them to be done
		if(!getCurrentPlayer().isBot)
			while(status != EventStatus.FINISHED)
			{
				try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); status = EventStatus.FINISHED; }
			}
		//Once it's finished, the execute method ends and control passes back to the game controller
	}
	private void openMarket(boolean firstTime)
	{
		StringBuilder shopMenu = new StringBuilder();
		shopMenu.append("```\n");
		if(firstTime)
		{
			//Get a greeting from the file
			try
			{
				List<String> list = Files.readAllLines(Paths.get("MarketGreetings.txt"));
				shopMenu.append(list.get((int)(Math.random()*list.size())));
			}
			catch (IOException e)
			{
				shopMenu.append("It's the RtaB Market!");
			}
		}
		shopMenu.append("\n\nAvailable Wares:\n");
		if(validOptions.contains("BUY BOOST"))
			shopMenu.append(String.format("BUY BOOST - +%d%% Boost (Cost: $%,d)\n",
					buyBoostAmount, buyBoostAmount*game.applyBaseMultiplier(BUY_BOOST_PRICE) + repeatPenalty()));
		if(validOptions.contains("SELL BOOST"))
			shopMenu.append(String.format("SELL BOOST - $%,d (Cost: %d%% Boost)\n",
					sellBoostAmount*game.applyBaseMultiplier(SELL_BOOST_PRICE), sellBoostAmount));
		if(validOptions.contains("BUY GAME"))
			shopMenu.append(String.format("BUY GAME - %s (Cost: $%,d)\n", minigameOffered.getName(), effectiveGamePrice + repeatPenalty()));
		if(validOptions.contains("SELL GAME"))
			shopMenu.append(String.format("SELL GAME - $%,d (Cost: Your Minigames)\n", getCurrentPlayer().games.size()*effectiveGamePrice*3/4));
		if(validOptions.contains("BUY PEEK"))
			shopMenu.append(String.format("BUY PEEK - 1 Peek (Cost: $%,d)\n", game.applyBaseMultiplier(BUY_PEEK_PRICE) + repeatPenalty()));
		if(validOptions.contains("SELL PEEK"))
			shopMenu.append(String.format("SELL PEEK - $%,d (Cost: 1 Peek)\n", game.applyBaseMultiplier(SELL_PEEK_PRICE)));
		if(validOptions.contains("BUY LIFE"))
			shopMenu.append(String.format("BUY LIFE - 1 Life (Cost: $%,d)\n", game.applyBaseMultiplier(10_000)));
		if(validOptions.contains("BUY COMMAND"))
			shopMenu.append(String.format("BUY COMMAND - Random Hidden Command (Cost: $%,d)\n", 
					game.applyBaseMultiplier(BUY_COMMAND_PRICE*(commandPrice/10)) + repeatPenalty()));
		if(validOptions.contains("BUY INFO"))
			shopMenu.append(String.format("BUY INFO - List of Remaining Spaces (Cost: $%,d)\n", game.applyBaseMultiplier(BUY_INFO_PRICE) + repeatPenalty()));
		if(validOptions.contains("CHAOS"))
		{
			shopMenu.append(String.format("\nCHAOS - %s\n      (Cost: %s)\n", chaosOption.getReward(game, player),chaosOption.getRisk(game, player)));
			//Build up suspense
			game.channel.sendMessage(":warning: **WARNING: CHAOS OPTION DETECTED** :warning:").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		}
		if(firstTime) //Can't rob the market if you've already started shopping
		{
			shopMenu.append("\nRob the Market - Choose your weapon:\nROB ROCK\nROB PAPER\nROB SCISSORS\n");
			int weaponChoice = (int)(Math.random()*RPSOption.values().length);
			int backupChoice = (int)(Math.random()*(RPSOption.values().length-1));
			if(backupChoice >= weaponChoice)
				backupChoice++;
			shopWeapon = RPSOption.values()[weaponChoice];
			backupWeapon = RPSOption.values()[backupChoice];
		}
		shopMenu.append("\nLEAVE\n");
		shopMenu.append(String.format("\nCurrent Cash: %s$%,d\n", 
				getCurrentPlayer().getRoundDelta() >= 0 ? "+" : "-", Math.abs(getCurrentPlayer().getRoundDelta())));
		shopMenu.append("Type the capitalised words to make your selection.\n```");
		//Send the messages
		if(firstTime)
			game.channel.sendMessage(getCurrentPlayer().getSafeMention()+", you have ninety seconds to make a selection!").queue();
		else
			game.channel.sendMessage(getCurrentPlayer().getSafeMention()+", would you like to buy more?").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		game.channel.sendMessage(shopMenu.toString()).queue();
		//Find out what we're doing
		if(getCurrentPlayer().isBot)
		{
			//Pick randomly, but rob instead of buying info
			int chosenPick = (int)(Math.random() * (validOptions.size()-4));
			if(validOptions.get(chosenPick).equals("BUY INFO"))
			{
				//COMMIT ROBBERY
				switch ((int) (Math.random() * 3)) {
					case 0 -> resolveShop("ROB ROCK");
					case 1 -> resolveShop("ROB PAPER");
					case 2 -> resolveShop("ROB SCISSORS");
					default -> resolveShop("LEAVE"); //should never happen
				}
			}
			else
				resolveShop(validOptions.get(chosenPick));
		}
		else
		{
			//Set up flow trap and wait for input
			status = EventStatus.WAITING;
			RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
                            e.getChannel().getId().equals(game.channel.getId()) && e.getAuthor().equals(getCurrentPlayer().user)
                                && validOptions.contains(e.getMessage().getContentStripped().toUpperCase()),
					//Parse it and call the method that does stuff
					e ->
                            resolveShop(e.getMessage().getContentStripped().toUpperCase()),
					90,TimeUnit.SECONDS, () ->
					{
						if(status == EventStatus.WAITING)
							resolveShop("LEAVE");
					});
		}
	}
	
	private int repeatPenalty()
	{
		//Add on 1/10000ths total bank x the square of items already bought
		//(if they buy 5 things, this results in 0 + 1 + 4 + 9 + 16 = 30/10000 of total cash bank, or 0.3%) 
		return game.applyBaseMultiplier((getCurrentPlayer().money/10_000)*itemsBought*itemsBought);
	}
	
	private void resolveShop(String choice)
	{
		status = EventStatus.RESOLVING;
		//Removing one-chance options from the list no matter what they chose so they aren't offered again
		validOptions.removeAll(Arrays.asList("CHAOS", "BUY LIFE", "ROB ROCK","ROB PAPER","ROB SCISSORS"));
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		switch(choice)
		{
		case "BUY BOOST":
			game.channel.sendMessage("Boost bought!").queue();
			getCurrentPlayer().addMoney(-1*buyBoostAmount*game.applyBaseMultiplier(BUY_BOOST_PRICE) - repeatPenalty(), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().addBooster(buyBoostAmount);
			itemsBought ++;
			validOptions.removeAll(Arrays.asList("BUY BOOST", "SELL BOOST"));
			break;
		case "SELL BOOST":
			game.channel.sendMessage("Boost sold!").queue();
			getCurrentPlayer().addMoney(sellBoostAmount*game.applyBaseMultiplier(SELL_BOOST_PRICE), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().addBooster(-1*sellBoostAmount);
			validOptions.removeAll(Arrays.asList("BUY BOOST", "SELL BOOST"));
			break;
		case "BUY GAME":
			game.channel.sendMessage("Minigame bought!").queue();
			getCurrentPlayer().addMoney(-1*effectiveGamePrice - repeatPenalty(), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().games.add(minigameOffered);
			itemsBought ++;
			validOptions.removeAll(Arrays.asList("BUY GAME", "SELL GAME"));
			break;
		case "SELL GAME":
			game.channel.sendMessage("Minigames sold!").queue();
			getCurrentPlayer().addMoney(getCurrentPlayer().games.size()*effectiveGamePrice, MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().games.clear();
			validOptions.removeAll(Arrays.asList("BUY GAME", "SELL GAME"));
			break;
		case "BUY PEEK":
			game.channel.sendMessage("Peek bought!").queue();
			getCurrentPlayer().addMoney(-1*game.applyBaseMultiplier(BUY_PEEK_PRICE) - repeatPenalty(), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().peeks++;
			itemsBought ++;
			validOptions.removeAll(Arrays.asList("BUY PEEK", "SELL PEEK"));
			break;
		case "SELL PEEK":
			if(getCurrentPlayer().peeks > 0)
			{
				game.channel.sendMessage("Peek sold!").queue();
				getCurrentPlayer().addMoney(game.applyBaseMultiplier(SELL_PEEK_PRICE), MoneyMultipliersToUse.NOTHING);
				getCurrentPlayer().peeks--;
			}
			else
				game.channel.sendMessage("Empty-space-where-a-peek-used-to-be sold for FREE AIR!").queue();
			validOptions.removeAll(Arrays.asList("BUY PEEK", "SELL PEEK"));
			break;
		case "BUY LIFE":
			game.channel.sendMessage("Oooh, too slow. We *just* ran out!").queue();
			validOptions.remove("BUY LIFE");
			break;
		case "BUY COMMAND":
			game.channel.sendMessage("Command bought!").queue();
			getCurrentPlayer().addMoney(-1*game.applyBaseMultiplier(BUY_COMMAND_PRICE*(commandPrice/10)) - repeatPenalty(), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().awardHiddenCommand();
			itemsBought ++;
			validOptions.remove("BUY COMMAND");
			break;
		case "BUY INFO":
			game.channel.sendMessage("Information coming your way!").queue();
			getCurrentPlayer().addMoney(-1*game.applyBaseMultiplier(BUY_INFO_PRICE) - repeatPenalty(), MoneyMultipliersToUse.NOTHING);
			validOptions.remove("BUY INFO");
			itemsBought ++;
			if(!getCurrentPlayer().isBot) //A bot should never get here and we don't want to try sending a message to it if it somehow does
			{
				if(game.spacesLeft > 0)
				{
					//Prepare the 2D list
					ArrayList<ArrayList<String>> gridList = new ArrayList<>(SpaceType.values().length);
					for(int i=0; i<SpaceType.values().length; i++)
						gridList.add(new ArrayList<>());
					//Get the list of remaining spaces
					for(int i=0; i<game.boardSize; i++)
						if(!game.pickedSpaces[i])
							gridList.get(game.gameboard.getType(i).ordinal())
								.add(game.gameboard.truesightSpace(i, game.baseNumerator, game.baseDenominator));
					//Shuffle each category
					for(ArrayList<String> next : gridList)
						Collections.shuffle(next);
					//Build the list message
					StringBuilder gridListMessage = new StringBuilder();
					gridListMessage.append("Remaining spaces:\n");
					for(ArrayList<String> next : gridList)
					{
						for(int i=0; i<next.size(); i++)
						{
							gridListMessage.append(next.get(i));
							if(i+1 < next.size())
								gridListMessage.append(" | ");
						}
						if(!next.isEmpty())
							gridListMessage.append("\n");
					}
					//and finally send it to them
					getCurrentPlayer().user.openPrivateChannel().queue(
							(channel) -> channel.sendMessage(gridListMessage.toString()).queueAfter(1,TimeUnit.SECONDS));
				}
				else
				{
					//...there's no spaces left to show them so let's just meme
					try
					{
						List<String> list = Files.readAllLines(Paths.get("MarketInfoMemes.txt"));
						getCurrentPlayer().user.openPrivateChannel().queue(
								(channel) -> channel.sendMessage(list.get((int)(Math.random()*list.size()))).queueAfter(1,TimeUnit.SECONDS));
					}
					catch (IOException e)
					{
						getCurrentPlayer().user.openPrivateChannel().queue(
								(channel) -> channel.sendMessage("There are no remaining spaces.").queueAfter(1,TimeUnit.SECONDS));
					}
				}
			}
			break;
		case "BUY TRIFORCE":
			game.channel.sendMessage("Your triforce is here: https://www.youtube.com/watch?v=nsCIeklgp1M").queue();
			validOptions.remove("BUY TRIFORCE");
		case "ROB ROCK":
			commitRobbery(RPSOption.ROCK);
			break;
		case "ROB PAPER":
			commitRobbery(RPSOption.PAPER);
			break;
		case "ROB SCISSORS":
			commitRobbery(RPSOption.SCISSORS);
			break;
		case "CHAOS":
			chaosOption.applyResult(game, player);
			status = EventStatus.FINISHED;
			break;
		case "LEAVE":
			game.channel.sendMessage("Alright, see you next time.").queue();
			status = EventStatus.FINISHED;
		}
		//Bots don't multibuy, and if there's nothing left for a player to do but leave then we can wrap up immediately too
		if(getCurrentPlayer().isBot || validOptions.size() == 1)
			status = EventStatus.FINISHED;
		if(status != EventStatus.FINISHED)
			openMarket(false);
	}
	void commitRobbery(RPSOption weapon)
	{
		game.channel.sendMessage("You confidently stride up to the shopkeeper with your trusty "+weapon.toString().toLowerCase()
				+", intent on stealing as much as you can...").queue();
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		//you know rtab has gone too far when you're writing rock-paper-scissors fanfiction
		//...or not far enough? (-JerryEris)
		switch (weapon) {
			case ROCK -> {
				switch (shopWeapon) {
					case ROCK -> {
						game.channel.sendMessage("...and find them carrying a rock of their own. A tie?!").queue();
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						switch (backupWeapon) {
							case PAPER -> {
								game.channel.sendMessage("They then reach into a drawer and pull out a sheet of paper... *oh no*.").queue();
								robberyFailure();
							}
							case SCISSORS -> {
								game.channel.sendMessage("At an impasse, they reach into their pocket but find only a pair of scissors. Got'em!").queue();
								robberySuccess();
							}
							default ->
									game.channel.sendMessage("Then the game glitched, and you ran away before anything bad could happen.").queue();
						}
					}
					case PAPER -> {
						game.channel.sendMessage("...but then you spot their menacing glare, "
								+ "and the obviously-superior sheet of paper in their hand. Whoops!").queue();
						robberyFailure();
					}
					case SCISSORS -> {
						game.channel.sendMessage("They try to fight back with a pair of scissors, "
								+ "but your rock quickly breaks it and they flee. Success!").queue();
						robberySuccess();
					}
				}
			}
			case PAPER -> {
				switch (shopWeapon) {
					case ROCK -> {
						game.channel.sendMessage("They grab a rock to fight back with, "
								+ "but at the sight of your obviously-superior paper they flee in terror. Success!").queue();
						robberySuccess();
					}
					case PAPER -> {
						game.channel.sendMessage("...and find them carrying some paper of their own. A tie?!").queue();
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						switch (backupWeapon) {
							case ROCK -> {
								game.channel.sendMessage("At an impasse, they reach down but find only a rock on the ground. Got'em!").queue();
								robberySuccess();
							}
							case SCISSORS -> {
								game.channel.sendMessage("Glaring at you, they reach into their pocket and draw a pair of scissors. Oh dear...").queue();
								robberyFailure();
							}
							default ->
									game.channel.sendMessage("Then the game glitched, and you ran away before anything bad could happen.").queue();
						}
					}
					case SCISSORS -> {
						game.channel.sendMessage("...but they pull a pair of scissors from their pocket and cut your paper in two. Whoops!").queue();
						robberyFailure();
					}
				}
			}
			case SCISSORS -> {
				switch (shopWeapon) {
					case ROCK -> {
						game.channel.sendMessage("...but they grab a rock from the ground and quickly destroy your scissors. Whoops!").queue();
						robberyFailure();
					}
					case PAPER -> {
						game.channel.sendMessage("They grab a sheet of paper from a drawer, but you cut it in two. Success!").queue();
						robberySuccess();
					}
					case SCISSORS -> {
						game.channel.sendMessage("...and find them carrying some scissors of their own. A tie?!").queue();
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						switch (backupWeapon) {
							case ROCK -> {
								game.channel.sendMessage("They blink, then grab a rock off the ground to fight back with. Oh dear...").queue();
								robberyFailure();
							}
							case PAPER -> {
								game.channel.sendMessage("At an impasse, they reach into a drawer but find only a sheet of paper. Got'em!").queue();
								robberySuccess();
							}
							default ->
									game.channel.sendMessage("Then the game glitched, and you ran away before anything bad could happen.").queue();
						}
					}
				}
			}
		}
	}
	void robberySuccess()
	{
		//You get a pretty awesome grab bag!
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		game.channel.sendMessage("The shopkeeper dealt with, you make off with the following...").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		if(minigameOffered == null)
			game.awardGame(player, Board.generateSpaces(1, game.players.size(), Game.values()).get(0));
		else
			game.awardGame(player, minigameOffered);
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		game.awardBoost(player, Boost.P150);
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		game.awardCash(player, Cash.P1000K);
		if(getCurrentPlayer().hiddenCommand == HiddenCommand.NONE)
			getCurrentPlayer().awardHiddenCommand();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); } //mini-suspense lol
		game.awardEvent(player, EventType.PEEK_REPLENISH);
		status = EventStatus.FINISHED;
	}
	void robberyFailure()
	{
		int penalty = game.calculateBombPenalty(player);
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		game.channel.sendMessage(String.format("%s was arrested. $%,d fine.",
				getCurrentPlayer().getName(), Math.abs(penalty))).queue();
		StringBuilder extraResult = game.players.get(player).blowUp(penalty,false);
		if(extraResult != null)
			game.channel.sendMessage(extraResult).queue();
		status = EventStatus.FINISHED;
	}
}
