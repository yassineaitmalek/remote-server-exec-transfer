package test;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressMonitorOutputStream extends OutputStream {
  private OutputStream out;
  private long totalBytes;
  private long bytesTransferred;

  public ProgressMonitorOutputStream(OutputStream out, long totalBytes) {
    this.out = out;
    this.totalBytes = totalBytes;
    this.bytesTransferred = 0;

  }

  @Override
  public void write(int b) throws IOException {
    out.write(b);
    bytesTransferred++;
    printProgress();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
    bytesTransferred += len;
    printProgress();
  }

  private void printProgress() {
    int progressPercentage = (int) ((bytesTransferred * 100) / totalBytes);
    System.out.print("\rProgress: " + progressPercentage + "%");
  }

  @Override
  public void close() throws IOException {
    out.close();
  }
}