package tel.discord.rtab.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
	
	static final String[] GREETING_QUOTES = {"BUY SOMETHIN' WILL YA!",
			"Look at this market, filled with glamourous prizes!",
			"It has been 5,000 years since my last customer.",
			"I can't rest in peace if you don't buy something...",
			"Money burning a hole in yer pocket? Time to spend!",
			"Welcome, meow! How can I help you today, meow?",
			"Feel free to browse, but try not to carouse!",
			"Selected items for your convenience!",
			"NO REFUNDS",
			"What are ya buyin'? What are ya sellin'?"};

	int buyBoostAmount, sellBoostAmount;
	Game minigameOffered = null;
	LinkedList<String> validOptions;
	RPSOption shopWeapon, backupWeapon;
	EventStatus status = EventStatus.PREPARING;
	ChaosOption chaosOption = null;
	
	private enum RPSOption
	{
		ROCK, PAPER, SCISSORS;
	}
	private enum EventStatus
	{
		PREPARING, WAITING, RESOLVING, FINISHED;
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
				return true;
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
					if(i != player && game.players.get(i).status == PlayerStatus.ALIVE)
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
				game.players.get(player).status = game.players.get(player).games.size()>0 ? PlayerStatus.FOLDED : PlayerStatus.OUT;
				game.playersAlive--;
				game.players.get(player).splitAndShare = false;
			}
		},
		BUY_WINSTREAK("+%d.%d Streak", "$%,d")
		{
			int getPrice(int currentStreak)
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
				return game.players.get(player).winstreak < 120 && game.players.size() <= 8;
			}
			void applyResult(GameController game, int player)
			{
				game.channel.sendMessage("Chaos Option Selected. Good luck in the bonus game!").queue();
				game.players.get(player).addMoney(
						game.applyBaseMultiplier(getPrice(game.players.get(player).winstreak)), MoneyMultipliersToUse.NOTHING);
				game.players.get(player).addWinstreak(40 - (game.players.get(player).winstreak % 40));
			}
		};
		
		String reward;
		String risk;
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
		//Introduction
		this.game = game;
		this.player = player;
		game.channel.sendMessage("It's the **RtaB Market**!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		//Decide on basic offerings
		int boostBuyable = getCurrentPlayer().getRoundDelta() / game.applyBaseMultiplier(BUY_BOOST_PRICE);
		buyBoostAmount = Math.max(0, (int)((Math.random()*.8+.2)*boostBuyable));
		int boostAvailable = getCurrentPlayer().booster / 2;
		sellBoostAmount = boostAvailable > 50 ? (int)((Math.random()*.4+.1)*boostAvailable)+1 : 0;
		int effectiveGamePrice = game.applyBaseMultiplier(GAME_PRICE) / game.playersAlive;
		if(getCurrentPlayer().getRoundDelta() >= effectiveGamePrice)
			minigameOffered = Board.generateSpaces(1, game.players.size(), Game.values()).get(0);
		//25% chance of chaos option
		if(Math.random() < 0.25)
			chaosOption = ChaosOption.values()[(int)(Math.random()*ChaosOption.values().length)];
		//Create list of options
		validOptions = new LinkedList<String>();
		StringBuilder shopMenu = new StringBuilder();
		shopMenu.append("```\n");
		shopMenu.append(GREETING_QUOTES[(int)(Math.random()*GREETING_QUOTES.length)]+"\n\n");
		shopMenu.append("Available Wares:\n");
		if(buyBoostAmount > 0)
		{
			shopMenu.append(String.format("BUY BOOST - +%d%% Boost (Cost: $%,d)\n",
					buyBoostAmount, buyBoostAmount*game.applyBaseMultiplier(BUY_BOOST_PRICE)));
			validOptions.add("BUY BOOST");
		}
		if(sellBoostAmount > 0)
		{
			shopMenu.append(String.format("SELL BOOST - $%,d (Cost: %d%% Boost)\n",
					sellBoostAmount*game.applyBaseMultiplier(SELL_BOOST_PRICE), sellBoostAmount));
			validOptions.add("SELL BOOST");
		}
		if(minigameOffered != null)
		{
			shopMenu.append(String.format("BUY GAME - %s (Cost: $%,d)\n", minigameOffered.getName(), effectiveGamePrice));
			validOptions.add("BUY GAME");
		}
		if(getCurrentPlayer().games.size() > 0)
		{
			shopMenu.append(String.format("SELL GAME - $%,d (Cost: Your Minigames)\n", getCurrentPlayer().games.size()*effectiveGamePrice));
			validOptions.add("SELL GAME");
		}
		if(getCurrentPlayer().getRoundDelta() >= game.applyBaseMultiplier(BUY_PEEK_PRICE))
		{
			shopMenu.append(String.format("BUY PEEK - 1 Peek (Cost: $%,d)\n", game.applyBaseMultiplier(BUY_PEEK_PRICE)));
			validOptions.add("BUY PEEK");
		}
		if(getCurrentPlayer().peeks > 0)
		{
			shopMenu.append(String.format("SELL PEEK - $%,d (Cost: 1 Peek)\n", game.applyBaseMultiplier(SELL_PEEK_PRICE)));
			validOptions.add("SELL PEEK");
		}
		if(true)
		{
			shopMenu.append(String.format("BUY COMMAND - Random Hidden Command (Cost: $%,d)\n", game.applyBaseMultiplier(BUY_COMMAND_PRICE)));
			validOptions.add("BUY COMMAND");
		}
		if(true)
		{
			shopMenu.append(String.format("BUY INFO - List of Remaining Spaces (Cost: $%,d)\n", game.applyBaseMultiplier(BUY_INFO_PRICE)));
			validOptions.add("BUY INFO");
		}
		if(chaosOption != null && chaosOption.checkCondition(game, player))
		{
			shopMenu.append(String.format("\nCHAOS - %s\n      (Cost: %s)\n", chaosOption.getReward(game, player),chaosOption.getRisk(game, player)));
			validOptions.add("CHAOS");
			//Build up suspense
			game.channel.sendMessage(":warning: **WARNING: CHAOS OPTION DETECTED** :warning:").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		}
		shopMenu.append("\nRob the Market - Choose your weapon:\nROB ROCK\nROB PAPER\nROB SCISSORS\n\nLEAVE\n\n"
				+ "Type the capitalised words to make your selection.\n```");
		validOptions.addAll(Arrays.asList("ROB ROCK","ROB PAPER","ROB SCISSORS","LEAVE"));
		int weaponChoice = (int)(Math.random()*RPSOption.values().length);
		int backupChoice = (int)(Math.random()*(RPSOption.values().length-1));
		if(backupChoice >= weaponChoice)
			backupChoice++;
		shopWeapon = RPSOption.values()[weaponChoice];
		backupWeapon = RPSOption.values()[backupChoice];
		//Send the messages
		game.channel.sendMessage(getCurrentPlayer().getSafeMention()+", you have sixty seconds to make a selection!").queue();
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
				switch((int)(Math.random()*3))
				{
				case 0:
					resolveShop("ROB ROCK");
					break;
				case 1:
					resolveShop("ROB PAPER");
					break;
				case 2:
					resolveShop("ROB SCISSORS");
					break;
				default:
					resolveShop("LEAVE"); //should never happen
				}
			}
			else
				resolveShop(validOptions.get(chosenPick));
		}
		else
		{
			//Set up flow trap and wait for input
			status = EventStatus.WAITING;
			ScheduledFuture<?> warnPlayer = game.timer.schedule(() -> 
			{
				game.channel.sendMessage(getCurrentPlayer().getSafeMention() + 
						", twenty seconds left to make a decision!").queue();
			}, 40, TimeUnit.SECONDS);
			RaceToABillionBot.waiter.waitForEvent(MessageReceivedEvent.class,
					//Right player and channel
					e ->
					{
						return e.getChannel().equals(game.channel) && e.getAuthor().equals(getCurrentPlayer().user)
							&& validOptions.contains(e.getMessage().getContentStripped().toUpperCase());
					},
					//Parse it and call the method that does stuff
					e -> 
					{
						warnPlayer.cancel(false);
						resolveShop(e.getMessage().getContentStripped().toUpperCase());
					},
					60,TimeUnit.SECONDS, () ->
					{
						if(status == EventStatus.WAITING)
							resolveShop("LEAVE");
					});
			while(status != EventStatus.FINISHED)
			{
				try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); status = EventStatus.FINISHED; }
			}
			//Once it's finished, the execute method ends and control passes back to the game controller
		}
	}
	private void resolveShop(String choice)
	{
		status = EventStatus.RESOLVING;
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		switch(choice)
		{
		case "BUY BOOST":
			game.channel.sendMessage("Boost bought!").queue();
			getCurrentPlayer().addMoney(-1*buyBoostAmount*game.applyBaseMultiplier(BUY_BOOST_PRICE), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().addBooster(buyBoostAmount);
			break;
		case "SELL BOOST":
			game.channel.sendMessage("Boost sold!").queue();
			getCurrentPlayer().addMoney(sellBoostAmount*game.applyBaseMultiplier(SELL_BOOST_PRICE), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().addBooster(-1*sellBoostAmount);
			break;
		case "BUY GAME":
			game.channel.sendMessage("Minigame bought!").queue();
			getCurrentPlayer().addMoney(-1*game.applyBaseMultiplier(GAME_PRICE)/game.playersAlive, MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().games.add(minigameOffered);
			break;
		case "SELL GAME":
			game.channel.sendMessage("Minigames sold!").queue();
			getCurrentPlayer().addMoney(
					getCurrentPlayer().games.size()*game.applyBaseMultiplier(GAME_PRICE)/game.playersAlive, MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().games.clear();
			break;
		case "BUY PEEK":
			game.channel.sendMessage("Peek bought!").queue();
			getCurrentPlayer().addMoney(-1*game.applyBaseMultiplier(BUY_PEEK_PRICE), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().peeks++;
			break;
		case "SELL PEEK":
			game.channel.sendMessage("Peek sold!").queue();
			getCurrentPlayer().addMoney(game.applyBaseMultiplier(SELL_PEEK_PRICE), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().peeks--;
			break;
		case "BUY COMMAND":
			game.channel.sendMessage("Command bought!").queue();
			getCurrentPlayer().addMoney(-1*game.applyBaseMultiplier(BUY_COMMAND_PRICE), MoneyMultipliersToUse.NOTHING);
			getCurrentPlayer().awardHiddenCommand();
			break;
		case "BUY INFO":
			game.channel.sendMessage("Information coming your way!").queue();
			getCurrentPlayer().addMoney(-1*game.applyBaseMultiplier(BUY_INFO_PRICE), MoneyMultipliersToUse.NOTHING);
			if(!getCurrentPlayer().isBot) //A bot should never get here and we don't want to try sending a message to it if it somehow does
			{
				//Prepare the 2D list
				ArrayList<ArrayList<String>> gridList = new ArrayList<ArrayList<String>>(SpaceType.values().length);
				for(int i=0; i<SpaceType.values().length; i++)
					gridList.add(new ArrayList<String>());
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
			break;
		case "CHAOS":
			chaosOption.applyResult(game, player);
			break;
		case "ROB ROCK":
			commitRobbery(RPSOption.ROCK);
			break;
		case "ROB PAPER":
			commitRobbery(RPSOption.PAPER);
			break;
		case "ROB SCISSORS":
			commitRobbery(RPSOption.SCISSORS);
			break;
		case "LEAVE":
			game.channel.sendMessage("Not buying? Well, if you say so.").queue();
		}
		status = EventStatus.FINISHED;
	}
	void commitRobbery(RPSOption weapon)
	{
		game.channel.sendMessage("You confidently stride up to the shopkeeper with your trusty "+weapon.toString().toLowerCase()
				+", intent on stealing as much as you can...").queue();
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		//you know rtab has gone too far when you're writing rock-paper-scissors fanfiction
		switch(weapon)
		{
		case ROCK:
			switch(shopWeapon)
			{
			case ROCK:
				game.channel.sendMessage("...and find them carrying a rock of their own. A tie?!").queue();
				try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
				switch(backupWeapon)
				{
				case PAPER:
					game.channel.sendMessage("They then reach into a drawer and pull out a sheet of paper... *oh no*.").queue();
					robberyFailure();
					break;
				case SCISSORS:
					game.channel.sendMessage("At an impasse, they reach into their pocket but find only a pair of scissors. Got'em!").queue();
					robberySuccess();
					break;
				default:
					game.channel.sendMessage("Then the game glitched, and you ran away before anything bad could happen.").queue();
					break;
				}
				break;
			case PAPER:
				game.channel.sendMessage("...but then you spot their menacing glare, "
						+ "and the obviously-superior sheet of paper in their hand. Whoops!").queue();
				robberyFailure();
				break;
			case SCISSORS:
				game.channel.sendMessage("They try to fight back with a pair of scissors, "
						+ "but your rock quickly breaks it and they flee. Success!").queue();
				robberySuccess();
				break;
			}
			break;
		case PAPER:
			switch(shopWeapon)
			{
			case ROCK:
				game.channel.sendMessage("They grab a rock to fight back with, "
						+ "but at the sight of your obviously-superior paper they flee in terror. Success!").queue();
				robberySuccess();
				break;
			case PAPER:
				game.channel.sendMessage("...and find them carrying some paper of their own. A tie?!").queue();
				try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
				switch(backupWeapon)
				{
				case ROCK:
					game.channel.sendMessage("At an impasse, they reach down but find only a rock on the ground. Got'em!").queue();
					robberySuccess();
					break;
				case SCISSORS:
					game.channel.sendMessage("Glaring at you, they reach into their pocket and draw a pair of scissors. Oh dear...").queue();
					robberyFailure();
					break;
				default:
					game.channel.sendMessage("Then the game glitched, and you ran away before anything bad could happen.").queue();
					break;
				}
				break;
			case SCISSORS:
				game.channel.sendMessage("...but they pull a draw of scissors from their pocket and cut your paper in two. Whoops!").queue();
				robberyFailure();
				break;
			}
			break;
		case SCISSORS:
			switch(shopWeapon)
			{
			case ROCK:
				game.channel.sendMessage("...but they grab a rock from the ground and quickly destroy your scissors. Whoops!").queue();
				robberyFailure();
				break;
			case PAPER:
				game.channel.sendMessage("They grab a sheet of paper from a drawer, but you cut it in two. Success!").queue();
				robberySuccess();
				break;
			case SCISSORS:
				game.channel.sendMessage("...and find them carrying some scissors of their own. A tie?!").queue();
				try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
				switch(backupWeapon)
				{
				case ROCK:
					game.channel.sendMessage("They blink, then grab a rock off the ground to fight back with. Oh dear...").queue();
					robberyFailure();
					break;
				case PAPER:
					game.channel.sendMessage("At an impasse, they reach into a drawer but find only a sheet of paper. Got'em!").queue();
					break;
				default:
					game.channel.sendMessage("Then the game glitched, and you ran away before anything bad could happen.").queue();
					break;
				}
				break;
			}
			break;
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
		if(getCurrentPlayer().hiddenCommand == null)
			getCurrentPlayer().awardHiddenCommand();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); } //mini-suspense lol
		game.awardEvent(player, EventType.PEEK_REPLENISH);
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
	}
}