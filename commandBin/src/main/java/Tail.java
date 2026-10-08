import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;

/**
 * {@code tail [-n <count>] [<file>...]}: prints the last {@code count} lines (default {@code 10})
 * of each file.
 *
 * <p>With more than one file, each file's output is preceded by an {@code "==> <file> <=="}
 * header line, with a blank line separating consecutive files' output (no header for a single
 * file or standard input).
 */
public class Tail extends ShellCommand {
    private static final int DEFAULT_COUNT = 10;

    public Tail(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Tail.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        //create an array list, remove one line at a time
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
                System.err.println("Usage: tail [-n <count>] [<file>...]");
                return;
            }
        }
        else
            count = DEFAULT_COUNT;
        if(cmdArgs.length == 0 || (cmdArgs.length == 2 && cmdArgs[0].equals("-n"))) //keyboard input
        {
            Scanner scan = new Scanner(System.in);
            int j = count;
            ArrayList<String> fileLines = new ArrayList<>();
            while(scan.hasNextLine())
                fileLines.add(scan.nextLine());
            if(count > fileLines.size())
                    j = fileLines.size();
            for(int k = fileLines.size()-j; k < fileLines.size(); k++)
                {
                    if(fileLines.get(k).equals(""))
                    {
                        if(k > 0)
                        {
                            k--;
                            System.out.println(fileLines.get(k));
                            break;
                        }
                    }
                    System.out.println(fileLines.get(k));
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
                    System.err.println("tail: " + f.toString() + ": No such file or directory");
                    continue;
                }
                if(f.isDirectory())
                {
                    if(multipleFiles)
                        System.out.println();
                    System.err.println("tail: " + f.toString() + ": Is a directory");
                    continue;
                }
                ArrayList<String> file = new ArrayList<>();
                Scanner scan = new Scanner(f);
                while(scan.hasNextLine())
                {
                    file.add(scan.nextLine());
                }
                scan.close();
                if(multipleFiles)
                    System.out.println();
                if(i < cmdArgs.length-1)
                    multipleFiles = true;
                int j = count;
                if(count > file.size())
                    j = file.size();
                if(multipleFiles)
                    System.out.println("==> " + cmdArgs[i] + " <==");
                for(int k = file.size()-j; k < file.size(); k++)
                {
                    System.out.print(file.get(k));
                    if(k != file.size()-1 || !multipleFiles)
                        System.out.println();
                }
            }
    }
}
