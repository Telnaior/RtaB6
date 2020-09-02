package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class StatsCommand extends Command {
    public StatsCommand()
    {
        this.name = "stats";
        this.help = "view stats about the leaderboard";
    }
	@Override
	protected void execute(CommandEvent event) {
		try
		{
			List<String> list = Files.readAllLines(Paths.get("scores"+event.getChannel().getId()+".csv"));
			String[] record;
			boolean isBot;
			int money;
			long totalMoney = 0;
			int totalCount = 0;
			long humanMoney = 0;
			int humanCount = 0;
			long botMoney = 0;
			int botCount = 0;
			int[] moneyTable = new int[11];
			//Get top 10, or fewer if list isn't long enough
			for(String nextEntry : list)
			{
				/*
				 * record format:
				 * record[0] = uID
				 * record[1] = name
				 * record[2] = money
				 */
				record = nextEntry.split("#");
				isBot = (record[0].startsWith("-"));
				money = Integer.parseInt(record[2]);
				totalMoney += money;
				totalCount ++;
				//Split based on if they're a bot or not
				if(isBot)
				{
					botMoney += money;
					botCount ++;
				}
				else
				{
					humanMoney += money;
					humanCount ++;
				}
				moneyTable[money/100000000] ++;
			}
			StringBuilder response = new StringBuilder().append("```\n");
			response.append(String.format("Total Money: $%,14d\n",totalMoney));
			if(humanCount != 0 && botCount != 0)
			{
				response.append(String.format("Human Total: $%,14d\n",humanMoney));
				response.append(String.format("  Bot Total: $%,14d\n",botMoney));
				response.append("\n");
			}
			if(totalCount != 0)
				response.append(String.format("Average Money:  $%,11d\n",totalMoney/totalCount));
			if(humanCount != 0 && botCount != 0)
			{
				response.append(String.format("Human Average:  $%,11d\n",humanMoney/humanCount));
				response.append(String.format("  Bot Average:  $%,11d\n",botMoney/botCount));
			}
			//New line to split apart the club amounts
			response.append("\n");
			response.append("Cash Clubs:\n");
			for(int i=10; i>=0; i--)
			{
				//Add the block above to it
				if(i<10)
					moneyTable[i] += moneyTable[i+1];
				//If it's unique, print it
				if(moneyTable[i] > 0 && (i==10 || moneyTable[i] > moneyTable[i+1]))
				{
					if(i==10)
						response.append(String.format("$  1B : %2$3d\n",i,moneyTable[i]));
					else if(i==0)
						response.append(String.format("Total : %2$3d\n",i,moneyTable[i]));
					else
						response.append(String.format("$%1$d00M+: %2$3d\n",i,moneyTable[i]));
				}
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