package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.dv8tion.jda.internal.utils.tuple.Pair;
import tel.discord.rtab.Achievement;
import tel.discord.rtab.Player;
import tel.discord.rtab.board.Game;

public class RaceDeal extends MiniGameWrapper
{
	static final String NAME = "Race Deal";
	static final String SHORT_NAME = "RD";
	static final boolean BONUS = true;
	List<Integer> randomNegativeValues = Arrays.asList(-7_500_000, -5_000_000, -2_500_000, -1_000_000, -800_000, -600_000, -400_000, -200_000, -100_000);
	List<Integer> randomBlueValues = Arrays.asList(50_000, 100_000, 150_000, 200_000, 250_000, 300_000, 400_000, 500_000, 750_000);
	List<Integer> randomRedValues = Arrays.asList(2_000_000, 2_500_000, 3_000_000, 3_500_000, 4_000_000, 4_500_000, 5_000_000,
							6_000_000, 7_500_000, 8_000_000, 9_000_000, 10_000_000, 12_500_000, 15_000_000, 17_500_000, 20_000_000,
							25_000_000, 30_000_000, 40_000_000, 50_000_000, 75_000_000); //Less structure here good luck
	ArrayList<Pair<Integer,SpecialType>> valueList; //Left half is the value, right half designates cash vs something weird
	ArrayList<Integer> caseList;
	boolean[] openedCases;
	int casesLeft;
	int mysteryChanceBase;
	boolean mysteryChance;
	ArrayList<Integer> mysteryChanceGrid;
	int chosenCase;
	int offer;
	boolean acceptedOffer;
	Pair<Integer,SpecialType> prizeWon;
	int casesToOpen;
	int round;

	private enum SpecialType
	{
		CASH, BILLION, MYSTERY_CHANCE, MAX_BOOST, BONUS_GAMES
	}

