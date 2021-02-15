package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class PlayerLevel
{
	String guildID, playerID, name;
	int playerLevel, championLevel, achievementLevel, recordRow;
	long playerXP, champXP;
	
	public PlayerLevel(String guildID, String playerID, String name)
	{
		this.guildID = guildID;
		this.playerID = playerID;
		this.name = name;
		//Default values
		playerLevel = 0;
		championLevel = 0;
		achievementLevel = 0;
		playerXP = 0;
		champXP = 0;
		recordRow = -1;
		//Try and load their current levels
		List<String> list;
		try
		{
			list = Files.readAllLines(Paths.get("levels","levels"+guildID+".csv"));
		}
		catch(IOException e)
		{
			System.out.println("No savefile found for "+guildID+", creating.");
			list = new LinkedList<String>();
			try
			{
				Files.createFile(Paths.get("levels","levels"+guildID+".csv"));
			}
			catch (IOException e1)
			{
				System.err.println("Couldn't create it either. Oops.");
				e1.printStackTrace();
				return;
			}
		}
		String[] record;
		for(int i=0; i<list.size(); i++)
		{
			/*
			 * record format:
			 * record[0] = uID
			 * record[1] = name
			 * record[2] = player level
			 * record[3] = player level xp
			 * record[4] = champion level
			 * record[5] = champion level xp
			 * record[6] = achievement level
			 */
			record = list.get(i).split("#");
			if(record[0].equals(playerID))
			{
				recordRow = i;
				playerLevel = Integer.parseInt(record[2]);
				playerXP = Integer.parseInt(record[3]);
				championLevel = Integer.parseInt(record[4]);
				champXP = Integer.parseInt(record[5]);
				achievementLevel = Integer.parseInt(record[6]);
				break; //We found the player we're done here
			}
		}
	}
	
	public boolean saveLevel()
	{
		StringBuilder toPrint = new StringBuilder();
		toPrint.append(playerID);
		toPrint.append("#"+name);
		toPrint.append("#"+playerLevel);
		toPrint.append("#"+playerXP);
		toPrint.append("#"+championLevel);
		toPrint.append("#"+champXP);
		toPrint.append("#"+achievementLevel);
		try
		{
			Path file = Paths.get("levels","levels"+guildID+".csv");
			Path oldFile = Files.move(file, file.resolveSibling("levels"+guildID+"old.csv"));
			List<String> list = Files.readAllLines(file);
			if(recordRow == -1)
				list.add(toPrint.toString());
			else
				list.set(recordRow, toPrint.toString());
			Files.write(file, list);
			Files.delete(oldFile);
			return true;
		}
		catch(IOException e)
		{
			System.err.println("Could not save level data.");
			return false;
		}
	}
	
	//Getters
	public int getTotalLevel()
	{
		return playerLevel + championLevel + achievementLevel;
	}
	public int getPlayerLevel()
	{
		return playerLevel;
	}
	public int getChampLevel()
	{
		return championLevel;
	}
	public int getAchievementLevel()
	{
		return achievementLevel;
	}
	public long getPlayerXP()
	{
		return playerXP;
	}
	public long getChampXP()
	{
		return champXP;
	}
	
	public boolean setXP(long newXP)
	{
		int oldLevel = playerLevel;
		playerLevel = 0;
		playerXP = 0;
		addXP(newXP);
		return oldLevel > playerLevel;
	}
	public boolean addXP(long addedXP)
	{
		playerXP += addedXP;
		return checkLevelUp();
	}
	boolean checkLevelUp()
	{
		boolean increasedLevel = false;
		while(playerXP >= getRequiredXP())
		{
			playerXP -= getRequiredXP();
			playerLevel += 1;
			increasedLevel = true;
		}
		return increasedLevel;
	}
	public long getRequiredXP()
	{
		int newLevel = playerLevel+1;
		return 5_000_000 * newLevel;
	}
	
	//total XP requirement is $1b x (new level)^3
	public boolean setChampXP(long newXP)
	{
		int oldLevel = championLevel;
		championLevel = 0;
		champXP = 0;
		addChampXP(newXP);
		return oldLevel > championLevel;
	}
	public boolean addChampXP(long addedXP)
	{
		champXP += addedXP;
		return checkChampLevelUp();
	}
	boolean checkChampLevelUp()
	{
		boolean increasedLevel = false;
		while(champXP >= getRequiredChampXP())
		{
			champXP -= getRequiredChampXP();
			championLevel += 1;
			increasedLevel = true;
		}
		return increasedLevel;
	}
	public long getRequiredChampXP()
	{
		int newLevel = championLevel+1;
		return (3_000_000_000L * (long)Math.pow(newLevel,2)) - (3_000_000_000L * newLevel) + 1_000_000_000L;
	}
}
