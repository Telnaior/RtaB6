package tel.discord.rtab.games;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.LinkedList;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.RtaBMath;

public class Globetrotter extends MiniGameWrapper
{
	static final int BASE_FLIGHT_COST = 10;
	static final int FLIGHT_COST_INCREMENT = 2;
	static final int MAX_FLIGHTS = 4;
	static final int BOARD_SIZE = 9;
	ArrayList<City> currentDestinations;
	ArrayList<Double> currentMultipliers;
	LinkedList<City> visitedDestinations;
	City currentLocation;
	int cashOnHand, cashBanked;
	int totalFlights;
	int bombLocation;
	int doubleLocation;
	int spacesLeft;
	boolean[] pickedSpaces;
	int prizeValue;
	boolean isAlive;
	
	private enum City
	{
		//Payout formula = 500k + 10*(distancefromhome^1.2)
		HOME("RtaB Home Base", 0, 0, 0),
		STARDEW("Stardew Valley", -6, 4, 530_504),
		WOW("Stormwind", 32, 23, 729_236),
		KH("Twilight Town", -26, -55, 880_160),
		SONIC("Spagonia", 32, -57, 907_598),
		MASSEFFECT("The Citadel", -39, 58, 932_975),
		MARIO("Mushroom Kingdom", -61, 35, 939_977),
		ZELDA("Kakariko Village", 56, 72, 1_048_689),
		CELESTE("Celeste Mountain", 45, -79, 1_066_787),
		GTA("Los Santos", -67, -72, 1_073_562),
		POKEMON("Celadon City", 36, 84, 1_090_894),
		RUNESCAPE("Varrock", -68, 78, 1_094_041),
		HOLLOW("Hallownest", -46, 89, 1_125_661),
		STARFOX("Corneria", -75, 119, 1_192_696),
		POE("Oriath", -70, 122, 1_220_395),
		BIOSHOCK("Rapture", 61, -121, 1_225_132),
		SKYRIM("Whiterun", 66, -127, 1_252_636),
		SH("Silent Hill", -52, 121, 1_290_217),
		HL2("City 17", 65, -168, 1_342_299),
		SPLATOON("Inkopolis", 5, -114, 1_337_773),
		WITCHER("Beauclair", -30, -142, 1_509_328),
		LOL("Summoner's Rift", 36, -163, 1_579_404),
		FFVII("Midgar", -35, -171, 1_610_030),
		DIABLO("Tristram", -19, -152, 1_634_115),
		FFX("Bevelle", -31, -170, 1_643_221);
		
		String name;
		double latitude;
		double longitude;
		int basePayout;
		City(String name, int latitude, int longitude, int basePayout)
		{
			this.name = name;
			this.basePayout = basePayout;
			//We state lat and long in degrees but then internally store them in radians
			this.latitude = latitude * (Math.PI/180);
			this.longitude = longitude * (Math.PI/180);
		}
		
		int distanceTo(City other)
		{
			//THIS METHOD CONTAINS MATH
			return (int)(acos(sin(this.latitude)*sin(other.latitude)
					+cos(this.latitude)*cos(other.latitude)*cos(other.longitude-this.longitude))*6371);
		}
		
		String headingTo(City other)
		{
			double latDifference = other.latitude - this.latitude;
			//Longitude is a little more complex to keep within a -179 - 180 range 
			double longDifference = (other.longitude - this.longitude);
			if(longDifference > PI) longDifference -= 2*PI;
			if(longDifference < -1*PI) longDifference += 2*PI;
			//Handle identical longitude first to prevent division by zero
			if(longDifference == 0)
				return latDifference >= 0 ? "N" : "S";
			//Force both differences to be positive for easier calculation
			String lon = longDifference >= 0 ? "E" : "W";
			longDifference = abs(longDifference);
			String lat = latDifference >= 0 ? "N" : "S";
			latDifference = abs(latDifference);
			//Get the ratio between vertical and horizontal movement
			//this isn't actually accurate to great circle math btw (we're treating the world like a cylinder rather than a sphere)
			//but doing it this way gives a result that's likely to be more useful for the player
			double heading = atan(latDifference/longDifference);
			//Now we if/else the right combo using pi/16 boundaries
			if(heading <= 2*PI/16)
				return lon;
			if(heading < 3*PI/16)
				return lon+lat+lon;
			if(heading <= 5*PI/16)
				return lat+lon;
			if(heading < 6*PI/16)
				return lat+lat+lon;
			return lat;
		}
	}
	
	void printCityTable()
	{
		//this is just a debug method that nothing actually uses
		sendMessage("Printing city table...");
		for(int i=0; i<City.values().length; i++)
			for(int j=i+1; j<City.values().length; j++)
			{
				City city1 = City.values()[i];
				City city2 = City.values()[j];
				System.out.println(city1.name+" to "+city2.name+": "+city1.distanceTo(city2)+"km "+city1.headingTo(city2));
			}
	}
	