	@Override
	void startGame()
	{
		casesLeft = 26;
		round = 0;
		//Start by adding the fixed values
		valueList = new ArrayList<Pair<Integer,SpecialType>>(casesLeft);
		valueList.add(Pair.of(applyBaseMultiplier(-10_000_000), SpecialType.CASH));
		valueList.add(Pair.of(1, SpecialType.CASH)); //Just like the regular DoND minigame, this isn't multiplied
		valueList.add(Pair.of(applyBaseMultiplier(1_000_000), SpecialType.CASH)); //Lowest Red Value
		valueList.add(Pair.of(applyBaseMultiplier(9_990_000), SpecialType.MAX_BOOST)); //+999% Boost
		valueList.add(Pair.of(applyBaseMultiplier(100_000_000), SpecialType.CASH));
		valueList.add(Pair.of(applyBaseMultiplier(123_456_789), SpecialType.BONUS_GAMES)); //All Bonus Games - this may be undervalued but it'll push them to win it
		valueList.add(Pair.of(1_000_000_000 - getCurrentPlayer().money, SpecialType.BILLION)); //$1,000,000,000 - the value of which is reduced by the player's bank
		int negativeToAdd = 6;
		int blueToAdd = 5;
		int redToAdd = 8;
		//Calculate mystery chance value as the average of all players' cash amounts
		mysteryChanceBase = 0;
		for(Player next : players)
			mysteryChanceBase += next.money / players.size(); 
		mysteryChanceBase = (mysteryChanceBase + getCurrentPlayer().money) / 2; //Finally, average it with the current player's money
		mysteryChanceBase = Math.max(mysteryChanceBase, 1_234_567); // Minimum value to make sure Mystery Chance is interesting / playable
		mysteryChance = false;
		int mysteryChanceValue = mysteryChanceBase - getCurrentPlayer().money; //And this becomes the amount they stand to gain/lose on average
		//Then add it, replacing a random value as relevant
		valueList.add(Pair.of(mysteryChanceValue, SpecialType.MYSTERY_CHANCE));
		if(mysteryChanceValue > 1_000_000)
			redToAdd --;
		else
			negativeToAdd --;
		//Now shuffle the random values and use them to fill the gaps
		Collections.shuffle(randomNegativeValues);
		Collections.shuffle(randomBlueValues);
		Collections.shuffle(randomRedValues);
		for(int i=0; i<negativeToAdd; i++)
			valueList.add(Pair.of(applyBaseMultiplier(randomNegativeValues.get(i)), SpecialType.CASH));
		for(int i=0; i<blueToAdd; i++)
			valueList.add(Pair.of(applyBaseMultiplier(randomBlueValues.get(i)), SpecialType.CASH));
		for(int i=0; i<redToAdd; i++)
			valueList.add(Pair.of(applyBaseMultiplier(randomRedValues.get(i)), SpecialType.CASH));
		//Finally, sort the board
		valueList.sort(new AscendingValueSorter());
		//Now shuffle the values into the cases
		caseList = new ArrayList<>(casesLeft);
		for(int i=0; i<casesLeft; i++)
			caseList.add(i);
		Collections.shuffle(caseList);
		openedCases = new boolean[casesLeft];
		chosenCase = -1; //No case selected yet
		casesToOpen = -1;
		acceptedOffer = false;
		//Alright, we got ourselves organised, give them the achievement for making it here and tell them what's happening
		Achievement.TWENTY.check(getCurrentPlayer());
		LinkedList<String> output = new LinkedList<>();
		output.add("For reaching a streak bonus of x20, you have earned the right to play the final bonus game!");
		output.add("Race Deal is a lot like regular Deal or No Deal, except the stakes are a lot higher.");
		output.add("In fact, one of the twenty-six cases in front of you contains one billion dollars!");
		output.add("There are also some negative values, however, so be extra careful not to get stuck with one of them.");
		output.add("In addition, there are three cases that will award other prizes in lieu of cash.");
		output.add("Winning +999% Boost will max out your boost, and award you a small monetary prize based on the excess.");
		output.add("Winning 4 Bonus Games will give you the opportunity to play Supercash, Digital Fortress, Spectrum, and Hypercube in turn.");
		output.add("Finally, if won, Mystery Chance will remove and replace your *entire cash bank* with a new value decided at random. "
				+ "This could earn you hundreds of millions, or *cost* you the same. Win it at your own risk.");
		output.add("You will choose six cases to open before the first offer, and each future round will require one case less "
				+ "until the final six cases are opened one at a time.");
		output.add("Before that, however, you must first decide which case you want to be yours.");
		output.add("There will not be any opportunity to change your case later, so choose wisely and good luck!");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}

	static class AscendingValueSorter implements Comparator<Pair<Integer, SpecialType>>
	{
		@Override
		public int compare(Pair<Integer, SpecialType> arg0, Pair<Integer, SpecialType> arg1)
		{
			//Check for the $1b space to make sure it always sits in the lower-right position
			if(arg0.getRight() == SpecialType.BILLION)
				return 1_000_000_000 - arg1.getLeft();
			else if(arg1.getRight() == SpecialType.BILLION)
				return arg0.getLeft() - 1_000_000_000;
			else
				return arg0.getLeft() - arg1.getLeft();
		}
	}

	@Override
	void playNextTurn(String input)
	{
		//We run all the validation in this method, then pass it on to the corresponding method (depending on gamestate) to resolve
		if(mysteryChance) //They're in mystery chance, PLEASE tell me this will never happen
		{
			if(isNumber(input) && checkValidMCNumber(input))
			{
				resolveMysteryChance(Integer.parseInt(input)-1);
			}
			else
				getInput();
		}
		else if(casesToOpen == 0) //Offer has been made, say deal or no deal
		{
			String choice = input.toUpperCase();
			choice = choice.replaceAll("\\s","");
			if(choice.equals("REFUSE") || choice.equals("NODEAL") || choice.equals("ND"))
				resolveOffer(false);
			else if(choice.equals("ACCEPT") || choice.equals("DEAL") || choice.equals("D"))
				resolveOffer(true);
			else
				getInput();
		}
		else if(casesToOpen == -1) //Choosing which case is theirs
		{
			if(isNumber(input) && checkValidNumber(input))
				chooseCase(Integer.parseInt(input)-1);
			else
				getInput();
		}
		else //Choosing a case to open
		{
			if(!isNumber(input))
			{
				getInput();
			}
			else if(!checkValidNumber(input))
			{
				sendMessage("Invalid pick.");
				getInput();
			}
			else
			{
				openCase(Integer.parseInt(input)-1);
			}
		}
	}
	
