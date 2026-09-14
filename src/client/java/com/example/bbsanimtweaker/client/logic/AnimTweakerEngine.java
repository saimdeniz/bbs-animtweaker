package com.example.bbsanimtweaker.client.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnimTweakerEngine
{
    private static final Logger LOGGER = LoggerFactory.getLogger("BBS-AT");
    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    /** Pre-compiled regex for locating the "animations" block in a JSON file. */
    private static final java.util.regex.Pattern ANIM_BLOCK_PATTERN =
        java.util.regex.Pattern.compile("\"animations\"\\s*:\\s*\\{");

    /** Pre-compiled regex for collapsing pretty-printed JSON arrays onto single lines. */
    private static final java.util.regex.Pattern ARRAY_FORMAT_PATTERN =
        java.util.regex.Pattern.compile("\\[\\s*([^\\[\\]\\{\\}]+?)\\s*\\]");


    /**
     * Holds the result of extracting the animations block from a JSON string.
     * Contains the substring, its start index, and its end index (exclusive).
     */
    private static class AnimationsBlockResult
    {
        final String block;
        final int matcherStart; // start of the "animations": key
        final int blockStart;   // start of the '{' character
        final int blockEnd;     // end of the '}' character (exclusive, i.e. endIdx + 1)

        AnimationsBlockResult(String block, int matcherStart, int blockStart, int blockEnd)
        {
            this.block = block;
            this.matcherStart = matcherStart;
            this.blockStart = blockStart;
            this.blockEnd = blockEnd;
        }
    }

    public static boolean renameAnimation(File targetFile, String oldName, String newName)
    {
        if (oldName.equals(newName)) return true;
        
        try
        {
            String content = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
            
            // Extract animations block
            AnimationsBlockResult result = extractAnimationsBlock(content);
            if (result == null) return false;

            // Parse animations into JsonObject to preserve number formats
            JsonObject animations = JsonParser.parseString(result.block).getAsJsonObject();
            
            if (!animations.has(oldName)) return false;

            // Rename key
            JsonElement animData = animations.remove(oldName);
            animations.add(newName, animData);

            // Splice back
            spliceAndSave(targetFile, content, animations, result);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to rename animation '{}' to '{}' in {}", oldName, newName, targetFile.getName(), e);
            return false;
        }
    }

    public static boolean injectQueries(File targetFile, String targetBone, List<String> targetAnimations, 
                                        String xQuery, String yQuery, String zQuery, String mathModifier)
    {
        String modifierStr = (mathModifier != null && !mathModifier.trim().isEmpty()) ? " " + mathModifier.trim() : "";

        try
        {
            String content = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
            
            AnimationsBlockResult result = extractAnimationsBlock(content);
            if (result == null) return false;

            JsonObject animations = JsonParser.parseString(result.block).getAsJsonObject();
            
            for (Map.Entry<String, JsonElement> animEntry : animations.entrySet())
            {
                String animName = animEntry.getKey();
                if (targetAnimations != null && !targetAnimations.isEmpty() && !targetAnimations.contains("[All Animations]")) {
                    if (!targetAnimations.contains(animName)) continue;
                }

                if (!animEntry.getValue().isJsonObject()) continue;
                JsonObject animData = animEntry.getValue().getAsJsonObject();

                String groupKey = "groups";
                if (animData.has("bones") && !animData.has("groups")) {
                    groupKey = "bones";
                }

                if (!animData.has(groupKey)) {
                    animData.add(groupKey, new JsonObject());
                }

                if (animData.get(groupKey).isJsonObject())
                {
                    JsonObject groups = animData.getAsJsonObject(groupKey);
                    
                    if (!groups.has(targetBone))
                    {
                        // Create missing bone data
                        JsonObject newBoneData = new JsonObject();
                        String xVal = xQuery != null ? "0" + xQuery + modifierStr : "0";
                        String yVal = yQuery != null ? "0" + yQuery + modifierStr : "0";
                        String zVal = zQuery != null ? "0" + zQuery + modifierStr : "0";

                        if (groupKey.equals("groups")) {
                            JsonArray kf = new JsonArray();
                            kf.add(0); kf.add("linear");
                            kf.add(xVal);
                            kf.add(yVal);
                            kf.add(zVal);
                            JsonArray rotates = new JsonArray();
                            rotates.add(kf);
                            newBoneData.add("rotate", rotates);
                        } else {
                            JsonArray kf = new JsonArray();
                            kf.add(xVal);
                            kf.add(yVal);
                            kf.add(zVal);
                            JsonObject rotMap = new JsonObject();
                            rotMap.add("0.0", kf);
                            newBoneData.add("rotation", rotMap);
                        }
                        groups.add(targetBone, newBoneData);
                    }
                    else if (groups.get(targetBone).isJsonObject())
                    {
                        JsonObject boneData = groups.getAsJsonObject(targetBone);
                        String rotateKey = boneData.has("rotate") ? "rotate" : (boneData.has("rotation") ? "rotation" : null);

                        if (rotateKey == null)
                        {
                            // Create missing rotation data
                            String xVal = xQuery != null ? "0" + xQuery + modifierStr : "0";
                            String yVal = yQuery != null ? "0" + yQuery + modifierStr : "0";
                            String zVal = zQuery != null ? "0" + zQuery + modifierStr : "0";

                            if (groupKey.equals("groups")) {
                                JsonArray kf = new JsonArray();
                                kf.add(0); kf.add("linear");
                                kf.add(xVal);
                                kf.add(yVal);
                                kf.add(zVal);
                                JsonArray rotates = new JsonArray();
                                rotates.add(kf);
                                boneData.add("rotate", rotates);
                            } else {
                                JsonArray kf = new JsonArray();
                                kf.add(xVal);
                                kf.add(yVal);
                                kf.add(zVal);
                                JsonObject rotMap = new JsonObject();
                                rotMap.add("0.0", kf);
                                boneData.add("rotation", rotMap);
                            }
                        }
                        else if (boneData.get(rotateKey).isJsonArray())
                        {
                            JsonArray rotates = boneData.getAsJsonArray(rotateKey);
                            for (int k = 0; k < rotates.size(); k++)
                            {
                                if (rotates.get(k).isJsonArray())
                                {
                                    JsonArray kf = rotates.get(k).getAsJsonArray();
                                    if (kf.size() >= 5) {
                                        kf.set(2, processJsonRotationValue(kf.get(2), xQuery, modifierStr));
                                        kf.set(3, processJsonRotationValue(kf.get(3), yQuery, modifierStr));
                                        kf.set(4, processJsonRotationValue(kf.get(4), zQuery, modifierStr));
                                    } else if (kf.size() == 3) {
                                        kf.set(0, processJsonRotationValue(kf.get(0), xQuery, modifierStr));
                                        kf.set(1, processJsonRotationValue(kf.get(1), yQuery, modifierStr));
                                        kf.set(2, processJsonRotationValue(kf.get(2), zQuery, modifierStr));
                                    }
                                }
                            }
                        }
                        else if (boneData.get(rotateKey).isJsonObject())
                        {
                            JsonObject rotates = boneData.getAsJsonObject(rotateKey);
                            for (Map.Entry<String, JsonElement> kfEntry : rotates.entrySet())
                            {
                                if (kfEntry.getValue().isJsonArray())
                                {
                                    JsonArray kf = kfEntry.getValue().getAsJsonArray();
                                    if (kf.size() >= 5) {
                                        kf.set(2, processJsonRotationValue(kf.get(2), xQuery, modifierStr));
                                        kf.set(3, processJsonRotationValue(kf.get(3), yQuery, modifierStr));
                                        kf.set(4, processJsonRotationValue(kf.get(4), zQuery, modifierStr));
                                    } else if (kf.size() == 3) {
                                        kf.set(0, processJsonRotationValue(kf.get(0), xQuery, modifierStr));
                                        kf.set(1, processJsonRotationValue(kf.get(1), yQuery, modifierStr));
                                        kf.set(2, processJsonRotationValue(kf.get(2), zQuery, modifierStr));
                                    }
                                }
                                else if (kfEntry.getValue().isJsonObject())
                                {
                                    JsonObject kfMap = kfEntry.getValue().getAsJsonObject();
                                    if (kfMap.has("post") && kfMap.get("post").isJsonArray())
                                    {
                                        JsonArray kf = kfMap.getAsJsonArray("post");
                                        if (kf.size() >= 5) {
                                            kf.set(2, processJsonRotationValue(kf.get(2), xQuery, modifierStr));
                                            kf.set(3, processJsonRotationValue(kf.get(3), yQuery, modifierStr));
                                            kf.set(4, processJsonRotationValue(kf.get(4), zQuery, modifierStr));
                                        } else if (kf.size() == 3) {
                                            kf.set(0, processJsonRotationValue(kf.get(0), xQuery, modifierStr));
                                            kf.set(1, processJsonRotationValue(kf.get(1), yQuery, modifierStr));
                                            kf.set(2, processJsonRotationValue(kf.get(2), zQuery, modifierStr));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            spliceAndSave(targetFile, content, animations, result);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to inject queries into {}", targetFile.getName(), e);
            return false;
        }
    }

    private static JsonElement processJsonRotationValue(JsonElement val, String queryName, String modifierStr)
    {
        if (queryName == null) return val;
        if (val == null || val.isJsonNull()) return new JsonPrimitive("0" + queryName + modifierStr);

        String valStr;
        try
        {
            valStr = val.getAsString();
        }
        catch (Exception e)
        {
            return val;
        }

        String cleanVal = valStr;

        // Safely remove only the target query and its modifier without wiping out trailing math expressions
        cleanVal = cleanVal.replaceAll("(?i)[\\+\\-]?query\\.head_pitch(\\s*[*+\\-/]\\s*\\d*\\.?\\d*)?", "");
        cleanVal = cleanVal.replaceAll("(?i)[\\+\\-]?query\\.head_yaw(\\s*[*+\\-/]\\s*\\d*\\.?\\d*)?", "");

        if (cleanVal.isEmpty() || cleanVal.equals("0") || cleanVal.equals("0.0")) {
            return new JsonPrimitive("0" + queryName + modifierStr);
        }

        return new JsonPrimitive(cleanVal + queryName + modifierStr);
    }

    public static boolean removeQueries(File targetFile, String boneName, List<String> targetAnimations)
    {
        try
        {
            String content = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
            AnimationsBlockResult result = extractAnimationsBlock(content);
            if (result == null) return false;

            JsonObject animations = JsonParser.parseString(result.block).getAsJsonObject();
            boolean changed = false;

            for (Map.Entry<String, JsonElement> animEntry : animations.entrySet())
            {
                String animName = animEntry.getKey();
                if (targetAnimations != null && !targetAnimations.isEmpty() && !targetAnimations.contains("[All Animations]")) {
                    if (!targetAnimations.contains(animName)) continue;
                }

                if (!animEntry.getValue().isJsonObject()) continue;
                JsonObject animObj = animEntry.getValue().getAsJsonObject();

                String groupKey = "groups";
                if (animObj.has("bones") && !animObj.has("groups")) {
                    groupKey = "bones";
                }

                if (animObj.has(groupKey) && animObj.get(groupKey).isJsonObject())
                {
                    JsonObject groupsObj = animObj.getAsJsonObject(groupKey);
                    if (groupsObj.has(boneName) && groupsObj.get(boneName).isJsonObject())
                    {
                        JsonObject boneObj = groupsObj.getAsJsonObject(boneName);
                        String rotateKey = boneObj.has("rotate") ? "rotate" : (boneObj.has("rotation") ? "rotation" : null);
                        
                        if (rotateKey != null)
                        {
                            JsonElement rotateObj = boneObj.get(rotateKey);
                            if (rotateObj.isJsonArray())
                            {
                                JsonArray keyframes = rotateObj.getAsJsonArray();
                                for (int i = 0; i < keyframes.size(); i++)
                                {
                                    JsonElement kf = keyframes.get(i);
                                    if (kf.isJsonArray())
                                    {
                                        JsonArray kfData = kf.getAsJsonArray();
                                        if (kfData.size() >= 5)
                                        {
                                            kfData.set(2, cleanJsonRotationValue(kfData.get(2)));
                                            kfData.set(3, cleanJsonRotationValue(kfData.get(3)));
                                            kfData.set(4, cleanJsonRotationValue(kfData.get(4)));
                                            changed = true;
                                        }
                                        else if (kfData.size() == 3)
                                        {
                                            kfData.set(0, cleanJsonRotationValue(kfData.get(0)));
                                            kfData.set(1, cleanJsonRotationValue(kfData.get(1)));
                                            kfData.set(2, cleanJsonRotationValue(kfData.get(2)));
                                            changed = true;
                                        }
                                    }
                                }
                            }
                            else if (rotateObj.isJsonObject())
                            {
                                JsonObject rotates = rotateObj.getAsJsonObject();
                                for (Map.Entry<String, JsonElement> kfEntry : rotates.entrySet())
                                {
                                    if (kfEntry.getValue().isJsonArray())
                                    {
                                        JsonArray kfData = kfEntry.getValue().getAsJsonArray();
                                        if (kfData.size() >= 5) {
                                            kfData.set(2, cleanJsonRotationValue(kfData.get(2)));
                                            kfData.set(3, cleanJsonRotationValue(kfData.get(3)));
                                            kfData.set(4, cleanJsonRotationValue(kfData.get(4)));
                                            changed = true;
                                        } else if (kfData.size() == 3) {
                                            kfData.set(0, cleanJsonRotationValue(kfData.get(0)));
                                            kfData.set(1, cleanJsonRotationValue(kfData.get(1)));
                                            kfData.set(2, cleanJsonRotationValue(kfData.get(2)));
                                            changed = true;
                                        }
                                    }
                                    else if (kfEntry.getValue().isJsonObject())
                                    {
                                        JsonObject kfMap = kfEntry.getValue().getAsJsonObject();
                                        if (kfMap.has("post") && kfMap.get("post").isJsonArray())
                                        {
                                            JsonArray postList = kfMap.getAsJsonArray("post");
                                            if (postList.size() >= 5) {
                                                postList.set(2, cleanJsonRotationValue(postList.get(2)));
                                                postList.set(3, cleanJsonRotationValue(postList.get(3)));
                                                postList.set(4, cleanJsonRotationValue(postList.get(4)));
                                                changed = true;
                                            } else if (postList.size() >= 3) {
                                                postList.set(0, cleanJsonRotationValue(postList.get(0)));
                                                postList.set(1, cleanJsonRotationValue(postList.get(1)));
                                                postList.set(2, cleanJsonRotationValue(postList.get(2)));
                                                changed = true;
                                            }
                                        }
                                        if (kfMap.has("pre") && kfMap.get("pre").isJsonArray())
                                        {
                                            JsonArray preList = kfMap.getAsJsonArray("pre");
                                            if (preList.size() >= 5) {
                                                preList.set(2, cleanJsonRotationValue(preList.get(2)));
                                                preList.set(3, cleanJsonRotationValue(preList.get(3)));
                                                preList.set(4, cleanJsonRotationValue(preList.get(4)));
                                                changed = true;
                                            } else if (preList.size() >= 3) {
                                                preList.set(0, cleanJsonRotationValue(preList.get(0)));
                                                preList.set(1, cleanJsonRotationValue(preList.get(1)));
                                                preList.set(2, cleanJsonRotationValue(preList.get(2)));
                                                changed = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!changed) return true; // nothing changed

            // Use spliceAndSave for consistent formatting (same as injectQueries)
            spliceAndSave(targetFile, content, animations, result);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to remove queries from {}", targetFile.getName(), e);
            return false;
        }
    }

    private static JsonElement cleanJsonRotationValue(JsonElement val)
    {
        if (val == null || val.isJsonNull()) return val;
        if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isNumber()) return val;
        
        String s;
        try
        {
            s = val.getAsString();
        }
        catch (Exception e)
        {
            return val;
        }

        s = s.replaceAll("(?i)[\\+\\-]?query\\.head_pitch(\\s*[*+\\-/]\\s*\\d*\\.?\\d*)?", "");
        s = s.replaceAll("(?i)[\\+\\-]?query\\.head_yaw(\\s*[*+\\-/]\\s*\\d*\\.?\\d*)?", "");
        
        try {
            return new JsonPrimitive(Double.parseDouble(s));
        } catch (Exception e) {
            return new JsonPrimitive(s);
        }
    }

    public static List<String> getAnimationNames(File targetFile)
    {
        List<String> names = new ArrayList<>();
        if (targetFile == null || !targetFile.exists()) return names;

        try
        {
            String content = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
            AnimationsBlockResult result = extractAnimationsBlock(content);
            if (result != null)
            {
                JsonObject animations = JsonParser.parseString(result.block).getAsJsonObject();
                names.addAll(animations.keySet());
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to get animation names from {}", targetFile.getName(), e);
        }
        return names;
    }

    /**
     * Returns both animation names and tweaked status in a single file read + parse.
     * Key = animation name, Value = true if tweaked (contains query.head_pitch or query.head_yaw).
     */
    public static Map<String, Boolean> getAnimationsWithStatus(File targetFile)
    {
        Map<String, Boolean> result = new HashMap<>();
        if (targetFile == null || !targetFile.exists()) return result;

        try
        {
            String content = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
            AnimationsBlockResult blockResult = extractAnimationsBlock(content);
            if (blockResult != null)
            {
                JsonObject animations = JsonParser.parseString(blockResult.block).getAsJsonObject();
                for (Map.Entry<String, JsonElement> animEntry : animations.entrySet())
                {
                    boolean isTweaked = false;
                    if (animEntry.getValue().isJsonObject())
                    {
                        String animStr = animEntry.getValue().toString();
                        isTweaked = animStr.contains("query.head_pitch") || animStr.contains("query.head_yaw");
                    }
                    result.put(animEntry.getKey(), isTweaked);
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to get animations with status from {}", targetFile.getName(), e);
        }
        return result;
    }

    public static Set<String> getTweakedAnimations(File targetFile)
    {
        Set<String> tweaked = new HashSet<>();
        if (targetFile == null || !targetFile.exists()) return tweaked;

        try
        {
            String content = new String(Files.readAllBytes(targetFile.toPath()), StandardCharsets.UTF_8);
            AnimationsBlockResult blockResult = extractAnimationsBlock(content);
            if (blockResult != null)
            {
                JsonObject animations = JsonParser.parseString(blockResult.block).getAsJsonObject();
                for (Map.Entry<String, JsonElement> animEntry : animations.entrySet())
                {
                    if (animEntry.getValue().isJsonObject())
                    {
                        String animStr = animEntry.getValue().toString();
                        if (animStr.contains("query.head_pitch") || animStr.contains("query.head_yaw"))
                        {
                            tweaked.add(animEntry.getKey());
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to get tweaked animations from {}", targetFile.getName(), e);
        }
        return tweaked;
    }

    /**
     * Extracts the animations JSON block from the file content.
     * Returns both the block string and its positional indices to avoid redundant scanning in spliceAndSave.
     */
    private static AnimationsBlockResult extractAnimationsBlock(String content)
    {
        Matcher matcher = ANIM_BLOCK_PATTERN.matcher(content);
        if (!matcher.find()) return null;

        int matcherStart = matcher.start();
        int startIdx = matcher.end() - 1;
        int braceCount = 0;
        int endIdx = -1;
        boolean inStr = false;
        boolean escape = false;

        for (int i = startIdx; i < content.length(); i++)
        {
            char c = content.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inStr = !inStr; continue; }

            if (!inStr)
            {
                if (c == '{') braceCount++;
                else if (c == '}')
                {
                    braceCount--;
                    if (braceCount == 0)
                    {
                        endIdx = i;
                        break;
                    }
                }
            }
        }

        if (endIdx != -1)
        {
            return new AnimationsBlockResult(
                content.substring(startIdx, endIdx + 1),
                matcherStart,
                startIdx,
                endIdx + 1
            );
        }
        return null;
    }

    /**
     * Splices the new animations JSON object back into the original file content,
     * preserving the original file's formatting and indentation.
     * Reuses the positional indices from AnimationsBlockResult to avoid redundant brace-counting.
     */
    private static void spliceAndSave(File targetFile, String originalContent, JsonObject newAnimations, AnimationsBlockResult blockResult) throws IOException
    {
        String animDump = GSON.toJson(newAnimations);
        
        // Format array values into single lines just like original model formats
        Matcher arrMatcher = ARRAY_FORMAT_PATTERN.matcher(animDump);
        StringBuffer sb = new StringBuffer();
        while (arrMatcher.find()) {
            String inner = arrMatcher.group(1).replaceAll("\\s*\\n\\s*", " ").trim();
            arrMatcher.appendReplacement(sb, Matcher.quoteReplacement("[" + inner + "]"));
        }
        arrMatcher.appendTail(sb);
        String formattedAnim = sb.toString();

        // Detect indentation pattern of the file
        String fileIndent = "\t";
        for (String line : originalContent.split("\n")) {
            if (line.startsWith(" ")) {
                int spaces = line.length() - line.replaceAll("^\\s+", "").length();
                if (spaces > 0) {
                    fileIndent = " ".repeat(spaces);
                    break;
                }
            } else if (line.startsWith("\t")) {
                fileIndent = "\t";
                break;
            }
        }

        // Indent formatting
        String[] lines = formattedAnim.split("\n");
        StringBuilder indentedAnim = new StringBuilder(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            indentedAnim.append("\n").append(fileIndent).append(fileIndent).append(lines[i]);
        }

        String finalContent = originalContent.substring(0, blockResult.matcherStart) 
            + "\"animations\": " 
            + indentedAnim.toString() 
            + originalContent.substring(blockResult.blockEnd);

        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        try
        {
            Files.write(tempFile.toPath(), finalContent.getBytes(StandardCharsets.UTF_8));
            atomicMoveWithFallback(tempFile, targetFile);
        }
        catch (IOException e)
        {
            // Ensure the temp file does not linger on disk if the move fails
            if (tempFile.exists()) { tempFile.delete(); }
            throw e;
        }
    }

    /**
     * Attempts an atomic move; falls back to a regular replace-move on Windows
     * when ATOMIC_MOVE is not supported or the file is locked.
     */
    private static void atomicMoveWithFallback(File source, File target) throws IOException
    {
        try
        {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (java.nio.file.AtomicMoveNotSupportedException | java.nio.file.AccessDeniedException e)
        {
            LOGGER.warn("Atomic move not supported or access denied, falling back to regular move for {}", target.getName());
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
