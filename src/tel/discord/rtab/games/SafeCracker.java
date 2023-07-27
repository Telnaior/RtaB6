package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class SafeCracker extends MiniGameWrapper
{
	static final String NAME = "Safe Cracker";
	static final String SHORT_NAME = "Safe";
	static final boolean BONUS = false;
	static final int ATTEMPTS_ALLOWED = 3;
	static final List<Character> DIGITS = Arrays.asList('1','2','3','4','5','6','7','8','9','0');
	static final List<Integer> SAFE_DIGITS = Arrays.asList(5,7,9);
	static final List<String> SAFE_NAMES = Arrays.asList("BRONZE", "SILVER", "GOLD");
	static final List<Integer> SAFE_VALUES = Arrays.asList(200_000, 1_000_000, 7_500_000);
	ArrayList<Character> solution = new ArrayList<>();
	String[] guesses = new String[ATTEMPTS_ALLOWED + 1];
	boolean[] lockedIn;
	int digitsCorrect;
	int attemptsLeft;
	int chosenSafe;
	
	@Override
	void startGame()
	{
		LinkedList<String> output = new LinkedList<>();
		//Initialise stuff
		chosenSafe = -1;
		digitsCorrect = 0;
		attemptsLeft = enhanced ? ATTEMPTS_ALLOWED + 1 : ATTEMPTS_ALLOWED;
		//Provide help
		output.add("In Safe Cracker, you are attempting to guess the passcode for a safe to access the cash within it.");
		output.add("There are three safes available, guarded by a 5-digit, 7-digit, and 9-digit code respectively.");
		output.add("Each passcode uses the digits 1-n, using each digit once and once only.");
		output.add("Your job is to guess the passcode for the safe you choose.");
		output.add("You have three attempts to do so, "
				+ "and after each attempt you will be told which digits are in the right place.");
		output.add("If you can't crack the safe in three attempts, you will be locked out and win nothing.");
		if(enhanced)
			output.add("ENHANCE BONUS: You will have an extra attempt to crack your chosen safe.");
		output.add("Choose which safe you want to take on when you are ready, and good luck!");
		sendSkippableMessages(output);
		sendMessage(buildSafeTable());
		getInput();
	}
	
	String buildSafeTable()
	{
		StringBuilder output = new StringBuilder();
		output.append("```\nSafe   | Digits | Prize\n-------+--------+------------\n");
		for(int i=0; i<SAFE_NAMES.size(); i++)
			output.append(String.format("%-6s | %6d | $%,10d\n", SAFE_NAMES.get(i), SAFE_DIGITS.get(i), applyBaseMultiplier(SAFE_VALUES.get(i))));
		output.append("```");
		return output.toString();
	}
	
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(chosenSafe == -1)
		{
			for(int i=0; i<SAFE_NAMES.size(); i++)
			{
				if(pick.equalsIgnoreCase(SAFE_NAMES.get(i)) || pick.equalsIgnoreCase(SAFE_NAMES.get(i).substring(0,1)))
				{
					chosenSafe = i;
					output.add(String.format("You chose the %s safe. The passcode uses the digits 1-%d and you'll earn $%,d "
							+ "if you can crack it in %s attempts. Good luck!",SAFE_NAMES.get(i),SAFE_DIGITS.get(i),
							applyBaseMultiplier(SAFE_VALUES.get(i)),enhanced?"four":"three"));
					//Prepare the solution
					solution = new ArrayList<>(SAFE_DIGITS.get(i));
					for(int j=0; j<SAFE_DIGITS.get(i); j++)
						solution.add(DIGITS.get(j));
					Collections.shuffle(solution);
					lockedIn = new boolean[solution.size()];
					//And finish sending the messages
					output.add(generateBoard());
					sendMessages(output);
					break;
				}
			}
			getInput();
			return;
		}
		if(!isValidNumber(pick))
		{
			//Non-number or wrong size doesn't need feedback
			getInput();
			return;
		}
		//Subtract an attempt and record the guess
		attemptsLeft --;
		guesses[attemptsLeft] = pick;
		//Check to see how many digits are right
		for(int i=0; i<solution.size(); i++)
		{
			if(solution.get(i).equals(guesses[attemptsLeft].charAt(i)) && !lockedIn[i])
			{
				lockedIn[i] = true;
				digitsCorrect++;
			}
		}
		//Print output
		output.add("Submitting "+guesses[attemptsLeft]+"...");
		output.add("...");
		boolean winner = false;
		if(digitsCorrect == solution.size())
		{
			output.add(digitsCorrect + " digits correct, congratulations!");
			winner = true;
		}
		else if(digitsCorrect == 1)
			output.add(digitsCorrect + " digit correct.");
		else
			output.add(digitsCorrect + " digits correct.");
		if(!winner && attemptsLeft == 0)
			output.add("Unfortunately, you've been locked out of the safe.");
		output.add(generateBoard());
		sendMessages(output);
		if(winner)
			awardMoneyWon(applyBaseMultiplier(SAFE_VALUES.get(chosenSafe)));
		else if(attemptsLeft == 0)
			awardMoneyWon(0);
		else
			getInput();
	}

	private boolean isValidNumber(String message)
	{
		//Check that the character is a digit and that it's within the range
		char[] test = message.toCharArray();
		for(char check : test)
		{
			if(!Character.isDigit(check))
				return false;
			int digit = check - '0';
			if(digit < 1 || digit > SAFE_DIGITS.get(chosenSafe))
			{
				sendMessage("The passcode only uses digits from 1 to "+SAFE_DIGITS.get(chosenSafe)+".");
				return false;
			}
		}
		//Needs to be exactly the right length
		return (message.length() == solution.size());
	}

	private String generateBoard() {
		StringBuilder board = new StringBuilder();
		board.append("```\n");
		board.append("  S A F E\n  CRACKER\n\n");
		//Counting down in a loop, spooky
		for(int i = enhanced ? ATTEMPTS_ALLOWED : ATTEMPTS_ALLOWED - 1; i>=attemptsLeft; i--)
		{
			for(int j=3; j>chosenSafe; j--)
				board.append(" ");
			board.append(guesses[i]);
			board.append("\n");
		}
		if(attemptsLeft == 0)
		{
			for(int i=2; i>chosenSafe; i--)
				board.append(" ");
			for(int i=0; i<SAFE_DIGITS.get(chosenSafe)+2; i++)
				board.append("-");
			board.append("\n");
			for(int i=3; i>chosenSafe; i--)
				board.append(" ");
			for (Character character : solution) {
				board.append(character);
			}
			board.append("\n");
		}
		if(digitsCorrect < solution.size())
			for(int i = attemptsLeft; i > 0; i--)
			{
				for(int j=3; j>chosenSafe; j--)
					board.append(" ");
				for(int j=0; j<solution.size(); j++)
					board.append(lockedIn[j] ? solution.get(j) : "-");
				board.append("\n");
			}
		board.append("```");
		return board.toString();
	}
	
	@Override
	String getBotPick()
	{
		//Just pick a random safe
		if(chosenSafe == -1)
			return SAFE_NAMES.get((int)(Math.random()*SAFE_NAMES.size()));
		//This isn't a perfect way of doing it but whatever, it's a bot
		//Arrays.asList is fixed-size, so we copy it over to a new list we can actually add/remove to
		ArrayList<Character> digits = new ArrayList<>(SAFE_DIGITS.get(chosenSafe));
		for(int i=0; i<SAFE_DIGITS.get(chosenSafe); i++)
			digits.add(DIGITS.get(i));
		//Now remove anything we've already locked in
		for(int i=0; i<solution.size(); i++)
			if(lockedIn[i])
				digits.remove(solution.get(i));
		//Cycle the list once for every attempt used
		for(int i=ATTEMPTS_ALLOWED+1; i>attemptsLeft; i--)
		{
			digits.add(digits.get(0));
			digits.remove(0);
		}
		//Now start building up the result
		StringBuilder result = new StringBuilder();
		ListIterator<Character> nextDigit = digits.listIterator();
		//If we have the digit right then grab it directly
		//Otherwise grab the next digit from our set
		for(int i=0; i<solution.size(); i++)
		{
			if(lockedIn[i])
				result.append(solution.get(i));
			else
				result.append(nextDigit.next());
		}
		return result.toString();
	}

	@Override
	void abortGame()
	{
		//It's all-or-nothing, so they get nothing
		awardMoneyWon(0);
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
	@Override
	public String getEnhanceText()
	{
		return "You will be given an extra attempt to crack your chosen safe.";
	}
}