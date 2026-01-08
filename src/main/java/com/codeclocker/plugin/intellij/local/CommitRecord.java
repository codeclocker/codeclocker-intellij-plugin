package com.codeclocker.plugin.intellij.local;

/** Record of a single git commit with full details. */
public class CommitRecord {

  private String hash;
  private String message;
  private String author;
  private long timestamp;
  private int changedFilesCount;
  private String branch;

  public CommitRecord() {
    // Required for XML serialization
  }

  public CommitRecord(
      String hash,
      String message,
      String author,
      long timestamp,
      int changedFilesCount,
      String branch) {
    this.hash = hash;
    this.message = message;
    this.author = author;
    this.timestamp = timestamp;
    this.changedFilesCount = changedFilesCount;
    this.branch = branch;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public int getChangedFilesCount() {
    return changedFilesCount;
  }

  public void setChangedFilesCount(int changedFilesCount) {
    this.changedFilesCount = changedFilesCount;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }
}
