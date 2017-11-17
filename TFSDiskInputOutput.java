//*****************************************************************************************************************************
// TFSDiskInputOutput.java - August 1 2016
// Nick Simmons T00033019
// Disk I/O API for TFSFileSystem.java
//*****************************************************************************************************************************
import java.io.*;

public class TFSDiskInputOutput
{
	private static final short BLOCK_SIZE = 128;
	private static RandomAccessFile raf = null;
	private static File f;
	private static boolean driveStatus;

	// create disk file
	public static int tfs_dio_create(byte[] name, int nlength, int size)
	{
		String fileName;
			
		// create disk file
		try
		{
			fileName = new String(name, "UTF-8");
			
			f = new File(fileName);
			if (!f.exists()) // check if the file exists
			{
				f.createNewFile(); // create a new file	
				raf = new RandomAccessFile(f, "rw"); // open a file for random access with "r", "rw"
				raf.setLength(size * BLOCK_SIZE);
			}
			else
				return -1;
		}
			
		catch (IOException e)
		{
			System.out.println("Uh oh, got an IOException error!" + e.getMessage());
			return -1;
		}
			
		finally
		{
			// close file after use
			if (raf != null)
			{
				try
				{
					raf.close();
				}
					
				catch (IOException ioe) { }
			}
		}
			
		return 0;
	}	
	
	// open existing drive file
	public static int tfs_dio_open(byte[] name, int nlength)
	{
		String fileName;
		
		// open drive file
		try
		{
			fileName = new String(name, "UTF-8");
		
			f = new File(fileName);
			if (f.exists()) // check if the file exists
				raf = new RandomAccessFile(f, "rw"); // open a file for random access with "r", "rw"

			else
				return -1;
		}
		
		catch (IOException e)
		{
			System.out.println("Uh oh, got an IOException error!" + e.getMessage());
			return -1;
		}
		
		driveStatus = true;
		return 0;
	}			
	
	// return number of blocks in file
	public static int tfs_dio_get_size()
	{		
		int fileSize = -1;
		
		try
		{
			if (f.exists()) // check if the file exists
				fileSize = ((int)f.length() / BLOCK_SIZE);	
		}
		
		catch (NullPointerException e)
		{
			System.out.println("Uh oh, got an IOException error!" + e.getMessage());
			return fileSize;
		}

		return fileSize;
	}							
	
	// read a specified disk block
	public static int tfs_dio_read_block(int block_no, byte[] buf)
	{	
		try
		{
			raf.seek(block_no*BLOCK_SIZE);
			raf.read(buf, 0, BLOCK_SIZE);
		}
		
		catch (NullPointerException npe)
		{
			System.out.println("Uh oh, got an IOException error!" + npe.getMessage());
			return -1;			
		}
		catch (IOException e)
		{
			System.out.println("Uh oh, got an IOException error!" + e.getMessage());
			return -1;
		}
		
		return 0;
	}
	
	// write a specified disk block
	public static int tfs_dio_write_block(int block_no, byte[] buf)	
	{	
		try
		{
			raf.seek(block_no*BLOCK_SIZE);
			raf.write(buf, 0, BLOCK_SIZE);
		}
		catch (NullPointerException npe)
		{
			System.out.println("Uh oh, got an IOException error!" + npe.getMessage());
			return -1;			
		}
		
		catch (IOException e)
		{
			System.out.println("Uh oh, got an IOException error!" + e.getMessage());
			return -1;
		}
		
		return 0;
	}
	
	// close disk file
	public static int tfs_dio_close()		
	{
		// close disk if open
		if (raf != null)
		{
			try
			{
				raf.close();
				driveStatus = false;
				return 0;
			}
				
			catch (IOException ioe) { }
			
		}
		
		// failure to close file
		return -1;
	}			
	
	// return block of disk size in bytes
	public static int getBlockSize()
	{
		return BLOCK_SIZE;
	}
	
	public static boolean getDriveStatus()
	{
		return driveStatus;
	}
}