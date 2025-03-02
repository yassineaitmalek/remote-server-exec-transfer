package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SSH {

  private static final String REMOTE_HOST = "host";
  private static final int PORT = 22;
  private static final String USERNAME = "a";
  private static final String PASSWORD = "a";
  private static final String COMMAND = "sleep 30"; // Replace with the actual command
  private static final String REMOTE_FOLDER_PATH = "/path/to/remote/folder";
  private static final String LOCAL_FILE_PATH = "/path/to/local/folder";

  private static final List<String> FILE_NAMES = Arrays.asList(
      "jenkins1.msi",
      "jenkins2.msi",
      "jenkins3.msi");

  public void onFailSession(Exception exception, Session session) {

    exception.printStackTrace();

    Optional.ofNullable(session)
        .filter(Objects::nonNull)
        .filter(Session::isConnected)
        .ifPresent(e -> {
          try {
            e.disconnect();
            System.out.println("Session disconnected.");
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });
  }

  public void onFailSftp(Exception exception, ChannelSftp channelSftp) {
    exception.printStackTrace();
    Optional.ofNullable(channelSftp)
        .filter(ChannelSftp::isConnected)
        .ifPresent(e -> {
          try {
            e.disconnect();
            System.out.println("Sftp Channel disconnected.");
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });
  }

  public void onFailExec(Exception exception, ChannelExec channelExec) {
    exception.printStackTrace();
    Optional.ofNullable(channelExec)
        .filter(ChannelExec::isConnected)
        .ifPresent(e -> {
          try {
            e.disconnect();
            System.out.println("Sftp Channel disconnected.");
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });
  }

  public Session getSession() {
    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession(USERNAME, REMOTE_HOST, PORT);
      session.setPassword(PASSWORD);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();
      System.out.println("Connected to the server.");
      return session;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

  }

  public void execute() {

    Session session = null;
    try {
      // Establish SSH session

      session = getSession();

      // Execute command and wait for completion
      executeCommand(session, COMMAND);

      for (String fileName : FILE_NAMES) {
        transferFileWithProgress(session, REMOTE_FOLDER_PATH, LOCAL_FILE_PATH, fileName);
      }

      session.disconnect();
      System.out.println("Session disconnected.");

    } catch (Exception e) {
      onFailSession(e, session);
    }
  }

  private String path(String folderPath, String fileName) {
    return Paths.get(folderPath, fileName).toString();
  }

  private void executeCommand(Session session, String command) throws JSchException, IOException {
    ChannelExec channelExec = null;
    try {
      channelExec = (ChannelExec) session.openChannel("exec");
      channelExec.setCommand(command);
      channelExec.setInputStream(null);
      channelExec.setErrStream(System.err);

      InputStream in = channelExec.getInputStream();
      channelExec.connect();

      System.out.println("Executing command: " + command);

      // Read command output
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
      }

      // Wait until the command is done
      while (!channelExec.isClosed()) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      System.out.println("Command execution completed with exit status: " + channelExec.getExitStatus());
      channelExec.disconnect();
    } catch (Exception e) {
      onFailExec(e, channelExec);
    }

  }

  private void transferFile(Session session, String remotePath, String localPath, String fileName) {

    ChannelSftp channelSftp = null;
    try {
      channelSftp = (ChannelSftp) session.openChannel("sftp");
      channelSftp.connect();

      System.out.println("Starting file transfer from: " + path(remotePath, fileName));
      channelSftp.cd(remotePath);
      channelSftp.get(fileName, path(localPath, fileName));
      System.out.println("File transferred successfully to: " + path(localPath, fileName));
      channelSftp.disconnect();
    } catch (Exception e) {

      onFailSftp(e, channelSftp);
    }

  }

  private void transferFileWithProgress(Session session, String remotePath, String localPath, String fileName)
      throws JSchException, SftpException, IOException {
    ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
    File localFile = new File(path(localPath, fileName));
    try (OutputStream outputStream = new FileOutputStream(localFile)) {
      channelSftp.connect();

      channelSftp.cd(remotePath);
      // Get the remote file size
      long remoteFileSize = channelSftp.lstat(fileName).getSize();

      // Use a progress stream to track the progress
      ProgressMonitorOutputStream progressStream = new ProgressMonitorOutputStream(outputStream, remoteFileSize);

      System.out.println("Starting file transfer from: " + path(remotePath, fileName));

      channelSftp.get(fileName, progressStream);
      progressStream.close(); // Close the stream after copying
      System.out.println("File transferred successfully to: " + path(localPath, fileName));

      channelSftp.disconnect();
    } catch (Exception e) {
      onFailSftp(e, channelSftp);
    }
  }

}
