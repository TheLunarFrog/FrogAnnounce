package main.java.me.thelunarfrog.FrogAnnounce;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The FrogAnnounce core. Handles loops, grabbing configuration values from ConfigurationManager, commands, and all announcements. API will be found here, too.
 * 
 * @author Dan | TheLunarFrog
 * @version 2.0.10.13
 * @category main
 * 
 */
public class FrogAnnounce extends JavaPlugin{
	private PluginDescriptionFile pdfFile;
	protected FrogLog logger;
	public Permission permission = null;
	protected String tag, joinMessage;
	protected int interval, taskId = -1, counter = 0;
	protected boolean running = false, random, permissionsEnabled = false, toGroups, usingPerms, showJoinMessage = false, showConsoleAnnouncements = false;
	protected List<String> strings, Groups;
	protected ArrayList<String> ignoredPlayers = null;
	private ConfigurationHandler cfg = null;
	/** Static accessor */
	public static FrogAnnounce p;

	@Override
	public void onEnable(){
		FrogAnnounce.p = this;
		this.pdfFile = this.getDescription();
		this.logger = new FrogLog();
		this.cfg = new ConfigurationHandler(this);
		this.cfg.loadConfig();
		if(this.strings==null){
			this.strings = new ArrayList<String>();
			this.strings.add("This plugin may be improperly configured. Please ensure all announcements have matching quotation marks around them. See plugin help pages for more info.");
			this.interval = 5;
		}
		if(this.usingPerms)
			this.checkPermissionsVaultPlugins();
		if(this.showJoinMessage)
			super.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
		this.logger.info("Settings loaded "+this.strings.size()+" announcements!");
		this.running = this.turnOn(null);
		this.logger.info("Version "+this.pdfFile.getVersion()+" by TheLunarFrog has been enabled!");
	}

