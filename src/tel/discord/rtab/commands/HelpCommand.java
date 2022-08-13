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
		switch (name) {
			case "" -> {
				helpList.append("Race to a Billion is a game where you pick spaces off a board to win cash, "
						+ "while trying not to hit your opponents' bombs.\n");
				helpList.append("Type `!start` or `!join` to get into a game. "
						+ "If you need to leave a game before it starts, type `!quit`.\n");
				helpList.append("Once the game starts, you'll need to DM the bot with a space to place a bomb on.\n");
				helpList.append("Your opponents will do the same, and then you'll take turns choosing spaces.\n");
				helpList.append("All actions have a time limit, so don't go AFK or bad things may happen!\n");
				helpList.append("If you pick a space with a bomb hidden behind it, "
						+ "you'll blow up and lose cash, your booster, and your streak bonus, then be ejected from the game.\n");
				helpList.append("The last player standing wins the game, and earns a cash bonus for their efforts.\n");
				helpList.append("Your total cash bank builds up from round to round, "
						+ "and the objective is to be the first to reach one billion dollars.\n");
				helpList.append("To see a list of help files, type `!help list`.\n");
				helpList.append("The home server for Race to a Billion can be found here: https://discord.gg/QzQP9n4D6r");
			}
			case "spaces" -> {
				helpList.append("Most spaces on the gameboard are cash. If you find one, the money is added to your total bank.\n");
				helpList.append("Other spaces hide boosters - these apply a multiplier to all the cash you win.\n");
				helpList.append("There are also minigames on the board. "
						+ "If you find one, you must win the game to be able to play it.\n");
				helpList.append("These give you the chance to win some extra cash, so try your best.\n");
				helpList.append("Finally, there are events hidden on the board that can trigger all sorts of special things.\n");
				helpList.append("Events are unpredictable and could help you or hurt you, so good luck!");
			}
			case "peek", "peeks" -> {
				helpList.append("At the beginning of each game, you have one PEEK to use.\n");
				helpList.append("You may use it at any time during the game by typing `!peek` in the game channel, "
						+ "followed by the space you wish to peek at.\n");
				helpList.append("For example, to peek at space 13, type `!peek 13`.\n");
				helpList.append("You will be privately told what TYPE of space it is, but not its exact contents.\n");
				helpList.append("Use this information wisely during your game!\n");
			}
			case "boost", "booster" -> {
				helpList.append("You have a 'booster', which defaults to 100% and is displayed next to your score.\n");
				helpList.append("Some spaces on the board can add or subtract from this booster.\n");
				helpList.append("Most of the money you earn and lose will be multiplied by your current booster.\n");
				helpList.append("This includes the bomb penalty, so watch out!\n");
				helpList.append("The maximum booster possible is 999%, and the minimum is 10%. "
						+ "If your booster goes past the cap in either direction, the excess is converted into cash.\n");
				helpList.append("Boosters carry over between games, but will (usually) be reset when you hit a bomb.\n");
			}
			case "streak" -> {
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
			}
			case "newbie" -> {
				helpList.append("For your first ten games in the season, you are considered to be under newbie protection.\n");
				helpList.append("During this time, your bomb penalties are reduced to 40% of what they would otherwise be.\n");
				helpList.append("In addition, you do not lose lives while under newbie protection, "
						+ "so you can play freely while learning about the game.");
			}
			case "lives" -> {
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
			}
			case "bet", "betting" -> {
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
			}
			case "enhance" -> {
				helpList.append("As you progress through a season, you will occasionally unlock enhancement slots.\n");
				helpList.append("These slots are earned based on how many lives you spend. You will get your first one after roughly a week of play.\n");
				helpList.append("These enhancement slots can be used to enhance minigames! Enhancing a minigame gives you an advantage when you play it.\n");
				helpList.append("Enhanced minigames are also more likely to show up from the bonus bag, market, and minigames for all!\n");
				helpList.append("To see your progress toward earning enhancement slots, type !enhance in the game channel.\n");
				helpList.append("Note that once a minigame has been enhanced, this choice is permanent and only resets between seasons.\n");
			}
			case "custom" -> {
				helpList.append("To add the bot to your own server, follow this link:\n");
				helpList.append("<https://discord.com/api/oauth2/authorize?client_id=466545049598165014&permissions=268815424&scope=bot>\n");
				helpList.append("The following commands can then be used to set up the bot as needed:\n");
				helpList.append("```\n");
				helpList.append("!addchannel     - Tells the bot to prepare this channel as a game channel\n");
				helpList.append("!listchannels   - Lists all game channels registered for the server\n");
				helpList.append("!modifychannel  - Allows you to configure the settings for the channel (type !help channelsettings for info here)\n");
				helpList.append("!enablechannel  - Opens the channel up for games to be played\n");
				helpList.append("!disablechannel - Stops further games from being played (use this if you need to adjust the settings)\n");
				helpList.append("!resetseason    - Resets everyone to $0, DELETING all scores and clearing the leaderboard\n");
				helpList.append("!archiveseason  - Saves a completed season to the history log and resets everyone to $0\n");
				helpList.append("```\n");
				helpList.append("For further support, visit #branch-server-support in the RtaB home server.");
			}
			case "channelsettings", "settings" -> {
				helpList.append("```\n");
				helpList.append("ResultChannel: Enter the ID of a channel and the bot will copy end-game result messages to that channel\n");
				helpList.append("                 (This works like #result-log in the home server)\n\n");
				helpList.append("BaseMultiplier: Enter a fraction and all cash won and lost in the game will be multiplied by this value\n");
				helpList.append("                 (Use this to speed up or slow down how long it will take to reach a billion)\n\n");
				helpList.append("BotCount: Enter a number from 0 to 80 to control how many AI players will be enabled\n");
				helpList.append("                 (Setting this to 0 disables AI players altogether)\n\n");
				helpList.append("DemoTime: Enter a number of minutes and a demo game will run after this amount of inactivity in the game channel\n");
				helpList.append("                 (Setting this to 0 disables demo games)\n\n");
				helpList.append("MinPlayers: The minimum number of players that must be signed up in each round\n");
				helpList.append("                 (The game will offer to add AI players to reach this number if AI players are enabled)\n\n");
				helpList.append("MaxPlayers: The maximum number of players that can be part of a single round\n");
				helpList.append("                 (This cannot be set higher than the default 16)\n\n");
				helpList.append("MaxLives: The number of lives each player receives per day (20 hours)\n");
				helpList.append("                 (Setting this to 0 forces everyone to pay a life penalty every game)\n\n");
				helpList.append("LifePenalty: 0 = no life penalty (infinite lives), 1 = flat $1m life penalty, 2 = penalty scales to current score,\n");
				helpList.append("                 3 = scaling penalty that increases with additional lives lost, 4 = cannot play when out of lives\n\n");
				helpList.append("VerboseBotMinigames: Setting this to true will display AI player minigames in full\n");
				helpList.append("                 (This can take a while if an AI player wins a lot of minigames)\n\n");
				helpList.append("DoBonusGames: Setting this to false will disable earning bonus games via winstreak multipliers\n");
				helpList.append("                 (This means no Supercash, Digital Fortress, etc)\n\n");
				helpList.append("CountsToPlayerLevel: Setting this to true will enable the player level / achievement system for this channel\n");
				helpList.append("                 (Enabling this for multiple channels will sum their totals together for level purposes)\n");
				helpList.append("```");
			}
			case "list", default -> {
				helpList.append("```\n");
				helpList.append("!commands     - View a full list of commands\n");
				helpList.append("!help         - Explains the basics of the game\n");
				helpList.append("!help spaces  - Explains the types of spaces on the board\n");
				helpList.append("!help peek    - Explains how to use peeks in-game\n");
				helpList.append("!help boost   - Explains the booster mechanics\n");
				helpList.append("!help streak  - Explains the streak bonus multiplier\n");
				helpList.append("!help newbie  - Explains newbie protection\n");
				helpList.append("!help lives   - Explains the life system\n");
				helpList.append("!help enhance - Explains how to enhance your favourite minigames\n");
				helpList.append("!help custom  - Explains how to add the bot to your own server\n");
				helpList.append("!help channelsettings - Describes the channel settings available under !modifychannel\n");
				//helpList.append("!help bet    - Explains the betting system in the Super Bot Challenge\n");
				helpList.append("```");
			}
		}
		event.replyInDm(helpList.toString());
	}
}