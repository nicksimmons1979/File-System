//*****************************************************************************************************************************
// TFSFileSystem.java - August 1 2016
// Nick Simmons T00033019
// TFS API
//*****************************************************************************************************************************

import java.util.*;

// BCB Structure (128B) - Boot control block, reserved for future use.
// random temp data

// FAT Structure (65535B for current implementation) - holds 32767 block locations, split into byte arrays for disk i/o
// byte[] MSB (1B) * MAXIMUM_BLOCK_COUNT			   Index 0 unused due to fat record 0 = FREE_BLOCK	
// byte[] LSB (1B) * MAXIMUM_BLOCK_COUNT

// superBlock structure (8B) - Points to root block, first free block, total blocks and fat size in blocks
// rootPointer  (2B)
// freeBlock	(2B)
// blockCount	(2B)
// fatSize		(2B)

// Directory structure (29B) - 	Number of entries, parent block number, File name, is a file or directory, file size, location of files first block, 
// noEntries 	(4B)			To be implemented as ArrayList of Byte array
// parentBlockNo(2B)
// fileName 	(15B)			
// nameLength	(1B)
// isFile 		(1B)
// fileSize		(4B)
// firstBlock	(2B)


// FDT structure (27B)	-	File name, name length, is a file or directory, file size, location of files first block, offset for process R/W
// fileName		(15B)		To be implemented as ArrayList of Byte array
// nameLength	(1B)
// isFile 		(1B)
// fileSize 	(4B)	
// firstBlock 	(2B)
// filePointer	(4B)


public class TFSFileSystem
{	
	// constants
	private static final short MAXIMUM_BLOCK_COUNT = 32767;
	private static final short BCB_LOCATION = 0; // block location
	private static final short SUPER_BLOCK_LOCATION = 1; // block location
	private static final short FAT_LOCATION = 2; // block location
	private static final byte[] EOF = {(byte)(-1 & 0xff), (byte)(-1 >> 8 & 0xff)}; // end of file marker
	private static final byte[] FREE_BLOCK = {(byte)((0) & 0xff), (byte)((0) >> 8 & 0xff)}; // end of file marker
	private static final int FD_SIZE = 27;
	private static final int DIRECTORY_SIZE = 29;
	
	private static final String fileName = "TFSDiskFile";
	private static final byte[] driveName = fileName.getBytes();
	private static boolean mountStatus = false;
	
	// create arrays for drive storage, arrays can also serve as in memory storage
	private static byte[] bcb = new byte[TFSDiskInputOutput.getBlockSize()];
	private static byte[] superBlock = new byte[TFSDiskInputOutput.getBlockSize()];
	private static byte[] fatMSB = new byte[MAXIMUM_BLOCK_COUNT];
	private static byte[] fatLSB = new byte[MAXIMUM_BLOCK_COUNT];
	private static List<byte[]> directoryList = new ArrayList<byte[]>();
	private static List<byte[]> fileDescriptorTable = new ArrayList<byte[]>();
	
	/*
	 * TFS Constructor
	 */
	 
	public TFSFileSystem()
	{
		
	}
	
	/*
	 * TFS API
	 */
	 