	boolean checkValidNumber(String message)
	{
		int pick = Integer.parseInt(message)-1;
		return (pick >= 0 && pick < openedCases.length && !openedCases[pick] && pick != chosenCase);
	}
	
	boolean checkValidMCNumber(String message)
	{
		int pick = Integer.parseInt(message)-1;
		return (pick >= 0 && pick < 25);
	}
	
	private void chooseCase(int chosenCase)
	{
		this.chosenCase = chosenCase;
		LinkedList<String> output = new LinkedList<>();
		output.add("You have chosen **Case "+(chosenCase+1)+"**!");
		output.add("Does it contain one billion dollars? There's only one way to find out.");
		output.add("Let's play Race Deal!");
		sendMessages(output);
		advanceRound();
	}
	
	private void openCase(int chosenCase)
	{
		LinkedList<String> output = new LinkedList<>();
		casesLeft --;
		casesToOpen --;
		openedCases[chosenCase] = true;
		Pair<Integer,SpecialType> value = valueList.get(caseList.get(chosenCase));
		int valueHit = value.getLeft();
		int valueMagnitude = valueHit > 0 ? (int)Math.log10(valueHit) : 0;
		output.add("Case "+(chosenCase+1)+" contains...");
		if((Math.random() * 10) + (acceptedOffer ? 0 : 6) + valueMagnitude > casesLeft)
			output.add("...");
		output.add("**"+getDisplayName(value)+"**"+(valueMagnitude >= 7 && !acceptedOffer ? "." : "!"));
		if(casesToOpen == 0)
		{
			if(acceptedOffer) //Proveout offer
			{
				output.add("With "+casesLeft+" cases remaining, the Hostess would now have offered you...");
				generateOffer();
				output.add(String.format("**$%,d**", offer) + (offer < prizeWon.getLeft() ? "!" : "."));
				sendMessages(output);
				advanceRound();
			}
			else //Live Play
			{
				output.add("With "+casesLeft+" cases remaining, the Hostess would like to make you an offer.");
				output.add("If you surrender your case now, you will receive...");
				if(casesLeft <= 6)
					output.add("...");
				generateOffer();
				output.add(String.format("**THE HOSTESS OFFERS: $%,d**", offer));
				output.add(generateBoard());
				output.add(String.format("For $%,d, is it **Deal** or **No Deal**?", offer));
				sendMessages(output);
				getInput();
			}
		}
		else
		{
			output.add(generateBoard());
			sendMessages(output);
			getInput();
		}
	}
	
	private void resolveOffer(boolean accept)
	{
		if(accept)
		{
			acceptedOffer = true;
			prizeWon = Pair.of(offer, SpecialType.CASH);
			LinkedList<String> output = new LinkedList<>();
			output.add((offer >= 0 ? "Congratulations! " : "") + "It's a **DONE DEAL**!");
			output.add("Of course, one question remains... what would have followed had you declined this offer?");
			sendMessages(output);
		}
		else
		{
			sendMessage("**NO DEAL**!");
		}
		advanceRound();
	}
	
	private void advanceRound()
	{
		round++;
		LinkedList<String> output = new LinkedList<>();
		if(casesLeft == 2)
		{
			output.add("As there are only two cases left in play, we will now open your case.");
			output.add("Inside your case is...");
			Pair<Integer,SpecialType> playerCase = valueList.get(caseList.get(chosenCase));
			if(!acceptedOffer)
			{
				prizeWon = playerCase;
				output.add("..."); //double suspense lol
			}
			output.add("...");
			output.add("**"+getDisplayName(playerCase)+"**!");
			sendMessages(output);
			endGame();
		}
		else
		{
			casesToOpen = Math.max(7-round, 1);
			output.add("It is now Round "+round+".");
			output.add("You must open "+casesToOpen+" case"+(casesToOpen != 1 ? "s" : "")+" before the next offer.");
			output.add(generateBoard());
			sendMessages(output);
			getInput();
		}
	}

