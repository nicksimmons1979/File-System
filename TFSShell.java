//*****************************************************************************************************************************
// TFSShell.java - August 1 2016
// Nick Simmons T00033019
// Command shell for exercising TFSFileSystem
//*****************************************************************************************************************************

import java.io.*;
import java.util.*;

// command shell
public class TFSShell extends Thread  
{	
	public TFSShell()
	{
		
	}
	
	public void run()
	{
		readCmdLine();
	}
	
	// User interface routine 
	void readCmdLine()
	{
		String line, cmd, arg1, arg2, arg3, arg4;
		StringTokenizer stokenizer;
		Scanner scanner = new Scanner(System.in);

		System.out.println("Hal: Good morning, Dave!\n");
		
		while(true) {
			
			System.out.print("ush> ");
			
			line = scanner.nextLine();
			line = line.trim();
			stokenizer = new StringTokenizer(line);
			if (stokenizer.hasMoreTokens()) {
				cmd = stokenizer.nextToken();
				
				if (cmd.equals("mkfs"))
					mkfs();
				else if (cmd.equals("mount"))
					mount();
				else if (cmd.equals("unmount"))
					unmount();
				else if (cmd.equals("sync"))
					sync();
				else if (cmd.equals("prrfs"))
					prrfs();
				else if (cmd.equals("prmfs"))
					prmfs();				
					
				else if (cmd.equals("mkdir")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						mkdir(arg1);					
					}
					else
						System.out.println("Usage: mkdir directory");
				}
				else if (cmd.equals("rmdir")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						rmdir(arg1);					
					}
					else
						System.out.println("Usage: rmdir directory");
				}
				else if (cmd.equals("ls")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						ls(arg1);					
					}
					else
						System.out.println("Usage: ls directory");
				}
				else if (cmd.equals("create")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						create(arg1);					
					}
					else
						System.out.println("Usage: create file");
				}
				else if (cmd.equals("rm")) {
					if (stokenizer.hasMoreTokens()) {
						arg1 = stokenizer.nextToken();
						rm(arg1);					
					}
					else
						System.out.println("Usage: rm file");
				}
				else if (cmd.equals("print")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: print file position number");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: print file position number");
						continue;
					}					
					if (stokenizer.hasMoreTokens())
						arg3 = stokenizer.nextToken();
					else {
						System.out.println("Usage: print file position number");
						continue;
					}	
					try {
						print(arg1, Integer.parseInt(arg2), Integer.parseInt(arg3));
					} catch (NumberFormatException nfe) {
						System.out.println("Usage: print file position number");
					}			
				}
				else if (cmd.equals("append")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: append file number");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: append file number");
						continue;
					}					
					try {
						append(arg1, Integer.parseInt(arg2));
					} catch (NumberFormatException nfe) {
						System.out.println("Usage: append file number");
					}			
				}
				else if (cmd.equals("cp")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: cp file directory");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: cp file directory");
						continue;
					}					
					cp(arg1, arg2);
				}
				else if (cmd.equals("rename")) {
					if (stokenizer.hasMoreTokens())
						arg1 = stokenizer.nextToken();
					else {
						System.out.println("Usage: rename src_file dest_file");
						continue;
					}
					if (stokenizer.hasMoreTokens())
						arg2 = stokenizer.nextToken();
					else {
						System.out.println("Usage: rename src_file dest_file");
						continue;
					}					
					rename(arg1, arg2);
				}
					
				else if (cmd.equals("exit")) {
					exit();
					System.out.println("\nHal: Good bye, Dave!\n");
					scanner.close();
					break;
				}
				
				else
					System.out.println("-ush: " + cmd + ": command not found");
			}
		}
	}


/*
 * You need to implement these commands
 */
 	
	// create a drive file and file system
	void mkfs()
	{
		if(TFSFileSystem.tfs_mkfs() == 0)
			System.out.println("File system creation successful");
		else
			System.out.println("Does the volume already exist? Try 'mount'");
	}
	
	void mount()
	{
		if(TFSFileSystem.tfs_mount() == 0)
			System.out.println("Volume mounted");
		else
			System.out.println("Mount failed");
	}
	
	void sync()
	{
		if(TFSFileSystem.tfs_sync() == 0)
			System.out.println("Volume synchronized");
		else
			System.out.println("Synchronization failed");
	}
	
	// dump file system from disk to console
	void prrfs()
	{
		if (!TFSFileSystem.tfs_getMountStatus())
			System.out.println("Volume not mounted");
		else
			System.out.println(TFSFileSystem.tfs_prrfs());
	}
	
	// dump file system from memory to console
	void prmfs()
	{
		if (!TFSFileSystem.tfs_getMountStatus())
			System.out.println("Volume not mounted");
		else
			System.out.println(TFSFileSystem.tfs_prmfs());
	}

	void mkdir(String directory)
	{
		return;
	}
	
	void rmdir(String directory)
	{
		return;
	}
	
	void ls(String directory)
	{
		return;
	}
	
	void create(String file)
	{
		return;
	}
	
	void rm(String file)
	{
		return;
	}
	
	void print(String file, int position, int number)
	{
		return;
	}
	
	void append(String file, int number)
	{
		return;
	}
	
	void cp(String file, String directory)
	{
		return;
	}
	
	void rename(String source_file, String destination_file)
	{
		return;
	}
	
	void exit()
	{
		TFSFileSystem.tfs_exit();
	}
	
	void unmount()
	{
		if(TFSFileSystem.tfs_unmount() == 0)
			System.out.println("Volume unmounted");
		else
			System.out.println("Failure to unmount volume");	
	}
}

 // main method
class TFSMain
{
	public static void main(String argv[]) throws InterruptedException
	{
		TFSFileSystem tfs = new TFSFileSystem();
		TFSShell shell = new TFSShell();
		
		shell.start();
	//	try {
			shell.join();
	//	} catch (InterruptedException ie) {}
	}
}



