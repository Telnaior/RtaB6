package tel.discord.rtab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class GameBot
{
	String botID;
	String name;
	String humanID;
	
	GameBot(String guildID, int botNumber) throws IOException
	{
		//Get this guild's botlist
		List<String> list;
		try
		{
			list = Files.readAllLines(Paths.get("guilds","bots"+guildID+".csv"));
		}
		catch(IOException e)
		{
			list = Files.readAllLines(Paths.get("guilds","botsdefault.csv"));
		}
		//Get the relevant bot
		String[] record = list.get(botNumber).split("#");
		botID = record[0];
		name = record[1];
		humanID = record[2];
	}
}
