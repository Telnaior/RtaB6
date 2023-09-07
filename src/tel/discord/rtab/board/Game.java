package tel.discord.rtab.board;

import tel.discord.rtab.games.*;

public enum Game implements WeightedSpace
{
	//Minigame Pool
	//Minigames awarding weird things appear first
	OVERFLOW(2)			{ public MiniGame getGame() { return new Overflow(); } },		//Author: JerryEris
	//PvP games next so the opponent doesn't fall asleep waiting for them
	COLOUR_OF_MONEY(1)	{ public MiniGame getGame() { return new ColourOfMoney(); } },	//Author: Telna
	//Regular cash games
	DEAL_OR_NO_DEAL(2)	{ public MiniGame getGame() { return new DealOrNoDeal(); } },	//Author: Telna
	MATH_TIME(2)		{ public MiniGame getGame() { return new MathTime(); } },		//Author: Telna
	SAFE_CRACKER(2)		{ public MiniGame getGame() { return new SafeCracker(); } },	//Author: Telna
	STRIKE_IT_RICH(2)	{ public MiniGame getGame() { return new StrikeItRich(); } },	//Author: Telna
	TRIPLE_PLAY(22)		{ public MiniGame getGame() { return new TriplePlay(); } },		//Author: Telna
	CLOSE_SHAVE(2)		{ public MiniGame getGame() { return new CloseShave(); } },		//Author: JerryEris
	STARDUST(2)			{ public MiniGame getGame() { return new Stardust(); } },		//Author: NicoHolic777
	BOMB_ROULETTE(2)	{ public MiniGame getGame() { return new BombRoulette(); } },	//Author: StrangerCoug
	MONEY_CARDS(2)		{ public MiniGame getGame() { return new MoneyCards(); } },		//Author: StrangerCoug
	PUNCH_A_BUNCH(2)	{ public MiniGame getGame() { return new PunchABunch(); } },    //Author: StrangerCoug
	SHUT_THE_BOX(2)		{ public MiniGame getGame() { return new ShutTheBox(); } },		//Author: StrangerCoug
	ZILCH(2)			{ public MiniGame getGame() { return new Zilch(); } },			//Author: Strangercoug
	BUMPER_GRAB(2)		{ public MiniGame getGame() { return new BumperGrab(); } },		//Author: Tara
	
	//Games rotated out
	TIC_TAC_BOMB(0)		{ public MiniGame getGame() { return new TicTacBomb(); } },		//Author: Telna
	TESTGAME(0)			{ public MiniGame getGame() { return new TestGame(); } },		//Author: The Triforce
	BOOSTER_SMASH(0)	{ public MiniGame getGame() { return new BoosterSmash(); } },	//Author: Telna
	FTROTS(0)			{ public MiniGame getGame() { return new FTROTS(); } },			//Author: Telna
	GAMBLE(0)			{ public MiniGame getGame() { return new Gamble(); } },			//Author: Telna
	OPTIMISE(0)			{ public MiniGame getGame() { return new Optimise(); } },		//Author: Telna
	COINFLIP(0)			{ public MiniGame getGame() { return new CoinFlip(); } },		//Author: Amy
	MINEFIELD_MULTI(0)	{ public MiniGame getGame() { return new MinefieldMulti(); } },	//Author: Amy
	THE_OFFER(0)		{ public MiniGame getGame() { return new TheOffer(); } },		//Author: Amy
	CALL_YOUR_SHOT(0)	{ public MiniGame getGame() { return new CallYourShot(); } },	//Author: JerryEris
	DOUBLE_TROUBLE(0)	{ public MiniGame getGame() { return new DoubleTrouble(); } },	//Author: JerryEris
	DOUBLE_ZEROES(0)	{ public MiniGame getGame() { return new DoubleZeroes(); } },	//Author: JerryEris
	OPEN_PASS(0)		{ public MiniGame getGame() { return new OpenPass(); } },		//Author: JerryEris
	UP_AND_DOWN(0)		{ public MiniGame getGame() { return new UpAndDown(); } },		//Author: JerryEris
	DEUCES_WILD(0)		{ public MiniGame getGame() { return new DeucesWild(); } },		//Author: StrangerCoug
	HILO_DICE(0)		{ public MiniGame getGame() { return new HiLoDice(); } },		//Author: StrangerCoug
	SPLIT_WINNINGS(0)	{ public MiniGame getGame() { return new SplitWinnings(); } },	//Author: StrangerCoug
	//Seasonal event is no longer a minigame lol
	COMMISSIONER(0)		{ public MiniGame getGame() { return new TheCommissioner(); } },
	
	//Bonus Games - not in pool but earned through other means
	SUPERCASH(0)		{ public MiniGame getGame() { return new Supercash(); } },
	DIGITAL_FORTRESS(0)	{ public MiniGame getGame() { return new DigitalFortress(); } },
	SPECTRUM(0)			{ public MiniGame getGame() { return new Spectrum(); } },
	HYPERCUBE(0)		{ public MiniGame getGame() { return new Hypercube(); } },
	RACE_DEAL(0)		{ public MiniGame getGame() { return new RaceDeal(); } },
	SUPERBONUSROUND(0)	{ public MiniGame getGame() { return new SuperBonusRound(); } };
	
	final String fullName;
	final String shortName;
	final String enhanceText;
	final boolean bonus;
	final int weight;
	Game(int valueWeight)
	{
		fullName = getGame().getName();
		shortName = getGame().getShortName();
		bonus = getGame().isBonus();
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