	private int generateOffer()
	{
		//This is going to be very similar to the DoND minigame offer formula, with adjustment to make the fair deal work with negative values
		//Generate "fair deal" and average
		int average = 0;
		int fairDeal = 0;
		for(int i=0; i<caseList.size(); i++)
			if(!openedCases[i])
			{
				int value = valueList.get(caseList.get(i)).getLeft();
				average += value;
				fairDeal += Math.sqrt(Math.abs(value)) * (value < 0 ? -1 : 1);
			}
		average /= casesLeft;
		fairDeal /= casesLeft;
		fairDeal = (int)Math.pow(fairDeal,2) * (fairDeal < 0 ? -1 : 1);
		//Use the fair deal as the base of the offer, then add a portion of the average to it depending on round
		offer = fairDeal + ((average-fairDeal) * (20-casesLeft) / 40);
		//Add random factor: 0.90-1.10
		long temp = offer * (long)((Math.random()*21) + 90);
		offer = (int)(temp / 100);
		//We never want to offer them a season-winning amount - if they want that, they have to win it from the box
		if(getCurrentPlayer().money + offer >= 1_000_000_000)
			offer = 999_999_999 - getCurrentPlayer().money;
		//Finally, round it off
		if(Math.abs(offer) > 25_000_000)
			offer -= (offer%1_000_000);
		else if(Math.abs(offer) > 2_500_000)
			offer -= (offer%100_000);
		else if(Math.abs(offer) > 250_000)
			offer -= (offer%10_000);
		else if(Math.abs(offer) > 25_000)
			offer -= (offer%1_000);
		else if(Math.abs(offer) > 2_500)
			offer -= (offer%100);
		else if(Math.abs(offer) > 250)
			offer -= (offer%10);
		return offer;
	}
	
	private String getDisplayName(Pair<Integer,SpecialType> prize)
	{
		switch(prize.getRight())
		{
		case BILLION:
			return "$1,000,000,000";
		case MYSTERY_CHANCE:
			return "Mystery Chance";
		case MAX_BOOST:
			return "+999% Boost";
		case BONUS_GAMES:
			return "4 Bonus Games";
		case CASH:
		default:
			return String.format((prize.getLeft() < 0 ? "-" : "") + "$%,d", Math.abs(prize.getLeft()));
		}
	}
	
	private String generateBoard()
	{
		StringBuilder output = new StringBuilder();
		output.append("```\n");
		//Header
		if(acceptedOffer) //Tell them what they've won
			output.append(String.format("        $%,11d DEAL        \n", prizeWon.getLeft()));
		else if(casesToOpen == 0) //Tell them the offer
			output.append(String.format("       OFFER: $%,11d       \n", offer));
		else if(casesToOpen == -1) //Choosing a case
			output.append("         CHOOSE YOUR CASE         \n");
		else
			output.append("         "+casesToOpen + " CASE"+(casesToOpen!=1?"S":" ")+" TO OPEN         \n");
		//Display the remaining cases
		int[] rowStarts = new int[] {26,19,13,6,0};
		for(int i=1; i<rowStarts.length; i++)
		{
			output.append("    ");
			if(i%2==0)
				output.append("  ");
			for(int j=rowStarts[i]; j<rowStarts[i-1]; j++)
			{
				if(!openedCases[j] && chosenCase != j)
					output.append(String.format("%02d",j+1));
				else
					output.append("  ");
				output.append("  ");
			}
			output.append("\n");
		}
		if(chosenCase != -1)
			output.append(String.format("          YOUR CASE: %02d          \n",chosenCase+1));
		output.append("\n");
		//Now the values on the board
		//Start by figuring out which ones are there
		boolean[] valueOnBoard = new boolean[valueList.size()];
		for(int i=0; i<valueList.size(); i++)
			valueOnBoard[caseList.get(i)] = !openedCases[i];
		//Now displaying the board itself, which works basically the same way as the DoND minigame
		int nextValue = 0;
		for(int i=0; i<valueList.size(); i++)
		{
			if(valueOnBoard[nextValue])
			{
				switch(valueList.get(nextValue).getRight())
				{
				case BILLION:
					output.append(" $1,000,000,000");
					break;
				case MYSTERY_CHANCE:
					output.append(" Mystery Chance");
					break;
				case MAX_BOOST:
					output.append("   +999% Boost ");
					break;
				case BONUS_GAMES:
					output.append("  4 Bonus Games");
					break;
				case CASH:
				default:
					int value = valueList.get(nextValue).getLeft();
					output.append(String.format((value<0?"-":" ")+"$%,13d",Math.abs(value)));
				}
			}
			else
			{
				output.append("               ");
			}
			//Order is 0, 13, 1, 14, ..., 11, 24, 12, 25
			nextValue += valueList.size()/2;
			if(nextValue >= valueList.size())
				nextValue -= valueList.size() - 1;
			//Space appropriately
			output.append(i%2==0 ? "   " : "\n");
		}
		//Finally, close off the display and return
		output.append("```");
		return output.toString();
	}

