package de.themoep.timedscripts;

import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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

    private Set<TimerTask> timerTasks = new HashSet<TimerTask>();

    public ScriptManager(TimedScripts plugin) {
        this.plugin = plugin;
        ConfigurationSection globalSection = plugin.getConfig().getConfigurationSection("globalvariables");
        if(globalSection != null) {
            for(String name : globalSection.getKeys(false)) {
                setGlobalVariable(name, globalSection.getString(name));
            }
        }

        plugin.getLogger().info("Loading scripts from folder...");
        File scriptFolder = new File(plugin.getDataFolder(), "scripts");
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

    public boolean runScript(final CommandSender sender, String name, Map<String, String> replacements) {
        TimedScript script = getScript(name);
        if(script == null) {
            return false;
        }

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

        replacements.put("sender", senderName);
        replacements.put("senderworld", senderWorld);
        replacements.put("senderx", String.valueOf(senderLoc.getBlockX()));
        replacements.put("sendery", String.valueOf(senderLoc.getBlockY()));
        replacements.put("senderz", String.valueOf(senderLoc.getBlockZ()));
        replacements.put("senderyaw", String.valueOf(senderLoc.getYaw()));
        replacements.put("senderpitch", String.valueOf(senderLoc.getPitch()));
        replacements.put("senderlocation", senderLoc.getBlockX() + " " + senderLoc.getBlockY() + " " + senderLoc.getBlockZ());

        Timer timer = new Timer();
        Map<Double, List<TimedCommand>> commands = script.getCommands();
        for(Map.Entry<Double, List<TimedCommand>> entry : commands.entrySet()) {
            final List<String> commandList = new ArrayList<String>();
            for(TimedCommand command : entry.getValue()) {
                commandList.add(command.getCommand(replacements));
            }

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
            timerTasks.add(task);
            timer.schedule(task, (long) (entry.getKey() * 1000));
        }
        return true;
    }

    /**
     * Cancels all timers running
     */
    public void destroy() {
        Iterator<TimerTask> taskIterator = timerTasks.iterator();
        while(taskIterator.hasNext()) {
            TimerTask task = taskIterator.next();
            task.cancel();
            taskIterator.remove();
        }
    }
}
