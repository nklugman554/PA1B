import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.nio.file.Path;

/**
 * {@code wc [<file>...]}: prints newline, word, and byte counts for each file, and a total line
 * if more than one file is specified. A word is a non-zero-length sequence of characters
 * delimited by white space.
 */
public class Wc extends ShellCommand {
    /**
     * Format specifier for printing output counts. Do not change.
     */
    public static final String formatSpecifier = "%8d %8d %8d %s";

    public Wc(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Wc.class, args);
    }

    @Override
    protected void runCommand() throws IOException {
        int lines = 0;
        int words = 0;
        int chars = 0;
        if(cmdArgs.length == 0)
        {
            Scanner scan = new Scanner(System.in);
            while(scan.hasNextLine())
            {
                String line = scan.nextLine();
                if(line.equals(""))
                    continue;
                lines++;
                chars += line.length()+1;
                int end;
                while(line.length() > 0)
                {
                    end = line.indexOf(" ");
                    if(end == -1)
                        end = line.length();
                    words++;
                    if(end != line.length())
                        line = line.substring(end+1, line.length());
                    else
                        line = "";
                }
            }
            System.out.println(String.format(formatSpecifier, lines, words, chars, ""));
            scan.close();
        }
        for(String filename : cmdArgs)
        {
            Path p = Path.of(filename);
            int l = 0;
            int w = 0;
            int c = 0;
            File f = new File(p.toString());
            if(f.isDirectory())
            {
                System.err.println("wc: " + f.toString() + ": Is a directory");
                System.out.println(String.format(formatSpecifier, l, w, c, filename));
                continue;
            }
            if(!f.exists())
            {
                System.err.println("wc: " + f.toString() + ": No such file or directory");
                continue;
            }
            Scanner scan = new Scanner(f);
            while(scan.hasNextLine())
            {
                l++;
                lines++;
                String line = scan.nextLine();
                int end = 0;
                c += line.length() + 1;
                chars += line.length() + 1;
                if(!scan.hasNextLine() && !line.equals(""))
                {
                    c--;
                    chars--;
                }
                while(line.length() > 0)
                {
                    end = line.indexOf(" ");
                    if(end == -1)
                        end = line.length();
                    w++;
                    words++;
                    if(end != line.length())
                        line = line.substring(end+1, line.length());
                    else
                        line = "";
                }
            }
        System.out.println(String.format(formatSpecifier, l, w, c, filename));
        scan.close();
    }
    if(cmdArgs.length > 1)
        System.out.println(String.format(formatSpecifier, lines, words, chars, "total"));
    }
}
