package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Script {
    private final List<String> console;
    private String name;
    private Path scriptPath;

    public Script(String name) {
       this(name, null);
    }

    public Script(String name, Path scriptPath) {
        this.name = name;
        this.scriptPath = scriptPath;
        this.console = new ArrayList<>();
    }

    public void run() {
        Context context = ScriptManager.getInstance().createDefaultScriptContext();
        console.clear();
        console.add("[" + name + "] Running script: " + name);
        try {
            String code = Files.readString(scriptPath);
            Value result = context.eval("js", code);
            console.add("[" + name + "] Finished Running the Script : " + result);
        } catch (IOException e) {
            console.add("[" + name + "] Error reading script : " + e.getMessage());
        } catch (PolyglotException e) {
            Main.LOGGER.error("[{}] Error : {}", name, e.getMessage());
            console.add("[" + name + "] Error : " + e.getMessage());
            Value guest = e.getGuestObject();
            if (e.getCause() != null) {
                Main.LOGGER.error("[{}] Java Cause Exception: {}", name, e.getCause().getMessage(), e.getCause());
                console.add("[" + name + "] Java Cause : " + e.getCause().getMessage());
            }
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

}
