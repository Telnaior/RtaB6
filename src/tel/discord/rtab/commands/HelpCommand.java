package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class HelpCommand extends Command
{
    public HelpCommand()
    {
        this.name = "help";
        this.help = "learn about the rules of the game";
        this.guildOnly = false;
    }
    
	@Override
	protected void execute(CommandEvent event)
	{
		String name = event.getArgs();
		StringBuilder helpList = new StringBuilder();
		switch (name)
		{
		case "":
			helpList.append("Race to a Billion is a game where you pick spaces off a board to win cash, "
					+ "while trying not to hit your opponents' bombs.\n");
			helpList.append("Type !start or !join to get into a game. "
					+ "If you need to leave a game before it starts, type !quit.\n");
			helpList.append("Once the game starts, you'll need to DM the bot with a space to place a bomb on.\n");
			helpList.append("Your opponents will do the same, and then you'll take turns choosing spaces.\n");
			helpList.append("All actions have a time limit, so don't go AFK or bad things may happen!\n");
			helpList.append("If you pick a space with a bomb hidden behind it, "
					+ "you'll blow up and lose cash, your booster, and your streak bonus, then be ejected from the game.\n");
			helpList.append("The last player standing wins the game, and earns a cash bonus for their efforts.\n");
			helpList.append("Your total cash bank builds up from round to round, "
					+ "and the objective is to be the first to reach one billion dollars.\n");
			helpList.append("To see a list of help files, type '!help list'.\n");
			break;
		case "spaces":
			helpList.append("Most spaces on the gameboard are cash. If you find one, the money is added to your total bank.\n");
			helpList.append("Other spaces hide boosters - these apply a multiplier to all the cash you win.\n");
			helpList.append("There are also minigames on the board. "
					+ "If you find one, you must win the game to be able to play it.\n");
			helpList.append("These give you the chance to win some extra cash, so try your best.\n");
			helpList.append("Finally, there are events hidden on the board that can trigger all sorts of special things.\n");
			helpList.append("Events are unpredictable and could help you or hurt you, so good luck!");
			break;
		case "peek":
		case "peeks":
			helpList.append("At the beginning of each game, you have one PEEK to use.\n");
			helpList.append("You may use it at any time during the game by typing !peek in the game channel, "
					+ "followed by the space you wish to peek at.\n");
			helpList.append("For example, to peek at space 13, type '!peek 13'.\n");
			helpList.append("You will be privately told what TYPE of space it is, but not its exact contents.\n");
			helpList.append("Use this information wisely during your game!\n");
			break;
		case "boost":
		case "booster":
			helpList.append("You have a 'booster', which defaults to 100% and is displayed next to your score.\n");
			helpList.append("Some spaces on the board can add or subtract from this booster.\n");
			helpList.append("Most of the money you earn and lose will be multiplied by your current booster.\n");
			helpList.append("This includes the bomb penalty, so watch out!\n");
			helpList.append("The maximum booster possible is 999%, and the minimum is 10%. "
					+ "If your booster goes past the cap in either direction, the excess is converted into cash.\n");
			helpList.append("Boosters carry over between games, but will (usually) be reset when you hit a bomb.\n");
			break;
		case "streak":
			helpList.append("When you win a game, you earn a streak bonus. "
					+ "The amount won is determined by how many opponents you beat.\n");
			helpList.append("Each defeated opponent in around earns you +0.5 to your streak bonus.\n");
			helpList.append("However, if you share the victory with other players, the awarded amount can be reduced.\n");
			helpList.append("You also receive a win bonus. "
					+ "The base win bonus is $20,000 for every space picked during the game, plus any remaining bombs.\n");
			helpList.append("If the board was cleared entirely, the win bonus is doubled.\n");
			helpList.append("Finally, if there are multiple winners, the win bonus is shared between them.\n");
			helpList.append("Each player's share of the win bonus is then multiplied by their streak bonus.\n");
			helpList.append("Any minigames won also have the streak bonus multiplier applied to anything you win from them.\n");
			helpList.append("And if you get your streak bonus high enough, "
					+ "you will get to play special bonus games that can win you millions of dollars in one fell swoop.\n");
			helpList.append("However, when you lose a game, your streak bonus resets to x1. "
					+ "You have to keep winning if you want to keep it!");
			break;
		case "newbie":
			helpList.append("For your first ten games in the season, you are considered to be under newbie protection.\n");
			helpList.append("During this time, your bomb penalties are reduced to 40% of what they would otherwise be.\n");
			helpList.append("In addition, you do not lose lives while under newbie protection, "
					+ "so you can play freely while learning about the game.");
			break;
		case "lives":
			helpList.append("Once you are out of newbie protection, you will have a limited number of lives.\n");
			helpList.append("By default, you have five lives. Every time you blow up in a game, you lose a life.\n");
			helpList.append("You can check how many lives you have remaining with !lives. "
					+ "If you run out of lives you can keep playing, but every additional game will cost an entry fee.\n");
			helpList.append("The base entry fee is 1% of your total bank or $100,000, whichever is greater, "
					+ "and every additional life you lose in a day increases this fee by 20%.\n");
			helpList.append("But never fear! They'll all come back tomorrow. "
					+ "20 hours after you lose your first life, you'll be restocked to 5.\n");
			helpList.append("If you miss a day altogether, it'll give you an additional life above the base 5, "
					+ "so if you aren't a regular player it's likely you won't have to worry about lives at all.");
			break;
		case "bet":
		case "betting":
			helpList.append("The Super Bot Challenge is (mostly) just for the bots to play, "
					+ "but that doesn't mean there's nothing to do but watch.\n");
			helpList.append("The bot announces the players for each game five minutes before the game starts, "
					+ "and during that time you can place a bet on the player you think will win.\n");
			helpList.append("You start with 10,000 betting chips, "
					+ "and if you win you get 4x your bet plus any losing bets (shared between everyone who bet on the winner).\n");
			helpList.append("This means that on average you can win more than you lose, "
					+ "especially if more people bet on a single match!\n");
			helpList.append("If you run out of betting chips, you are limited to maximum bets of 1,000 until you win your way back in.\n");
			helpList.append("Betting chip totals will never be reset, so there's no rush on your quest to dominate.\n");
			helpList.append("Good luck with your betting, and enjoy this side mode!");
			break;
		case "list":
		default:
			helpList.append("```\n");
			helpList.append("!commands    - View a full list of commands\n");
			helpList.append("!help        - Explains the basics of the game\n");
			helpList.append("!help spaces - Explains the types of spaces on the board\n");
			helpList.append("!help peek   - Explains how to use peeks in-game\n");
			helpList.append("!help boost  - Explains the booster mechanics\n");
			helpList.append("!help streak - Explains the streak bonus multiplier\n");
			helpList.append("!help newbie - Explains newbie protection\n");
			helpList.append("!help lives  - Explains the life system\n");
			//helpList.append("!help bet    - Explains the betting system in the Super Bot Challenge\n");
			helpList.append("```");
			break;
		}
		event.replyInDm(helpList.toString());
	}
}