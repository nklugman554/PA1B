//import static org.junit.jupiter.api.DynamicTest.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * {@code cat}: prints the contents of one or more files to standard out, in order.
 *
 * <p>On a missing file or directory, print the error and keep going with the rest —
 * {@link #getFileInput} already throws a ready-to-print {@link IllegalArgumentException}.
 */
public class Cat extends ShellCommand {

    public Cat(String[] args) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        ShellCommand.start(Cat.class, args);
    }

    @Override
    protected void runCommand() throws FileNotFoundException{
        if(cmdArgs.length == 0)
        {
            Scanner scan = new Scanner(System.in);
            String str = "";
            while(scan.hasNextLine())
            {
                String line = scan.nextLine();
                 if(line.equals("exit"))
                {
                    System.out.println(str);
                    return;
                }
                if(line.equals("")) //weird symbol appears when running code
                {
                    str = str.substring(0, str.length()-1);
                    //break;
                }
                str += line;
                if(scan.hasNextLine())
                    str += "\n";
            }
            System.out.print(str);
            scan.close();
        }
        boolean error = false;
        boolean validFiles = false;
        for(String filename : cmdArgs)
        {
        File f = new File(filename);
        if(f.isDirectory())
        {
            if(validFiles)
                System.out.println(); //space between the file content and errors
            System.err.println("cat: " + f.toString() + ": Is a directory");
            error = true;
            continue;
        }
        if(!f.exists())
        {
            if(validFiles)
                System.out.println();
            System.err.println("cat: " + f.toString() + ": No such file or directory");
            error = true;
            continue;
        }
        Scanner scan = new Scanner(f);
        validFiles = true;
        while(scan.hasNextLine())
        {
            String line = scan.nextLine();
            if(scan.hasNextLine())
                System.out.println(line);
            else
                System.out.print(line);
        }
        scan.close();
        }
        if(!error)
            System.out.println();
    }
    
    }

