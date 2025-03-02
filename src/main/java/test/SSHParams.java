package test;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SSHParams {

  private String userName;
  private String remoteHost;
  private int port;
  private String password;

}
