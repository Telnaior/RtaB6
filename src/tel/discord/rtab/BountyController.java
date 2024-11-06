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
	
	void assignBounties(List<Player> players)
	{
		int totalBountyPool = applyBaseMultiplier(GameController.BOMB_PENALTY/2*players.size());
		boolean[] playersDone = new boolean[players.size()];
		int bountiesPlaced = 0;
		//We want to keep going until we've assigned enough bounties
		while(bountiesPlaced * BOUNTY_PLAYER_RATIO < players.size())
		{
			
			int maxPlayer = -1;
			int maxScore = 0;
			//Bounty score is equal to max(winstreak,booster) x (cash/100m+1) 
			for(int i=0; i<players.size(); i++)
			{
				if(playersDone[i])
					continue; //If someone's already had their bounty added, they don't get another one
				int score = Math.max(players.get(i).winstreak, players.get(i).booster/10);
				score *= 1+(players.get(i).currentCashClub);
				if(score > maxScore)
				{
					maxScore = score;
					maxPlayer = i;
				}
			}
			//If no one has enough points, don't even bother
			if(maxScore < MIN_BOUNTY_SCORE)
				break;
			//Assign a bounty to the top-scoring player
			int bounty = (int) Math.min(100_000_000, //cap bounty size even with stupid multiplier
					applyBaseMultiplier(maxScore*BOUNTY_PER_POINT));
			//If this isn't the first bounty and we already spent the funds, just save our cash
			if(totalBountyPool < bounty && bountiesPlaced > 0)
				break;
			players.get(maxPlayer).bounty += bounty;
			playersDone[maxPlayer] = true;
			bountiesPlaced ++;
		}
		//Finish by adding on any carry-over bounties
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
	
	int applyBaseMultiplier(int amount)
	{
		return RtaBMath.applyBaseMultiplier(amount, baseNumerator, baseDenominator);
	}
}
