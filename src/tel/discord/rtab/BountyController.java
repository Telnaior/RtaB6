package tel.discord.rtab;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONTokener;

public class BountyController
{
	static final int MIN_BOUNTY_SCORE = 20; //how interesting you need to be before we actually care
	static final int BOUNTY_PER_POINT = 5000; //how much cash your head is worth per point of bounty score
	static final int BOUNTY_PLAYER_RATIO = 4; //max of 1/n players will be bountied each game
	String channelID;
	int baseNumerator, baseDenominator;
	JSONObject bounties;
	
	BountyController(String channelID, int baseNumerator, int baseDenominator)
	{
		this.channelID = channelID;
		this.baseNumerator = baseNumerator;
		this.baseDenominator = baseDenominator;
		//Load!
		try
		{
			bounties = new JSONObject(new JSONTokener(Files.newInputStream(Paths.get("scores","bounties"+channelID+".json"))));
		}
		catch(IOException e)
		{
			//If we can't load it then it's probably just the first game, so leave everything as default
			bounties = new JSONObject();
		}
	}
	
	void carryOverBounties(List<Player> players)
	{
		//Add carry-over bounties to current bounty totals
		for(Player next : players)
		{
			next.bounty += bounties.optInt(next.uID);
			bounties.remove(next.uID);
		}
	}
	
	void saveData(List<Player> players)
	{
		//Add the leftover player bounties to the savedata
		for(Player next : players)
			if(next.bounty > 0)
				bounties.put(next.uID, next.bounty);
		try
		{
			bounties.write(new FileWriter(Paths.get("scores","bounties"+channelID+".json").toFile()), 4, 0).close();
		}
		catch (Exception e)
		{
			System.out.println("SAVE FAILED");
			e.printStackTrace();
		}
	}
}
