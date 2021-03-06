/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * Use of this source code is bound by the Real Time Genomics Limited Software Licence Agreement
 * for Academic Non-commercial Research Purposes only.
 *
 * If you did not receive a license accompanying this file, a copy must first be obtained by email
 * from support@realtimegenomics.com.  On downloading, using and/or continuing to use this source
 * code you accept the terms of that license agreement and any amendments to those terms that may
 * be made from time to time by Real Time Genomics Limited.
 */
package com.rtg.reader;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.rtg.util.io.BufferedRandomAccessFile;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.RandomAccessFileStream;
import com.rtg.util.io.SeekableStream;

/**
 * Base implementation of stream manager for reading sequences should only be used by DefaultSequencesReader
 *
 */
abstract class AbstractStreamManager {

  int readInt(InputStream is) throws IOException {
    IOUtils.readFully(is, mIntbuf, 0, 4);
    return ByteArrayIOUtils.bytesToIntBigEndian(mIntbuf, 0);
  }

  private final byte[] mIntbuf = new byte[4];

  protected DataFileIndex mIndex;
  private int mIndexPos;
  protected File mDir;
  private long mNumberSequences;

  protected int mIndexedSequenceFileNumber = -1;

  protected long mCurrentLower;
  protected long mCurrentUpper;

  protected RollingFile mPointers;
  protected RollingFile mData;

  protected long mDataLength;

  private final String mIndexFile;
  private final String mDataFilePrefix;

  /**
   * Creates a the stream manager
   *
   * @param dir Directory containing sequence data
   * @param numbersequences Number of sequences in directory
   * @param indexFile Name of file containing index
   * @param dataPrefix Filename prefix for data
   * @param pointerPrefix Filename prefix for pointers
   * @throws IOException If an I/O Error occurs
   */
  AbstractStreamManager(final File dir, final long numbersequences, final String indexFile, final String dataPrefix, final String pointerPrefix, long dataIndexVersion, DataFileOpener opener) throws IOException {
    mDir = dir;
    mNumberSequences = numbersequences;
    mIndexFile = indexFile;
    mDataFilePrefix = dataPrefix;
    loadIndex(dataIndexVersion);
    mData = new DataRollingFile(mDir, dataPrefix, mIndex.numberEntries(), mIndex, opener);
    mPointers = new RollingFile(mDir, pointerPrefix, mIndex.numberEntries());
  }

  protected abstract void seekImpl(final long seqNum) throws IOException;

  /**
   * Returns the random access file pointing at the last seek position
   *
   * @return the file handle
   */
  public SeekableStream getData() {
    return mData.randomAccessFile();
  }

  /**
   * Returns the length of the current data
   *
   * @return the length
   */
  public long getDataLength() {
    return mDataLength;
  }

  /**
   * Seeks to the given sequence number
   */
  void seek(final long seqNum) throws IOException {
    if (seqNum < 0) {
      throw new IllegalArgumentException();
    }
    if (seqNum < mCurrentLower || seqNum >= mCurrentUpper) {
      seqFile(seqNum);
      openFiles();
    } else {
      ensureFiles();
    }
    seekImpl(seqNum);
  }

  protected void ensureFiles() throws IOException {
    if (mData.currentFileNo() != mIndexedSequenceFileNumber) {
      if (!mData.openDataFile(mIndexedSequenceFileNumber)) {
        throw new CorruptSdfException("Expected file missing");
      }
      //we don't do funny things with pointer file
    }
  }

  private boolean loadIndex(long dataIndexVersion) throws IOException {
    mIndexPos = 0;
    mIndexedSequenceFileNumber = -1;
    mIndex = DataFileIndex.loadDataFileIndex(dataIndexVersion, new File(mDir, mIndexFile), mDataFilePrefix);
    if (mIndex.getTotalNumberSequences() != mNumberSequences) {
      throw new CorruptSdfException(mDir);
    }
    return true;
  }

  protected void loadNext() {
    long value;
    value = mIndex.numberSequences(mIndexPos++);
    mIndexedSequenceFileNumber++;
    mCurrentLower = mCurrentUpper;
    mCurrentUpper += value;
  }

  protected void openFiles() throws IOException {
    if (!mPointers.openDataFile(mIndexedSequenceFileNumber)) {
      throw new CorruptSdfException("Expected file missing");
    }
    if (!mData.openDataFile(mIndexedSequenceFileNumber)) {
      throw new CorruptSdfException("Expected file missing");
    }
  }

  private void seqFile(final long seqNum) throws IOException {
    while (!(seqNum >= mCurrentLower && seqNum < mCurrentUpper)) {
      if (seqNum < mCurrentLower) {

        mCurrentLower = mCurrentUpper = 0;
        mIndexedSequenceFileNumber = -1;
        mIndexPos = 0;
      }
      loadNext();
    }
  }

  /**
   * Closes Stream manager
   * @throws IOException If an IO Error occurs
   */
  public void close() throws IOException {
    if (mData != null) {
      mData.close();
    }
    if (mPointers != null) {
      mPointers.close();
    }
  }

  /**
   * return the entry sequence index
   * @return long[] containing number of sequences in each pointer file.
   */
  public DataFileIndex sequenceIndex() {
    return mIndex;
  }

  protected static class DataRollingFile extends RollingFile {
    private final DataFileIndex mDataIndex;
    private final DataFileOpener mOpener;

    public DataRollingFile(File fileDir, String filePrefix, int numFiles, DataFileIndex index, DataFileOpener opener) {
      super(fileDir, filePrefix, numFiles);
      mDataIndex = index;
      mOpener = opener;
    }

    @Override
    SeekableStream openFileInternal(File f, int fileNum) throws IOException {
      return mOpener.openRandomAccess(f, mDataIndex.dataSize(fileNum));
    }

  }

  protected static class RollingFile {
    private final File mFileDir;
    private final String mFilePrefix;
    private SeekableStream mCurrentFile;
    private int mCurrentFileNo;
    private final int mNumFiles;

    public RollingFile(File fileDir, String filePrefix, int numFiles) {
      mFileDir = fileDir;
      mFilePrefix = filePrefix;
      mCurrentFileNo = -1;
      mNumFiles = numFiles;
    }

    void close() throws IOException {
      if (mCurrentFile != null) {
        mCurrentFile.close();
        mCurrentFile = null;
      }
    }
    SeekableStream randomAccessFile() {
      return mCurrentFile;
    }

    int currentFileNo() {
      return mCurrentFileNo;
    }

    boolean rollFile() throws IOException {
      final int fileNum = mCurrentFileNo + 1;
      if (fileNum > mNumFiles) {
        throw new CorruptSdfException();
      }
      return openDataFile(fileNum);
    }

    SeekableStream openFileInternal(File f, int fileNum) throws IOException {
      return new RandomAccessFileStream(new BufferedRandomAccessFile(f, "r"));
    }

    boolean openDataFile(final int fileNum) throws IOException {
      final File f = new File(mFileDir, mFilePrefix + fileNum);
      if (f.exists()) {
        close();
        mCurrentFile = openFileInternal(f, fileNum);
        mCurrentFileNo = fileNum;
        return true;
      }
      return false;
    }
  }
}
