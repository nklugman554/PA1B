import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * {@code sort [-r] [<file>...]}: reads every line from all given files (or standard input, if
 * none given), concatenated in argument order, and prints them sorted lexicographically —
 * ascending by default, descending with {@code -r}.
 *
 * <p>Unlike {@code head}/{@code tail}, files are merged into one sorted stream rather than
 * processed (and headed) independently — that's how real {@code sort} behaves too.
 */
public class Sort extends ShellCommand {

    public Sort(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Sort.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        boolean reversed = false;
        if(cmdArgs.length == 0 || (cmdArgs.length == 1 && cmdArgs[0].equals("-r"))) //keyboard input
        {
            if(cmdArgs.length != 0 && cmdArgs[0].equals("-r") && !reversed)
                {
                    reversed = true;
                }
            Scanner scan = new Scanner(System.in);
            ArrayList<String> fileLines = new ArrayList<>();
            while(scan.hasNextLine())
                fileLines.add(scan.nextLine());
            Collections.sort(fileLines);
            if(reversed)
                {
                    for(int j = fileLines.size()-1; j >= 0; j--)
                    {
                        if(fileLines.get(j).equals(""))
                            continue;
                        System.out.println(fileLines.get(j));
                    }
                }
            else
            {
                for(int j = 0; j < fileLines.size(); j++)
                {
                    if(fileLines.get(j).equals(""))
                        continue;
                    System.out.println(fileLines.get(j));
                }
            }
            scan.close();
            return;
        }
        ArrayList<String> names = new ArrayList<>();
        for(String name : cmdArgs)
        {
            if(!name.equals("-r"))
                names.add(name);
        }
        Collections.sort(names);
        for(int i = 0; i < names.size(); i++)
            {
                if(cmdArgs[0].equals("-r") && !reversed)
                {
                    reversed = true;
                }
                File f = new File(names.get(i));
                if(!f.exists())
                {
                    System.err.println("sort: " + f.toString() + ": No such file or directory");
                    continue;
                }
                if(f.isDirectory())
                {
                    System.err.println("sort: " + f.toString() + ": Is a directory");
                    continue;
                }
                ArrayList<String> file = new ArrayList<>();
                Scanner scan = new Scanner(f);
                while(scan.hasNextLine())
                {
                    file.add(scan.nextLine());
                }
                scan.close();
                Collections.sort(file);
                if(reversed)
                {
                    for(int j = file.size()-1; j >= 0; j--)
                    {
                        System.out.println(file.get(j));
                    }
                }
                else
                {

                    for(int j = 0; j < file.size(); j++)
                    {
                        System.out.println(file.get(j));
                    }
                }
            }
    }
}
