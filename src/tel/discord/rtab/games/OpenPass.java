package tel.discord.rtab.games;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class OpenPass extends MiniGameWrapper
{
	static final String NAME = "Open, Pass";
	static final String SHORT_NAME = "Open";
	static final boolean BONUS = false;
	int total;
	int placed;
	int passed;
	List<Integer> numbers = Arrays.asList(50,30,15,10,9,8,7,5,3,2,-1,-1,-1,-1,-1,-1,-2,-3,-3,-4);
	// -1 = Times, -2 = Divided by, 3 = Plus, 4 = Minus
	int[] openedCards;
	int[] equation;
	int equPart;
	int equPartReal;
	int operators;
	int digits;
	int[] equationReal;
	boolean alive;
	boolean needNumber;
	boolean[] pickedSpaces;
	int lastSpace;
	int lastPick;
	int lastSign;
	int lastNumber;
	int finalMultiplier;
	
	/**
	 * Initializes the variables used in the minigame and prints the starting messages.
	 */
	@Override
	void startGame()
	{
		finalMultiplier = applyBaseMultiplier(5);
		alive = true;
		needNumber = true;
		lastSign = -9;
		lastNumber = 99;
		digits = 10;
		operators = 10;
		total = 0;
		placed = 0;
		equPart = 0;
		equPartReal = 0;
		passed = 0;
		while (numbers.get(0) < 1 || numbers.get(19) < 1)
		{
			Collections.shuffle(numbers);
		}
		openedCards = new int[numbers.size()];
		equation = new int[10];
		equationReal = new int[10];

		// Give 'em the run down
		LinkedList<String> output = new LinkedList<>();
		output.add("In Open, Pass, you will build an equation by opening ten boxes.");
		output.add("You have a series of 20 boxes. Ten hold the numbers 2, 3, 5, 7, 8, 9, 10, 15, 30, and 50.");
		output.add("The other ten are mathematical operators: 5 x's, 2 /'s, 2 +'s, and 1 -.");
		output.add("The boxes have been shuffled, and we will place them out one at a time.");
		output.add("You can choose to '**OPEN**' a box, locking it into place, or you can '**PASS**', discarding it.");
		output.add("If there are multiple numbers or operators in a row, only the rightmost one will count.");
		output.add("An operator at the end of the equation will not count, either.");
		output.add("Once ten boxes have been opened, the equation is evaluated from left to right, "
				+ String.format("and you'll win %d times the result!",finalMultiplier));
		output.add("Good luck! Let's bring out the first box for you...");
		sendSkippableMessages(output);
		output.clear();
		output.add(generateBoard());
		output.add("Here is the order of boxes - you can type !order at any time to see this again.");
		output.add(generateOrder());
		sendMessages(output);
		getInput();
	}

	/**
	 * Takes the next player input and uses it to play the next "turn" - up until the next input is required.
	 * @param pick  The next input sent by the player.
	 */
	@Override
	void playNextTurn(String pick)
	{
		LinkedList<String> output = new LinkedList<>();
		if(pick.equalsIgnoreCase("!string") || pick.equalsIgnoreCase("!order"))
		{
			output.add(generateOrder());
		}
		else if(pick.equalsIgnoreCase("OPEN") || pick.equalsIgnoreCase("O"))
		{
			output.add("The box is...");
			if (numbers.get(passed+placed) > 1 && needNumber)
			{
				output.add("***" + numbers.get(passed+placed) + "***! We'll keep that number in the equation for now.");
				equation[equPart] = numbers.get(passed+placed);
				equPart++;
				equationReal[equPartReal] = equation[equPart - 1];
				equPartReal++;
				digits--;
			}
			else if (numbers.get(passed+placed) > 1 && !needNumber)
			{
				output.add("***" + numbers.get(passed+placed) + "***. We'll replace the previous number with that one.");
				equation[equPart] = numbers.get(passed+placed);
				equPart++;
				equPartReal--;				
				equationReal[equPartReal] = equation[equPart - 1];
				equPartReal++;
				digits--;
			}
			else if (numbers.get(passed+placed) == -1 && !needNumber)
			{
				output.add("***x***! We'll keep that operator as part of the equation.");
				equation[equPart] = -1;
				equPart++;				
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
			}
			else if (numbers.get(passed+placed) == -3 && !needNumber)
			{
				output.add("***+***. We'll keep that operator as part of the equation.");
				equation[equPart] = -3;
				equPart++;				
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
			}
			else if (numbers.get(passed+placed) == -1 && needNumber)
			{
				output.add("***x***! We'll replace the previous operator with that one.");
				equation[equPart] = -1;
				equPart++;				
				equPartReal--;					
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
			}
			else if (numbers.get(passed+placed) == -3 && needNumber)
			{
				equation[equPart] = -3;
				equPart++;
				equPartReal--;					
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
				output.add("***+***. We'll replace the previous operator with that one.");
			}
			else if (numbers.get(passed+placed) == -2 && needNumber)
			{
				output.add("***/***. Unfortunately, we'll replace the previous operator with that one.");
				equation[equPart] = -2;
				equPart++;				
				equPartReal--;					
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
			}
			else if (numbers.get(passed+placed) == -4 && needNumber)
			{
				output.add("***-***. Unfortunately, we'll replace the previous operator with that one.");
				equation[equPart] = -4;
				equPart++;				
				equPartReal--;					
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
			}
			else if (numbers.get(passed+placed) == -2 && !needNumber)
			{
				output.add("***/***. Unfortunately, that operator goes in the equation.");
				equation[equPart] = -2;
				equPart++;								
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
			}
			else if (numbers.get(passed+placed) == -4 && !needNumber)
			{
				output.add("***-***. Unfortunately, that operator goes in the equation.");
				equation[equPart] = -4;
				equPart++;								
				if (equationReal[0] != 0)
				{
					equationReal[equPartReal] = equation[equPart - 1];
				}
				equPartReal++;
				operators--;
			}
			openedCards[placed] = numbers.get(passed+placed);
			placed++;
			if (placed == 10)
			{
				alive = false;
				//end game and do the thing
			}
			else 
			{
				output.add("Your next box has been placed. **OPEN** or **PASS**?");
			}
			output.add(generateBoard());
		}	
		else if(pick.equalsIgnoreCase("PASS") || pick.equalsIgnoreCase("P"))
		{
			if (10 - placed == 20 - (placed + passed + 1))
			{
				if (numbers.get(passed+placed) > 1)
				{
					digits--;
				}
				else
				{
					operators--;
				}
				output.add("Okay, we'll discard that one, but we must now **OPEN** the rest of the boxes, as there are no more boxes available to discard.");
				output.add("How will the board resolve? Let's find out...");
				passed++;
				
				while (!(placed == 10))
				{
					if (equPart == 0 && numbers.get(passed+placed) < 1)
					{
						equation[equPart] = numbers.get(passed+placed);
						equPart++;
						//Operator starting the equation, so don't add it to the real equation
						operators--;
					}
					else if (equPart == 0 && numbers.get(passed+placed) > 1)
					{
						equation[equPart] = numbers.get(passed+placed);
						needNumber = false;
						equPart++;
						equationReal[0] = equation[equPart];
						equPartReal++;
						digits--;
					}
					else if (numbers.get(passed+placed) > 1 && needNumber)
					{
						equation[equPart] = numbers.get(passed+placed);
						needNumber = false;
						equPart++;
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						digits--;
					}
					else if (numbers.get(passed+placed) > 1 && !needNumber)
					{
						equation[equPart] = numbers.get(passed+placed);
						equPart++;
						equPartReal--;				
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						digits--;
					}
					else if (numbers.get(passed+placed) == -1 && !needNumber)
					{
						equation[equPart] = -1;
						needNumber = true;
						equPart++;
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					else if (numbers.get(passed+placed) == -3 && !needNumber)
					{
						equation[equPart] = -3;
						needNumber = true;
						equPart++;
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					else if (numbers.get(passed+placed) == -1 && needNumber)
					{
						equation[equPart] = -1;
						equPart++;				
						equPartReal--;	
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					else if (numbers.get(passed+placed) == -3 && needNumber)
					{
						equation[equPart] = -3;
						equPart++;
						equPartReal--;	
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					else if (numbers.get(passed+placed) == -2 && needNumber)
					{
						equation[equPart] = -2;
						equPart++;				
						equPartReal--;	
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					else if (numbers.get(passed+placed) == -4 && needNumber)
					{
						equation[equPart] = -4;
						equPart++;				
						equPartReal--;	
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					else if (numbers.get(passed+placed) == -2 && !needNumber)
					{
						equation[equPart] = -2;
						needNumber = true;
						equPart++;				
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					else if (numbers.get(passed+placed) == -4 && !needNumber)
					{
						equation[equPart] = -4;
						needNumber = true;
						equPart++;				
						equationReal[equPartReal] = equation[equPart - 1];
						equPartReal++;
						operators--;
					}
					openedCards[placed] = numbers.get(passed+placed);
					placed++;
				}
				alive = false;
				output.add("...");
				output.add(generateBoard());
			}
			else
			{
				if (numbers.get(passed+placed) > 1)
				{
					digits--;
				}
				else
				{
					operators--;
				}
				output.add("Okay, we'll discard that one. Your next box has been placed. **OPEN** or **PASS**?");
				passed++;
				output.add(generateBoard());
			}
		}
		sendMessages(output);
		if(!alive)
			awardMoneyWon(total*finalMultiplier);
		else
			getInput();
	}

	String generateOrder()
	{
		StringBuilder orderString = new StringBuilder("```");
		for (int i = placed+passed; i < 20; i++)
		{
			if (numbers.get(i) > 1)
			{
				orderString.append("N ");
			}
			else
			{
				orderString.append("? ");
			}
		}
		return orderString + "```";
	}
	
	String generateBoard()
	{
		StringBuilder display = new StringBuilder();
		display.append("```\n");
		display.append("  OPEN, PASS \n");
		display.append("\n");
		display.append("Current Equation:\n");
		//display and evaluate equation
		total = 0;
		lastSign = -9;
		lastNumber = 99;
		needNumber = true;
		for(int i=0; i < placed; i++)
		{
			if (equation[i] > 0) //number
			{
					display.append(equation[i]);
					lastNumber = equation[i];
					needNumber = false;
			}
			else if (equation[i] == -1) //times
			{
					display.append("x");
					needNumber = true;

			}
			else if (equation[i] == -2) //divided by
			{
					display.append("/");
					needNumber = true;
			}
			else if (equation[i] == -3) //plus
			{
					display.append("+");
					needNumber = true;
			}
			else if (equation[i] == -4) //minus
			{
					display.append("-");
					needNumber = true;
			}
				display.append(" | ");
		}
	
		if (equPartReal < 10)
		{
			if (placed+passed == 20 || placed == 10)
			{
				display.append("\nGAME OVER\n");
			}
			else if (numbers.get(placed+passed) > 1) 
			{
				display.append("N\nCurrent box: NUMBER\n");
			}
			else
			{
				display.append("?\nCurrent box: OPERATOR\n");
			}
		}
		display.append("Evaluates to: ");
		for(int i=0; i < equPartReal; i++)
		{
			if (equationReal[i] > 0) //number
			{
					display.append(equationReal[i]);
			}
			else if (i < equPartReal - 1)
			{
				if (equationReal[i] == -1) //times
				{
						display.append("x");
				}
				else if (equationReal[i] == -2) //divided by
				{
						display.append("/");
				}
				else if (equationReal[i] == -3) //plus
				{
						display.append("+");
				}
				else if (equationReal[i] == -4) //minus
				{
						display.append("-");
				}
			}
			display.append(" ");
		}	
		for (int j = 0; j < equPartReal; j++)
		{
			if (j % 2 == 0) //number
			{
					if (lastSign == -9)
					{
						total = equationReal[j];
					}
					else if (lastSign == -1)
					{
						total = total * equationReal[j];
					}
					else if (lastSign == -2)
					{
						total = total / equationReal[j];
					}
					else if (lastSign == -3)
					{
						total = total + equationReal[j];
					}
					else if (lastSign == -4)
					{
						total = total - equationReal[j];
					}
			}
			else
			{
				lastSign = equationReal[j]; 
			}		
		}
		display.append(String.format("= %,d\n",total));
		display.append(String.format("Value x%d = $%,d\n", finalMultiplier, total * finalMultiplier));
		display.append(String.format("Boxes left: %d\n",20 - (placed + passed)));
		display.append(String.format("(Numbers: %d, ",digits));
		display.append(String.format("Operators: %d)\n",operators));
		
		display.append(String.format("Slots left: %d\n",10 - placed));
		display.append("```");
		return display.toString();
	}
	
	@Override
	String getBotPick()
	{
		if (placed == 0)
		{
			return "OPEN";
		}
		else if (placed == 1)
		{
			if (numbers.get(placed+passed) > 1)
			{
				return "PASS";
			}
			else
			{
				return "OPEN";
			}
		}
		//Previous was -, +, or /? open if oper and pass if number
		else if (equationReal[equPartReal - 1] < -1)
		{
			if (numbers.get(placed+passed) > 1)
			{
				return "PASS";
			}
			else
			{
				return "OPEN";
			}
		}
		else if (equationReal[equPartReal - 1] == -1)	//Previous was x? pass if oper and open if number
		{	

			if (numbers.get(placed+passed) > 1)
			{
				return "OPEN";
			}
			else
			{
				return "PASS";
			}
		}
		else if (lastNumber > 10 && lastSign == -1) 
		{
			//Previous was 50, 30, or 15 and second-previous was x? open if oper and pass if number
			if (numbers.get(placed+passed) > 1)
			{
				return "PASS";
			}
			else
			{
				return "OPEN";
			}
		}
		else if (lastNumber > 1 && lastSign == -1) 
		{
			//Previous was 10 or under and second-previous was x?
			//random chance to act like above (lower #s give less chance)
			int theChance = lastNumber * 9;
			if (numbers.get(placed+passed) > 1)
			{					
				if ((int)(Math.random() * 100) < theChance)
				{	
					return "PASS";
				}
				else
				{
					return "OPEN";
				}
			}
			else
			{
				if ((int)(Math.random()*100) < theChance)
				{
					return "OPEN";
				}
				else
				{
					return "PASS";
				}
			}
		}
		else
		{
			return "PASS";
			//I don't think there are any other scenarios I'm missing, but this is a fallback if there are
		}
	}

	@Override
	void abortGame()
	{
		//Sorry, I don't think there's a better way to handle it in a game this complex
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
}
