import java.io.File;
import java.util.Arrays;
import java.nio.file.Path;
/**
 * {@code ls [<path>...]}: lists files and directories. With no arguments, lists the current
 * directory.
 *
 * <p>Files print first (sorted by name), then directories (sorted by name), blank-line separated.
 * A directory's contents get a {@code "<dir>:"} header only when more than one argument was given.
 */

public class Ls extends ShellCommand {

    public Ls(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        ShellCommand.start(Ls.class, args);
    }

    @Override
    protected void runCommand(){
        // TODO: implement Ls.runCommand
        if(cmdArgs.length == 0 || cmdArgs[0].equals("."))
        {
            Path p = Path.of("").toAbsolutePath().normalize();
            File f = new File(p.toString());
            File[] files = f.listFiles();
            String[] filenames = new String[files.length];
            for(int i = 0; i < files.length; i++)
                filenames[i] = files[i].getName();
            Arrays.sort(filenames);
            for(String filename : filenames)
                System.out.println(filename);
            return;
        }
        boolean invalid = false;
        for(String filename : cmdArgs)
        {
            File f = new File(filename);
            if(!f.exists())
            {
                System.err.println("ls: " + f.toString() + ": No such file or directory");
                invalid = true;
                continue;
            }
        }
        boolean hasFiles = false;
        boolean hasDirectories = false;
        if(invalid)
        {
            for(int h = 0; h < cmdArgs.length; h++)
            {
                File f = new File(cmdArgs[h]);
                if(!f.exists())
                    continue;
                else if(!f.isDirectory())
                {
                    System.out.println(f);
                    hasFiles = true;
                }
                else
                    hasDirectories = true;
            }
        if(hasDirectories && hasFiles)
            System.out.println();
        }
        else
        {
            for(int h = cmdArgs.length-1; h >= 0; h--)
            {
                File f = new File(cmdArgs[h]);
                if(!f.isDirectory() && f.exists())
                {
                    System.out.println(f);
                    hasFiles = true;
                }
                else
                    hasDirectories = true;
            }
            if(hasDirectories && hasFiles)
                System.out.println();
        }
        for(int i = cmdArgs.length-1; i >= 0; i--)
        {
           //String f = Paths.get(filename).toString();
           File f = new File(cmdArgs[i]);
            if(f.isDirectory() && f.exists())
            {
                if(cmdArgs.length > 1)
                    System.out.println(f + ":");
                File[] files = f.listFiles();
                for(int j = files.length-1; j >= 0; j--)
                    System.out.println(files[j].getName());
                if(i > 0)
                    System.out.println();
            }
        }
    }   
}
    

