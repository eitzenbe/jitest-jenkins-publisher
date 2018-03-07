package et.seleniet.jitest.jenkins.junitpublisher;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import groovy.json.StringEscapeUtils;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import lombok.Getter;
import net.sf.json.JSONObject;

import javax.xml.bind.DatatypeConverter;

/**
 */
@Getter
public class JiTestJunitPublisher extends Notifier {

  private String jiraServer;
  private String jiraUser;
  private String jiraPwd;
  private String planKey;
  private String xmlFile;
  private String version;

  public void start() throws Exception {
  }

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public JiTestJunitPublisher(String jiraServer, String jiraUser, String jiraPwd, String planKey, String version,
      String xmlFile) {
    this.jiraServer = jiraServer;
    this.jiraUser = jiraUser;
    this.jiraPwd = jiraPwd;
    this.planKey = planKey;
    this.version = version;
    this.xmlFile = xmlFile;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
      BuildListener listener) throws InterruptedException, IOException {

    listener.getLogger().println("[INFO] Performing " + this.getClass().getSimpleName() + "...");

    // retrieve junit xml
    listener.getLogger().println("[INFO] Retrieving JUnit result file " + xmlFile + "...");
    FilePath fp = build.getProject().getWorkspace();
    fp = fp.child(xmlFile);
    String junitxml = IOUtils.toString(fp.read());

    
    // escaping xml file and creating JSON body
    //listener.getLogger().println("[DEBUG] JSON Escaping " + junitxml + "...");
    String escapedxml = StringEscapeUtils.escapeJavaScript(junitxml);
    String rawData = "{ \"version\": \"" + version + "\", \"xml\": \"" + escapedxml + "\" }";
    String type = "application/json; chartset=UTF-8";

    // and upload it to jira jitest rest api
    listener.getLogger().println("[INFO] Connecting to " + jiraUser + " @ " + jiraServer + "...");
    //TODO for jdk8 replace with Base64.getEncoder().encodeToString(...)
    String token = "Basic " + DatatypeConverter.printBase64Binary((jiraUser + ":"
            + jiraPwd).getBytes("UTF-8"));
    //listener.getLogger().println("[DEBUG] Authorization token: " + token);
    
    URL u = new URL(jiraServer + "/rest/jitest/latest/plan/createReportFromJUnitXML/" + planKey);
    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Authorization", token);
    conn.setRequestProperty("Content-Type", type);
    conn.setRequestProperty("Content-Length", String.valueOf(rawData.length()));
    
    listener.getLogger().println("[INFO] Creating new report for test plan " + planKey + " with given test results...");
    conn.connect();
    OutputStream os = conn.getOutputStream();
    os.write(rawData.getBytes("UTF-8"));
    int rescode = conn.getResponseCode();
//    String responsemsg = conn.getResponseMessage();
    if (rescode == 401) {
      listener.getLogger().println("[FAIL] ERROR 401 Unable to authenticate with configured credentials!");
      return false;
    } else if (rescode != 200) {
      String reshtml = IOUtils.toString(conn.getInputStream());
      listener.getLogger().println("[FAIL] Unable to upload junit xml, got error code " + rescode + " - " + reshtml);
      return false;
    }
    String reshtml = IOUtils.toString(conn.getInputStream());
    
    //TODO json parse reshtml and return link to Report in output
    JSONObject response = JSONObject.fromObject(reshtml);
    if (response.getString("status").equals("failure")) {
      listener.getLogger().println("[FAIL] An error happened " + response.getString("details"));
      return false; 
    } else {
      listener.getLogger().println("[INFO] Successfully created new report " + jiraServer + "/browse/" + response.getString("reportKey"));
      return true;
    }
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    // TODO Auto-generated method stub
    return BuildStepMonitor.BUILD;
  }

  @Override
  public JiTestJunitPublisherDescriptor getDescriptor() {
    return (JiTestJunitPublisherDescriptor) super.getDescriptor();
  }

}
