package test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Configuration
@ConfigurationProperties(prefix = "abb")
@NoArgsConstructor
@AllArgsConstructor
public class SSHParams {

  private String userName;
  private String remoteHost;
  private int port;
  private String password;

}