	@Override
	String getBotPick()
	{
		//A bot is in mystery chance????????????????????? ahahahahahahahahahhhh...
		if(mysteryChance)
			return String.valueOf((int)(Math.random()*25+1));
		//Choose cases at random and take the deal 20% of the time
		if(casesToOpen == 0)
		{
			if(Math.random() < 0.2)
				return "DEAL";
			else
				return "NO DEAL";
		}
		else
		{
			ArrayList<Integer> casesAvailable = new ArrayList<>(casesLeft);
			for(int i=0; i<openedCases.length; i++)
				if(!openedCases[i])
					casesAvailable.add(i);
			return String.valueOf(casesAvailable.get((int)(Math.random()*casesLeft))+1);
		}
	}

	@Override
	void abortGame()
	{
		//If there's an offer on the table or they just declined one, then take it
		//Otherwise award the minimum of $0 and the previous offer
		if(casesToOpen == 0 || casesToOpen == Math.max(1, 7-round))
			awardMoneyWon(offer);
		else
			awardMoneyWon(Math.min(0, offer));
	}
	
	private void endGame()
	{
		boolean sendMoreMessages = sendMessages;
		sendMessages = true;
		switch(prizeWon.getRight())
		{
		case BILLION:
			getCurrentPlayer().money = 1_000_000_000;
			if(getCurrentPlayer().isBot)
				sendMessage(getCurrentPlayer().getName().toUpperCase() + " WON **ONE BILLION DOLLARS** IN RACE DEAL!");
			else
				sendMessage("CONGRATULATIONS! YOU WIN **ONE BILLION DOLLARS**!");
			gameOver();
			break;
		case MYSTERY_CHANCE:
			if(getCurrentPlayer().isBot)
				sendMessage(getCurrentPlayer().getName() + " won **Mystery Chance** from Race Deal.");
			else
				sendMessage("Game Over. You won **Mystery Chance** from Race Deal.");
			sendMessages = sendMoreMessages;
			runMysteryChance();
			break;
		case MAX_BOOST:
			getCurrentPlayer().addBooster(999);
			if(getCurrentPlayer().isBot)
				sendMessage(getCurrentPlayer().getName() + " won **+999% Boost** from Race Deal.");
			else
				sendMessage("Game Over. You won **+999% Boost** from Race Deal.");
			gameOver();
			break;
		case BONUS_GAMES:
			if(getCurrentPlayer().isBot)
				sendMessage(getCurrentPlayer().getName() + " won **Four Bonus Games** from Race Deal.");
			else
				sendMessage("Game Over. You won **Four Bonus Games** from Race Deal.");
			runNextBonusGame(4,sendMoreMessages);
			gameOver();
			break;
		case CASH:
		default:
			awardMoneyWon(prizeWon.getLeft());
		}
	}
	