	public static int tfs_mkfs()
	{
		// to store data for root creation
		byte[] rootBlock = new byte[TFSDiskInputOutput.getBlockSize()];
		int rootFD;
		short rootLocation;
		
		// don't create volume if it already exists
		if (mountStatus)
		{
			System.out.println("Volume already exists");
			return -1;
		}
		// initialize BCB
		Arrays.fill(bcb,(byte)1); // temp garbage
				
		//initialize super block array
		// compute pointers, root directory
		superBlock[0] = (byte)((fatMSB.length * 2 / TFSDiskInputOutput.getBlockSize() + FAT_LOCATION + 2) & 0xff);
		superBlock[1] = (byte)(((fatMSB.length * 2 / TFSDiskInputOutput.getBlockSize() + FAT_LOCATION + 2) >> 8) & 0xff);
		
		// first free block
		superBlock[2] = (byte)((fatMSB.length * 2 / TFSDiskInputOutput.getBlockSize() + FAT_LOCATION + 3) & 0xff);
		superBlock[3] = (byte)(((fatMSB.length * 2 / TFSDiskInputOutput.getBlockSize() + FAT_LOCATION + 3) >> 8) & 0xff);
			
		// compute block count and fat size in blocks
		superBlock[4] = (byte)(MAXIMUM_BLOCK_COUNT & 0xff);
		superBlock[5] = (byte)((MAXIMUM_BLOCK_COUNT >> 8) & 0xff);
		
		short temp; // needed for ceiling fxn before bit mask
		temp = (short)Math.ceil((fatMSB.length * Short.SIZE / 8.0 / TFSDiskInputOutput.getBlockSize()));	
		superBlock[6] = (byte)Math.ceil((temp) & 0xff);
		temp = (short)Math.ceil(((fatLSB.length * Short.SIZE / 8.0) / TFSDiskInputOutput.getBlockSize()));	
		superBlock[7] = (byte)Math.ceil(((temp) >> 8) & 0xff);
		
		// create root directory
		// create a file descriptor
		rootLocation = (short)((superBlock[1] << 8) + (superBlock[0] & 0xff));
		rootFD = _tfs_open_fd("/".getBytes(), (byte)"/".length(), FD_SIZE, (short)rootLocation);	
	
		// create a directory listing
		_tfs_create_dir("/".getBytes(), (byte)"/".length(), FD_SIZE, (short)rootLocation, (byte)0);

		// write location to fat, fat index FAT_ROOT_LOCATION (0 is reserved), holds block ID of root fd/dir
		fatMSB[2] = superBlock[0];
		fatLSB[3] = superBlock[1];
		
		// create block for writing
		_tfs_puts_bytes_block(rootBlock, 0, fileDescriptorTable.get(rootFD), FD_SIZE);
		
		// create drive
		// create disk file
		if(TFSDiskInputOutput.tfs_dio_create(driveName, driveName.length, MAXIMUM_BLOCK_COUNT) != 0)
		{
				System.out.println("Failure to create disk file");
				return -1;
		}
	
		// store BCB to block BCB_LOCATION
		if (_tfs_write_bcb() != 0)
		{
			System.out.println("Boot control block write failed");
			return -1;
		}
		
		// store super block to block SUPER_BLOCK_LOCATION
		if (_tfs_write_pcb() != 0)
		{
			System.out.println("SuperBlock write failed");
			return -1;
		}

		// store FAT to drive
		if (_tfs_write_fat() != 0)
		{
			System.out.println("FAT write failed");
			return -1;
		}
		
		// write root FD to disk
		// should probably use the tfs_write_bytes_fd
		if (_tfs_write_bytes_fd(rootFD, rootBlock, FD_SIZE) != 0 )
		{
			System.out.println("Root creation failed");
			return -1;
		}
		
		return 0;
	}						

	public static int tfs_mount()
	{
		// check if drive mounted
		if (mountStatus)
		{
			return -1;
		}
		
		// read boot control block from BCB_BLOCK_LOCATION 
		if (_tfs_read_bcb() != 0)
		{
			System.out.println("Boot control block read failed");
			return -1;
		}
		
		// read super block from SUPER_BLOCK_LOCATION 
		if (_tfs_read_pcb() != 0)
		{
			System.out.println("SuperBlock read failed");
			return -1;
		}
		
		// load FAT from disk to memory
		if (_tfs_read_fat() != 0)
		{
			System.out.println("FAT read failed");
			return -1;
		}
		
		// drive now mounted
		mountStatus = true;
		return 0;
	}					

	public static int tfs_unmount()
	{
		// check if drive mounted
		if (!mountStatus)
		{
			return -1;
		}
		
		// store bcb block to disk
		if (_tfs_write_bcb() != 0)
		{
			System.out.println("Boot control block write failed");
			return -1;
		}
		
		// store super block to disk
		if (_tfs_write_pcb() != 0)
		{
			System.out.println("SuperBlock write failed");
			return -1;
		}
		
		// store FAT to disk
		if (_tfs_write_fat() != 0)
		{
			System.out.println("FAT write failed");
			return -1;
		}
		
		// set mount flag to false
		mountStatus = false;
		
		// flush in memory structures
		Arrays.fill(bcb,(byte)0);
		Arrays.fill(superBlock,(byte)0);
		Arrays.fill(fatMSB,(byte)0);
		Arrays.fill(fatLSB,(byte)0);
		
		return 0;
	}						

