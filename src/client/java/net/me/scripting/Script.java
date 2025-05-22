package net.me.scripting;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
// Import ScriptManager
import net.me.scripting.ScriptManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Script {
    private final List<String> console;
    private final Context context;
    private String name;
    private Path scriptPath;

    public Script(String name) {
       this(name, null);
    }

    public Script(String name, Path scriptPath) {
        this.name = name;
        this.scriptPath = scriptPath;
        this.console = new ArrayList<>();
        // Initialize context using ScriptManager's method
        this.context = ScriptManager.getInstance().createDefaultScriptContext();
    }

    public void run() {
        console.clear();
        console.add("[" + name + "] Running script: " + name);
        try {
            String code = Files.readString(scriptPath);
            Value result = context.eval("js", code);
            console.add("[" + name + "] Finished Running the Script : " + result);
        } catch (IOException e) {
            console.add("[" + name + "] Error reading script : " + e.getMessage());
        } catch (PolyglotException e) {
            console.add("[" + name + "] Error : " + e.getMessage());
            Value guest = e.getGuestObject();
            if (guest != null && guest.hasMember("stack")) {
                console.add("[" + name + "] JS : " + guest.getMember("stack").asString());
            }
        } catch (Exception e) {
            console.add("[" + name + "] Unexpected error : " + e.getMessage());
        }
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Path getScriptPath() {
        return scriptPath;
    }
    public void setScriptPath(Path scriptPath) {
        this.scriptPath = scriptPath;
    }

    public List<String> getConsole() {
        return List.copyOf(console);
    }
    public void clearConsole() {
        console.clear();
    }

    // Getter for the context
    public Context getDefaultContext() {
        return this.context;
    }
}
