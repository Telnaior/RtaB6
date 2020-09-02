package tel.discord.rtab.commands;

import java.util.List;

import com.jagrosh.jdautilities.command.Command;

abstract class ParsingCommand extends Command {
	int findUserInList(List<String> list, String userID, boolean searchByName)
	{
		int field;
		if(searchByName)
			field = 1;
		else
			field = 0;
		/*
		 * record format:
		 * record[0] = uID
		 * record[1] = name
		 */
		String[] record;
		for(int i=0; i<list.size(); i++)
		{
			record = list.get(i).split("#");
			if(record[field].compareToIgnoreCase(userID) == 0)
				return i;
		}
		return -1;
	}
	//Just strip out all non-digit characters
	String parseMention(String mention)
	{
		return mention.replaceAll("\\D","");
	}
}
