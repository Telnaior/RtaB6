package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class TopCommand extends Command {
    public TopCommand()
    {
        this.name = "top";
		this.aliases = new String[]{"leaderboard"};
        this.help = "view ten players on the leaderboard (top ten by default, or give page number)";
    }
	@Override
	protected void execute(CommandEvent event) {
		try {
			List<String> list = Files.readAllLines(Paths.get("scores"+event.getChannel().getId()+".csv"));
			StringBuilder response = new StringBuilder().append("```\n");
			String[] record;
			int offset = 0;
			try
			{
				//If this doesn't throw an exception we're good
				offset = Math.max(Integer.parseInt(event.getArgs()) - 1,0);
			}
			catch(NumberFormatException e1)
			{
				//We can swallow this, it's fine, just let it default to top ten
			}
			int money, moneyLength = 0;
			//Get top 10, or fewer if list isn't long enough
			for(int i=10*offset; i<Math.min(list.size(),(10*offset)+10); i++)
			{
				/*
				 * record format:
				 * record[0] = uID
				 * record[1] = name
				 * record[2] = money
				 */
				record = list.get(i).split("#");
				money = Integer.parseInt(record[2]);
				//Get the length to format all values to
				if(i%10 == 0)
				{
					moneyLength = String.valueOf(money).length();
					moneyLength += (moneyLength-1)/3;
				}
				response.append("#" + String.format("%03d",(i+1)) + ": $"); 
				response.append(String.format("%,"+moneyLength+"d",money));
				response.append(" -" + (record[0].startsWith("-")?"*":" ") + record[1] + "\n");
			}
			response.append("```");
			event.reply(response.toString());
		}
		catch (IOException e)
		{
			event.reply("This command must be used in a game channel.");
		}
	}

}
