import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple shell that mimics some basic features of bash. Java has no real fork/exec, so each
 * command runs as a separate forked {@code java} process (see {@link ProcessBuilder}) that locates
 * and loads the real command class at runtime via a {@link URLClassLoader} and
 * <a href="https://docs.oracle.com/javase/tutorial/reflect/">reflection</a> — {@code shell} has no
 * compile-time dependency on {@code commandBin}. See {@code README.md} for the full picture;
 * {@link #executeCommand} is where it comes together.
 */
public class Shell {
    public static final String COMMAND_PROMPT = "> ";
    private static final Path PATH = getPath();
    private final List<Pipeline> jobs = new ArrayList<>();
    private List<Integer> jobIds = new ArrayList<>();
    private int count = 1;
    private boolean isPipeline = false;
    /**
     * Represents the current working directory of the shell
     * (since the actual current working directory of a JVM cannot be changed).
     * This is the directory that command processes will be run in.
     */
    private Path cwd = Paths.get(System.getProperty("user.dir"));

    /**
     * Returns the path of the {@code Shell.class} file, via {@code getProtectionDomain()
     * .getCodeSource().getLocation()} and a
     * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html">URI</a>
     * conversion ({@link Paths#get(java.net.URI)} needs a {@code URI}, not a {@link URL}).
     */
    public static Path getCurrentClassPath() {
        try {
            return Paths.get(Shell.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Absolute path of {@code commandBin}'s compiled classes, computed relative to
     * {@link #getCurrentClassPath} so it isn't hardcoded to one machine's checkout location.
     */
    public static Path getPath() {
        return getCurrentClassPath().resolve("../../../commandBin/target/classes").toAbsolutePath().normalize();
    }

    public static void main(String[] args) {
        Shell shell = new Shell();
        if (args.length > 0) {
            shell.executeCommand(args[0]);
        } else {
            shell.runRepl();
        }
    }

    /**
     * Searches the PATH directory for a .class file corresponding to the given command.
     * Class names are converted from PascalCase to kebab-case before attempting to match.
     *
     * @param command the command name
     * @return the matched class name
     * @throws Exception if a match class cannot be found
     */
    private static String findCommandClass(String command) throws Exception {
        if(command.contains("cat"))
            return "Cat";
        if(command.contains("ls"))
            return "Ls";
        if(command.contains("pwd"))
            return "Pwd";
        if(command.contains("wc"))
            return "Wc";
        if(command.contains("grep"))
            return "Grep";
        if(command.contains("head"))
            return "Head";
        if(command.contains("tail"))
            return "Tail";
        if(command.contains("sort"))
            return "Sort";
        if(command.contains("sleep"))
            return "Sleep";
        if(command.contains("uniq"))
            return "Uniq";
        throw new Exception(command + ": command not found");
    }

    /**
     * Converts a PascalCase class name to kebab-case (e.g. {@code "WordCount"} to
     * {@code "word-count"}), splitting on lower-to-upper boundaries via
     * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html">lookaround regex</a>
     * so acronym runs stay together.
     *
     * @param className the java class name
     * @return the kebab-case equivalent command name
     */
    public static String classNameToCommandName(String className) {
        // Split on uppercase letter boundaries, handling acronyms safely
        String[] words = className.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
        // Lowercase and join with hyphens
        return Arrays.stream(words)
                .map(String::toLowerCase)
                .collect(Collectors.joining("-"));
    }

    public Path getCwd() {
        return cwd;
    }

    public void setCwd(Path path) {
        cwd = path;
    }

    /**
     * Runs the Read-Eval-Print Loop of the Shell. The command "exit" ends the loop.
     */
    public void runRepl() {
        boolean bool = true;
        String command;
        Scanner scan = new Scanner(System.in);
        while(bool)
        {
            System.out.print(COMMAND_PROMPT);
            command = scan.nextLine().trim();
            if(command.equals("exit"))
                bool = false;
            else
            {
                try{
                    //findCommandClass(command);
                    // Pipeline p = 
                    processInput(command);
                    //p.executePipeline();
                }
                catch(Exception e) {
                    System.err.println(command + ": command not found");
                }
                }
        }
            scan.close();
        }

    /**
     * Takes the input string and parses it to execute either a shell builtin or a pipeline of separate processes.
     *
     * @param input the command to execute
     * @return null if the input was a builtin command or if there was an error,
     * otherwise the pipeline that was executed (for testing purposes)
     */
    Pipeline processInput(String input) throws Exception{
        //if you run shell commands, just build it through functions of shell class and return null
        //otherwise construct it through pipeline
        if(input.isBlank())
            return null;
        if(tryExecuteBuiltin(input) == true)
                return null;
            Pipeline pipeline = new Pipeline(input);
            pipeline.executePipeline();
            if(pipeline.isBackground())
            {
            jobs.add(pipeline);
            jobIds.add(count);
            count++;
        }
        return pipeline;
    }

    /**
     * Executes the main method of the given command, passing along any additional args.
     * Should only be called when {@code Shell.main()} is called with non-zero number of args.
     * Does nothing if the given command is blank (only whitespace).
     *
     * <p><b>Given to you as a worked example</b> — this is where the class Javadoc's three ideas
     * (URI, ClassLoader, reflection) actually get used; you don't need to write this kind of code
     * yourself, but understanding it will help you debug {@link #findCommandClass} and, later,
     * extend the shell:
     * <ol>
     *     <li>{@link #findCommandClass} resolves the command name to a class name.</li>
     *     <li>{@link #PATH} (the directory holding {@code commandBin}'s compiled classes) is
     *     turned into a {@link URL}.</li>
     *     <li>A {@link URLClassLoader} opened on that URL loads the class — this is what makes
     *     the class reachable at all, since {@code commandBin} isn't on this module's classpath.</li>
     *     <li>{@code loadedClass.getMethod("main", String[].class)} then
     *     {@code .invoke(null, (Object) commandArgs)} calls that class's {@code main} via
     *     reflection — the same cast trick as {@code ShellCommand.start()}.</li>
     *     <li>Because this only ever runs inside a freshly-forked child JVM whose whole purpose
     *     was to run this one command, it reports its own exit status
     *     ({@code System.exit(0)}/{@code System.exit(1)}) like any Unix process would.</li>
     * </ol>
     * See {@code README.md} for a plainer walkthrough of the four ideas.
     *
     * @param command the command to execute
     */
    void executeCommand(String command) {
        if (command.isBlank()) {
            return;
        }
        String[] commandArgs = command.split("\\s+");
        String commandName = commandArgs[0];
        try {
            // Get Class name for the command
            String commandClass = findCommandClass(commandName);
            // Create URL pointing to the classpath location
            File file = new File(String.valueOf(PATH));
            URL url = file.toURI().toURL();

            // Define the isolated ClassLoader
            try (URLClassLoader loader = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader())) {
                // Load the target class
                Class<?> loadedClass = Class.forName(commandClass, true, loader);
                java.lang.reflect.Method mainMethod = loadedClass.getMethod("main", String[].class);
                mainMethod.invoke(null, (Object) commandArgs);
            }
            System.exit(0);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs {@code input} as a builtin ({@code cd}, {@code kill}, {@code jobs}) if it is one.
     * Builtins run in the main shell process, not forked, since they mutate the shell's own state
     * ({@code cwd}, {@code jobs}).
     *
     * @param input the command string
     * @return true if the command was a builtin, false otherwise
     */
    private boolean tryExecuteBuiltin(String input) {
       /*if(input.contains("cd"))
        {
            changeDirectory(input);
            return true;
        }
        if(input.contains("kill"))
        {
            String nums = input.substring(4, input.length()).trim();
            List <Integer> ids = new ArrayList<>();
            for(int i = 0; i < nums.length(); i++)
            {
                String num = nums.substring(i, i+1);
                try{
                int n = Integer.parseInt(num);
                ids.add(n);
                }
                catch (NumberFormatException e) {
                System.err.println("kill: arguments must be job IDs");
            }
            }
            killJobs(ids);
            return true;
        }
        if(input.contains("jobs"))
        {
            listJobs();
            return true;
        }*/
       
    String[] args = input.trim().split("\\s+");

    if (args.length == 0) {
        return false;
    }

    switch (args[0]) {
        case "cd":
            if (args.length != 2) {
                System.err.println("cd: expected one argument");
            } else {
                changeDirectory(args[1]);
            }
            return true;

        case "jobs":
            if (args.length != 1) {
                System.err.println("jobs: too many arguments");
            } else {
                listJobs();
            }
            return true;

        case "kill":
            if (args.length < 2) {
                System.err.println("kill: arguments must be job IDs");
                return true;
            }

            List<Integer> ids = new ArrayList<>();

            for (int i = 1; i < args.length; i++) {
                try {
                    ids.add(Integer.parseInt(args[i]));
                } catch (NumberFormatException e) {
                    System.err.println("kill: arguments must be job IDs");
                }
            }

            killJobs(ids);
            return true;

        default:
            return false;
        }
    }

    /**
     * Builds the {@code ProcessBuilder} for
     * {@code java -classpath <Shell's own classes> Shell "<command>"} — the fork workaround
     * described in the class Javadoc. Sets the child's working directory to {@link #cwd}, and
     * redirects output to a file if {@code command} ends in {@code > <file>}.
     *
     * @param command the string passed to the new {@code Shell} process as an argument
     *                (after removing the redirect if there is one)
     * @return the {@code ProcessBuilder} for the "forked" process
     * @throws IllegalArgumentException if a redirect file is given and it is a directory
     */
    ProcessBuilder buildForkedShell(String command) throws IllegalArgumentException{
        String actualCommand = command.trim();
    File outputFile = null;

    int redirectIndex = actualCommand.indexOf(">");
    if (redirectIndex != -1) {
        String redirectTarget = actualCommand.substring(redirectIndex + 1).trim();
        actualCommand = actualCommand.substring(0, redirectIndex).trim();

        outputFile = new File(redirectTarget);

        if (outputFile.isDirectory()) {
            System.err.println(redirectTarget + ": Is a directory");
            return null;
        }
    }

    ProcessBuilder pb = new ProcessBuilder(
           "java",
            "-classpath",
            getCurrentClassPath().toString(),
            Shell.class.getSimpleName(),
            actualCommand
    );

    pb.directory(cwd.toFile());

    pb.redirectError(ProcessBuilder.Redirect.INHERIT);

    if (outputFile != null) {
        pb.redirectOutput(outputFile);
    }
    //  else {
    //     pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
    // }


    return pb;
}
    

    /**
     * Changes the shell's working directory by updating the value of the {@code cwd} field.
     *
     * @param pathString the absolute or relative path string to change to
     */
    void changeDirectory(String pathString) {
        /*File f = new File(pathString);
        if(!f.exists())
        {
            System.err.println("cd: invalid-dir: No such file or directory");
        }
        if(f.isDirectory())
        {
            if(pathString.equals(".."))
            {
                String s = Path.of(pathString).toAbsolutePath().toString();
                s = s.substring(0, s.length()-3);
                Path p = Path.of(s);
                setCwd(p);
            }
            else
                setCwd(Path.of(pathString).toAbsolutePath().normalize());
        }
    }*/
   Path path = Paths.get(pathString);
   Path newPath = (path.isAbsolute() ? path : cwd.resolve(path)).normalize();
//    Path newPath = cwd.resolve(pathString).normalize();

    if (!Files.exists(newPath) || !Files.isDirectory(newPath)) {
        System.err.println("cd: " + pathString + ": No such file or directory");
        return;
    }

    cwd = newPath;
    }

    /**
     * Prints background jobs and their status to standard out.
     * A job should only be removed from the list if it was shown in a previous call with the "Done" or "Terminated" status.
     * Job numbering does not restart unless the job list becomes empty.
     */
    void listJobs() {
        for(int i = 0; i < jobs.size(); i++)
        {
            System.out.println("[" + (jobIds.get(i)) + "]       " + jobs.get(i).toString());
            if(jobs.get(i).toString().contains("Done") || jobs.get(i).toString().contains("Terminated"))
            {  
                jobs.remove(i);
                jobIds.remove(i);
                i--;
                if(jobs.size() == 0)
                    count = 1;
            }
        }
    }

    /**
     * Kills the given jobs. The ids must be existing job ids.
     *
     * @param ids the ids of the jobs to kill
     */
    void killJobs(List<Integer> ids) {
        for(int id: ids)
        {
            if(id > jobs.size())
            {
                System.err.println("kill: " + id + ": no such job");
                continue;
            }
            if(id <= 0)
            {
                System.err.println("kill: arguments must be job IDs");
                continue;
            }
            jobs.get(id-1).killPipeline();
        }
    }

    /**
     * Represents a pipeline: a list of commands that run as separate processes, with their inputs and outputs linked
     * by pipes.
     */
    public class Pipeline {
        private final String pipelineCommand;
        private final List<ProcessBuilder> pbs;
        private final boolean isBackground;
        private List<Process> processes = null;
        private boolean isDone = false;
        private boolean terminated = false;

        /**
         * Splits {@code input} on {@code |}, builds a forked process for each subcommand via
         * {@link #buildForkedShell}, and wires the first process's stdin and (unless already
         * redirected to a file) the last process's stdout to this shell's own. A trailing
         * {@code &} marks the pipeline as a background job.
         *
         * @param input the string command for the pipeline
         */
        public Pipeline(String input) throws Exception{
            /* 
            pipelineCommand = input;
            String search = " | ";
            if(input.contains("&"))
            {
                isBackground = true;
                search = " &";
            }
            else
                isBackground = false;
            int end = 0;
            pbs = new ArrayList<>();
            ProcessBuilder p;
            while(end < pipelineCommand.length())
            {
                end = input.indexOf(search);
                if(end == -1)
                {
                    p = buildForkedShell(input);
                    pbs.add(p);  
                    return;
                }
                else
                {
                    p = buildForkedShell(input.substring(0, end));
                    pbs.add(p);
                if(search.equals(" | "))
                    input = input.substring(end+3, input.length());
                else
                    input = input.substring(end+2, input.length());
                }
            } */
            pipelineCommand = input.trim();
            String command = input.trim();
            if (command.endsWith("&")) 
            {
                isBackground = true;
                command = command.substring(0, command.length() - 1).trim();
            } else {
                isBackground = false;
            }

            // pbs = new ArrayList<>();

            String[] commands = command.split("\\s*\\|\\s*");
            pbs = Arrays.stream(commands).map(Shell.this::buildForkedShell).toList();

            // for (String subcommand : commands) 
            //     {
            //     ProcessBuilder p = buildForkedShell(subcommand);
            //     if(p == null)
            //         continue;
            //         pbs.add(p);
            //     }
            pbs.getFirst().redirectInput(ProcessBuilder.Redirect.INHERIT);
            // ProcessBuilder last = getPbs().getLast();
            if (pbs.getLast().redirectOutput() == ProcessBuilder.Redirect.PIPE)
                pbs.getLast().redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }

        /**
         * Starts every process in the pipeline, connected by real OS pipes via
         * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ProcessBuilder.html#startPipeline(java.util.List)">ProcessBuilder#startPipeline</a>,
         * and waits for them to finish unless this is a background job.
         *
         * @return the list of processes in the pipeline
         */
        public List<Process> executePipeline() throws Exception {
            // if(pbs.isEmpty())
            //     return null;
            processes = ProcessBuilder.startPipeline(pbs);
            if(!isBackground)
                waitFor();
            return processes;
        }

        /**
         * Waits for all processes in the pipeline to complete.
         * Does nothing if {@code executePipeline()} has not been called.
         */
        void waitFor(){
            if(processes == null)
                return;
            try{
            for(Process process : processes)
            {
                process.waitFor();
            }
            isDone = true;
        }
        catch(InterruptedException e)
        {
           Thread.currentThread().interrupt();
        }
        }

        /**
         * Terminates all processes in the pipeline. {@link Process#destroy()} only requests
         * termination asynchronously, so this also waits for each process to actually exit
         * before returning.
         */
        public void killPipeline() {
            if(processes == null)
                return;
            for(Process process : processes)
            {
            if (process.isAlive())
                process.destroy();
            }

        try {
        for(Process process : processes)
            process.waitFor();
        isDone = true;
        terminated = true;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
        }

        /**
         * Checks if all processes in the pipeline have finished execution.
         *
         * @return false if any processes are still alive, otherwise true
         */
        public boolean isDone() {
            if (processes == null)
                return false;

            for (Process process : processes) 
            {
            if (process.isAlive())
                return false;
            }

            isDone = true;
            return true;
        }

        /**
         * Checks if the pipeline command is a background job.
         *
         * @return true if pipeline runs in the background, false if runs in the foreground
         */
        public boolean isBackground() {
            return isBackground;
        }

        /**
         * Checks if the pipeline command was terminated with a call to {@code killPipeline}.
         *
         * @return true if {@code killPipeline} was called, otherwise false
         */
        public boolean isTerminated() {
            return terminated;
        }

        public String getPipelineCommand() {
            return pipelineCommand;
        }

        public List<ProcessBuilder> getPbs() {
            return pbs;
        }

        public List<Process> getProcesses() throws Exception{
            // if(processes == null)
            // {
            //     executePipeline();
            //     killPipeline();
            //     return processes;
            // }
            return processes;
        }

        /** This job's {@code jobs} status: {@code "Terminated"}, {@code "Done"}, or {@code "Running"}. */
        public String getStatus() {
            return terminated ? "Terminated" : (isDone() ? "Done" : "Running");
        }

        @Override
        public String toString() {
            return String.format("%-20s %s", getStatus(), pipelineCommand);
        }
    }
}
