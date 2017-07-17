package de.themoep.timedscripts;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TimedScripts
 * Copyright (C) 2016 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public class ScriptManager {
    private final TimedScripts plugin;
    private Map<String, TimedScript> scriptMap = new HashMap<String, TimedScript>();
    private Map<String, String> globals = new HashMap<String, String>();

    private Map<TimerTask, String> timerTasks = new ConcurrentHashMap<TimerTask, String>();
    private File scriptFolder;

    public ScriptManager(TimedScripts plugin) {
        this.plugin = plugin;
        ConfigurationSection globalSection = plugin.getConfig().getConfigurationSection("globalvariables");
        if(globalSection != null) {
            for(String name : globalSection.getKeys(false)) {
                setGlobalVariable(name, globalSection.getString(name));
            }
        }

        plugin.getLogger().info("Loading scripts from folder...");
        scriptFolder = new File(plugin.getDataFolder(), "scripts");
        if(!scriptFolder.exists()) {
            if(scriptFolder.mkdir()) {
                plugin.getLogger().info("Scripts folder did not exist. Created it!");
            } else {
                plugin.getLogger().severe("Failed to create scripts folder!");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }
        if(!scriptFolder.isDirectory()) {
            plugin.getLogger().severe("Scripts folder is not a directory. Wat?");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }
        File[] scriptFiles = scriptFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.startsWith("-") && name.endsWith(".txt");
            }
        });
        if(scriptFiles == null || scriptFiles.length == 0) {
            plugin.getLogger().info("No script files in scripts folder found!");
            return;
        }
        for(File file : scriptFiles) {
            try {
                TimedScript script = new TimedScript(plugin, file);
                try {
                    plugin.getServer().getPluginManager().addPermission(new Permission("TimedScripts.command.run." + script.getName().toLowerCase()));
                    plugin.getServer().getPluginManager().addPermission(new Permission("TimedScripts.command.runwithvars." + script.getName().toLowerCase()));
                } catch(IllegalArgumentException ignored) { }
                addScript(script);
            } catch(FileNotFoundException e) {
                plugin.getLogger().severe("Script " + file.getName() + " not found in scripts folder? Where did it go?");
                e.printStackTrace();
            } catch(IOException e) {
                plugin.getLogger().severe("Error while loading script " + file.getName() + "!");
                e.printStackTrace();
            }
        }
    }

    public String setGlobalVariable(String name, String value) {
        return globals.put(name.toLowerCase(), value);
    }

    public String getGlobalVariable(String name) {
        return globals.get(name.toLowerCase());
    }

    public TimedScript addScript(TimedScript script) {
        return scriptMap.put(script.getName().toLowerCase(), script);
    }

    public TimedScript getScript(String name) {
        return scriptMap.get(name.toLowerCase());
    }

    public boolean runScript(CommandSender sender, String name) {
        return runScript(sender, name, new HashMap<String, String>());
    }

    public void runScript(CommandSender sender, TimedScript script) {
        runScript(sender, script, new HashMap<String, String>());
    }

    public boolean runScript(CommandSender sender, String name, Map<String, String> replacements) {
        TimedScript script = getScript(name);
        if(script == null) {
            return false;
        }
        runScript(sender, script, replacements);
        return true;
    }

    public void runScript(final CommandSender sender, TimedScript script, Map<String, String> vars) {
        String senderName = sender.getName();
        String senderWorld = plugin.getServer().getWorlds().get(0).getName();
        Location senderLoc = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        if(sender instanceof Entity) {
            Entity entity = (Entity) sender;
            senderWorld = entity.getWorld().getName();
            senderLoc = entity.getLocation();
            if(entity.getCustomName() != null) {
                senderName = entity.getCustomName();
            }
        } else if(sender instanceof BlockCommandSender) {
            BlockCommandSender blockSender = (BlockCommandSender) sender;
            senderWorld = blockSender.getBlock().getWorld().getName();
            senderLoc = blockSender.getBlock().getLocation();
        }

        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("sender", senderName);
        replacements.put("senderworld", senderWorld);
        replacements.put("senderx", String.valueOf(senderLoc.getBlockX()));
        replacements.put("sendery", String.valueOf(senderLoc.getBlockY()));
        replacements.put("senderz", String.valueOf(senderLoc.getBlockZ()));
        replacements.put("senderyaw", String.valueOf(senderLoc.getYaw()));
        replacements.put("senderpitch", String.valueOf(senderLoc.getPitch()));
        replacements.put("senderlocation", senderLoc.getBlockX() + " " + senderLoc.getBlockY() + " " + senderLoc.getBlockZ());
        replacements.putAll(vars);

        Timer timer = new Timer();
        Map<Double, List<TimedCommand>> commands = script.getCommands();
        for(Map.Entry<Double, List<TimedCommand>> entry : commands.entrySet()) {
            final List<String> commandList = new ArrayList<String>();
            for(TimedCommand command : entry.getValue()) {
                try {
                    commandList.add(command.getCommand(replacements));
                } catch (TimedCommand.MissingVariableException e) {
                    sender.sendMessage(ChatColor.RED + "Error while executing script " + script.getName() + "! " + ChatColor.YELLOW + e.getMessage());
                    return;
                }
            }

            if(entry.getKey() == 0) {
                for(String command : commandList) {
                    plugin.getServer().dispatchCommand(sender, command);
                }
            } else {

                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        new BukkitRunnable() {
                            public void run() {
                                for(String command : commandList) {
                                    plugin.getServer().dispatchCommand(sender, command);
                                }
                            }
                        }.runTask(plugin);
                        timerTasks.remove(this);
                    }
                };
                timerTasks.put(task, script.getName().toLowerCase());
                timer.schedule(task, (long) (entry.getKey() * 1000));
            }
        }
    }

    public boolean deleteScript(String scriptName) {
        TimedScript script = getScript(scriptName);
        if(script != null) {
            return deleteScript(script);
        }
        return false;
    }

    public boolean deleteScript(TimedScript script) {
        if(script.delete()) {
            stopScript(script);
            scriptMap.remove(script.getName());
            return true;
        }
        return false;
    }

    public boolean stopScript(TimedScript script) {
        if(!timerTasks.containsValue(script.getName().toLowerCase())) {
            return false;
        }
        Iterator<Map.Entry<TimerTask, String>> taskIt = timerTasks.entrySet().iterator();
        while(taskIt.hasNext()) {
            Map.Entry<TimerTask, String> entry = taskIt.next();
            if(entry.getValue().equals(script.getName().toLowerCase())) {
                entry.getKey().cancel();
                taskIt.remove();
            }
        }
        return true;
    }

    /**
     * Cancels all timers running
     */
    public void destroy() {
        Iterator<TimerTask> taskIterator = timerTasks.keySet().iterator();
        while(taskIterator.hasNext()) {
            TimerTask task = taskIterator.next();
            task.cancel();
            taskIterator.remove();
        }
    }

    /**
     * Get the folder the scripts are stored int
     * @return The script folder file
     */
    public File getFolder() {
        return scriptFolder;
    }

    /**
     * Helper method to create, add and save a new script. The creator gets send messages about the outcome of the method.
     * @param name The name of the script
     * @param creator The creator of the script
     * @return <tt>true</tt> if a new script was created; <tt>false</tt> if an error occurred
     */
    public boolean newScript(String name, CommandSender creator) {
        if(getScript(name) != null) {
            creator.sendMessage(ChatColor.RED + "There already exists a script with the name " + ChatColor.YELLOW + name);
            return false;
        }
        TimedScript newScript = new TimedScript(getFolder(), name, creator);
        if(newScript.save()) {
            addScript(newScript);
            try {
                plugin.getServer().getPluginManager().addPermission(new Permission("TimedScripts.command.run." + newScript.getName().toLowerCase()));
                plugin.getServer().getPluginManager().addPermission(new Permission("TimedScripts.command.runwithvars." + newScript.getName().toLowerCase()));
            } catch(IllegalArgumentException ignored) { }
            creator.sendMessage(ChatColor.GREEN + "Created new script " + ChatColor.YELLOW + newScript.getName());
            return true;
        } else {
            creator.sendMessage(ChatColor.RED + "An error occurred while saving the script " + newScript.getName() + "! Please take a look at the exception in the log.");
            return false;
        }
    }

    public Collection<TimedScript> getScripts() {
        return scriptMap.values();
    }
}
