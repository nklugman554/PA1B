import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * {@code uniq [-c] [<file>]}: collapses runs of <b>adjacent</b> duplicate lines into a single
 * line — non-adjacent duplicates are left alone, same as real {@code uniq} (pair with
 * {@code sort} first to dedupe an entire file). With {@code -c}, each output line is prefixed
 * with the number of times it occurred in its run.
 *
 * <p>Unlike the other commands, {@code uniq} takes at most <b>one</b> file argument — that's a
 * real constraint of the actual Unix command, not a simplification for this project.
 */
public class Uniq extends ShellCommand {

    public Uniq(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Uniq.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        boolean countParam = false;
        if(cmdArgs.length != 0 && cmdArgs[0].equals("-c"))
        {    
            countParam = true;
            if(cmdArgs.length > 2)
            {
                System.err.println("Usage: uniq [-c] [<file>]");
                return;
            }
        }
        if(cmdArgs.length == 0 || (cmdArgs.length == 1 && cmdArgs[0].equals("-c"))) //keyboard input
        {
            Scanner scan = new Scanner(System.in);
            int count = 1;
            int index = 0;
            String lastLine = "";
            while(scan.hasNextLine())
            {
                String line = scan.nextLine();
                if(line.equals(""))
                    continue;
                if(index == 0)
                {
                    lastLine = line;
                    index++;
                    continue;
                }
                if(line.equals(lastLine))
                {
                    count++;
                }
                else
                {
                    if(countParam)
                        System.out.print(count + " ");
                    System.out.println(lastLine);
                    count = 1;
                    lastLine = line;
                }
            }
            if(countParam)
                System.out.print(count + " ");
            System.out.println(lastLine);
            scan.close();
            return;
        }
        if(!cmdArgs[0].equals("-c") && cmdArgs.length > 1)
            {
                System.err.println("Usage: uniq [-c] [<file>]");
                return;
            }
        int i = 0;
        if(countParam)
            i++;
        File f = new File(cmdArgs[i]);
        if(!f.exists())
            {
                System.err.println("uniq: " + f.toString() + ": No such file or directory");
                return;
            }
            if(f.isDirectory())
            {
                System.err.println("uniq: " + f.toString() + ": Is a directory");
                return;
            }
        Scanner scan = new Scanner(f);
            int count = 1;
            int index = 0;
            String lastLine = "";
            while(scan.hasNextLine())
            {
                String line = scan.nextLine();
                if(index == 0)
                {
                    lastLine = line;
                    index++;
                    continue;
                }
                if(line.equals(lastLine))
                {
                    count++;
                }
                else
                {
                    if(countParam)
                        System.out.print(count + " ");
                    System.out.println(lastLine);
                    count = 1;
                    lastLine = line;
                }
            }
            if(countParam)
                System.out.print(count + " ");
            System.out.println(lastLine);
            scan.close();
        }
    }
    

