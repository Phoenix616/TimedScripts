package de.themoep.timedscripts;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class TimedScript {
    private final File file;
    private String name;
    private String creatorName;
    private UUID creatorId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private Map<Double, List<TimedCommand>> commands = new LinkedHashMap<Double, List<TimedCommand>>();

    public TimedScript(JavaPlugin plugin, File file) throws IOException {
        plugin.getLogger().info("Loading " + file.getName() + "...");
        this.file = file;
        this.name = file.getName().substring(0, file.getName().lastIndexOf('.'));

        Pattern authorPattern = Pattern.compile("Author: (\\w+)");
        Pattern uuidPattern = Pattern.compile("(\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12})");

        double currentTime = 0;

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        int lineNumber = 0;
        while((line = reader.readLine()) != null) {
            lineNumber++;
            if(line.startsWith("#")) {
                Matcher authorMatcher = authorPattern.matcher(line);
                if(authorMatcher.find()) {
                    creatorName = authorMatcher.group(1);
                }
                Matcher uuidMatcher = uuidPattern.matcher(line);
                if(uuidMatcher.find()) {
                    try {
                        creatorId = UUID.fromString(uuidMatcher.group(1));
                    } catch(IllegalArgumentException ignored) {
                        plugin.getLogger().warning(uuidMatcher.group(1) + " doesn't appear to be a valid uuid! Could not set creator id correctly!");
                    }
                }
            } else if(line.startsWith("-")){
                addCommand(currentTime, line.substring(1), false);
            } else {
                String timeStr = line.substring(0, line.indexOf(':'));
                try {
                    currentTime = Double.parseDouble(timeStr);
                    if(line.length() > timeStr.length() + 1) {
                        addCommand(currentTime, line.substring(timeStr.length() + 1), false);
                    }
                } catch(NumberFormatException e) {
                    plugin.getLogger().severe("Expected double on line " + lineNumber + ", found " + timeStr + "!");
                    plugin.getLogger().severe("Trying to parse the rest of the script anyways...");
                }
            }
        }
        reader.close();

    }

    public TimedScript(File scriptFolder, String name, CommandSender creator) {
        this.file = new File(scriptFolder, name + ".txt");
        this.name = name;
        this.creatorName = creator.getName();
        if(creator instanceof Player) {
            creatorId = ((Player) creator).getUniqueId();
        }
        save();
    }

    /**
     * Add a command at a specific time
     * @param time The time (in seconds)
     * @param command The string of the command to add
     */
    public void addCommand(double time, String command) {
        addCommand(time, command, true);
    }

    /**
     * Add a command at a specific time
     * @param time The time (in seconds)
     * @param command The string of the command to add
     * @param writeFile Whether or not the scripts should be written to file
     */
    public void addCommand(double time, String command, boolean writeFile) {
        addCommand(time, new TimedCommand(command), writeFile);
    }

    /**
     * Add a command at a specific time
     * @param time The time (in seconds)
     * @param command The TimedCommand to add
     */
    public void addCommand(double time, TimedCommand command) {
        addCommand(time, command, true);
    }

    /**
     * Add a command at a specific time
     * @param time The time (in seconds)
     * @param command The TimedCommand to add
     * @param writeFile Whether or not the scripts should be written to file
     */
    public void addCommand(double time, TimedCommand command, boolean writeFile) {
        if(!commands.containsKey(time) || commands.get(time) == null) {
            commands.put(time, new ArrayList<TimedCommand>());
        }
        commands.get(time).add(command);
        if(writeFile) {
            save();
        }
    }

    /**
     * Remove a command from a specific time entry
     * @param time The entry's time
     * @param index The index of the command
     * @return The old TimedCommand object; <tt>null</tt> if there is none with this index or that time
     */
    public TimedCommand removeCommand(double time, int index) {
        if(index < 0 || commands.get(time) == null || getCommands(time).size() >= index) {
            return null;
        }
        TimedCommand r = getCommands(time).remove(index);
        save();
        return r;
    }

    /**
     * Remove all commands equal to a specific (case insensitive) command string from a specific time
     * @param time The time
     * @param commandString The command as a string
     * @return The amount of commands removed; -1 if there where not entry at the specified time
     */
    public int removeCommand(double time, String commandString) {
        if(commands.get(time) == null) {
            return -1;
        }
        int startSize = getCommands(time).size();
        Iterator<TimedCommand> cmdIt = getCommands(time).iterator();
        while(cmdIt.hasNext()) {
            TimedCommand command = cmdIt.next();
            if(command.getCommand().equalsIgnoreCase(commandString)) {
                cmdIt.remove();
            }
        }
        save();
        return startSize - getCommands(time).size();
    }

    /**
     * Get a list of commands at a specific time
     * @param time The time (in seconds)
     * @return The list of TimedCommands; null if there are none
     */
    public List<TimedCommand> getCommands(double time) {
        return commands.get(time);
    }

    /**
     * Returns a copy of the commands
     * @return
     */
    public Map<Double, List<TimedCommand>> getCommands() {
        return new HashMap<Double, List<TimedCommand>>(commands);
    }

    public String getName() {
        return name;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    private List<String> getFileHead() {
        List<String> headText = new ArrayList<String>();
        headText.add("TimedScript: " + getName());
        headText.add("Author: " + getCreatorName() + " (" + getCreatorId() + ")");

        int headWidth = 0;
        for(String headLine : headText) {
            if(headLine.length() > headWidth) {
                headWidth = headLine.length();
            }
        }

        String divider = "-";
        for(int i = 1; i <= headWidth; i++) {
            divider += "-";
        }

        List<String> head = new ArrayList<String>();
        head.add("# " + divider + " #");
        for(String headLine : headText) {
            for(int i = headLine.length(); i <= headWidth; i++) {
                headLine += " ";
            }
            head.add("# " + headLine + " #");
        }
        head.add("# " + divider + " #");
        return head;
    }

    private static String formatTime(double time) {
        if(time == (long) time) {
            return String.format("%d", (long) time);
        }
        return String.format("%s", time);
    }

    private void save() {
        BufferedWriter writer = null;
        try {
            file.createNewFile();
            writer = new BufferedWriter(new FileWriter(file));

            for(String headLine : getFileHead()) {
                writer.write(headLine);
                writer.newLine();
            }

            for(Map.Entry<Double, List<TimedCommand>> entry : commands.entrySet()) {
                if(entry.getValue() == null || entry.getValue().size() == 0) {
                    continue;
                }
                writer.write(formatTime(entry.getKey()) + ":");
                if(entry.getValue().size() == 1) {
                    writer.write(" " + entry.getValue().get(0));
                    writer.newLine();
                } else {
                    writer.newLine();
                    for(TimedCommand command : entry.getValue()) {
                        writer.write("- " + command);
                        writer.newLine();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception ignored) {}
        }
    }
}
