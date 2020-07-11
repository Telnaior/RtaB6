package tel.discord.rtab;

public enum MoneyMultipliersToUse {
	NOTHING(false,false),
	BOOSTER_ONLY(true,false),
	BONUS_ONLY(false,true),
	BOOSTER_OR_BONUS(true,true);
	
	public boolean useBoost, useBonus;
	MoneyMultipliersToUse(boolean boost, boolean bonus)
	{
		useBoost = boost;
		useBonus = bonus;
	}
}
