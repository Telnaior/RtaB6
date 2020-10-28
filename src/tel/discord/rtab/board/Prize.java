package tel.discord.rtab.board;

//Created by JerryEris <3

public enum Prize {
	//TROPHIES, woo winners
	S1TROPHY (   70000,"a replica of Vash's Season 1 trophy"),
	S2TROPHY (   60000,"a replica of Charles510's Season 2 trophy"),
	S3TROPHY (   54000,"a replica of Archstered's Season 3 trophy"),
	S4TROPHY (   52000,"a replica of Lavina's Season 4 trophy"),
	S5TROPHY (   44000,"a replica of DatFatCat137's Season 5 trophy"),
	
	//(Ir)Regular prizes
	DB1	 	(   22805,"a DesertBuck"),
	GOVERNOR	(   26000,"the Governor's favourite"),
	PI		(   31416,"a fresh pi"),
	ECONSTANT	(   27183,"some e"),
	VOWEL		(     250,"a vowel"),
	QUESTION	(   64000,"the famous question"),
	BIGJON		(    1906,"the BigJon special"),	
	WEIRDAL		(   27000,"Weird Al's accordion"),
	ROCKEFELLER	(    1273,"a trip down Rockefeller Street"),
	EWW		(     144,"something gross"),
	HUNDREDG	(  100000,"a 100 Grand bar"),
	HUNTER		(   22475,"Superportal codes"), //Aaron and Atia
	FEUD		(   20000,"Fast Money"), //MattR
	JOKERSIKE	(       1,"a fake Joker"), //Lavina
	STCHARLES	(     140,"a trip to St. Charles Place"), //MattR
	SNOOKER		(     147,"a snooker table"), //KP
	DARTBOARD	(     180,"a dartboard"), //KP
	GRANDPIANO	(   52000,"a grand piano"), //Jumble
	GOTOGO		(     200,"Advance to GO"),
	NORMALCD	(   54321,"a Normal Countdown"),
	ZONK		(     123,"a Zonk"),
	FREELIVES	(    9900,"99 free lives! ...in Mario"),
	BUTTSPIE	(   18000,"a slice of Butterscotch Pie"),
	DISCORD		(   70013,"a Discord lamp"),
	PYRAMID		(  100000,"The Pyramid"),
	BIFORCE		(   22222,"The Biforce"),
	SMALLTHINGS	(     182,"All the Small Things"),
	LARSON		(  110237,"The Michael Larson special"), //MattR and SC
	BOARDWALK	(    2000,"rent at the Boardwalk Hotel"), //SC
	SECONDS		(   86400,"a dollar a second"),
	POWERPELLET	(   76500,"a Power Pellet"),
	SPREAD		(   57300,"the Spread Shot"),
	FLOKATI		(     350,"a Flokati Rug"),
	PEANUTS		(    1000,"a bucket of peanuts"),
	SIXTEENBIT	(   65536,"a 16-bit trophy"),
	ROADSTER	(   50000,"a sleek 1999 roadster"),
	BOWLING		(     300,"a bowling ball");
    
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
