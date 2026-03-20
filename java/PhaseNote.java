import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

// Start of Phase 1 Notes
public class PhaseNote {
    private static final Path NOTES_DIR = Path.of(System.getProperty("user.home"), ".notes");

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: No command provided.");
            System.err.println("Usage: java PhaseNote [command]");
            System.err.println("Try 'java PhaseNote help' for more information.");
            System.exit(1);
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "help" -> {
                showHelp();
                System.exit(0);
            }
            case "list" -> {
                boolean success = listNotes(NOTES_DIR);
                System.exit(success ? 0 : 1);
            }
            default -> {
                System.err.println("Error: Unknown command '" + command + "'");
                System.err.println("Try 'java PhaseNote help' for more information.");
                System.exit(1);
            }
        }
    }

    private static Map<String, String> parseYamlHeader(Path path) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("file", path.getFileName().toString());

        try {
            List<String> lines = Files.readAllLines(path);
            if (lines.isEmpty() || !lines.get(0).trim().equals("---")) {
                metadata.put("title", path.getFileName().toString());
                return metadata;
            }

            // Find the closing ---
            int closingIndex = -1;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("---")) {
                    closingIndex = i;
                    break;
                }
            }

            if (closingIndex == -1) {
                metadata.put("title", path.getFileName().toString());
                return metadata;
            }

            // Parse key-value pairs between the markers
            for (int i = 1; i < closingIndex; i++) {
                String line = lines.get(i).trim();
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    metadata.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            metadata.put("error", e.getMessage());
        }

        return metadata;
    }

    private static boolean listNotes(Path baseDir) {
        if (!Files.exists(baseDir)) {
            System.err.println("Error: Notes directory does not exist: " + baseDir);
            System.err.println("Create it with: mkdir -p ~/.notes/notes");
            return false;
        }

        Path searchPath = Files.exists(baseDir.resolve("notes")) ? baseDir.resolve("notes") : baseDir;
        List<Path> noteFiles;

        try (Stream<Path> walk = Files.walk(searchPath, 1)) {
            noteFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".md") || name.endsWith(".note") || name.endsWith(".txt");
                    })
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading notes directory: " + e.getMessage());
            return false;
        }

        if (noteFiles.isEmpty()) {
            System.out.println("No notes found in " + baseDir);
            return true;
        }

        System.out.println("Notes in " + baseDir + ":");
        System.out.println("=".repeat(60));

        for (Path file : noteFiles) {
            Map<String, String> metadata = parseYamlHeader(file);
            String title = metadata.getOrDefault("title", file.getFileName().toString());
            String created = metadata.getOrDefault("created", "N/A");
            String tags = metadata.getOrDefault("tags", "");

            System.out.println("\n" + file.getFileName());
            System.out.println("  Title: " + title);
            if (!"N/A".equals(created)) System.out.println("  Created: " + created);
            if (!tags.isEmpty()) System.out.println("  Tags: " + tags);
        }

        System.out.println("\n" + noteFiles.size() + " note(s) found.");
        return true;
    }

    private static void showHelp() {
        String helpText = String.format("""
            Future Proof Notes Manager v0.1
            
            Usage: java PhaseNote [command]
            
            Available commands:
              help    - Display this help information
              list    - List all notes in the notes directory
            
            Notes directory: %s
            
            Setup:
              mkdir -p ~/.notes/notes
              cp test-notes/*.md ~/.notes/notes/
            """, NOTES_DIR);
        System.out.println(helpText.trim());
    }
}
