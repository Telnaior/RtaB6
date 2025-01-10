package tel.discord.rtab.board;

import tel.discord.rtab.games.*;

public enum Game implements WeightedSpace
{
	//Minigame Pool
	//Seasonal events coded as minigames at the top
	COMMISSIONER(0)		{ public MiniGame getGame() { return new TheCommissioner(); } },
	//Minigames awarding weird things appear before others
	BOOSTER_SMASH(2)	{ public MiniGame getGame() { return new BoosterSmash(); } },	//Author: Telna
	OVERFLOW(2)			{ public MiniGame getGame() { return new Overflow(); } },		//Author: JerryEris
	//PvP games next so the opponent doesn't fall asleep waiting for them
	COLOUR_OF_MONEY(1)	{ public MiniGame getGame() { return new ColourOfMoney(); } },	//Author: Telna
	//Regular cash games
	DANGER_DICE(2)		{ public MiniGame getGame() { return new DangerDice(); } },		//Author: Telna
	DEAL_OR_NO_DEAL(2)	{ public MiniGame getGame() { return new DealOrNoDeal(); } },	//Author: Telna
	FTROTS(2)			{ public MiniGame getGame() { return new FTROTS(); } },			//Author: Telna
	TRIPLE_PLAY(2)		{ public MiniGame getGame() { return new TriplePlay(); } },		//Author: Telna
	MINEFIELD_MULTI(2)	{ public MiniGame getGame() { return new MinefieldMulti(); } },	//Author: Amy
	CLOSE_SHAVE(2)		{ public MiniGame getGame() { return new CloseShave(); } },		//Author: JerryEris
	STARDUST(2)			{ public MiniGame getGame() { return new Stardust(); } },		//Author: NicoHolic777
	FIFTY_YARD_DASH(2)	{ public MiniGame getGame() { return new FiftyYardDash(); } },	//Author: StrangerCoug
	MONEY_CARDS(2)		{ public MiniGame getGame() { return new MoneyCards(); } },		//Author: StrangerCoug
	SHUT_THE_BOX(2)		{ public MiniGame getGame() { return new ShutTheBox(); } },		//Author: StrangerCoug
	ZILCH(2)			{ public MiniGame getGame() { return new Zilch(); } },			//Author: StrangerCoug
	BUMPER_GRAB(2)		{ public MiniGame getGame() { return new BumperGrab(); } },		//Author: Tara
	
	//Games rotated out
	TESTGAME(0)			{ public MiniGame getGame() { return new TestGame(); } },		//Author: The Triforce
	GAMBLE(0)			{ public MiniGame getGame() { return new Gamble(); } },			//Author: Telna
	MATH_TIME(0)		{ public MiniGame getGame() { return new MathTime(); } },		//Author: Telna
	OPTIMISE(0)			{ public MiniGame getGame() { return new Optimise(); } },		//Author: Telna
	SAFE_CRACKER(0)		{ public MiniGame getGame() { return new SafeCracker(); } },	//Author: Telna
	STRIKE_IT_RICH(0)	{ public MiniGame getGame() { return new StrikeItRich(); } },	//Author: Telna
	COINFLIP(0)			{ public MiniGame getGame() { return new CoinFlip(); } },		//Author: Amy
	THE_OFFER(0)		{ public MiniGame getGame() { return new TheOffer(); } },		//Author: Amy
	CALL_YOUR_SHOT(0)	{ public MiniGame getGame() { return new CallYourShot(); } },	//Author: JerryEris
	DOUBLE_TROUBLE(0)	{ public MiniGame getGame() { return new DoubleTrouble(); } },	//Author: JerryEris
	//DOUBLE_ZEROES(0)	{ public MiniGame getGame() { return new DoubleZeroes(); } },	//Author: JerryEris (disabled until fixed)
	OPEN_PASS(0)		{ public MiniGame getGame() { return new OpenPass(); } },		//Author: JerryEris
	UP_AND_DOWN(0)		{ public MiniGame getGame() { return new UpAndDown(); } },		//Author: JerryEris
	BOMB_ROULETTE(0)	{ public MiniGame getGame() { return new BombRoulette(); } },	//Author: StrangerCoug
	PUNCH_A_BUNCH(0)	{ public MiniGame getGame() { return new PunchABunch(); } },    //Author: StrangerCoug
	SPLIT_WINNINGS(0)	{ public MiniGame getGame() { return new SplitWinnings(); } },	//Author: StrangerCoug
	
	//Negative (not in pool but 'earned' through other means)
	TIC_TAC_BOMB(0)		{ public MiniGame getGame() { return new BowserTicTacBomb(); } },//Author: Telna
	LOSER_WHEEL(0)		{ public MiniGame getGame() { return new LoserWheel(); } },		 //Author: Telna
	
	//Bonus Games - not in pool but earned through other means
	SUPERCASH(0)		{ public MiniGame getGame() { return new Supercash(); } },
	GLOBETROTTER(0)		{ public MiniGame getGame() { return new Globetrotter(); } },
	DIGITAL_FORTRESS(0)	{ public MiniGame getGame() { return new DigitalFortress(); } },
	SPECTRUM(0)			{ public MiniGame getGame() { return new Spectrum(); } },
	HYPERCUBE(0)		{ public MiniGame getGame() { return new Hypercube(); } },
	RACE_DEAL(0)		{ public MiniGame getGame() { return new RaceDeal(); } },
	PARTICLE_ACCEL(0)	{ public MiniGame getGame() { return new ParticleAccelerator(); } }, //DO NOT LEAK THIS
	SUPERBONUSROUND(0)	{ public MiniGame getGame() { return new SuperBonusRound(); } };
	
	
	final String fullName;
	final String shortName;
	final String enhanceText;
	final boolean bonus;
	final boolean isNegative;
	final int weight;
	Game(int valueWeight)
	{
		fullName = getGame().getName();
		shortName = getGame().getShortName();
		bonus = getGame().isBonus();
		isNegative = getGame().isNegativeMinigame();
		enhanceText = getGame().getEnhanceText();
		weight = valueWeight; 
	}
	public String getName()
	{
		return fullName;
	}
	public String getShortName()
	{
		return shortName;
	}
	public boolean isBonus()
	{
		return bonus;
	}
	public boolean isNegative()
	{
		return isNegative;
	}
	public String getEnhanceText()
	{
		return enhanceText;
	}
	//Returns a new instance of the requested minigame
	public abstract MiniGame getGame();
	
	@Override
	public int getWeight(int playerCount)
	{
		//Minigame types don't care about playercount
		return weight;
	}
}