	private void runNextBonusGame(int gamesToGo, boolean sendMessages)
	{
		MiniGame bonusGame;
		switch(gamesToGo)
		{
		case 4:
			bonusGame = Game.SUPERCASH.getGame();
			break;
		case 3:
			bonusGame = Game.DIGITAL_FORTRESS.getGame();
			break;
		case 2:
			bonusGame = Game.SPECTRUM.getGame();
			break;
		case 1:
			bonusGame = Game.HYPERCUBE.getGame();
			break;
		case 0:
		default:
			gameOver();
			return;
		}
		//Set up the thread we'll send to the game
		Thread postGame = new Thread(() -> {
			//Recurse to get to the next minigame
			runNextBonusGame(gamesToGo-1, sendMessages);
		});
		postGame.setName(String.format("%s - %s - %s", channel.getName(), getCurrentPlayer().getName(), bonusGame.getName()));
		bonusGame.initialiseGame(channel, sendMessages, baseNumerator, baseDenominator, 1, players, player, postGame, false);
	}
	
	private void runMysteryChance()
	{
		mysteryChance = true;
		//Set up a grid of 25 spaces
		mysteryChanceGrid = new ArrayList<>(25);
		//First space is the base value itself
		mysteryChanceGrid.add(mysteryChanceBase);
		//Next the twelve negative spaces, based on percentages of the "working logarithm"
		double baseLogarithm = Math.log10(mysteryChanceBase);
		double workingLogarithm = baseLogarithm - 6.0;
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.99)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.97)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.94)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.90)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.86)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.81)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.75)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.69)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.63)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.57)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.50)+6.0));
		mysteryChanceGrid.add((int)Math.pow(10,(workingLogarithm*.44)+6.0));
		//Then the twelve positive spaces
		double remainderLogarithm = 9.0 - baseLogarithm;
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.90)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.81)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.73)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.66)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.59)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.53)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.48)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.43)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.39)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.35)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.31)));
		mysteryChanceGrid.add((int)Math.pow(10,9-(remainderLogarithm*.28)));
		//Finally, shuffle them up
		Collections.shuffle(mysteryChanceGrid);
		//Print debug values as proof of legitimacy
		for(int i=0; i<mysteryChanceGrid.size(); i++)
			System.out.print(String.format("%d: $%,d ",i+1,mysteryChanceGrid.get(i)));
		System.out.println();
		//Tell them what's up
		LinkedList<String> output = new LinkedList<>();
		output.add("Welcome to **Mystery Chance**.");
		output.add("That you are here now proves that you are willing to risk everything in the pursuit of glory.");
		output.add("So let's not keep you waiting...");
		output.add("```\nMYSTERY CHANCE\n01 02 03 04 05\n06 07 08 09 10\n11 12 13 14 15\n16 17 18 19 20\n21 22 23 24 25\n```");
		output.add("You're not here to mess around, so we won't. "
				+ "Simply choose one of these 25 spaces to learn what your new cash total is. Best of luck!");
		sendMessages(output);
		getInput();
	}
	
	private void resolveMysteryChance(int spacePicked)
	{
		LinkedList<String> output = new LinkedList<>();
		output.add("You chose Space "+(spacePicked+1)+".");
		output.add("Here we go... one digit at a time.");
		int newScore = mysteryChanceGrid.get(spacePicked);
		sendMessages(output);
		for(int i=1; i<=9; i++)
		{
			if(!getCurrentPlayer().isBot)
				try { Thread.sleep(1000*i); } catch(InterruptedException e) { e.printStackTrace(); } //Ever-increasing delay
			sendMessage(String.format("```\n$%,11d\n```", (int)(newScore%(Math.pow(10, i)))));
		}
		output.clear();
		sendMessages = true; //We need to see this for bots too
		sendMessage(String.format("Congratulations! Your new score is **$%,d**!",newScore));
		getCurrentPlayer().money = newScore;
		gameOver();
	}

	@Override
	public String getName()
	{
		return NAME;
	}
	@Override
	public String getShortName()
	{
		return SHORT_NAME;
	}
	@Override
	public boolean isBonus()
	{
		return BONUS;
	}
}
