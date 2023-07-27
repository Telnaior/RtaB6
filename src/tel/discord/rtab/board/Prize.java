package tel.discord.rtab.board;

//Created by JerryEris <3

public enum Prize {
	//TROPHIES, woo winners
	S1TROPHY (   70000,"a replica of Vash's Season 1 trophy"),
	S2TROPHY (   60000,"a replica of Charles510's Season 2 trophy"),
	S3TROPHY (   54000,"a replica of Archstered's Season 3 trophy"),
	S4TROPHY (   52000,"a replica of Lavina's Season 4 trophy"),
	S5TROPHY (   44000,"a replica of DatFatCat137's Season 5 trophy"),
	S6TROPHY (   36000,"a replica of GamerCrazy's Season 6 trophy"),
	S7TROPHY (   56000,"a replica of Lulu's Mystic Sailboat's Season 7 trophy"),
	S8TROPHY (   48000,"a replica of CouponBoy5's Season 8 trophy"),
	S9TROPHY (  630000,"a replica of CouponBoy5's Lucky Season 9 trophy"),
	S10TROPHY(   28000,"a replica of LA_Bud's Season 10 trophy"),
	S11TROPHY(  110000,"a replica of vincentw's Season 11 trophy"),
	
	//(Ir)Regular prizes
	JOKERSIKE	(       1,"a fake Joker"), //RtaB meme
	ZONK		(     123,"a Zonk"), //Let's Make a Deal
	STCHARLES	(     140,"a trip to St. Charles Place"), //Monopoly
	EWW			(     144,"something gross"), //Twelve dozen = a gross
	SNOOKER		(     147,"a snooker table"), //Maximum Break in Snooker
	DARTBOARD	(     180,"a dartboard"), //3x Triple 20
	SMALLTHINGS	(     182,"All the Small Things"), //Song by Blink-182
	GOTOGO		(     200,"Advance to GO"), //Monopoly
	VOWEL		(     250,"a vowel"), //Wheel of Fortune
	TWEET		(     280,"a Tweet"), //280 characters in a Tweet
	BOWLING		(     300,"a bowling ball"), //Maximum score in Bowling
	UNIFORCES	(     333,"the Triforce!!! Wait, no, that's just three Monoforces"), //Try again
	FLOKATI		(     350,"a Flokati Rug"), //Press Your Luck joke prize
	SLOTJACKPOT	(     777,"the JACKPOT... on a slot machine"), //7-7-7 is a traditionally high-value line on a slot machine
	PEANUTS		(    1000,"a bucket of peanuts"), //Blaseball
	ROCKEFELLER	(    1273,"a trip down Rockefeller Street"), //Eurovision song that became an osu! meme
	BIGJON		(    1906,"the BigJon special"), //BigJon
	BOARDWALK	(    2000,"rent at the Boardwalk Hotel"), //Monopoly (Three cheap prizes, who'd have thought)
	ATARI		(    2600,"a wood-paneled Atari system"), //The Atari VCS, also known as the Atari 2600
	FREELIVES	(    9900,"99 free lives! ...in Mario"), //100 coins for an extra life
	BUTTSPIE	(   18000,"a slice of Butterscotch Pie"), //Undertale
	FEUD		(   20000,"Fast Money"), //Family Feud
	BIFORCE		(   22222,"The Biforce"), //Still haven't found the Triforce, huh? Better keep looking!
	HUNTER		(   22475,"Superportal codes"), //Spyro 2
	DB1	 		(   22805,"a DesertBuck"), //Desert Bus for Hope
	GOVERNOR	(   26000,"the Governor's favourite"), //Deal or No Deal UK
	WEIRDAL		(   27000,"Weird Al's accordion"), //Weird Al
	ECONSTANT	(   27183,"some e"), //Maths
	PI			(   31416,"a fresh pi"), //Maths
	GOLDWII		(   36000,"a gold-plated Nintendo Wii"), //Once owned by Queen Elizabeth II
	ROADSTER	(   50000,"a sleek 1999 roadster"), //Smash TV
	NORMALCD	(   54321,"a Normal Countdown"), //Not a Final Countdown
	SPREAD		(   57300,"the Spread Shot"), //Konami, who created Contra, is 573 in Goroawase wordplay
	QUESTION	(   64000,"the famous question"), //The $64,000 Question
	SIXTEENBIT	(   65536,"a 16-bit trophy"), //2^16
	DISCORD		(   70013,"a Discord lamp"), //My Little Pony
	POWERPELLET	(   76500,"a Power Pellet"), //Namco, who created Pacman, is 765 in Goroawase wordplay
	SECONDS		(   86400,"a dollar a second"), //For a day
	GRANDPIANO	(   88000,"a grand piano"), //Number of keys on a piano
	HUNDREDG	(  100000,"a 100 Grand bar"), //Candy
	PYRAMID		(  100000,"The Pyramid"), //The $100,000 Pyramid
	LARSON		(  110237,"The Michael Larson special"), //Winnings of the man who exploited Press Your Luck
	VINTTWEET	(  140000,"a vintage Tweet"), //There used to be 140 characters in a Tweet
	PIZZABOX	(  458000,"an infinitely regenerating pizza box"); //SCP-458
    
    private final int prizeValue;
    private final String prizeName;

    Prize(int theValue, String theName) {
        this.prizeValue = theValue;
        this.prizeName = theName;
    }

    public String getPrizeName() {
        return prizeName;
    }

    public int getPrizeValue() {
        return prizeValue;
    }
}
