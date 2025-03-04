package test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.Session;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("terminal")
@Component
@RequiredArgsConstructor
public class BatchCommand implements CommandLineRunner {

  private final ApplicationContext applicationContext;

  private final SshService sshService;

  private static final String REMOTE_FOLDER_PATH = "/path/to/remote/folder";

  private static final String LOCAL_FILE_PATH = "./files" + "/" + DateUtility.nameWithDate("GP");

  private static final List<String> FILE_NAMES = Arrays.asList("file.txt");

  private static final String COMMAND = "cd /home/a/test && ./script.sh google.com 10"; // Replace with the actual
                                                                                        // command

  @Override
  public void run(String... args) throws Exception {

    Try.run(this::execute).onFailure(e -> log.error(e.getMessage(), e));

    log.info("Shutting down application...");
    SpringApplication.exit(applicationContext, () -> 0);
  }

  public void execute() {

    Consumer<Session> executeCommand = session -> sshService.executeCommand(session, COMMAND);

    Consumer<Session> transferFile = session -> FILE_NAMES.stream()
        .forEach(e -> sshService.transferFileWithProgress(session, REMOTE_FOLDER_PATH,
            LOCAL_FILE_PATH, e));

    sshService.execute(executeCommand, transferFile);
  }
}