	@Override
	public void onDisable(){
		this.turnOff(true, null);
		this.logger.info("Version "+this.pdfFile.getVersion()+" has been disabled.");
	}

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(final ClassNotFoundException e){
			e.printStackTrace();
		}catch(final InstantiationException e){
			e.printStackTrace();
		}catch(final IllegalAccessException e){
			e.printStackTrace();
		}catch(final UnsupportedLookAndFeelException e){
			e.printStackTrace();
		}finally{
			JOptionPane.showMessageDialog(null, "Sorry, but FrogAnnounce is a Bukkit plugin, and cannot be run directly like you've attempted.\nTo use the plugin, download and set up a Bukkit Minecraft server, and in the root directory, create a folder called\n\"plugins\" (no quotes, and assuming it hasn't already been created for you), and put this JAR file (FrogAnnounce.jar) there.\nWhen you've done that, start the Bukkit server using the command line java -jar \"path to Bukkit.jar\",\nor if it's already running, type \"reload\" (no quotes) into the command-line.", "FrogAnnounce", JOptionPane.OK_OPTION);
			System.exit(0);
		}
	}

	private boolean permit(final CommandSender sender, final String perm){
		if(sender instanceof Player)
			if(sender.isOp())
				return true;
			else
				return sender.hasPermission(perm);
		else
			return true;
	}

	private void turnOff(final boolean disabled, final CommandSender player){
		if(this.running){
			this.getServer().getScheduler().cancelTask(this.taskId);
			this.sendMessage(player, 0, "Announcer disabled!");
			this.running = false;
		}else if(!disabled)
			this.sendMessage(player, 2, "The announcer is not running!");
	}

	private boolean turnOn(final CommandSender player){
		if(!this.running){
			if(this.strings.size()>0){
				this.taskId = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Announcer(this), this.interval*1200, this.interval*1200);
				if(this.taskId==-1){
					this.sendMessage(player, 2, "The announcer module has failed to start! Please check your configuration. If this does not fix it, then submit a support ticket on the BukkitDev page for FrogAnnounce.");
					return false;
				}else{
					this.counter = 0;
					this.sendMessage(player, 0, "Success! Now announcing every "+this.interval+" minute(s)!");
					return true;
				}
			}else{
				this.sendMessage(player, 2, "The announcer failed to start! There are no announcements!");
				return false;
			}
		}else{
			this.sendMessage(player, 2, ChatColor.DARK_RED+"Announcer is already running.");
			return true;
		}
	}

	private void reloadPlugin(final CommandSender player){
		if(this.running){
			this.turnOff(false, null);
			this.cfg.loadConfig();
			this.running = this.turnOn(player);
			this.sendMessage(player, 0, "FrogAnnounce has been successfully reloaded!");
			this.sendMessage(player, 0, "Settings loaded "+this.strings.size()+" announcements!");
		}else
			this.sendMessage(player, 2, "No announcements running!");
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String commandLabel, final String[] args){
		if(commandLabel.equalsIgnoreCase("fa")||commandLabel.equalsIgnoreCase("frogannounce")){
			if(this.permit(sender, "frogannounce.admin")||this.permit(sender, "frogannounce.*")){
				try{
					if(args.length==0){
						this.sendMessage(sender, 0, "FrogAnnounce version: "+this.pdfFile.getVersion());
						this.sendMessage(sender, 0, "For help, use /fa help.");
					}else if(args[0].equalsIgnoreCase("help")||args[0].equalsIgnoreCase("?")){
						if(args.length==2)
							this.returnHelp(sender, args[1]);
						else
							this.returnHelp(sender, "0");
					}else if(args[0].equalsIgnoreCase("on"))
						this.running = this.turnOn(sender);
					else if(args[0].equalsIgnoreCase("off"))
						this.turnOff(false, sender);
					else if(args[0].equalsIgnoreCase("version")||args[0].equalsIgnoreCase("v"))
						this.sendMessage(sender, 0, "Current version: "+this.pdfFile.getVersion());
					else if(args[0].equalsIgnoreCase("ignore")||args[0].equalsIgnoreCase("optout")||args[0].equalsIgnoreCase("opt-out")){
						if(args.length==2)
							this.ignorePlayer(sender, args[1]);
						else
							this.ignorePlayer(sender, sender.getName());
					}else if(args[0].equalsIgnoreCase("unignore")||args[0].equalsIgnoreCase("optin")||args[0].equalsIgnoreCase("opt-in")){
						if(args.length==2)
							this.unignorePlayer(sender, args[1]);
						else
							this.unignorePlayer(sender, sender.getName());
					}else if(args[0].equalsIgnoreCase("interval")||args[0].equalsIgnoreCase("int"))
						this.setInterval(args, sender);
					else if(args[0].equalsIgnoreCase("random")||args[0].equalsIgnoreCase("rand"))
						this.setRandom(args, sender);
					else if(args[0].equalsIgnoreCase("broadcast")||args[0].equalsIgnoreCase("bc"))
						this.broadcastMessage(args[1], sender);
					else if(args[0].equalsIgnoreCase("restart")||args[0].equalsIgnoreCase("reload")){
						this.reloadPlugin(sender);
						this.reloadConfig();
					}else if(args[0].equalsIgnoreCase("list")){
						this.sendMessage(sender, 0, "Loaded announcements:");
						for(final String s: this.strings)
							this.sendMessage(sender, 0, this.strings.indexOf(s)+". "+this.colourizeText(s));
					}else if(args[0].equalsIgnoreCase("add")){
						final StringBuilder sb = new StringBuilder();
						for(int i = 1; i<args.length; i++)
							sb.append(args[i]+" ");
						this.strings.add(sb.toString().trim());
						this.cfg.config.set("Announcer.Strings", this.strings);
						this.cfg.saveConfig();
						this.sendMessage(sender, 0, "Successfully added the announcement \""+sb.toString().trim()+"\" to the configuration. Reloading config...");
						this.reloadPlugin(sender);
					}else if(args[0].equalsIgnoreCase("manualbroadcast")||args[0].equalsIgnoreCase("mbc")){
						final StringBuilder sb = new StringBuilder();
						for(int i = 1; i<args.length; i++)
							sb.append(args[i]+" ");
						if(this.tag.isEmpty())
							this.getServer().broadcastMessage(this.colourizeText(sb.toString().trim()));
						else
							this.getServer().broadcastMessage(this.tag+" "+this.colourizeText(sb.toString().trim()));
					}else if(args[0].equalsIgnoreCase("remove")||args[0].equalsIgnoreCase("delete")||args[0].equalsIgnoreCase("rem")||args[0].equalsIgnoreCase("del")){
						int i = 0;
						if(args.length==2)
							try{
								i = Integer.parseInt(args[1]);
								try{
									this.sendMessage(sender, 0, "Removing announcement "+i+" ("+this.strings.get(i)+")...");
									this.strings.remove(i);
									this.cfg.config.set("Announcer.Strings", this.strings);
									this.cfg.saveConfig();
									this.sendMessage(sender, 0, "Announcement "+i+" successfully removed. Reloading configuration...");
									this.reloadPlugin(sender);
								}catch(final IndexOutOfBoundsException e){
									this.sendMessage(sender, 1, "Error: There are only "+this.strings.size()+" announcements. You must count from 0!");
								}
							}catch(final NumberFormatException e){
								this.sendMessage(sender, 1, "Please enter an announcement index.");
							}
						else
							this.sendMessage(sender, 1, "You must specify an index to remove.");
					}else{
						this.sendMessage(sender, 1, "That didn't seem like a valid command. Here's some help...");
						if(args.length==2)
							this.returnHelp(sender, args[1]);
						else
							this.returnHelp(sender, "0");
					}
				}catch(final ArrayIndexOutOfBoundsException e){
					return false;
				}
				return true;
			}else if(args.length>=1){
				if(args[0].equalsIgnoreCase("ignore")||args[0].equalsIgnoreCase("optout")||args[0].equalsIgnoreCase("opt-out")){
					if(args.length==2){
						if(this.permit(sender, "frogannounce.optout.other"))
							this.ignorePlayer(sender, args[1]);
						else
							this.sendMessage(sender, 1, "You don't have permission to access that command.");
					}else if(this.permit(sender, "frogannounce.optout"))
						this.ignorePlayer(sender, sender.getName());
					else
						this.sendMessage(sender, 1, "You don't have permission to access that command.");
					return true;
				}else if(args[0].equalsIgnoreCase("unignore")||args[0].equalsIgnoreCase("optin")||args[0].equalsIgnoreCase("opt-in")){
					if(args.length==2){
						if(this.permit(sender, "frogannounce.optin.other"))
							this.unignorePlayer(sender, args[1]);
						else
							this.sendMessage(sender, 1, "You don't have permission to access that command.");
					}else if(this.permit(sender, "frogannounce.optin"))
						this.unignorePlayer(sender, sender.getName());
					else
						this.sendMessage(sender, 1, "You don't have permission to access that command.");
					return true;
				}
			}else
				this.sendMessage(sender, 1, ChatColor.RED+"Sorry, but you don't have access to that command.");
			return true;
		}
		return false;
	}

	public void returnHelp(final CommandSender sender, final String pageString){
		final String or = ChatColor.WHITE.toString()+"|";
		final String heading = ChatColor.DARK_GREEN.toString();
		final String main = ChatColor.GOLD.toString();
		final String command = ChatColor.AQUA.toString();
		final String obligatory = ChatColor.DARK_RED.toString();
		final String optional = ChatColor.GRAY.toString();
		try{
			int page;
			page = Integer.parseInt(pageString);
			if(page==1||page==0){
				this.sendMessage(sender, 0, main+"*"+heading+"Help for FrogAnnounce "+this.pdfFile.getVersion()+" (1/3)"+main+"*");
				this.sendMessage(sender, 0, command+"/fa <help"+or+command+"?>"+main+" - Show this message.");
				this.sendMessage(sender, 0, command+"/fa <on"+or+command+"off>"+main+" - Start or stop FrogAnnounce.");
				this.sendMessage(sender, 0, command+"/fa <restart"+or+command+"reload>"+main+" - Restart FrogAnnounce.");
				this.sendMessage(sender, 0, command+"/fa <interval"+or+command+"int>"+obligatory+" <minutes>"+main+" - Set the time between each announcement.");
				this.sendMessage(sender, 0, command+"/fa <random"+or+command+"rand>"+obligatory+" <on"+or+obligatory+"off>"+main+" - Set random or consecutive.");
				this.sendMessage(sender, 0, command+"/fa <broadcast"+or+command+"bc>"+obligatory+" <AnnouncementIndex>"+main+" - Announces the announcement specified by the index immediately. Will not interrupt the normal order/time. Please note that this starts at 0.");
				this.sendMessage(sender, 0, ChatColor.GOLD+"Use /fa help 2 to see the next page.");
			}else if(page==2){
				this.sendMessage(sender, 0, main+"*"+heading+"Help for FrogAnnounce "+this.pdfFile.getVersion()+" (2/3)"+main+"*");
				this.sendMessage(sender, 0, command+"/fa <add "+or+command+"add> "+obligatory+"<announcement message>"+main+" - Adds an announcement to the list. (Command /faadd or /fa-add is not a typo; technical restrictions forced this.)");
				this.sendMessage(sender, 0, command+"/fa <remove "+or+command+"delete"+or+command+"rem"+or+command+"del> "+obligatory+"<announcementIndex>"+main+" - Removes the specified announcement (announcementIndex = announcement number from top to bottom in the file; starts at 0).");
				this.sendMessage(sender, 0, command+"/fa <manualbroadcast"+or+command+"mbc"+obligatory+"<Message>"+main+" - Announces a message to the entire server. Ignores groups in the config.");
				this.sendMessage(sender, 0, ChatColor.GOLD+"Use /fa help 3 to see the next page.");
			}else if(page==3){
				this.sendMessage(sender, 0, main+"*"+heading+"Help for FrogAnnounce "+this.pdfFile.getVersion()+" (3/3)"+main+"*");
				this.sendMessage(sender, 0, command+"/fa <ignore"+or+command+"optout> "+optional+"[playerName] "+main+" - Ignore announcements. As long as you are ignored, you will not receive announcements. Specifying another player's name will force them to ignore announcements. Saves through disconnect.");
				this.sendMessage(sender, 0, command+"/fa <unignore"+or+command+"optin> "+optional+"[playerName] "+main+" - Unignore announcements. You will receive announcements as normal again. Specifying another player's name will force them to see announcements again. Saves through disconnect.");
				this.sendMessage(sender, 0, ChatColor.GRAY+"There are no more pages of help.");
			}else
				this.sendMessage(sender, 0, "There's no page "+page+".");
		}catch(final NumberFormatException e){
			this.sendMessage(sender, 0, "You must specify a page - positive integers only.");
		}
	}

	protected String colourizeText(String announce){
		announce = announce.replaceAll("&AQUA;", ChatColor.AQUA.toString());
		announce = announce.replaceAll("&BLACK;", ChatColor.BLACK.toString());
		announce = announce.replaceAll("&BLUE;", ChatColor.BLUE.toString());
		announce = announce.replaceAll("&DARK_AQUA;", ChatColor.DARK_AQUA.toString());
		announce = announce.replaceAll("&DARK_BLUE;", ChatColor.DARK_BLUE.toString());
		announce = announce.replaceAll("&DARK_GRAY;", ChatColor.DARK_GRAY.toString());
		announce = announce.replaceAll("&DARK_GREEN;", ChatColor.DARK_GREEN.toString());
		announce = announce.replaceAll("&DARK_PURPLE;", ChatColor.DARK_PURPLE.toString());
		announce = announce.replaceAll("&RED;", ChatColor.RED.toString());
		announce = announce.replaceAll("&DARK_RED;", ChatColor.DARK_RED.toString());
		announce = announce.replaceAll("&GOLD;", ChatColor.GOLD.toString());
		announce = announce.replaceAll("&GRAY;", ChatColor.GRAY.toString());
		announce = announce.replaceAll("&GREEN;", ChatColor.GREEN.toString());
		announce = announce.replaceAll("&LIGHT_PURPLE;", ChatColor.LIGHT_PURPLE.toString());
		announce = announce.replaceAll("&PURPLE;", ChatColor.LIGHT_PURPLE.toString());
		announce = announce.replaceAll("&PINK;", ChatColor.LIGHT_PURPLE.toString());
		announce = announce.replaceAll("&WHITE;", ChatColor.WHITE.toString());
		announce = announce.replaceAll("&b;", ChatColor.AQUA.toString());
		announce = announce.replaceAll("&0;", ChatColor.BLACK.toString());
		announce = announce.replaceAll("&9;", ChatColor.BLUE.toString());
		announce = announce.replaceAll("&3;", ChatColor.DARK_AQUA.toString());
		announce = announce.replaceAll("&1;", ChatColor.DARK_BLUE.toString());
		announce = announce.replaceAll("&8;", ChatColor.DARK_GRAY.toString());
		announce = announce.replaceAll("&2;", ChatColor.DARK_GREEN.toString());
		announce = announce.replaceAll("&5;", ChatColor.DARK_PURPLE.toString());
		announce = announce.replaceAll("&4;", ChatColor.DARK_RED.toString());
		announce = announce.replaceAll("&6;", ChatColor.GOLD.toString());
		announce = announce.replaceAll("&7;", ChatColor.GRAY.toString());
		announce = announce.replaceAll("&a;", ChatColor.GREEN.toString());
		announce = announce.replaceAll("&d;", ChatColor.LIGHT_PURPLE.toString());
		announce = announce.replaceAll("&c;", ChatColor.RED.toString());
		announce = announce.replaceAll("&f;", ChatColor.WHITE.toString());
		announce = announce.replaceAll("&e;", ChatColor.YELLOW.toString());
		announce = announce.replaceAll("&k;", ChatColor.MAGIC.toString());
		announce = announce.replaceAll("&MAGIC;", ChatColor.MAGIC.toString());
		announce = announce.replaceAll("&BOLD;", ChatColor.BOLD.toString());
		announce = announce.replaceAll("&ITALIC;", ChatColor.ITALIC.toString());
		announce = announce.replaceAll("&STRIKE;", ChatColor.STRIKETHROUGH.toString());
		announce = announce.replaceAll("&UNDERLINE;", ChatColor.UNDERLINE.toString());
		announce = announce.replaceAll("&RESET;", ChatColor.RESET.toString());
		return announce;
	}

	protected void broadcastMessage(final String s, final CommandSender player){
		int _int = 0;
		try{
			_int = Integer.parseInt(s);
			if(_int>this.strings.size()-1)
				this.sendMessage(player, 1, "You specified a number that does not correspond to any of the announcements in the file. Remember: it starts at 0! Operation aborted.");
			else
				this.announce(_int, false);
		}catch(final NumberFormatException e){
			this.sendMessage(player, 1, "Only numbers can be entered as an index. Remember to start counting at 0.");
		}
	}

	protected Boolean setupPermissions(){
		final RegisteredServiceProvider<Permission> permissionProvider = super.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if(permissionProvider!=null)
			this.permission = permissionProvider.getProvider();
		return this.permission!=null;
	}

	private void setRandom(final String[] args, final CommandSender player){
		final boolean s = Boolean.parseBoolean(args[1]);
		if(s!=this.random){
			this.random = s;
			this.cfg.config.set("Settings.Random", s);
			if(s==true)
				this.sendMessage(player, 0, "Announcer has been successfully changed to announce randomly. Reloading configuration...");
			else
				this.sendMessage(player, 0, "Announcer has been successfully changed to announce in sequence. Reloading configuration...");
			this.cfg.saveConfig();
			this.reloadPlugin(player);
		}else if(this.random==true)
			this.sendMessage(player, 1, "The announcer is already set to announce randomly! There's no need to change it!");
		else
			this.sendMessage(player, 1, "The announcer is already set to not announce randomly! There's no need to change it!");
	}

	private void setInterval(final String[] cmdArgs, final CommandSender player){
		final int newInterval = Integer.parseInt(cmdArgs[1]);
		if(newInterval!=this.interval){
			this.interval = newInterval;
			this.cfg.config.set("Settings.Interval", this.interval);
			this.cfg.saveConfig();
			this.sendMessage(player, 0, "Announcement interval has successfully been changed to "+this.interval+". Reloading configuration...");
			this.reloadPlugin(player);
		}else
			this.sendMessage(player, 1, "The announcement interval is already set to "+this.interval+"! There's no need to change it!");
	}

	public void checkPermissionsVaultPlugins(){
		final Plugin vault = this.getServer().getPluginManager().getPlugin("Vault");
		if(vault!=null){
			if(this.setupPermissions()!=null){
				this.logger.info("Vault hooked successfully.");
				this.usingPerms = true;
			}else if(this.setupPermissions()==null){
				this.logger.info("Vault wasn't found. Defaulting to OP/Non-OP system.");
				this.usingPerms = false;
			}
		}else
			this.logger.warning("Vault is not in your plugins directory! This plugin has a soft dependency of Vault, but if you don't have it, this will still work (you just can't use permission-based stuff).");
	}

	private void ignorePlayer(final CommandSender player, final String other){
		Player otherPlayer = this.getServer().getPlayer(other);
		if(other.equals(player.getName()))
			otherPlayer = (Player) player;
		else
			otherPlayer = this.getServer().getPlayer(other);
		if(otherPlayer!=null&&otherPlayer==player){
			if(this.permit(player, "frogannounce.ignore")){
				if(!this.ignoredPlayers.contains(player.getName())){
					this.ignoredPlayers.add(otherPlayer.getName());
					this.cfg.config.set("ignoredPlayers", this.ignoredPlayers);
					this.cfg.saveConfig();
					this.sendMessage(otherPlayer, 0, ChatColor.GRAY+"You are now being ignored by FrogAnnounce. You will no longer receive announcements from it until you opt back in.");
				}else
					this.sendMessage(player, 1, "That player is already being ignored.");
			}else
				this.sendMessage(player, 1, "You don't have sufficient permission to opt another player out of FrogAnnounce's announcements. Sorry!");
		}else if(otherPlayer!=null&&otherPlayer!=player){
			if(this.permit(player, "frogannounce.ignore.other")){
				if(!this.ignoredPlayers.contains(otherPlayer.getName())){
					this.ignoredPlayers.add(otherPlayer.getName());
					this.cfg.config.set("ignoredPlayers", this.ignoredPlayers);
					this.cfg.saveConfig();
					this.sendMessage(player, 0, "Success! The player has been added to FrogAnnounce's ignore list and will no longer see its announcements until he/she opts back in.");
					this.sendMessage(otherPlayer, 0, ChatColor.GRAY+"You are now being ignored by FrogAnnounce. You will no longer receive announcements from it until you opt back in.");
				}else
					this.sendMessage(player, 1, "You're already being ignored by FrogAnnounce.");
			}else
				this.sendMessage(player, 1, "You don't have sufficient permission to opt another player out of FrogAnnounce's announcements. Sorry!");
		}else
			this.sendMessage(player, 1, "That player isn't online right now.");
	}

	private void unignorePlayer(final CommandSender player, final String other){
		Player otherPlayer;
		if(other.isEmpty())
			otherPlayer = (Player) player;
		else
			otherPlayer = this.getServer().getPlayer(other);
		if(otherPlayer!=null&&otherPlayer==player){
			if(this.permit(player, "frogannounce.unignore")){
				if(this.ignoredPlayers.contains(player.getName())){
					this.ignoredPlayers.remove(otherPlayer.getName());
					this.cfg.config.set("ignoredPlayers", this.ignoredPlayers);
					this.cfg.saveConfig();
					this.sendMessage(otherPlayer, 0, ChatColor.GRAY+"You are no longer being ignored by FrogAnnounce. You will receive announcements until you opt out of them again.");
				}else
					this.sendMessage(player, 1, "You're already not being ignored.");
			}else
				this.sendMessage(player, 1, "You don't have sufficient permission to opt another player back into FrogAnnounce's announcements. Sorry!");
		}else if(otherPlayer!=null&&otherPlayer!=player){
			if(this.permit(player, "frogannounce.unignore.other"))
				if(this.ignoredPlayers.contains(otherPlayer.getName())){
					this.ignoredPlayers.remove(otherPlayer.getName());
					this.cfg.config.set("ignoredPlayers", this.ignoredPlayers);
					this.cfg.saveConfig();
					this.sendMessage(player, 0, "Success! The player has been removed from FrogAnnounce's ignore list and will see its announcements again until he/she opts out again.");
					this.sendMessage(otherPlayer, 0, ChatColor.GRAY+"You are no longer being ignored by FrogAnnounce. You will receive announcements until you opt out of them again.");
				}else
					this.sendMessage(player, 1, "That player is already not being ignored.");
		}else
			this.sendMessage(player, 1, "That player isn't online right now!");
	}

	protected void sendMessage(final CommandSender sender, final int severity, final String message){
		if(sender instanceof Player){
			if(severity==0)
				sender.sendMessage(ChatColor.DARK_GREEN+"[FrogAnnounce] "+ChatColor.GREEN+message);
			else if(severity==1)
				sender.sendMessage(ChatColor.DARK_GREEN+"[FrogAnnounce] "+ChatColor.RED+message);
			else if(severity==2)
				sender.sendMessage(ChatColor.DARK_GREEN+"[FrogAnnounce] "+ChatColor.DARK_RED+message);
		}else if(severity==0)
			this.logger.info(message);
		else if(severity==1)
			this.logger.warning(message);
		else if(severity==2)
			this.logger.severe(message);
	}

	protected void sendMessage(final Player player, final int severity, final String message){
		if(player!=null){
			if(severity==0)
				player.sendMessage(ChatColor.DARK_GREEN+"[FrogAnnounce] "+ChatColor.GREEN+message);
			else if(severity==1)
				player.sendMessage(ChatColor.DARK_GREEN+"[FrogAnnounce] "+ChatColor.RED+message);
			else if(severity==2)
				player.sendMessage(ChatColor.DARK_GREEN+"[FrogAnnounce] "+ChatColor.DARK_RED+message);
		}else if(severity==0)
			this.logger.info(message);
		else if(severity==1)
			this.logger.warning(message);
		else if(severity==2)
			this.logger.severe(message);
	}

	protected void announce(final int index, final boolean automatic){
		String announce = "";
		if(automatic&&this.random){
			final Random randomise = new Random();
			final int selection = randomise.nextInt(this.strings.size());
			announce = this.strings.get(selection);
		}else if(automatic){
			announce = this.strings.get(this.counter);
			this.counter++;
			if(this.counter>=this.strings.size())
				this.counter = 0;
		}else
			announce = this.strings.get(index);
		if(!announce.startsWith("&USE-CMD;")){
			String[] a = announce.split("&GROUPS;");
			if(this.showConsoleAnnouncements)
				if(automatic)
					this.logger.info("Automatically announcing: "+announce);
				else
					this.logger.info("Manually announcing: "+announce);
			if(this.usingPerms){
				if(this.toGroups){
					final List<String> received = new ArrayList<String>();
					final Player[] players = this.getServer().getOnlinePlayers();
					for(final Player p: players)
						if(!received.contains(p.getName())){
							for(final String group: this.Groups)
								if(this.permission.playerInGroup(p.getWorld().getName(), p.getName(), group)&&!this.ignoredPlayers.contains(p.getName()))
									for(String line: announce.split("&NEW_LINE;")){
										if(this.tag.equals("")||this.tag.equals(" ")||this.tag.isEmpty())
											line = this.colourizeText(line);
										else
											line = this.tag+" "+this.colourizeText(line);
										if(!this.ignoredPlayers.contains(p.getName()))
											if(this.tag.equals("")||this.tag.equals(" ")||this.tag.isEmpty())
												p.sendMessage(line);
											else
												p.sendMessage(line);
									}
							received.add(p.getName());
						}
				}else if(a.length>1){
					final Player[] players = Bukkit.getServer().getOnlinePlayers();
					final List<String> received = new ArrayList<String>();
					announce = a[0];
					a = a[1].split(",");
					if(a.length>1){
						for(final String group: a)
							for(final Player p: players)
								if(!this.ignoredPlayers.contains(p.getName()))
									if(this.permission.playerInGroup(p, group))
										if(!received.contains(p.getName())){
											for(final String s: announce.split("&NEW_LINE;"))
												p.sendMessage(this.colourizeText(s));
											received.add(p.getName());
										}else
											continue;
									else
										continue;
					}else
						for(final Player p: players)
							if(this.permission.playerInGroup(p, a[0]))
								for(final String s: announce.split("&NEW_LINE;"))
									p.sendMessage(this.colourizeText(s));
				}else
					this.normalAnnouncement(announce);
			}else
				this.normalAnnouncement(announce);
		}else{
			announce = announce.replace("&USE-CMD;", "/");
			if(this.showConsoleAnnouncements)
				if(automatic)
					this.logger.info("Automatically using command: "+announce);
				else
					this.logger.info("Manually invoking command: "+announce);
			this.getServer().dispatchCommand(this.getServer().getConsoleSender(), announce);
		}
	}

	protected void normalAnnouncement(final String announce){
		final Player[] onlinePlayers = this.getServer().getOnlinePlayers();
		for(final Player p: onlinePlayers)
			for(final String line: announce.split("&NEW_LINE;"))
				if(!this.ignoredPlayers.contains(p.getName()))
					if(this.tag.equals("")||this.tag.equals(" ")||this.tag.isEmpty())
						p.sendMessage(this.colourizeText(line));
					else
						p.sendMessage(this.tag+" "+this.colourizeText(line));
	}

	class Announcer implements Runnable{
		private final FrogAnnounce plugin;

		@Override
		public void run(){
			this.plugin.announce(-1, true);
		}

		protected Announcer(final FrogAnnounce plugin){
			this.plugin = plugin;
		}
	}
}