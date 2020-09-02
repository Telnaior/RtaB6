package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.internal.utils.tuple.MutablePair;
import tel.discord.rtab.board.Game;
import tel.discord.rtab.board.HiddenCommand;


public class Player implements Comparable<Player>
{
	static final int MAX_BOOSTER = 999;
	static final int MIN_BOOSTER =  10;
	static final int MAX_LIVES = 5;
	GameController game;
	User user;
	public String name;
	String uID;
	boolean isBot;
	int lives;
	Instant lifeRefillTime;
	public int money;
	int oldMoney;
	int currentCashClub;
	public int booster;
	int winstreak;
	int oldWinstreak;
	int newbieProtection;
	HiddenCommand hiddenCommand;
	//Event fields
	int peek;
	int jokers;
	public int boostCharge;
	int jackpot;
	boolean splitAndShare;
	boolean threshold;
	boolean warned;
	public PlayerStatus status;
	public LinkedList<Game> games;
	LinkedList<Integer> knownBombs;
	LinkedList<Integer> safePeeks;
	LinkedList<MutablePair<Integer,Integer>> annuities;
	//Constructor for humans
	Player(Member playerName, GameController game, String botName)
	{
		user = playerName.getUser();
		uID = user.getId();
		//Bots don't get newbie protection, and neither do humans playing as bots
		if(botName == null)
		{
			name = playerName.getEffectiveName();
			newbieProtection = 10;
		}
		else
		{
			name = botName;
			newbieProtection = 0;
		}
		isBot = false;
		initPlayer(game);
	}
	//Constructor for bots
	Player(GameBot botName, GameController game)
	{
		user = null;
		name = botName.name;
		uID = botName.botID;
		isBot = true;
		newbieProtection = 0;
		initPlayer(game);
	}
	
