import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.File;

/**
 * {@code head [-n <count>] [<file>...]}: prints the first {@code count} lines (default {@code 10})
 * of each file.
 *
 * <p>With more than one file, each file's output is preceded by an {@code "==> <file> <=="}
 * header line, with a blank line separating consecutive files' output (no header for a single
 * file or standard input).
 */
public class Head extends ShellCommand {
    private static final int DEFAULT_COUNT = 10;

    public Head(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Head.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        int count;
        boolean countParam = false;
        boolean multipleFiles = false;
        if(cmdArgs[0].equals("-n"))
        {
            try
            {
                count = Integer.parseInt(cmdArgs[1]);
                countParam = true;
            }
            catch(NumberFormatException e) 
            {
                System.err.println("Usage: head [-n <count>] [<file>...]");
                return;
            }
        }
        else
            count = DEFAULT_COUNT;
        if(cmdArgs.length == 0 || (cmdArgs.length == 2 && cmdArgs[0].equals("-n"))) //keyboard input
        {
            Scanner scan = new Scanner(System.in);
            int j = count;
            while(scan.hasNextLine() && j > 0)
            {
                String line = scan.nextLine();
                if(line.equals(""))
                    continue;
                j--;
                System.out.println(line);
            }
            return;
        }
        for(int i = 0; i < cmdArgs.length; i++)
            {
                if(countParam)
                {
                    i += 2;
                    countParam = false;
                }
                File f = new File(cmdArgs[i]);
                if(!f.exists())
                {
                    System.err.println("head: " + f.toString() + ": No such file or directory");
                    continue;
                }
                if(f.isDirectory())
                {
                    System.err.println("head: " + f.toString() + ": Is a directory");
                    continue;
                }
                Scanner scan = new Scanner(f);
                int j = count;
                if(multipleFiles)
                    System.out.println();
                if(i < cmdArgs.length-1)
                    multipleFiles = true;
                if(multipleFiles)
                    System.out.println("==> " + cmdArgs[i] + " <==");
                while(scan.hasNextLine() && j != 0)
                {
                    j--;
                    System.out.println(scan.nextLine());
                }
            }
            
    }
}
