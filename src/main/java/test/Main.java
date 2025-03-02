package test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.jcraft.jsch.Session;

public class Main {

  public static final String REMOTE_HOST = "host";
  public static final int PORT = 22;
  public static final String USERNAME = "a";
  public static final String PASSWORD = "a";

  public static final String REMOTE_FOLDER_PATH = "/path/to/remote/folder";
  public static final String LOCAL_FILE_PATH = "/path/to/local/folder";

  public static final List<String> FILE_NAMES = Arrays.asList(
      "jenkins1.msi",
      "jenkins2.msi",
      "jenkins3.msi");

  private static final String COMMAND = "sleep 30"; // Replace with the actual command

  public static void main(String[] args) {

    SSHParams sshParams = new SSHParams(USERNAME, REMOTE_HOST, PORT, PASSWORD);

    SSH ssh = new SSH(sshParams);

    Consumer<Session> executeCommand = session -> ssh.executeCommand(session, COMMAND);

    Consumer<Session> transferFile = session -> FILE_NAMES.stream()
        .forEach(e -> ssh.transferFileWithProgress(session, REMOTE_FOLDER_PATH, LOCAL_FILE_PATH, e));

    ssh.execute(executeCommand, transferFile);
  }

}
