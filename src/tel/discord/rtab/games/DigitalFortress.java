package tel.discord.rtab.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.games.objs.Jackpots;

public class DigitalFortress extends MiniGameWrapper
{
	static final String NAME = "Digital Fortress";
	static final String SHORT_NAME = "Fortress";
	static final boolean BONUS = true;
	static final int PRIZE_PER_DIGIT = 2500000;
	static final int ATTEMPTS_ALLOWED = 4;
	List<Character> solution = Arrays.asList('0','1','2','3','4','5','6','7','8','9');
	String[] guesses = new String[ATTEMPTS_ALLOWED];
	boolean[] lockedIn;
	int digitsCorrect;
	int attemptsLeft;
	int jackpot;
	
	@Override
	void startGame()
	{
		jackpot = Jackpots.DIGITAL.getJackpot(channel);
		LinkedList<String> output = new LinkedList<>();
		//Initialise stuff
		Collections.shuffle(solution);
		lockedIn = new boolean[solution.size()];
		digitsCorrect = 0;
		attemptsLeft = ATTEMPTS_ALLOWED;
		//Streak bonus achievement
		Achievement.EIGHT.check(getPlayer());
		//Provide help
		output.add("For reaching a streak bonus of x8, you have earned the right to play the second bonus game!");
		output.add("In Digital Fortress, you can win a jackpot which currently stands at "+String.format("$%,d!",applyBaseMultiplier(jackpot)));
		output.add("The computer has created a secret ten-digit passcode, using each digit once and once only.");
		output.add("Your job is to guess this passcode.");
		output.add("You have four attempts to do so, "
				+ "and after each attempt you will be told which digits are in the right place.");
		output.add("If you crack the code completely, you will win the jackpot!");
		output.add("Otherwise, once you have been locked out after four attempts, "
				+ "you will earn "+String.format("$%,d",applyBaseMultiplier(PRIZE_PER_DIGIT))+" for each digit you had correct.");
		output.add("Submit your first guess at the passcode when you are ready, and good luck!");
		sendSkippableMessages(output);
		sendMessage(generateBoard());
		getInput();
	}
	
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
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
		if(digitsCorrect == solution.size())
		{
			output.add(digitsCorrect + " digits correct, congratulations!");
			sendMessages(output);
			output.clear();
			Achievement.DIGITAL_JACKPOT.check(getPlayer());
		}
		else if(digitsCorrect == 1)
			output.add(digitsCorrect + " digit correct.");
		else
			output.add(digitsCorrect + " digits correct.");
		if(digitsCorrect < solution.size())
			output.add(generateBoard());
		sendMessages(output);
		if(isGameOver())
			awardMoneyWon(getMoneyWon());
		else
			getInput();
	}

	private boolean isValidNumber(String message)
	{
		//If this doesn't throw an exception we're good
		char[] test = message.toCharArray();
		for(char check : test)
		{
			if(!Character.isDigit(check))
			{
				return false;
			}
		}
		//Needs to be exactly ten digits
		return (message.length() == solution.size());
	}

	private String generateBoard() {
		StringBuilder board = new StringBuilder();
		board.append("```\n");
		board.append("DIGITAL FORTRESS\n");
		//Counting down in a loop, spooky
		for(int i=3; i>=attemptsLeft; i--)
		{
			board.append("   ");
			board.append(guesses[i]);
			board.append("\n");
		}
		if(attemptsLeft == 0)
			board.append("  ------------\n");
		board.append("   ");
		for(int i=0; i<solution.size(); i++)
		{
			if(lockedIn[i] || attemptsLeft == 0)
				board.append(solution.get(i));
			else
				board.append("-");
		}
		board.append("\n```");
		return board.toString();
	}

	private boolean isGameOver()
	{
		return (digitsCorrect == solution.size() || attemptsLeft <= 0);
	}

	private int getMoneyWon()
	{
		//Award the jackpot and reset it if they won it, otherwise increment it and award the basic prize
		if(digitsCorrect == solution.size())
		{
			Jackpots.DIGITAL.resetJackpot(channel);
			return applyBaseMultiplier(jackpot);
		}
		else
		{
			Jackpots.DIGITAL.addToJackpot(channel,500_000);
			return (applyBaseMultiplier(PRIZE_PER_DIGIT * digitsCorrect));
		}
	}
	
	@Override
	String getBotPick()
	{
		//Arrays.asList is fixed-size, so we copy it over to a new list we can actually add/remove to
		List<Character> digitsOld = Arrays.asList('0','1','2','3','4','5','6','7','8','9');
		ArrayList<Character> digits = new ArrayList<>(11);
		if(attemptsLeft == ATTEMPTS_ALLOWED)
		{
			digits.addAll(digitsOld);
			Collections.shuffle(digits); //don't just 1234567890 every game
		}
		else
			for(Character next : guesses[attemptsLeft].toCharArray())
				digits.add(next);
		//Now remove anything we've already locked in
		for(int i=0; i<solution.size(); i++)
			if(lockedIn[i])
				digits.remove(solution.get(i));
		//Cycle the list once to get a new guess
		digits.add(digits.getFirst());
		digits.removeFirst();
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
		//Treat it as though they achieve nothing with their remaining guesses
		awardMoneyWon(getMoneyWon());
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