	public static int tfs_sync()	
	{
		// check if drive mounted
		if (!mountStatus)
		{
			return -1;
		}
		
		// store bcb block to block BCB_BLOCK_LOCATION
		if (_tfs_write_bcb() != 0)
		{
			System.out.println("Boot control block write failed");
			return -1;
		}
		
		// store super block to block SUPER_BLOCK_LOCATION
		if (_tfs_write_pcb() != 0)
		{
			System.out.println("SuperBlock write failed");
			return -1;
		}
		
		// store FAT to drive
		if (_tfs_write_fat() != 0)
		{
			System.out.println("FAT write failed");
			return -1;
		}
		
		return 0;
	}						

	// return string with block and fat info of drive
	public static String tfs_prrfs()	
	{		
		// create arrays for drive storage
		byte[] bcbTemp = new byte[TFSDiskInputOutput.getBlockSize()];
		byte[] superBlockTemp = new byte[TFSDiskInputOutput.getBlockSize()];
		byte[] fatMSBTemp = new byte[MAXIMUM_BLOCK_COUNT];
		byte[] fatLSBTemp = new byte[MAXIMUM_BLOCK_COUNT];
		
		String diskInfo = "";
		short bitshiftTemp; // used for byte->short conversion
				
		// open drive to read
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return "Fail to open drive";
		}
		
		// read BCB from block BCB_LOCATION
		if (TFSDiskInputOutput.tfs_dio_read_block(BCB_LOCATION, bcbTemp) != 0)
		{
			System.out.println("BCB read failed");
			return "BCB read failed";
		}
		
		// read super block from block SUPER_BLOCK_LOCATION
		if (TFSDiskInputOutput.tfs_dio_read_block(SUPER_BLOCK_LOCATION, superBlockTemp) != 0)
		{
			System.out.println("SuperBlock read failed");
			return "SuperBlock read failed";
		}
		
		// read fat from block 2 -> (MAXIMUM_BLOCK_COUNT / TFSDiskInputOutput.getBlockSize())
		byte[] rBuffer = new byte[TFSDiskInputOutput.getBlockSize()];
		int fatIndex = 0;
		
		// read all fat blocks from disk
		for (int readIndex = 0; readIndex < (int) Math.ceil(fatMSBTemp.length * 2 / TFSDiskInputOutput.getBlockSize()); readIndex++)
		{			
			// read single fat block from disk
			if (TFSDiskInputOutput.tfs_dio_read_block(FAT_LOCATION+readIndex, rBuffer) != 0)
			{
				System.out.println("FAT read failed");
				return "FAT read failed";
			}
			
			// construct fat byte array from read block
			for (int bufferIndex = 0; bufferIndex < TFSDiskInputOutput.getBlockSize(); fatIndex++,bufferIndex += 2)
			{
				fatMSBTemp[fatIndex] = rBuffer[bufferIndex];
				fatLSBTemp[fatIndex] = rBuffer[bufferIndex+1];
			}
		}

		// convert byte arrays to string
		
		// convert BCB to string
		diskInfo = diskInfo.concat("BCB: Block " + BCB_LOCATION );
		
		for (int index = 0; index < TFSDiskInputOutput.getBlockSize(); index++)
		{			
			if (index % 32 == 0) // for column count
				diskInfo = diskInfo.concat("\n");
			diskInfo = diskInfo.concat(" " + bcbTemp[index]);	
		}

		// convert SuperBlock to string
		diskInfo = diskInfo.concat("\n\nSuperBlock: Block " + SUPER_BLOCK_LOCATION);
		
		for (int index = 0; index < TFSDiskInputOutput.getBlockSize(); index++)
		{			
			if (index % 32 == 0) // for column count
				diskInfo = diskInfo.concat("\n"); // for column count
			diskInfo = diskInfo.concat(" " + superBlockTemp[index]);	
		}

