package com.codeclocker.plugin.intellij.local;

/** Record of per-file VCS changes (additions/removals) within an hour for a project. */
public class FileChangeRecord {

  private String fileName;
  private long additions;
  private long removals;
  private String extension;

  public FileChangeRecord() {
    // Required for XML serialization
  }

  public FileChangeRecord(String fileName, long additions, long removals, String extension) {
    this.fileName = fileName;
    this.additions = additions;
    this.removals = removals;
    this.extension = extension;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public long getAdditions() {
    return additions;
  }

  public void setAdditions(long additions) {
    this.additions = additions;
  }

  public long getRemovals() {
    return removals;
  }

  public void setRemovals(long removals) {
    this.removals = removals;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }
}
