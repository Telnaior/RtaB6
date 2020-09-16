package tel.discord.rtab.games;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public enum Jackpots
{
	BOWSER		(0),
	SUPERCASH	(10_000_000),
	DIGITAL		(25_000_000);
	
	public int resetValue;
	Jackpots(int base)
	{
		resetValue = base;
	}
	
	public int getJackpot(String channelID)
	{
		//Find the savefile
		List<String> list;
		try
		{
			list = Files.readAllLines(Paths.get("scores","jackpots"+channelID+".csv"));
		}
		catch(IOException e)
		{
			System.out.println("No jackpot file found for "+channelID+", creating.");
			list = new LinkedList<String>();
			try
			{
				Files.createFile(Paths.get("scores","jackpots"+channelID+".csv"));
			}
			catch(IOException e1)
			{
				System.out.println("Couldn't create it either. oops.");
				e1.printStackTrace();
				return resetValue;
			}
		}
		//Then search it for the relevant jackpot
		for(String next : list)
		{
			String[] record = next.split("#");
			if(record[0].equals(this.toString()))
			{
				int jackpot = Integer.parseInt(record[1]);
				return Math.max(jackpot, resetValue);
			}
		}
		//If we didn't find it, return the default value
		return resetValue;
	}
	
	public void setJackpot(String channelID, int value)
	{
		try
		{
			LinkedList<String> list = new LinkedList<String>();
			list.addAll(Files.readAllLines(Paths.get("scores","jackpots"+channelID+".csv")));
			Path file = Paths.get("jackpots"+channelID+".csv");
			Path oldFile = Files.move(file, file.resolveSibling("jackpots"+channelID+"old.csv"));
			//Find the relevant jackpot in the list and update its value
			boolean foundJackpot = false;
			ListIterator<String> iterator = list.listIterator();
			while(iterator.hasNext())
			{
				String[] record = iterator.next().split("#");
				if(record[0].equals(this.toString()))
				{
					foundJackpot = true;
					iterator.set(this.toString() + "#" + value);
				}
			}
			//If we didn't find it in the list, add it as a new line
			if(!foundJackpot)
				list.add(this.toString() + "#" + value);
			Files.write(file, list);
			Files.delete(oldFile);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void resetJackpot(String channelID)
	{
		setJackpot(channelID, resetValue);
	}
}
