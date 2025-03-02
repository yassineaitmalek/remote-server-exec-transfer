package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SSH {

  private final SSHParams sshParams;

  public void onFailSession(Exception exception, Session session) {

    exception.printStackTrace();
    Optional.ofNullable(session)
        .filter(Objects::nonNull)
        .filter(Session::isConnected)
        .ifPresent(e -> {
          try {
            e.disconnect();
            log.info("Session disconnected.");
          } catch (Exception ex) {
            log.error(ex.getMessage(), exception);
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
            log.info("Sftp Channel disconnected.");
          } catch (Exception ex) {
            log.error(ex.getMessage(), exception);
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
            log.info("Sftp Channel disconnected.");
          } catch (Exception ex) {
            log.error(ex.getMessage(), exception);
          }
        });
  }

  public Session getSession() {
    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession(sshParams.getPassword(), sshParams.getRemoteHost(), sshParams.getPort());
      session.setPassword(sshParams.getPassword());
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();
      log.info("Connected to the server.");
      return session;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }

  }

  public void execute(Consumer<Session>... consumers) {

    Session session = null;
    try {

      session = getSession();

      for (Consumer<Session> consumer : consumers) {
        consumer.accept(session);
      }

      session.disconnect();
      log.info("Session disconnected.");

    } catch (Exception e) {
      onFailSession(e, session);
    }
  }

  public String path(String folderPath, String fileName) {
    return Paths.get(folderPath, fileName).toString();
  }

  public void executeCommand(Session session, String command) {
    ChannelExec channelExec = null;
    try {
      channelExec = (ChannelExec) session.openChannel("exec");
      channelExec.setCommand(command);
      channelExec.setInputStream(null);
      channelExec.setErrStream(System.err);

      @Cleanup
      InputStream in = channelExec.getInputStream();
      channelExec.connect();

      log.info("Executing command: " + command);

      // Read command output
      @Cleanup
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line;
      while (Objects.nonNull(line = reader.readLine())) {
        log.info(line);
      }

      // Wait until the command is done
      while (!channelExec.isClosed()) {
        Utils.sleepMiliSeconds(500);
      }

      log.info("Command execution completed with exit status: " + channelExec.getExitStatus());
      channelExec.disconnect();
    } catch (Exception e) {
      onFailExec(e, channelExec);
    }

  }

  public void transferFile(Session session, String remotePath, String localPath, String fileName) {

    ChannelSftp channelSftp = null;
    try {
      channelSftp = (ChannelSftp) session.openChannel("sftp");
      channelSftp.connect();

      log.info("Starting file transfer from: " + path(remotePath, fileName));
      channelSftp.cd(remotePath);
      channelSftp.get(fileName, path(localPath, fileName));
      log.info("File transferred successfully to: " + path(localPath, fileName));
      channelSftp.disconnect();
    } catch (Exception e) {

      onFailSftp(e, channelSftp);
    }

  }

  public void transferFileWithProgress(Session session, String remotePath, String localPath, String fileName) {
    ChannelSftp channelSftp = null;

    File localFile = new File(path(localPath, fileName));
    try (OutputStream outputStream = new FileOutputStream(localFile)) {
      channelSftp = (ChannelSftp) session.openChannel("sftp");
      channelSftp.connect();

      channelSftp.cd(remotePath);
      // Get the remote file size
      long remoteFileSize = channelSftp.lstat(fileName).getSize();

      // Use a progress stream to track the progress
      @Cleanup
      ProgressMonitorOutputStream progressStream = new ProgressMonitorOutputStream(outputStream, remoteFileSize);

      log.info("Starting file transfer from: " + path(remotePath, fileName));

      channelSftp.get(fileName, progressStream);
      progressStream.close(); // Close the stream after copying
      log.info("File transferred successfully to: " + path(localPath, fileName));

      channelSftp.disconnect();
    } catch (Exception e) {
      onFailSftp(e, channelSftp);
    }
  }

}
