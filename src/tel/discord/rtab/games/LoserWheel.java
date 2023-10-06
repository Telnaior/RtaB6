package tel.discord.rtab.games;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Random;

import net.dv8tion.jda.api.entities.Message;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.RtaBMath;
import tel.discord.rtab.games.objs.Jackpots;

public class LoserWheel extends MiniGameWrapper
{
	static final String NAME = "the Loser Wheel";
	static final String SHORT_NAME = "Loser";
	static final boolean BONUS = false;
	
	static final int[] BASE_VALUES = { 250_000, 1_000_000, 1_000_000, 2_000_000, 500_000, 90,
						500_000, 1_500_000, 1_500_000, 3_000_000, 1_000_000, 1,
						750_000, 2_000_000, 2_000_000, 4_000_000, 1_500_000, 100_000,
						1_000_000, 2_500_000, 2_500_000, 5_000_000, 2_000_000, 2};
	static final WheelSpace[] BASE_TYPES = {
			WheelSpace.CASH, WheelSpace.CASH, WheelSpace.LOAN, WheelSpace.CASH, WheelSpace.CASH, WheelSpace.BOOST,
			WheelSpace.CASH, WheelSpace.CASH, WheelSpace.LOAN, WheelSpace.CASH, WheelSpace.CASH, WheelSpace.PERCENTAGE,
			WheelSpace.CASH, WheelSpace.CASH, WheelSpace.LOAN, WheelSpace.CASH, WheelSpace.CASH, WheelSpace.ANNUITY,
			WheelSpace.CASH, WheelSpace.CASH, WheelSpace.LOAN, WheelSpace.CASH, WheelSpace.CASH, WheelSpace.PERCENTAGE};
	int[] spaceValues;
	WheelSpace[] spaceTypes;

    enum WheelSpace
    {
    	CASH(true), LOAN(true), BOOST(false), PERCENTAGE(false), ANNUITY(true), BIG_JUMBLE(false);
    	boolean needsMultiplying;
    	WheelSpace(boolean needsMultiplying)
    	{
    		this.needsMultiplying = needsMultiplying;
    	}
    }
	
	@Override
	void startGame()
	{
		//This should never happen outside of practice lol
		if(enhanced)
			getPlayer().threshold = true;
		//Instructions? more like memes
		if(getPlayer().uID.equals("346189542002393089") && RtaBMath.random() < 0.5) //you know who you are
			sendMessage("Welcome to the Loser Wheel! In this game, you **SUFFER**");
		else if(RtaBMath.random() < 0.005) //some things are truly not meant to be witnessed
			try { sendMessages(Files.readAllLines(Paths.get("LoserWheelSong.txt"))); }
			catch (IOException e) {	sendMessage("Did You Know: The Loser Wheel has its own song!"); }
		else
			sendMessage("Welcome to the Loser Wheel! In this game, we spin the wheel and you win whatever it lands on! Good luck!!");
		if(getPlayer().threshold)
			sendMessage("Oh dear, and you're in a threshold situation... you're really going to need that luck.");
		loserWheel();
	}
	
	private void loserWheel()
	{
		//Initialise the wheel
		spaceValues = new int[BASE_VALUES.length];
		spaceTypes = new WheelSpace[BASE_TYPES.length];
		for(int i=0; i<BASE_TYPES.length; i++)
		{
			spaceTypes[i] = BASE_TYPES[i];
			if(spaceTypes[i].needsMultiplying)
			{
				spaceValues[i] = applyBaseMultiplier(BASE_VALUES[i]);
				if(getPlayer().threshold && spaceTypes[i] != WheelSpace.LOAN)
					spaceValues[i] *= 4;
			}
			//Special handling for percentage bank spaces
			else if(spaceTypes[i] == WheelSpace.PERCENTAGE)
			{
				int percentage = BASE_VALUES[i];
				if(getPlayer().threshold)
					percentage *= 4;
				spaceValues[i] = RtaBMath.applyBankPercentBaseMultiplier(percentage, baseNumerator, baseDenominator);
				if(spaceValues[i] == 0)
				{
					spaceTypes[i] = WheelSpace.BOOST;
					spaceValues[i] = 50;
				}
			}
			else
				spaceValues[i] = BASE_VALUES[i];
		}
		//If the loans are piling up, consider asking for repayment
		if(RtaBMath.random() * applyBaseMultiplier(100_000_000) < Jackpots.LOSER_WHEEL.getJackpot(channel))
			spaceTypes[11] = WheelSpace.BIG_JUMBLE;
		//Go for a spin!
		int pointer = spinWheel();
		//And resolve accordingly
		switch(spaceTypes[pointer])
		{
			case CASH -> {
				sendMessage(String.format("That's a **$%,d** penalty. Better luck next time!", spaceValues[pointer]));
				awardMoneyWon(-1 * spaceValues[pointer]);
			}
			case LOAN -> {
				sendMessage(String.format("Wait, you won **$%,d**? Uh, let's just call that one a loan.", spaceValues[pointer]));
				Jackpots.LOSER_WHEEL.addToJackpot(channel, spaceValues[pointer]);
				awardMoneyWon(spaceValues[pointer]);
			}
			case BOOST -> {
				sendMessage(String.format("Oh, **-%d%% Boost**. Well, better luck next life?", spaceValues[pointer]));
				getPlayer().addBooster(-1 * spaceValues[pointer]);
				sendCustomEndgameMessage(String.format("-%d%% Boost", spaceValues[pointer]));
			}
			case PERCENTAGE -> {
				int sacrifice = getPlayer().money * spaceValues[pointer] / 100;
				sendMessage(String.format("Ah, ah, %d%% of your bank? Thank you for the **$%,d**!", spaceValues[pointer], sacrifice));
				awardMoneyWon(-1 * sacrifice);
			}
			case ANNUITY -> {
				int annuityTurns = (int)(RtaBMath.random()*26)+25; //25-50 turns
				sendMessage(String.format("Hey, a **$%,d** per turn penalty for, say, **%d turns**!", spaceValues[pointer], annuityTurns));
				getPlayer().addAnnuity(spaceValues[pointer], annuityTurns);
				sendCustomEndgameMessage(String.format("-$%,d/turn for %d turns", spaceValues[pointer], annuityTurns));
			}
			case BIG_JUMBLE -> {
				int repayment = Jackpots.LOSER_WHEEL.getJackpot(channel);
				LinkedList<String> output = new LinkedList<>();
				output.add("Gosh, you've finally decided to repay all those loans we've handed out? Thank you so much!");
				output.add("We haven't been keeping track of who received how much, so you'll be paying for everyone.");
				output.add(String.format("That'll be a total of **$%,d**. Pleasure doing business with you!",repayment));
				getPlayer().addMoney(-1 * repayment, MoneyMultipliersToUse.NOTHING);
				Jackpots.LOSER_WHEEL.resetJackpot(channel);
				sendCustomEndgameMessage(String.format("-$%,d", repayment));
			}
		}
	}
	
