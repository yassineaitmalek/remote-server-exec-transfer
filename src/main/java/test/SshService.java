package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import io.vavr.control.Try;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SshService {

  private final SSHParams sshParams;

  public void onFailSession(Exception exception, Session session) {

    exception.printStackTrace();
    Optional.ofNullable(session)
        .filter(Objects::nonNull)
        .filter(Session::isConnected)
        .ifPresent(e -> Try.run(() -> {
          e.disconnect();
          log.info("Session disconnected.");
        })
            .onFailure(ex -> log.error(ex.getMessage(), ex)));
  }

  public void onFailSftp(Exception exception, ChannelSftp channelSftp) {

    exception.printStackTrace();
    Optional.ofNullable(channelSftp)
        .filter(ChannelSftp::isConnected)
        .ifPresent(e -> Try.run(() -> {
          e.disconnect();
          log.info("Sftp Channel disconnected.");
        })
            .onFailure(ex -> log.error(ex.getMessage(), ex)));
  }

  public void onFailExec(Exception exception, ChannelExec channelExec) {
    exception.printStackTrace();
    Optional.ofNullable(channelExec)
        .filter(ChannelExec::isConnected)
        .ifPresent(e -> Try.run(() -> {
          e.disconnect();
          log.info("Exec Channel disconnected.");
        })
            .onFailure(ex -> log.error(ex.getMessage(), ex)));
  }

  public Session getSession() {
    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession(sshParams.getUserName(), sshParams.getRemoteHost(), sshParams.getPort());
      session.setPassword(sshParams.getPassword());
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();
      log.info("Connected to the server.");
      return session;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }

  }

  @SafeVarargs
  public final void execute(Consumer<Session>... consumers) {
    execute(Arrays.asList(consumers));

  }

  public void execute(List<Consumer<Session>> consumers) {

    Session session = null;
    try {

      session = getSession();

      execute(session, consumers);

      session.disconnect();
      log.info("Session disconnected.");

    } catch (Exception e) {
      onFailSession(e, session);
    }
  }

  private void execute(Session session, List<Consumer<Session>> consumers) {

    Optional.of(consumers)
        .orElseGet(Collections::emptyList)
        .stream()
        .filter(Objects::nonNull)
        .reduce(Consumer::andThen)
        .ifPresent(consumer -> consumer.accept(session));
  }

  public String path(String folderPath, String fileName) {
    return Paths.get(folderPath, fileName).toString();
  }

  public String pathNorm(String folderPath, String fileName) {
    return Paths.get(folderPath, fileName).normalize().toAbsolutePath().toString();
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
    FileUtility.createFolder(localPath);
    try {
      channelSftp = (ChannelSftp) session.openChannel("sftp");
      channelSftp.connect();
      log.info("Starting file transfer from: " + path(remotePath, fileName));
      channelSftp.cd(remotePath);
      channelSftp.get(fileName, pathNorm(localPath, fileName));
      log.info("File transferred successfully to: " + pathNorm(localPath, fileName));
      channelSftp.disconnect();
    } catch (Exception e) {

      onFailSftp(e, channelSftp);
    }

  }

  public void transferFileWithProgress(Session session, String remotePath, String localPath, String fileName) {
    ChannelSftp channelSftp = null;
    FileUtility.createFolder(localPath);
    File localFile = new File(pathNorm(localPath, fileName));
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
      log.info("File transferred successfully to: " + pathNorm(localPath, fileName));

      channelSftp.disconnect();
    } catch (Exception e) {
      onFailSftp(e, channelSftp);
    }
  }

}
