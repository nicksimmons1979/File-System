//*****************************************************************************************************************************
// ExerciseDIO.java - July 25 2016
// Nick Simmons T00033019
// Driver class to exercise methods of TFSDiskInputOut.java
//*****************************************************************************************************************************

import java.util.Arrays;
import java.util.Random;

public class ExerciseDIO
{
	private static final int MAXIMUM_BLOCK_COUNT = 65535; 
	private static String name = "TFSDiskFile";
	private static byte[] diskName = name.getBytes(); 
	private static byte[] writeBuffer = new byte[128];
	private static byte[] readBuffer = new byte[128];
	
	public static void testDIO()
	{
		Random rand = new Random();
		int randomBlock = rand.nextInt(MAXIMUM_BLOCK_COUNT);
		
		// fill buffer with zeros;
		for (int index = 0; index < writeBuffer.length; index++)
			writeBuffer[index] = (byte)index;
	
		// compare readBuffer / writeBuffer pre file access
		System.out.println("RW buffers before file access");
		System.out.println("writeBuffer");
		System.out.println(Arrays.toString(writeBuffer));
		System.out.println("readBuffer");
		System.out.println(Arrays.toString(readBuffer));
		
		// create disk file
		System.out.println("\nCreating disk file: " + name);
		if(TFSDiskInputOutput.tfs_dio_create(diskName, diskName.length, MAXIMUM_BLOCK_COUNT) == 0)
			System.out.println("Disk created successfully");
		else
			System.out.println("Failure to create disk");
	
		// open disk file
		System.out.println("Opening disk file " + name);
		if (TFSDiskInputOutput.tfs_dio_open(diskName, diskName.length) == 0)
			System.out.println("Disk opened successfully");
		else
			System.out.println("Failure to open disk");

		// get disk block count
		System.out.println("Disk block count:" + TFSDiskInputOutput.tfs_dio_get_size());
	
		// write block to disk
		System.out.println("Write to block " + randomBlock + " on disk...");
		if (TFSDiskInputOutput.tfs_dio_write_block(randomBlock, writeBuffer) == 0)
			System.out.println("Successful block write");
		else
			System.out.println("Block write failed");

		// read block from disk 
		System.out.println("Read block " + randomBlock + " from disk...");
		if (TFSDiskInputOutput.tfs_dio_read_block(randomBlock, readBuffer) == 0)
			System.out.println("Successful block read");
		else
			System.out.println("Block read failed");
		
		// close disk file
		System.out.println("Closing disk file");
		if(TFSDiskInputOutput.tfs_dio_close() == 0)
			System.out.println("Disk closed successfully");
		else
			System.out.println("Failure to close disk");
	
		// compare readBuffer / writeBuffer
		System.out.println("RW buffers after file access");
		System.out.println("writeBuffer");
		System.out.println(Arrays.toString(writeBuffer));
		System.out.println("readBuffer");
		System.out.println(Arrays.toString(readBuffer));
	}
	
	// write zeros to entire disk
	public static void formatDisk()
	{
		byte[] buffer = new byte[128];
		Arrays.fill(buffer,(byte)0);
		
		for (int index = 0; index < MAXIMUM_BLOCK_COUNT; index++)
			TFSDiskInputOutput.tfs_dio_write_block(index, buffer);
	}
}