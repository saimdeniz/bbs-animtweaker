package com.example.bbsanimtweaker.client.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory undo/redo manager with persistent original-state snapshot support.
 *
 * - Undo/Redo stacks are in-memory (cleared on game restart).
 * - Original snapshot is persisted to disk as a .original file so that
 *   "Reset to Original" works even after game restart.
 */
public class UndoManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger("BBS-AT");
    private static final int MAX_UNDO_DEPTH = 15;
    private static final int MAX_REDO_DEPTH = 15;
    private static final String ORIGINAL_SUFFIX = ".original";

    private static final Map<String, FileHistory> historyMap = new HashMap<>();

    private static class FileHistory
    {
        /** In-memory original snapshot (loaded from disk on first access). */
        String originalSnapshot;
        /** Stack of previous states for undo. */
        final Deque<String> undoStack = new ArrayDeque<>();
        /** Stack of undone states for redo. */
        final Deque<String> redoStack = new ArrayDeque<>();
    }

    /**
     * Returns the .original snapshot file path for a given model file.
     */
    private static File getOriginalFile(File file)
    {
        return new File(file.getAbsolutePath() + ORIGINAL_SUFFIX);
    }

    /**
     * Captures the original state of the file.
     * - If a .original file already exists on disk, loads it into memory.
     * - If not, saves the current file content as both the in-memory snapshot and the .original file.
     * Called once when a model is first opened.
     */
    public static void captureOriginal(File file)
    {
        if (file == null || !file.exists()) return;

        String key = file.getAbsolutePath();
        if (historyMap.containsKey(key)) return; // already captured this session

        FileHistory history = new FileHistory();

        File originalFile = getOriginalFile(file);
        try
        {
            if (originalFile.exists())
            {
                // Load existing persistent snapshot
                history.originalSnapshot = new String(Files.readAllBytes(originalFile.toPath()), StandardCharsets.UTF_8);
            }
            else
            {
                // First time: save current state as the original
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                history.originalSnapshot = content;
                Files.write(originalFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            }
            historyMap.put(key, history);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to capture original snapshot for {}", file.getName(), e);
        }
    }

    /**
     * Saves the current file state to the undo stack. Called before each inject/remove operation.
     * Also ensures captureOriginal has been called (auto-captures if not).
     * Clears the redo stack (new action branch).
     */
    public static void pushState(File file)
    {
        if (file == null || !file.exists()) return;

        String key = file.getAbsolutePath();
        FileHistory history = historyMap.get(key);
        if (history == null)
        {
            // Auto-capture if not yet captured
            captureOriginal(file);
            history = historyMap.get(key);
            if (history == null) return;
        }

        try
        {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            history.undoStack.push(content);
            history.redoStack.clear();

            // Trim oldest entries if exceeding max depth
            while (history.undoStack.size() > MAX_UNDO_DEPTH)
            {
                history.undoStack.removeLast();
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to push undo state for {}", file.getName(), e);
        }
    }

    /**
     * Undoes the last operation: restores the previous state from the undo stack
     * and pushes the current state onto the redo stack.
     */
    public static boolean undo(File file)
    {
        if (file == null) return false;

        String key = file.getAbsolutePath();
        FileHistory history = historyMap.get(key);
        if (history == null || history.undoStack.isEmpty()) return false;

        try
        {
            // Save current state to redo stack
            String currentContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            history.redoStack.push(currentContent);

            // Trim oldest redo entries if exceeding max depth
            while (history.redoStack.size() > MAX_REDO_DEPTH)
            {
                history.redoStack.removeLast();
            }

            // Restore from undo stack
            String previousContent = history.undoStack.pop();
            writeFileAtomically(file, previousContent);
            return true;
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to undo for {}", file.getName(), e);
            return false;
        }
    }

    /**
     * Redoes the last undone operation: restores the state from the redo stack
     * and pushes the current state onto the undo stack.
     */
    public static boolean redo(File file)
    {
        if (file == null) return false;

        String key = file.getAbsolutePath();
        FileHistory history = historyMap.get(key);
        if (history == null || history.redoStack.isEmpty()) return false;

        try
        {
            // Save current state to undo stack
            String currentContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            history.undoStack.push(currentContent);

            // Trim oldest undo entries if exceeding max depth
            while (history.undoStack.size() > MAX_UNDO_DEPTH)
            {
                history.undoStack.removeLast();
            }

            // Restore from redo stack
            String redoContent = history.redoStack.pop();
            writeFileAtomically(file, redoContent);
            return true;
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to redo for {}", file.getName(), e);
            return false;
        }
    }

    /**
     * Resets the file to its original state (the persistent snapshot from first-ever BBS AT interaction).
     * Pushes the current state to the undo stack so the user can undo the reset.
     */
    public static boolean resetToOriginal(File file)
    {
        if (file == null) return false;

        String key = file.getAbsolutePath();
        FileHistory history = historyMap.get(key);
        if (history == null || history.originalSnapshot == null) return false;

        try
        {
            // Save current state to undo stack before resetting
            String currentContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            history.undoStack.push(currentContent);
            history.redoStack.clear();

            // Trim if needed
            while (history.undoStack.size() > MAX_UNDO_DEPTH)
            {
                history.undoStack.removeLast();
            }

            writeFileAtomically(file, history.originalSnapshot);
            return true;
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to reset to original for {}", file.getName(), e);
            return false;
        }
    }

    /**
     * Accepts the current file state as the new original.
     * Overwrites the persistent .original file and updates the in-memory snapshot.
     */
    public static boolean acceptAsNewOriginal(File file)
    {
        if (file == null || !file.exists()) return false;

        String key = file.getAbsolutePath();
        FileHistory history = historyMap.get(key);
        if (history == null)
        {
            captureOriginal(file);
            return true;
        }

        try
        {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            history.originalSnapshot = content;

            // Overwrite the persistent .original file
            File originalFile = getOriginalFile(file);
            Files.write(originalFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

            // Clear undo/redo stacks since we're establishing a new baseline
            history.undoStack.clear();
            history.redoStack.clear();

            return true;
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to accept new original for {}", file.getName(), e);
            return false;
        }
    }

    public static boolean canUndo(File file)
    {
        if (file == null) return false;
        FileHistory history = historyMap.get(file.getAbsolutePath());
        return history != null && !history.undoStack.isEmpty();
    }

    public static boolean canRedo(File file)
    {
        if (file == null) return false;
        FileHistory history = historyMap.get(file.getAbsolutePath());
        return history != null && !history.redoStack.isEmpty();
    }

    public static boolean hasOriginal(File file)
    {
        if (file == null) return false;

        // Check in-memory first
        FileHistory history = historyMap.get(file.getAbsolutePath());
        if (history != null && history.originalSnapshot != null) return true;

        // Check disk
        return getOriginalFile(file).exists();
    }

    /**
     * Clears in-memory history for the given file. The persistent .original file remains on disk.
     */
    public static void clearHistory(File file)
    {
        if (file == null) return;
        historyMap.remove(file.getAbsolutePath());
    }

    /**
     * Writes content to a file atomically (via temp file + move).
     * Falls back to regular move on Windows if atomic move is not supported.
     */
    private static void writeFileAtomically(File targetFile, String content) throws IOException
    {
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        try
        {
            Files.write(tempFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            try
            {
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (java.nio.file.AtomicMoveNotSupportedException | java.nio.file.AccessDeniedException e)
            {
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            // Ensure the temp file does not linger on disk if write or move fails
            if (tempFile.exists()) { tempFile.delete(); }
            throw e;
        }
    }
}