	void initPlayer(GameController game)
	{
		this.game = game;
		lives = MAX_LIVES;
		lifeRefillTime = Instant.now().plusSeconds(72000);
		money = 0;
		booster = 100;
		winstreak = 10;
		peek = 1;
		jokers = 0;
		boostCharge = 0;
		hiddenCommand = HiddenCommand.NONE;
		splitAndShare = false;
		threshold = false;
		warned = false;
		status = PlayerStatus.OUT;
		games = new LinkedList<>();
		knownBombs = new LinkedList<>();
		safePeeks = new LinkedList<>();
		annuities = new LinkedList<>();
		List<String> list;
		try
		{
			list = Files.readAllLines(Paths.get("scores","scores"+game.channel.getId()+".csv"));
		}
		catch(IOException e)
		{
			System.out.println("No savefile found for "+game.channel.getName()+", creating.");
			list = new LinkedList<String>();
			try
			{
				Files.createFile(Paths.get("scores","scores"+game.channel.getId()+".csv"));
			}
			catch (IOException e1)
			{
				System.err.println("Couldn't create it either. Oops.");
				e1.printStackTrace();
				return;
			}
		}
		String[] record;
		for(int i=0; i<list.size(); i++)
		{
			/*
			 * record format:
			 * record[0] = uID
			 * record[1] = name
			 * record[2] = money
			 * record[3] = booster
			 * record[4] = winstreak
			 * record[5] = newbieProtection
			 * record[6] = lives
			 * record[7] = time at which lives refill
			 * record[8] = saved hidden command
			 * record[9] = saved boost charge
			 * record[10] = annuities
			 */
			record = list.get(i).split("#");
			if(record[0].equals(uID))
			{
				money = Integer.parseInt(record[2]);
				booster = Integer.parseInt(record[3]);
				winstreak = Integer.parseInt(record[4]);
				newbieProtection = Integer.parseInt(record[5]);
				lives = Integer.parseInt(record[6]);
				lifeRefillTime = Instant.parse(record[7]);
				hiddenCommand = HiddenCommand.valueOf(record[8]);
				boostCharge = Integer.parseInt(record[9]);
				//The annuities structure is more complicated, we can't just parse it in directly like the others
				String savedAnnuities = record[10];
				savedAnnuities = savedAnnuities.replaceAll("[^\\d,-]", "");
				String[] annuityList = savedAnnuities.split(",");
				for(int j=1; j<annuityList.length; j+=2)
					annuities.add(MutablePair.of(Integer.parseInt(annuityList[j-1]), Integer.parseInt(annuityList[j])));
				//If we're short on lives and we've passed the refill time, restock them
				//Or if we still have lives but it's been 20 hours since we lost any, give an extra
				while(lifeRefillTime.isBefore(Instant.now()))
				{
					if(lives < MAX_LIVES)
						lives = MAX_LIVES;
					else
						lives++;
					lifeRefillTime = lifeRefillTime.plusSeconds(72000);
				}
				break;
			}
		}
		oldMoney = money;
		currentCashClub = money/100_000_000;
		oldWinstreak = winstreak;
	}
	int giveAnnuities()
	{
		//If they're out of lives, annuities are paused
		if(lives <= 0)
			return 0;
		int totalPayout = 0;
		//Run through the iterator and tally up the payments
		ListIterator<MutablePair<Integer, Integer>> iterator = annuities.listIterator();
		while(iterator.hasNext())
		{
			MutablePair<Integer,Integer> nextAnnuity = iterator.next();
			totalPayout += nextAnnuity.getLeft();
			//If it's a temporary annuity, reduce the duration by one and remove it if it's gone, or update it if it's not
			if(nextAnnuity.getRight() > 0)
			{
				nextAnnuity.setRight(nextAnnuity.getRight()-1);
				if(nextAnnuity.getRight() == 0)
					iterator.remove();
				else
					iterator.set(nextAnnuity);
			}
		}
		//Then return the total amount to be paid
		return totalPayout;
	}
	public StringBuilder addMoney(int amount, MoneyMultipliersToUse multipliers)
	{
		//Start with the base amount
		long adjustedPrize = amount;
		//Boost and bonus don't stack - if both are permitted, only use whichever is greater
		if((multipliers.useBoost && !multipliers.useBonus) || (multipliers.useBoost && booster >= winstreak*10))
		{
			//Multiply by the booster (then divide by 100 since it's a percentage)
			adjustedPrize *= booster;
			adjustedPrize /= 100;
		}
		//And if it's a "bonus" (win bonus, minigames, the like), multiply by winstreak ("bonus multiplier") too
		//But make sure they still get something even if they're on x0
		if((multipliers.useBonus && !multipliers.useBoost) || (multipliers.useBonus && winstreak*10 > booster))
		{
			adjustedPrize *= Math.max(10,winstreak);
			adjustedPrize /= 10;
		}
		//Dodge overflow by capping at +-$1,000,000,000 while adding the money
		if(adjustedPrize + money > 1_000_000_000) money = 1_000_000_000;
		else if(adjustedPrize + money < -1_000_000_000) money = -1_000_000_000;
		else money += adjustedPrize;
		//Build the string if we need it
		if(adjustedPrize != amount)
		{
			StringBuilder resultString = new StringBuilder();
			resultString.append("...which gets ");
			resultString.append(Math.abs(adjustedPrize) < Math.abs(amount) ? "drained" : "boosted");
			resultString.append(" to **");
			if(adjustedPrize<0)
				resultString.append("-");
			resultString.append("$");
			resultString.append(String.format("%,d**",Math.abs(adjustedPrize)));
			resultString.append(adjustedPrize<amount ? "." : "!");
			return resultString;
		}
		return null;
	}
	void addBooster(int amount)
	{
		booster += amount;
		//Convert excess boost to cash
		int excessBoost = 0;
		if(booster > MAX_BOOSTER)
		{
			excessBoost = booster - MAX_BOOSTER;
			addMoney(10000*excessBoost, MoneyMultipliersToUse.NOTHING);
			game.channel.sendMessage(String.format("Excess boost converted to **$%,d**!",10000*excessBoost)).queue();
			booster = MAX_BOOSTER;
		}
		if(booster < MIN_BOOSTER)
		{
			excessBoost = booster - MIN_BOOSTER;
			addMoney(10000*excessBoost, MoneyMultipliersToUse.NOTHING);
			booster = MIN_BOOSTER;
			game.channel.sendMessage(String.format("Excess boost converted to **-$%,d**.",-10000*excessBoost)).queue();
		}
	}
	public int bankrupt()
	{
		int lostMoney = money - oldMoney;
		money = oldMoney;
		return lostMoney;
	}
	public StringBuilder blowUp(int penalty, boolean holdLoot)
	{
		//Start with modifiers the main controller needs
		game.repeatTurn = 0;
		game.playersAlive --;
		//Just fold if they've got a minigame lock so they still play their games
		if(holdLoot && games.size() > 0)
		{
			status = PlayerStatus.FOLDED;
		}
		else
		{
			games.clear();
			status = PlayerStatus.OUT;
		}
		//Bomb penalty needs to happen before resetting their booster
		if(threshold) penalty *= 4;
		//Set their refill time if this is their first life lost, then dock it if they aren't in newbie protection
		if(newbieProtection <= 0)
		{
			if(lives == MAX_LIVES)
				lifeRefillTime = Instant.now().plusSeconds(72000);
			if(lives == 1)
			{
				game.channel.sendMessage(getSafeMention() + ", you are out of lives. "
						+ "Further games today will incur an entry fee.").queue();
			}
			lives --;
		}
		StringBuilder output = addMoney(penalty,MoneyMultipliersToUse.BOOSTER_ONLY);
		//If they've got a split and share, they're in for a bad time
		if(splitAndShare)
		{
				game.channel.sendMessage("Because " + getSafeMention() + " had a split and share, "
						+ "2% of their total will be given to each living player.")
						.queueAfter(1,TimeUnit.SECONDS);
				int moneyLost = (int)(money/50);
				addMoney(-1*moneyLost*game.playersAlive,MoneyMultipliersToUse.NOTHING);
				//game.splitMoney(moneyLost*game.playersAlive, MoneyMultipliersToUse.NOTHING, true);
		}
		//Wipe their booster if they didn't hit a boost holder
		if(!holdLoot)
		{
			booster = 100;
			boostCharge = 0;
		}
		//Wipe everything else too, and dock them a life
		winstreak = 10;
		hiddenCommand = HiddenCommand.NONE;
		//Dumb easter egg
		if(money <= -1000000000)
		{
			game.channel.sendMessage("I'm impressed, "
					+ "but no you don't get anything special for getting your score this low.").queue();
			game.channel.sendMessage("See you next season!").queueAfter(1,TimeUnit.SECONDS);
		}
		return output;
	}
	@Override
	public int compareTo(Player other)
	{
		//THIS ISN'T CONSISTENT WITH EQUALS
		//Sort by round delta, descending order
		return (other.money - other.oldMoney) - (money - oldMoney);
	}
	/*
	 * If the player is human, gets their name as a mention
	 * If they aren't, just gets their name because user = null and null pointers are bad news bears yo!
	 */
	public String getSafeMention()
	{
		return isBot ? name : user.getAsMention();
	}
	
	public String printBombs()
	{
		StringBuilder result = new StringBuilder();
		result.append(name);
		result.append(":");
		for(int bomb : knownBombs)
		{
			result.append(" ");
			result.append(String.format("%02d",bomb+1));
		}
		return result.toString();
	}
	
	public String getName()
	{
		return name;
	}
}