		bitshiftTemp = (short)((superBlockTemp[1] << 8) + (superBlockTemp[0] & 0xff));
		diskInfo = diskInfo.concat("\n\nrootPointer: block " + bitshiftTemp);
		bitshiftTemp = (short)((superBlockTemp[3] << 8) + (superBlockTemp[2] & 0xff));
		diskInfo = diskInfo.concat("\nfreeBlock: block " + bitshiftTemp );
		bitshiftTemp = (short)((superBlockTemp[5] << 8) + (superBlockTemp[4] & 0xff));
		diskInfo = diskInfo.concat("\nblockCount: " + bitshiftTemp);
		bitshiftTemp = (short)((superBlockTemp[7] << 8) + (superBlockTemp[6] & 0xff));
		diskInfo = diskInfo.concat("\nfatSize in blocks: " + bitshiftTemp);
		

		// convert FAT to string
		// last 63 elements read from disk are past FAT data. exclude from string
		// since DIO reads in blocks of 128 bytes, last block will have garbage
		bitshiftTemp = (short)((superBlockTemp[1] << 8) + (superBlockTemp[0] & 0xff));
		diskInfo = diskInfo.concat("\n\nFAT Table\n");
		diskInfo = diskInfo.concat("Blocks [" + FAT_LOCATION + "," + (bitshiftTemp - 1) + "]");
		for (int index = 0; index < MAXIMUM_BLOCK_COUNT - 63; index++)
		{
			if (index % (TFSDiskInputOutput.getBlockSize()/8) == 0)
				diskInfo = diskInfo.concat("\n"); // for column count
			
			bitshiftTemp = (short)((fatMSBTemp[index] << 8) + (fatLSBTemp[index] & 0xff));
			diskInfo = diskInfo.concat(" " + bitshiftTemp);
		}
	
