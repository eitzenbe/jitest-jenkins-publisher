package et.seleniet.jitest.jenkins.junitpublisher;

import hudson.model.BuildListener;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JiTestJunitPublisherTest {

  //@Test
  public void testUpload() throws IOException, InterruptedException {
    BuildListener listener = Mockito.mock(BuildListener.class);
    Mockito.when(listener.getLogger()).thenReturn(System.out);
    JiTestJunitPublisher sut = new JiTestJunitPublisher("http://jitest.eitzen.at", "admin", "!fuffna01", "JIT-35", "Hüena_2_3_0", "junit.xml");
    System.out.println("WD:" + new File(".").getAbsolutePath());
    String junitxml = IOUtils.toString(new FileInputStream(new File("src/test/resources/junit.xml")));
    sut.performRESTCall(junitxml, listener);
  }
}