	int spinWheel()
	{
	    Random r = new Random();
    	int index = r.nextInt(spaceTypes.length);
    	if(sendMessages)
    	{
    		Message wheelMessage = channel.sendMessage(displayRoulette(index)).complete();
    		//Start with a 0.5-second delay
    		int delay = 500 + r.nextInt(250);
    		try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    		do
    		{
    			//Move along one space on the wheel
    			index ++;
    			index %= spaceTypes.length;
    			//Update the roulette display
    			wheelMessage.editMessage(displayRoulette(index)).queue();
    			//Then increase the delay randomly, and wait for that amount of time
    			delay += r.nextInt(250);
    			try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    		}
    		//Stop once we reach a 2.5-second delay
    		while(delay < 2500);
    	}
    	else
    	{
    		//Just simulate the spin quickly and quietly
    		int delay = 500 + r.nextInt(250);
    		do
    		{
    			index ++;
    			index %= spaceTypes.length;
    			delay += r.nextInt(250);
    		}
    		while(delay < 2500);
    	}
		return index;
	}
	
    private String displayRoulette(int index)
    {
		StringBuilder board = new StringBuilder().append("```\n");
		//Iterate through each space on the board
		for(int i=-2; i<=2; i++)
		{
			if(i == 0)
				board.append("> ");
			else
				board.append("  ");
			int wheelPosition = (index+i) % spaceTypes.length;
			if(wheelPosition < 0)
				wheelPosition += spaceTypes.length;
			board.append(switch(spaceTypes[wheelPosition])
				{
			case CASH -> String.format("-$%,d",spaceValues[wheelPosition]);
			case LOAN -> String.format("+$%,d", spaceValues[wheelPosition]);
			case BOOST -> String.format("-%d%% Boost", spaceValues[wheelPosition]);
			case PERCENTAGE -> String.format("-%d%% Bank", spaceValues[wheelPosition]);
			case ANNUITY -> String.format("-$%,d/turn", spaceValues[wheelPosition]);
			case BIG_JUMBLE -> "Loan Repayment";
			default -> "";
				});
			board.append("\n");
		}
		board.append("```");
		return board.toString();
    }
    
	void sendCustomEndgameMessage(String prize)
	{
		StringBuilder resultString = new StringBuilder();
		if(getPlayer().isBot)
			resultString.append(getPlayer().getName()).append(" won ");
		else
			resultString.append("Game Over. You won ");
		resultString.append(String.format("**%s** from ",prize));
		if(gameMultiplier > 1)
			resultString.append(String.format("%d copies of ",gameMultiplier));
		resultString.append(getName()).append(".");
		//We want the endgame result to show up unconditionally
		sendMessages = true;
		sendMessage(resultString.toString());
		gameOver();
	}

	//at the farmer's market with my so-called "input handler"
	@Override void playNextTurn(String input) { /*imagine thinking you get to have input over the loser wheel lmfao*/ }
	@Override String getBotPick() {	/*don't worry, the AI has just as much input over the loser wheel as you do*/ return ""; }
	@Override void abortGame() { /*????????????????*/ }
	
	//Getters
	@Override public String getName() { return NAME; }
	@Override public String getShortName() { return SHORT_NAME; }
	@Override public boolean isBonus() { return BONUS; }
	@Override public boolean isNegativeMinigame() { return true; }
	
}
