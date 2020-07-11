package tel.discord.rtab;

import net.dv8tion.jda.api.entities.TextChannel;

public class GameController
{
	
	public TextChannel gameChannel;
	int baseMultiplierNumerator, baseMultiplierDenominator;
	
	public GameController(TextChannel gameChannel, int baseNumerator, int baseDenominator)
	{
		this.gameChannel = gameChannel;
		baseMultiplierNumerator = baseNumerator;
		baseMultiplierDenominator = baseDenominator;
		int baseMultiplierGCD = findGCD(baseNumerator, baseDenominator);
		baseMultiplierNumerator /= baseMultiplierGCD;
		baseMultiplierDenominator /= baseMultiplierGCD;
		if(baseNumerator == 2 && baseDenominator == 1)
			gameChannel.sendMessage("OMG OMG DOUBLE CASH").queue();
		else if(baseNumerator == 3 && baseDenominator == 1)
			gameChannel.sendMessage("OMG OMG OMG **TRIPLE CASH**").queue();
		else if(baseNumerator == 4 && baseDenominator == 1)
			gameChannel.sendMessage("OMG OMG OMG OMG ***QUADRA CASH***").queue();
		else if(baseNumerator == 5 && baseDenominator == 1)
			gameChannel.sendMessage("OMG OMG OMG OMG OMG ***__P E N T A C A S H__***").queue();
		else if(baseNumerator == 1 && baseDenominator == 2)
		{
			gameChannel.sendMessage("omg half cash?").queue();
			gameChannel.sendMessage("Doesn't the race take long enough already?").queue();
			gameChannel.sendMessage("Well, if you say so.").queue();
		}
		else if(baseNumerator == 9 && baseDenominator == 1)
			gameChannel.sendMessage("now this is just getting silly.").queue();
		else
			gameChannel.sendMessage("OMG IT'S A GAME CHANNEL but i don't know how to run a game yet sorry").queue();
	}
	
    /*
     * Java method to find GCD of two number using Euclid's method
     * @return GDC of two numbers in Java
     */
    private static int findGCD(int number1, int number2) {
        //base case
        if(number2 == 0){
            return number1;
        }
        return findGCD(number2, number1%number2);
    }
	
}
