package de.themoep.timedscripts;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TimedScripts
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class TimedScripts extends JavaPlugin {

    private ScriptManager scriptManager;

    public void onEnable() {
        saveDefaultConfig();
        scriptManager = new ScriptManager(this);
        scriptManager.loadScripts();
        getCommand("timedscript").setExecutor(new TimedScriptCommand(this));
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if ("timedscripts".equalsIgnoreCase(cmd.getName())) {
            if (args.length > 0) {
                if (sender.hasPermission("timedscripts.admin")) {
                    boolean stop = false;
                    boolean reload = false;
                    List<String> scripts = new ArrayList<>();
                    for (String arg : args) {
                        if ("load".equalsIgnoreCase(arg) || "reload".equalsIgnoreCase(arg)) {
                            reload = true;
                        } else if ("stop".equalsIgnoreCase(args[0])) {
                            stop = true;
                        } else {
                            scripts.add(arg);
                        }
                    }
                    if (stop) {
                        if (scripts.isEmpty()) {
                            scriptManager.destroy();
                            sender.sendMessage(ChatColor.YELLOW + "All running scripts stopped!");
                        } else {
                            for (String scriptName : scripts) {
                                TimedScript script = scriptManager.getScript(scriptName);
                                if (script != null) {
                                    if (scriptManager.stopScript(script)) {
                                        sender.sendMessage(ChatColor.YELLOW + "Script " + script.getName() + " stopped!");
                                    } else {
                                        sender.sendMessage(ChatColor.YELLOW + "Script " + script.getName() + " was not running!");
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.RED + "No script with the name " + scriptName + " found!");
                                    return false;
                                }
                            }
                        }
                    }
                    if (reload) {
                        reloadConfig();
                        getServer().getScheduler().runTaskAsynchronously(this, () -> {
                            if (scripts.isEmpty()) {
                                scriptManager.loadScripts();
                                sender.sendMessage(ChatColor.GREEN + "All scripts reloaded!");
                            } else {
                                for (String scriptName : scripts) {
                                    TimedScript script = scriptManager.loadScript(scriptName);
                                    if (script != null) {
                                        sender.sendMessage(ChatColor.GREEN + script.getName() + " loaded!");
                                    } else {
                                        sender.sendMessage(ChatColor.RED + scriptName + " could not be loaded? Please take a look at the log");
                                    }
                                }
                            }
                        });
                    }
                    if (!stop && !reload) {
                        return false;
                    }
                } else {
                    sender.sendMessage("You don't have the permission TimedScripts.admin");
                }
            } else {
                sender.sendMessage(ChatColor.AQUA + "List of TimedScripts:");
                if (getScriptManager().getScripts().size() > 0) {
                    for (TimedScript script : getScriptManager().getScripts()) {
                        sender.sendMessage(" " + script.getName() + " by " + script.getCreatorName());
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "None");
                }
            }
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!"timedscripts".equalsIgnoreCase(cmd.getName())) {
            return null;
        }
        return args.length == 0 || (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) ? Collections.singletonList("reload") : new ArrayList<String>();
    }

}
