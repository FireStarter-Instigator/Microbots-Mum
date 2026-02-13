package net.runelite.client.plugins.microbot.util.grounditem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.discord.Rs2Discord;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ValuableDropTracker
{
    private final Client client;
    private final Gson gson;
    private final File logFile;
    private final String clientId;
    private static final int MAX_LOCK_ATTEMPTS = 10;
    private static final int LOCK_RETRY_DELAY_MS = 50;
    private static final int CLAIM_DURATION_MS = 3000; // 3 seconds to claim an alert

    public ValuableDropTracker(Client client) {
        this.client = client;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.clientId = loadClientIdFromConfig();

        // Create logs directory on Desktop
        String userHome = System.getProperty("user.home");
        File logsDir = new File(userHome, "Desktop/microbot-logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        this.logFile = new File(logsDir, "valuable-drops.json");
    }

    private String loadClientIdFromConfig() {
        try {
            // Try multiple possible config locations
            File[] possiblePaths = {
                    new File("./runelite-client/src/main/resources/config.json"),
                    new File("./config.json"),
                    new File(System.getProperty("user.home"), ".runelite/config.json")
            };

            for (File configFile : possiblePaths) {
                if (configFile.exists()) {
                    try (FileReader reader = new FileReader(configFile)) {
                        ConfigData config = gson.fromJson(reader, ConfigData.class);

                        if (config != null && config.accountData != null && !config.accountData.isEmpty()) {
                            var firstAccount = config.accountData.get(0);
                            if (firstAccount.containsKey("0")) {
                                AccountInfo account = gson.fromJson(
                                        gson.toJsonTree(firstAccount.get("0")),
                                        AccountInfo.class
                                );
                                if (account.username != null && account.username.length() >= 3) {
                                    return account.username.substring(0, 3).toLowerCase();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Microbot.log("Failed to load client ID from config: " + e.getMessage());
        }

        return "unk-" + UUID.randomUUID().toString().substring(0, 5);
    }

    private static final int MIN_VALUE = 50_000;
    private static final int ALERT_VALUE = 1_000_000;
    private static final int RADIUS = 20;
    private static final int EXPIRY_SECONDS = 120;
    private static final double DUPLICATE_WINDOW_SECONDS = 5;

    private final List<ItemRecord> tracked = Collections.synchronizedList(new ArrayList<>());

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        TileItem item = event.getItem();
        Tile tile = event.getTile();
        if (client.getLocalPlayer() == null) return;

        WorldPoint itemLoc = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
        WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        if (itemLoc.distanceTo(playerLoc) > RADIUS) return;

        int gePrice = Microbot.getItemManager().getItemPrice(item.getId());
        int totalValue = gePrice * item.getQuantity();
        if (totalValue < MIN_VALUE) return;

        // Create record
        ItemRecord record = new ItemRecord(item.getId(), item.getQuantity(), totalValue,
                client.getWorld(), itemLoc);

        // Avoid duplicate detection (same location+world within short window)
        if (isRecentDuplicate(record)) return;

        // Add and cleanup expired entries
        synchronized (tracked) {
            tracked.add(record);
            cleanup();
        }

        // Get recent tracked drops within 2 minutes (same world)
        List<ItemRecord> recentDrops;
        synchronized (tracked) {
            recentDrops = tracked.stream()
                    .filter(r -> r.world == record.world
                            && r.time.isAfter(Instant.now().minusSeconds(EXPIRY_SECONDS)))
                    .collect(Collectors.toList());
        }

        // Count how many of those are "valuable" drops (≥140k)
        long valuableCount = recentDrops.stream()
                .filter(r -> r.totalValue >= 140_000)
                .count();

        int totalRecentValue = recentDrops.stream().mapToInt(r -> r.totalValue).sum();

        // 🪙 CASE — at least 3 valuable drops (≥140k each) and total ≥1M
        if (valuableCount >= 3 && totalRecentValue >= ALERT_VALUE)
        {
            if (tryClaimAlert(recentDrops)) {
                sendDiscordAlert(recentDrops, false);
                synchronized (tracked) {
                    tracked.clear();
                }
            }
            return;
        }

        // 💎 CASE — at least 3 items total AND accumulated total ≥1M
        if (recentDrops.size() >= 3 && totalRecentValue >= ALERT_VALUE)
        {
            if (tryClaimAlert(recentDrops)) {
                sendDiscordAlert(recentDrops, true);
                synchronized (tracked) {
                    tracked.clear();
                }
            }
            return;
        }

        // Log status for high-value items waiting for more drops
        if (record.totalValue >= ALERT_VALUE)
        {
            Microbot.log("High-value single drop detected — waiting for 2 more items (140k+ each)...");
        }
        else if (totalRecentValue >= ALERT_VALUE && recentDrops.size() < 3)
        {
            Microbot.log("Drop party accumulating (" + recentDrops.size() + " items, " +
                    String.format("%,d", totalRecentValue) + " gp) — need 3+ items total...");
        }
    }

    private boolean isRecentDuplicate(ItemRecord newRecord)
    {
        Instant cutoff = Instant.now().minusMillis((long)(DUPLICATE_WINDOW_SECONDS * 1000));

        synchronized (tracked) {
            boolean inMemoryDuplicate = tracked.stream().anyMatch(r ->
                    r.itemId == newRecord.itemId &&
                            r.world == newRecord.world &&
                            r.location.equals(newRecord.location) &&
                            r.time.isAfter(cutoff)
            );

            if (inMemoryDuplicate) {
                return true;
            }
        }

        return checkJsonForDuplicate(newRecord, cutoff);
    }

    private boolean checkJsonForDuplicate(ItemRecord newRecord, Instant cutoff)
    {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                if (!logFile.exists()) return false;

                try (FileChannel channel = FileChannel.open(logFile.toPath(), StandardOpenOption.READ)) {
                    FileLock lock = channel.tryLock(0, Long.MAX_VALUE, true);
                    if (lock == null) {
                        Thread.sleep(LOCK_RETRY_DELAY_MS);
                        continue;
                    }

                    try {
                        channel.position(0);
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate((int) channel.size());
                        channel.read(buffer);
                        buffer.flip();
                        String jsonContent = new String(buffer.array(), java.nio.charset.StandardCharsets.UTF_8);

                        List<DropLog> logs = gson.fromJson(jsonContent, new TypeToken<List<DropLog>>(){}.getType());
                        if (logs == null || logs.isEmpty()) return false;

                        int startIdx = Math.max(0, logs.size() - 50);

                        for (int i = logs.size() - 1; i >= startIdx; i--) {
                            DropLog log = logs.get(i);

                            try {
                                LocalDateTime logTime = LocalDateTime.parse(log.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                Instant logInstant = logTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                                if (logInstant.isBefore(cutoff)) continue;
                                if (log.world != newRecord.world) continue;

                                for (DropItem logItem : log.items) {
                                    if (logItem.itemId == newRecord.itemId &&
                                            logItem.location != null &&
                                            logItem.location.equals(newRecord.location.toString())) {
                                        return true;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        return false;

                    } finally {
                        lock.release();
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                if (attempt == 2) {
                    Microbot.log("Error checking for duplicates: " + e.getMessage());
                }
            }
        }
        return false;
    }

    private void cleanup()
    {
        Instant cutoff = Instant.now().minusSeconds(EXPIRY_SECONDS);
        tracked.removeIf(r -> r.time.isBefore(cutoff));
    }

    /**
     * Try to claim this alert by writing a claim record to the JSON file.
     * Returns true if this client successfully claimed it, false if another client already did.
     */
    private boolean tryClaimAlert(List<ItemRecord> items)
    {
        String alertSignature = generateAlertSignature(items);

        for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
            try {
                // Ensure file exists
                if (!logFile.exists()) {
                    logFile.createNewFile();
                    try (FileWriter writer = new FileWriter(logFile)) {
                        writer.write("[]");
                    }
                }

                try (FileChannel channel = FileChannel.open(logFile.toPath(),
                        StandardOpenOption.READ, StandardOpenOption.WRITE)) {

                    FileLock lock = channel.tryLock();
                    if (lock == null) {
                        Thread.sleep(LOCK_RETRY_DELAY_MS);
                        continue;
                    }

                    try {
                        // Read existing logs
                        channel.position(0);
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate((int) channel.size());
                        channel.read(buffer);
                        buffer.flip();
                        String jsonContent = new String(buffer.array(), java.nio.charset.StandardCharsets.UTF_8);

                        List<DropLog> logs = gson.fromJson(jsonContent, new TypeToken<List<DropLog>>(){}.getType());
                        if (logs == null) logs = new ArrayList<>();

                        // Check if another client already claimed this alert recently
                        Instant claimCutoff = Instant.now().minusMillis(CLAIM_DURATION_MS);
                        boolean alreadyClaimed = logs.stream()
                                .filter(log -> {
                                    try {
                                        LocalDateTime logTime = LocalDateTime.parse(log.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                        Instant logInstant = logTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
                                        return logInstant.isAfter(claimCutoff);
                                    } catch (Exception e) {
                                        return false;
                                    }
                                })
                                .anyMatch(log -> alertSignature.equals(log.alertSignature));

                        if (alreadyClaimed) {
                            Microbot.log("[Client " + clientId + "] Alert already claimed by another client");
                            return false;
                        }

                        // Claim it by writing a minimal claim record
                        DropLog claimLog = new DropLog();
                        claimLog.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        claimLog.clientId = clientId;
                        claimLog.world = items.get(0).world;
                        claimLog.alertSignature = alertSignature;
                        claimLog.isClaim = true;

                        logs.add(claimLog);

                        // Write back
                        String newContent = gson.toJson(logs);
                        byte[] bytes = newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        java.nio.ByteBuffer writeBuffer = java.nio.ByteBuffer.wrap(bytes);

                        channel.truncate(0);
                        channel.position(0);
                        channel.write(writeBuffer);

                        Microbot.log("[Client " + clientId + "] Successfully claimed alert");
                        return true;

                    } finally {
                        lock.release();
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Microbot.log("Interrupted while claiming alert: " + e.getMessage());
                return false;
            } catch (IOException e) {
                if (attempt == MAX_LOCK_ATTEMPTS - 1) {
                    Microbot.log("Failed to claim alert after max attempts: " + e.getMessage());
                    return false;
                }
                try {
                    Thread.sleep(LOCK_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Generate a unique signature for this alert based on items and location
     */
    private String generateAlertSignature(List<ItemRecord> items)
    {
        String itemSignature = items.stream()
                .sorted(Comparator.comparingInt(i -> i.itemId))
                .map(i -> i.itemId + "x" + i.quantity + "@" + i.location.toString())
                .collect(Collectors.joining("|"));

        return Integer.toHexString(itemSignature.hashCode());
    }

    private void sendDiscordAlert(List<ItemRecord> items, boolean isDropParty)
    {
        String time = java.time.LocalTime.now().withNano(0).toString();
        int totalItems = items.size();
        int world = items.get(0).world;
        int totalValue = items.stream().mapToInt(i -> i.totalValue).sum();

        // Calculate direction & distance
        WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        ItemRecord ref = items.get(0);
        int dx = ref.location.getX() - playerLoc.getX();
        int dy = ref.location.getY() - playerLoc.getY();
        int distance = playerLoc.distanceTo(ref.location);

        String direction = "";
        if (Math.abs(dx) >= Math.abs(dy))
            direction = dx > 0 ? "east" : "west";
        else
            direction = dy > 0 ? "north" : "south";

        String messageEmoji = isDropParty ? "💎" : "🪙";

        String message;
        if (totalItems == 1)
        {
            ItemRecord i = items.get(0);
            String name = Microbot.getItemManager().getItemComposition(i.itemId).getName();
            message = String.format("%s %s x%,d (%,d gp) — World %d [%s] — %dm %s",
                    messageEmoji, name, i.quantity, i.totalValue, world, time, distance, direction);
        }
        else
        {
            String itemList = items.stream()
                    .map(i -> String.format("%s x%,d",
                            Microbot.getItemManager().getItemComposition(i.itemId).getName(),
                            i.quantity))
                    .collect(Collectors.joining(", "));

            message = String.format("%s %,d items (%,d gp total) — World %d [%s] — %dm %s\n%s",
                    messageEmoji, totalItems, totalValue, world, time, distance, direction, itemList);
        }

        Rs2Discord.sendWebhookMessage(message);
        Microbot.log("[Client " + clientId + "] Sent Discord alert: " + message);

        logFullDropToJson(items, totalValue, world);
    }

    /**
     * Log the full drop details to JSON (only after successfully sending alert)
     */
    private void logFullDropToJson(List<ItemRecord> items, int totalValue, int world)
    {
        for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                    try (FileWriter writer = new FileWriter(logFile)) {
                        writer.write("[]");
                    }
                }

                try (FileChannel channel = FileChannel.open(logFile.toPath(),
                        StandardOpenOption.READ, StandardOpenOption.WRITE)) {

                    FileLock lock = channel.tryLock();
                    if (lock == null) {
                        Thread.sleep(LOCK_RETRY_DELAY_MS);
                        continue;
                    }

                    try {
                        channel.position(0);
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate((int) channel.size());
                        channel.read(buffer);
                        buffer.flip();
                        String jsonContent = new String(buffer.array(), java.nio.charset.StandardCharsets.UTF_8);

                        List<DropLog> logs = gson.fromJson(jsonContent, new TypeToken<List<DropLog>>(){}.getType());
                        if (logs == null) logs = new ArrayList<>();

                        DropLog log = new DropLog();
                        log.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        log.clientId = clientId;
                        log.world = world;
                        log.totalValue = totalValue;
                        log.itemCount = items.size();
                        log.alertSignature = generateAlertSignature(items);
                        log.isClaim = false;
                        log.items = items.stream()
                                .map(i -> {
                                    WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
                                    int dx = i.location.getX() - playerLoc.getX();
                                    int dy = i.location.getY() - playerLoc.getY();
                                    int distance = playerLoc.distanceTo(i.location);
                                    String direction;
                                    if (Math.abs(dx) >= Math.abs(dy))
                                        direction = dx > 0 ? "east" : "west";
                                    else
                                        direction = dy > 0 ? "north" : "south";

                                    return new DropItem(
                                            Microbot.getItemManager().getItemComposition(i.itemId).getName(),
                                            i.itemId,
                                            i.quantity,
                                            i.totalValue,
                                            i.location.toString(),
                                            direction,
                                            distance
                                    );
                                })
                                .collect(Collectors.toList());

                        logs.add(log);

                        String newContent = gson.toJson(logs);
                        byte[] bytes = newContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        java.nio.ByteBuffer writeBuffer = java.nio.ByteBuffer.wrap(bytes);

                        channel.truncate(0);
                        channel.position(0);
                        channel.write(writeBuffer);

                        Microbot.log("[Client " + clientId + "] Logged full drop details: " + totalValue + " gp");
                        return;

                    } finally {
                        lock.release();
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Microbot.log("Interrupted while logging drop: " + e.getMessage());
                return;
            } catch (IOException e) {
                if (attempt == MAX_LOCK_ATTEMPTS - 1) {
                    Microbot.log("Max retry attempts reached. Full drop details not logged.");
                }
                try {
                    Thread.sleep(LOCK_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static class ItemRecord
    {
        final int itemId, quantity, totalValue, world;
        final WorldPoint location;
        final Instant time = Instant.now();

        ItemRecord(int id, int qty, int val, int world, WorldPoint loc)
        {
            this.itemId = id;
            this.quantity = qty;
            this.totalValue = val;
            this.world = world;
            this.location = loc;
        }
    }

    private static class DropLog
    {
        String timestamp;
        String clientId;
        int world;
        Integer totalValue;  // Nullable for claim records
        Integer itemCount;   // Nullable for claim records
        String alertSignature;
        boolean isClaim;
        List<DropItem> items;
    }

    private static class DropItem
    {
        String name;
        int itemId;
        int quantity;
        int value;
        String location;
        String direction;
        int distance;

        DropItem(String name, int itemId, int quantity, int value, String location, String direction, int distance)
        {
            this.name = name;
            this.itemId = itemId;
            this.quantity = quantity;
            this.value = value;
            this.location = location;
            this.direction = direction;
            this.distance = distance;
        }
    }

    private static class ConfigData
    {
        List<Map<String, Object>> accountData;
    }

    private static class AccountInfo
    {
        String username;
        String password;
    }
}