		return diskInfo;
	}					

	public static String tfs_prmfs()
	{		
		String diskInfo = "";
		short bitshiftTemp; // used for byte->short conversion

		// convert byte arrays to string
		
		// convert BCB to string
		diskInfo = diskInfo.concat("BCB: Block " + BCB_LOCATION );
		
		for (int index = 0; index < TFSDiskInputOutput.getBlockSize(); index++)
		{			
			if (index % 32 == 0) // for column count
				diskInfo = diskInfo.concat("\n");
			diskInfo = diskInfo.concat(" " + bcb[index]);	
		}

		// convert SuperBlock to string
		diskInfo = diskInfo.concat("\n\nSuperBlock: Block " + SUPER_BLOCK_LOCATION);
		
		for (int index = 0; index < TFSDiskInputOutput.getBlockSize(); index++)
		{			
			if (index % 32 == 0) // for column count
				diskInfo = diskInfo.concat("\n"); // for column count
			diskInfo = diskInfo.concat(" " + superBlock[index]);	
		}

		bitshiftTemp = (short)((superBlock[1] << 8) + (superBlock[0] & 0xff));
		diskInfo = diskInfo.concat("\n\nrootPointer: block " + bitshiftTemp);
		bitshiftTemp = (short)((superBlock[3] << 8) + (superBlock[2] & 0xff));
		diskInfo = diskInfo.concat("\nfreeBlock: block " + bitshiftTemp );
		bitshiftTemp = (short)((superBlock[5] << 8) + (superBlock[4] & 0xff));
		diskInfo = diskInfo.concat("\nblockCount: " + bitshiftTemp);
		bitshiftTemp = (short)((superBlock[7] << 8) + (superBlock[6] & 0xff));
		diskInfo = diskInfo.concat("\nfatSize in blocks: " + bitshiftTemp);
		
		// convert FAT to string
		// last 63 elements read from disk are past FAT data. exclude from string
		// since DIO reads in blocks of 128 bytes, last block will have garbage
		bitshiftTemp = (short)((superBlock[1] << 8) + (superBlock[0] & 0xff));
		diskInfo = diskInfo.concat("\n\nFAT Table\n");
		diskInfo = diskInfo.concat("Blocks [" + FAT_LOCATION + "," + (bitshiftTemp - 1) + "]");
		for (int index = 0; index < MAXIMUM_BLOCK_COUNT - 63; index++)
		{
			if (index % (TFSDiskInputOutput.getBlockSize()/8) == 0)
				diskInfo = diskInfo.concat("\n"); // for column count
			
			bitshiftTemp = (short)((fatMSB[index] << 8) + (fatLSB[index] & 0xff));
			diskInfo = diskInfo.concat(" " + bitshiftTemp);
		}
	
		return diskInfo;
	}

	public static int tfs_open(byte[] name, int nlength)
	{
		return -1;
	}			

	public static int tfs_read(int file_id, byte[] buf, int blength)	
	{
		return -1;
	}

	public static int tfs_write(int file_id, byte[] buf, int blength)
	{
		return -1;
	}	

	public static int tfs_seek(int file_id, int position)
	{
		return -1;
	}	

	public static void tfs_close(int file_id)
	{
		return;
	}			

	public static int tfs_create(byte[] name, int nlength)
	{
		return -1;
	}		

	public static int tfs_delete(byte[] name, int nlength)		
	{
		return -1;
	}	

	public static int tfs_create_dir(byte[] name, int nlength)	
	{
		return -1;
	}	

	public static int tfs_delete_dir(byte[] name, int nlength)	
	{
		return -1;
	}	
	
	// exit file system, unmount and close drive
	public static int tfs_exit()
	{
		// unmount fs
		if (tfs_getMountStatus() == true)
		{
			if (tfs_unmount() != 0)
			{
				System.out.println("Failure to unmount drive");
				return -1;
			}

			else
				System.out.println("Volume unmounted");			
		}		
		
		// add future flag in-case disk already closed 
		if (TFSDiskInputOutput.getDriveStatus() == true)
		{
			if (TFSDiskInputOutput.tfs_dio_close() != 0)
			{
				System.out.println("Failure to close drive");
				return -1;
			}
		}
		
		return 0;
	}
	
	public static boolean tfs_getMountStatus()
	{
		return mountStatus;
	}
	
	/*
	 * TFS private methods to handle in-memory structures
	 */
	

	 
 	private static int _tfs_read_block(int block_no, byte buf[])
 	{
 		return -1;
 	}
 	
 	private static int _tfs_write_block(int block_no, byte buf[])
 	{
 		return -1;
 	}
 	
 	private static int _tfs_write_bcb()
 	{
		// open drive to write
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
 		
		// store BCB to block BCB_LOCATION
		if (TFSDiskInputOutput.tfs_dio_write_block(BCB_LOCATION, bcb) != 0)
		{
			System.out.println("BCB write failed");
			return -1;
		}
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		
		return 0;
 	}
 	
 	private static int _tfs_read_bcb()
 	{
		// open drive to write
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
 		
 		// read BCB from block BCB_LOCATION
		if (TFSDiskInputOutput.tfs_dio_read_block(BCB_LOCATION, bcb) != 0)
		{
			System.out.println("BCB read failed");
			return -1;
		}
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		
		return 0;
 	}
 	
 	// write super block from memeory to disk
 	private static int _tfs_write_pcb()
 	{	
		// open drive to write
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
		
		// store super block to block SUPER_BLOCK_LOCATION
		if (TFSDiskInputOutput.tfs_dio_write_block(SUPER_BLOCK_LOCATION, superBlock) != 0)
		{
			System.out.println("SuperBlock write failed");
			return -1;
		}
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		
		return 0;
 	}
 	
 	// read superblock from disk to memory
 	private static int _tfs_read_pcb()
 	{
		// open drive to read
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
		
		// read super block from SUPER_BLOCK_LOCATION 
		if (TFSDiskInputOutput.tfs_dio_read_block(SUPER_BLOCK_LOCATION, superBlock) != 0)
		{
			System.out.println("SuperBlock read failed");
			return -1;
		} 	
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		
		return 0;
 	}
 	
 	// read FAT from disk to in memory structures
 	private static int _tfs_read_fat()
 	{		
		// open drive to read
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
		
		// read FAT_LOCATION -> block (MAXIMUM_BLOCK_COUNT * 2 / TFSDiskInputOutput.getBlockSize()) + 2 to FAT
		// fat requires 511.98 blocks to read from disk for current implementation
		byte[] rBuffer = new byte[TFSDiskInputOutput.getBlockSize()];
		int fatIndex = 0;
		
		// read all fat blocks to disk file
		for (int readIndex = 0; readIndex < (int) Math.ceil(fatMSB.length * 2 / TFSDiskInputOutput.getBlockSize()); readIndex++)
		{			
			// read block from disk
			if (TFSDiskInputOutput.tfs_dio_read_block(FAT_LOCATION+readIndex, rBuffer) != 0)
			{
				System.out.println("FAT read failed");
				return -1;
			}
			
			// construct FAT from block
			for (int bufferIndex = 0; bufferIndex < TFSDiskInputOutput.getBlockSize(); fatIndex++,bufferIndex += 2)
			{
				fatMSB[fatIndex] = rBuffer[bufferIndex];
				fatLSB[fatIndex] = rBuffer[bufferIndex+1];
			}
		}
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		
		return 0;
 	}
 	
 	// read FAT from disk to in memory structures
 	private static int _tfs_write_fat()
 	{
		// open drive to read
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
		
		// store fat to block FAT_LOCATION -> block (MAXIMUM_BLOCK_COUNT * 2 / TFSDiskInputOutput.getBlockSize()) + 2
		// fat requires 511.98 blocks to write to disk for current implementation
		byte[] wBuffer = new byte[TFSDiskInputOutput.getBlockSize()];
		int fatIndex = 0;
		
		// write all fat blocks to disk file
		for (int writeIndex = 0; writeIndex < (int) Math.ceil(fatMSB.length * 2 / TFSDiskInputOutput.getBlockSize()); writeIndex++)
		{			
			// construct fat block for writing
			for (int bufferIndex = 0; bufferIndex < TFSDiskInputOutput.getBlockSize(); fatIndex++,bufferIndex += 2)
			{
				wBuffer[bufferIndex] = fatMSB[fatIndex];
				wBuffer[bufferIndex+1] = fatLSB[fatIndex];
			}
			
			// write constructed fat block to disk
			if (TFSDiskInputOutput.tfs_dio_write_block(FAT_LOCATION+writeIndex, wBuffer) != 0)
			{
				System.out.println("FAT write failed");
				return -1;
			}
		}
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		
		return 0;
 	}
 	
 	// find a free fat record
 	private static int _tfs_get_block_fat()
 	{
 		byte[] temp = new byte[2];
 		
 		// traverse fat until a free location is found, index 0 reserved
		for (int index = 1; index < fatMSB.length; index++)
		{			
			temp[0] = fatMSB[index];
			temp[1] = fatLSB[index];

			if ((temp[0] == FREE_BLOCK[0]) && (temp[1] == FREE_BLOCK[1]))
			{
				return index;
			}
		}

 		return -1;
 	}
 	
 	// marks a fat record as free
 	private static void _tfs_return_block_fat(short block_no)
 	{
 		// erase block record from fat
		fatMSB[block_no] = (byte)0; 
		fatLSB[block_no] = (byte)0; 
 	}
 	
 	private static int _tfs_open_fd(byte[] fileName, byte nameLength, int fileSize, short firstBlock)
 	{
 		// fileName		(15B)	
 		// nameLength	(1B)
 		// isFile 		(1B)
 		// fileSize 	(4B)	
 		// firstBlock 	(2B)
 		// filePointer	(4B)
 		
 		// create FD array
 		byte[] fileDescriptor = new byte[27];
 		
 		// initialize array
 		// store name
 		for (int index = 0; index < fileName.length; index++)
 			fileDescriptor[index] = fileName[index];
 		
 		// store nameLength
 		fileDescriptor[15] = nameLength;
 		
 		// store file size
 		fileDescriptor[17] = (byte)(fileSize >>> 24);
 		fileDescriptor[18] = (byte)(fileSize >>> 16);
 		fileDescriptor[19] = (byte)(fileSize >>> 8);
 		fileDescriptor[20] = (byte) fileSize;
 		
 		// store first block location
 		fileDescriptor[21] = (byte)(firstBlock & 0xff);
 		fileDescriptor[22] = (byte)((firstBlock >> 8) & 0xff);				
 		
 		// create entry in fileDescriptorTable
 		fileDescriptorTable.add(fileDescriptor);

 		return fileDescriptorTable.size() - 1;
 	}
 	
 	// create a new directory, name is not full path
 	private static int _tfs_create_dir(byte[] fileName, byte nameLength, int fileSize, short firstBlock, byte isFile)
 	{
 		// noEntries 	(4B)			
 		// parentBlockNo(2B)
 		// fileName 	(15B)			
 		// nameLength	(1B)
 		// isFile 		(1B)
 		// fileSize		(4B)
 		// firstBlock	(2B)
 		
 		// create directory array
 		byte[] dir = new byte[DIRECTORY_SIZE];
 		
 		// initialize array
 		
 		// store name
 		for (int index = 6; index < fileName.length + 6; index++)
 			dir[index] = fileName[index - 6];
 		
 		// store nameLength
 		dir[21] = nameLength;
 		
 		// is file?
 		dir[22] = (byte)0;
 		
 		// store first block location
 		dir[27] = (byte)(firstBlock & 0xff);
 		dir[28] = (byte)((firstBlock >> 8) & 0xff);				
 		
 		// create entry in fileDescriptorTable
 		directoryList.add(dir);

 		return directoryList.size() - 1;
 	}
 	
 	// remove entry from file desciptor table
 	private static int _tfs_close_fd(int fileDescriptor)
 	{
 		try
 		{
 			fileDescriptorTable.remove(fileDescriptor);
 		}
 		
 		catch (UnsupportedOperationException e)
 		{
 			return -1;
 		}
 		catch (IndexOutOfBoundsException ie)
 		{
 			return -1;
 		}
 		
 		return 0;
 	}
 	
 	// stores bytes into a block sized buffer
 	private static void _tfs_puts_bytes_block(byte[] block, int offset, byte[] buffer, int length)
 	{
 		for (int index = 0; index < buffer.length; index++)
 		{
 			block[index+offset] = buffer[index];
 		}
 		
 	}
 	
 	// write bytes via file descriptor pointer
 	private static int _tfs_write_bytes_fd(int fd, byte buf[], int length)
 	{
 		short blockNumber;
 		blockNumber = (short)((fileDescriptorTable.get(fd)[22] << 8) + (fileDescriptorTable.get(fd)[21] & 0xff));
 		
		// open drive to read
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
 		
		if (TFSDiskInputOutput.tfs_dio_write_block(blockNumber, buf) != 0)
		{
			System.out.println("Block write failed");
			return -1;
		}
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		 		
 		return 0;
 	}
 	
 	// read bytes via file descriptor pointer
 	private static int _tfs_read_bytes_fd(int fd, byte buf[], int length)
 	{
 		short blockNumber;
 		blockNumber = (short)((fileDescriptorTable.get(fd)[22] << 8) + (fileDescriptorTable.get(fd)[21] & 0xff));
 		
		// open drive to read
		if (TFSDiskInputOutput.tfs_dio_open(driveName, driveName.length) != 0)
		{
			System.out.println("Failure to open disk file");
			return -1;
		}
 		
		if (TFSDiskInputOutput.tfs_dio_read_block(blockNumber, buf) != 0)
		{
			System.out.println("Block read failed");
			return -1;
		}
		
		// close drive
		if (TFSDiskInputOutput.tfs_dio_close() != 0)
		{
			System.out.println("Failure to close disk file");
			return -1;
		}
		 		
 		return 0;
 	}
 	
 	// advance file descriptor pointer
 	private static int _tfs_seek_fd(int fd, int offset)
 	{
 		// convert an offset to byte[4], write to FD.filePointer
 		try
 		{
 			fileDescriptorTable.get(fd)[23] = (byte)(offset >>> 24);
 			fileDescriptorTable.get(fd)[24] = (byte)(offset >>> 16);
 			fileDescriptorTable.get(fd)[25] = (byte)(offset >>> 8);
 			fileDescriptorTable.get(fd)[26] = (byte) offset;
 		}
 		
 		catch (IndexOutOfBoundsException ie)
 		{
 			return -1;
 		}
 		
 		return 0;
 	}
 	
 	// probably need to change method to traverse FAT to find actual block number, not how many blocks in total
 	private static int _tfs_get_block_no_fd(int fd, int offset)
 	{
		short blockNumber;

 		try
 		{
 	 		blockNumber = (short)((fileDescriptorTable.get(fd)[22] << 8) + (fileDescriptorTable.get(fd)[21] & 0xff));
 		}
 		
 		catch (IndexOutOfBoundsException ie)
 		{
 			return -1;
 		}
 		
 		return (offset / TFSDiskInputOutput.getBlockSize()) + blockNumber;
 	}
 	
 	
 	private static int _tfs_read_directory_fd(int fd, byte[] is_file, byte[] nlength, byte[][] name, int[] first_block_no, int[] file_size)
 	{
 		return 0;
 	}
}