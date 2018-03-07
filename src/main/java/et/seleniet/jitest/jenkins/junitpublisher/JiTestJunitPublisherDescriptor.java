package et.seleniet.jitest.jenkins.junitpublisher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.acegisecurity.AccessDeniedException;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import javax.xml.bind.DatatypeConverter;

@Extension
public class JiTestJunitPublisherDescriptor extends BuildStepDescriptor<Publisher> {

  public JiTestJunitPublisherDescriptor() {
    super(JiTestJunitPublisher.class);
    load();
  }

  @Override
  public boolean isApplicable(Class arg0) {
    return true;
  }

  @Override
  public String getDisplayName() {
    return "JiTest junit result publisher";
  }

  public FormValidation doCheckJiraServer(@QueryParameter String jiraServer) {
    if (!jiraServer.startsWith("http://") && !jiraServer.startsWith("https://")) {
      return FormValidation.error("Jira server must be given as web address, e.g. https://www.servername.com");
    }    
    return FormValidation.ok();
  }
  
  public FormValidation doCheckPlanKey(@QueryParameter String planKey) {
    FormValidation err = FormValidation.error("Plan key must be of format XXXX-DDDD, where X stands for an upper case letter and D for a digit!");
    if (planKey.isEmpty()) {
      return FormValidation.error("Plan key must not be empty!");
    }
    int dash = planKey.indexOf("-");
    if (dash == -1) {
      return err;
    }
    for (int i = 0; i < dash; i++) {
      char c = planKey.charAt(i);
      if (c < 'A' || c > 'Z') {
        return err;
      }
    }
    for (int i = dash+1; i < planKey.length(); i++) {
      char c = planKey.charAt(i);
      if (c < '0' || c > '9') {
        return err;
      }
    }
    return FormValidation.ok();
  }
  
  public FormValidation doTestConnection(@QueryParameter("jiraServer") String jiraServer, @QueryParameter("jiraUser") final String jiraUser,
      @QueryParameter("jiraPwd") final String jiraPwd) {
    try {
      String type = "application/json; chartset=UTF-8";
      //TODO for jdk8 replace with Base64.getEncoder().encodeToString(...)
      String token = "Basic " + DatatypeConverter.printBase64Binary((jiraUser + ":" + jiraPwd).getBytes("UTF-8"));
      
      while (jiraServer.endsWith("/")) {
        jiraServer = jiraServer.substring(0, jiraServer.length()-1);
      }
      URL u = new URL(jiraServer + "/rest/api/2/issue/createmeta");
      HttpURLConnection conn = (HttpURLConnection) u.openConnection();
      conn.setDoOutput(true);
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", token);
      conn.setRequestProperty("Content-Type", type);
      conn.setRequestProperty("Content-Length", "10");
      String resmsg = conn.getResponseMessage();
      int rescode = conn.getResponseCode();
      String reshtml = "No Response";
      try {
        reshtml = IOUtils.toString(conn.getInputStream());
      } catch (Exception e) {
        
      }
      if (rescode != 200) { 
        return FormValidation.error("[HTTP" + rescode +"] Unable to authenticate (JIRA Captcha active?), got " + resmsg + " / " + reshtml);
      }      
      return FormValidation.okWithMarkup("<span style='font-weight:bolder;color:green;'>Success</span>");
    } catch (Exception e) {
      return FormValidation.error("Failed to connect to jira server: " + e.getMessage());
    }
  }
}
