package tel.discord.rtab;

public final class RtaBMath
{
	//Private constructor to prevent instantiation
	private RtaBMath() 
	{
	    throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated.");
	}
	
	public static int calculateEntryFee(int money, int lives)
	{
		int entryFee = Math.max(money/500,20000);
		entryFee *= 5 - lives;
		return entryFee;
	}
	
	public static int applyBaseMultiplier(int amount, int baseNumerator, int baseDenominator)
	{
		long midStep = amount * (long)baseNumerator;
		long endStep = midStep / baseDenominator;
		if(endStep > 1_000_000_000)
			endStep = 1_000_000_000;
		if(endStep < -1_000_000_000)
			endStep = -1_000_000_000;
		return (int)endStep;
	}
}
