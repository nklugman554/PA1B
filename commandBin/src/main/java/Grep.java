import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/**
 * {@code grep <pattern> [<file>...]}: prints every line, from the given files or from standard
 * input, that matches the {@link java.util.regex.Pattern regular expression} {@code pattern}.
 *
 * <p>With more than one file, each matching line is prefixed with {@code "<filename>:"}.
 */
public class Grep extends ShellCommand {

    public Grep(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Grep.class, args);
    }

    @Override
    protected void runCommand() throws IOException{
        // TODO: implement Grep.runCommand
        if(cmdArgs.length == 0)
        {
            System.err.println("Usage: grep <pattern> [<file>...]");
            return;
        }
        Pattern p = Pattern.compile(cmdArgs[0]);
        String search = p.pattern();
        if(cmdArgs.length == 1)
        {
            Scanner scan = new Scanner(System.in);
            String str = "";
            while(scan.hasNextLine())
            {
                String line = scan.nextLine();
                if(line.contains(search))
                    str += line + "\n";
            }
            System.out.print(str);
            scan.close();
        }
        boolean multipleFiles = false;
        if(cmdArgs.length > 2)
            multipleFiles = true;
        for(int i = 1; i < cmdArgs.length; i++)
        {
        String filename = cmdArgs[i];
        File f = new File(filename);
        if(f.isDirectory())
        {
            System.err.println("grep: " + f.toString() + ": Is a directory");
            continue;
        }
        if(!f.exists())
        {
            System.err.println("grep: " + f.toString() + ": No such file or directory");
            continue;
        }
        Scanner scan = new Scanner(f);
        while(scan.hasNextLine())
        {
            String line = scan.nextLine();
            //Matcher matcher = p.matcher(line);
            if(line.contains(search))
                {
                if(multipleFiles)
                    System.out.print(filename + ":");
                if(scan.hasNextLine())
                    System.out.println(line);
                else
                    System.out.print(line);
                }
        }
        scan.close();
        }
    }
}
