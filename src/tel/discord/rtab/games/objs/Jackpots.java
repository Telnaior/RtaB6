package tel.discord.rtab.games.objs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;

public enum Jackpots
{
	BOWSER		(0),
	SUPERCASH	(10_000_000),
	DIGITAL		(25_000_000),
	CYS_GOLD	(4_000_000);
	
	public final int resetValue;
	Jackpots(int base)
	{
		resetValue = base;
	}
	
	public int getJackpot(MessageChannel channel)
	{
		//If it's a private channel, jackpots don't accumulate
		if(channel.getType() == ChannelType.PRIVATE)
			return resetValue;
		//Find the savefile
		List<String> list;
		try
		{
			list = Files.readAllLines(Paths.get("scores","jackpots"+channel.getId()+".csv"));
		}
		catch(IOException e)
		{
			System.out.println("No jackpot file found for "+channel.getId()+", creating.");
			list = new LinkedList<>();
			try
			{
				Files.createFile(Paths.get("scores","jackpots"+channel.getId()+".csv"));
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
	
	//Shortcut for when we don't know the value but we do know how much to increment by
	public void addToJackpot(MessageChannel channel, int value)
	{
		setJackpot(channel, getJackpot(channel)+value);
	}
	
	public void setJackpot(MessageChannel channel, int value)
	{
		//No progressives in private channels
		if(channel.getType() == ChannelType.PRIVATE)
			return;
		try
		{
			LinkedList<String> list = new LinkedList<>();
			Path file = Paths.get("scores","jackpots"+channel.getId()+".csv");
			list.addAll(Files.readAllLines(file));
			//Find the relevant jackpot in the list and update its value
			boolean foundJackpot = false;
			ListIterator<String> iterator = list.listIterator();
			while(iterator.hasNext())
			{
				String[] record = iterator.next().split("#");
				if(record[0].equals(this.toString()))
				{
					foundJackpot = true;
					iterator.set(this + "#" + value);
				}
			}
			//If we didn't find it in the list, add it as a new line
			if(!foundJackpot)
				list.add(this + "#" + value);
			Path oldFile = Files.move(file, file.resolveSibling("jackpots"+channel.getId()+"old.csv"));
			Files.write(file, list);
			Files.delete(oldFile);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void resetJackpot(MessageChannel channel)
	{
		setJackpot(channel, resetValue);
	}
}