	void superReveal()
	{
		sendMessage("Critical Error: Supercash not found.");
		try { Thread.sleep(10000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		sendMessage("Reporting this problem to Microsoft...");
		try { Thread.sleep(10000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		sendMessage("Searching for solutions...");
		try { Thread.sleep(10000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		sendMessage("Solution found.");
		try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		sendMessage("Applying solution: Rotate Bonus Game...");
		try { Thread.sleep(10000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		sendMessage("Problem fixed successfully.");
		try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		//Announce FOR REAL!
		sendMessage(getPlayer().getSafeMention()+", you've unlocked a bonus game: **Globetrotter**!");
		try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	@Override
	void startGame()
	{
		//Initialise the game
		isAlive = true;
		totalFlights = 0;
		cashBanked = 0;
		cashOnHand = 0;
		currentLocation = City.HOME;
		prizeValue = 0;
		currentDestinations = new ArrayList<>(4);
		currentMultipliers = new ArrayList<>(4);
		visitedDestinations = new LinkedList<>();
		for(int i=0; i<MAX_FLIGHTS; i++)
			rollNewDestination();
		spacesLeft = BOARD_SIZE;
		pickedSpaces = new boolean[BOARD_SIZE];
		//We do a little cleverness here to ensure the bomb and double don't land on the same space while keeping the weightings even
		bombLocation = (int)(RtaBMath.random()*BOARD_SIZE);
		doubleLocation = (int)(RtaBMath.random()*(BOARD_SIZE-1));
		if(doubleLocation >= bombLocation)
			doubleLocation ++;
		//Send instructions
		LinkedList<String> output = new LinkedList<>();
		output.add("Welcome to the Race to a Billion private airport!");
		output.add("Here, you can fly around the world to collect prizes from exotic locations.");
		output.add("However, one of the possible prizes is a bomb that will end your game.");
		output.add("You can fly home at any point to secure your winnings before continuing...");
		output.add("...but every flight you take has a cost, and that cost will increase after each journey.");
		output.add("The game ends when you pick a bomb or clean out the prize board. Good luck!");
		sendSkippableMessages(output);
		sendMessage(generateDepartureBoard());
		getInput();
	}

	@Override
	void playNextTurn(String input)
	{
		LinkedList<String> output = new LinkedList<>();
		//If there's a prize currently up for grabs, they need to pick a space on the prize board
		if(prizeValue > 0)
		{
			//Pick a prize?
			if(isValidPrizePick(input))
			{
				int pick = Integer.parseInt(input)-1;
				spacesLeft --;
				pickedSpaces[pick] = true;
				output.add("Space "+(pick+1)+" picked...");
				if(pick == bombLocation)
				{
					output.add("It's a **BOMB**.");
					cashOnHand = 0;
					isAlive = false;
				}
				else if(pick == doubleLocation)
				{
					output.add("It's a **DOUBLE PRIZE**!");
					cashOnHand += prizeValue*2;
					output.add(String.format("That earns you **$%,d**, bringing your cash on hand to $%,d!", prizeValue*2, cashOnHand));
				}
				else
				{
					output.add("It's a **PRIZE**!");
					cashOnHand += prizeValue;
					output.add(String.format("That earns you **$%,d**, bringing your cash on hand to $%,d.", prizeValue, cashOnHand));
				}
				prizeValue = 0;
				if(isAlive)
				{
					if(spacesLeft == 1)
					{
						output.add("That's the last prize on the board, so let's fly you home!");
						cashOnHand -= getFlightCost(currentLocation, City.HOME);
						output.add(String.format("After the flight cost of $%,d, you've just banked **$%,d**!",
								getFlightCost(currentLocation, City.HOME), cashOnHand));
						cashBanked += cashOnHand;
						cashOnHand = 0;
						isAlive = false;
					}
					else
					{
						output.add("Where are you flying next?");
						output.add(generateDepartureBoard());
					}
				}
			}
		}
		//With no prize up for grabs, they need to fly somewhere on the departure board
		else
		{
			int flight = parseValidFlight(input);
			if(flight != -1)
			{
				int flightCost = getFlightCost(currentLocation, currentDestinations.get(flight));
				cashOnHand -= flightCost;
				totalFlights ++;
				currentLocation = currentDestinations.get(flight);
				visitedDestinations.add(currentLocation);
				prizeValue = getCityPayout(flight);
				currentDestinations.remove(flight);
				currentMultipliers.remove(flight);
				rollNewDestination();
				if(currentLocation == City.HOME)
				{
					output.add(String.format("Welcome home - after the flight cost of $%,d, you've just banked **$%,d**!",
							flightCost, cashOnHand));
					cashBanked += cashOnHand;
					cashOnHand = 0;
					output.add(generateDepartureBoard());
				}
				else
				{
					output.add("Welcome to "+currentLocation.name+"!");
					output.add(String.format("That flight cost you $%,d, but a prize here will earn you $%,d...", flightCost, prizeValue));
					output.add(generatePrizeBoard());
				}
			}
		}
		
		sendMessages(output);
		if(!isAlive)
		{
			if(cashBanked >= applyBaseMultiplier(10_000_000))
				Achievement.GLOBETROTTER_JACKPOT.check(getPlayer());
			awardMoneyWon(cashBanked);
		}
		else
			getInput();
	}
	
	int parseValidFlight(String input)
	{
		if(!isNumber(input))
		{
			for(int i=0; i<MAX_FLIGHTS; i++)
				if(input.equalsIgnoreCase(currentDestinations.get(i).name))
					return i;
			return -1;
		}
		else
		{
			int pick = Integer.parseInt(input)-1;
			return (pick >= 0 && pick < MAX_FLIGHTS) ? pick : -1;
		}
	}
	
	boolean isValidPrizePick(String input)
	{
		if(!isNumber(input))
			return false;
		int pick = Integer.parseInt(input)-1;
		return pick >= 0 && pick < BOARD_SIZE && !pickedSpaces[pick];
	}
	
	int getCityPayout(int position)
	{
		return applyBaseMultiplier((int)(currentDestinations.get(position).basePayout * currentMultipliers.get(position)));
	}
	
	void rollNewDestination()
	{
		//0.85-1.15x multiplier to payout for the new location
		currentMultipliers.add(RtaBMath.random()*0.3+0.85);
		//If we aren't at home, always give us an option to go home
		if(currentLocation != City.HOME && !currentDestinations.contains(City.HOME))
		{
			currentDestinations.add(City.HOME);
			return;
		}
		//Determine destination list
		ArrayList<City> eligibleDestinations = new ArrayList<>();
		for(City next : City.values())
		{
			if(next != City.HOME && !currentDestinations.contains(next) && !visitedDestinations.contains(next))
				eligibleDestinations.add(next);
		}
		//Pick one at random
		currentDestinations.add(eligibleDestinations.get((int)(RtaBMath.random()*eligibleDestinations.size())));
	}
	
	int getFlightCost(City city1, City city2)
	{
		int costPerKm = BASE_FLIGHT_COST + (totalFlights * FLIGHT_COST_INCREMENT);
		return applyBaseMultiplier(city1.distanceTo(city2) * costPerKm);
	}
	
	String generateDepartureBoard()
	{
		StringBuilder result = new StringBuilder();
		result.append("**GLOBETROTTER DEPARTURE BOARD**\n");
		result.append(String.format("$%,d on hand, $%,d banked%n%n", cashOnHand, cashBanked));
		result.append(String.format("Current Flight Cost: $%,d/km",
				applyBaseMultiplier(BASE_FLIGHT_COST + (totalFlights * FLIGHT_COST_INCREMENT))));
		//String.format sure gets nasty real quick huh
		for(int i=0; i<MAX_FLIGHTS; i++)
		{
			if(currentDestinations.get(i) == City.HOME)
				result.append(String.format("%n%1$d - $%5$,d flight to **%2$s** (*%3$,dkm %4$s*) - BANK",
						i+1, currentDestinations.get(i).name, currentLocation.distanceTo(currentDestinations.get(i)),
						currentLocation.headingTo(currentDestinations.get(i)),
						getFlightCost(currentLocation,currentDestinations.get(i))));
			else
				result.append(String.format("%n%1$d - $%5$,d flight to **%2$s** (*%3$,dkm %4$s*) - $%6$,d prize value",
					i+1, currentDestinations.get(i).name, currentLocation.distanceTo(currentDestinations.get(i)),
							currentLocation.headingTo(currentDestinations.get(i)),
							getFlightCost(currentLocation,currentDestinations.get(i)), getCityPayout(i)));
		}
		return result.toString();
	}
	
	String generatePrizeBoard()
	{
		StringBuilder result = new StringBuilder();
		result.append("```\n");
		result.append("PRIZE\nBOARD\n\n");
		for(int i=0; i<BOARD_SIZE; i++)
		{
			result.append(pickedSpaces[i] ? " " : i+1);
			result.append(i%3==2?"\n":" ");
		}
		result.append("```");
		return result.toString();
	}

	@Override
	String getBotPick()
	{
		if(prizeValue > 0)
		{
			ArrayList<Integer> openSpaces = new ArrayList<>(BOARD_SIZE);
			for(int i=0; i<BOARD_SIZE; i++)
				if(!pickedSpaces[i])
					openSpaces.add(i+1);
			return String.valueOf(openSpaces.get((int)(RtaBMath.random()*openSpaces.size())));
		}
		else
		{
			int maxProfit = 0;
			int bestFlight = -1;
			for(int i=0; i<MAX_FLIGHTS; i++)
			{
				int profit;
				if(currentDestinations.get(i) == City.HOME)
					profit = (int)(RtaBMath.random()*cashOnHand);
				else
					profit = getCityPayout(i) - getFlightCost(currentLocation, currentDestinations.get(i));
				if(profit > maxProfit)
				{
					maxProfit = profit;
					bestFlight = i;
				}
			}
			return String.valueOf(bestFlight+1);
		}
	}

	@Override
	void abortGame()
	{
		//Treat it like a bomb
		awardMoneyWon(cashBanked);
	}

	@Override public String getName() { return "Globetrotter"; }
	@Override public String getShortName() { return "Globe"; }
	@Override public boolean isBonus() { return true; }
}
