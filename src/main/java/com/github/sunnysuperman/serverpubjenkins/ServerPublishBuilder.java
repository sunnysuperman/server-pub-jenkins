package com.github.sunnysuperman.serverpubjenkins;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.amazonaws.util.StringInputStream;
import com.github.sunnysuperman.commons.utils.FileUtil;
import com.github.sunnysuperman.commons.utils.StringUtil;
import com.github.sunnysuperman.serverpub.L;
import com.github.sunnysuperman.serverpub.L.SimpleLogger;
import com.github.sunnysuperman.serverpub.RegexUtils;
import com.github.sunnysuperman.serverpub.ServerPublish;
import com.github.sunnysuperman.serverpub.ansible.Command;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;

public class ServerPublishBuilder extends Builder {

    private final String vars;

    @DataBoundConstructor
    public ServerPublishBuilder(String vars) {
        this.vars = vars;
    }

    public String getVars() {
        return vars;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private static class MyLogger implements SimpleLogger {
        PrintStream stream;

        public MyLogger(PrintStream stream) {
            super();
            this.stream = stream;
        }

        @Override
        public void info(String msg) {
            stream.println(msg);
        }

        @Override
        public void error(String msg, Throwable ex) {
            if (msg != null) {
                stream.println(msg);
            }
            if (ex != null) {
                ex.printStackTrace(stream);
            }
        }

    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        // register logger
        L.register(new MyLogger(listener.getLogger()));
        // register command env
        Command.registerEnv(build, launcher, listener);
        int code;
        try {
            L.info("Raw vars: " + vars);
            Map<String, String> params = new HashMap<>();
            for (Entry<String, String> entry : build.getEnvironment(listener).entrySet()) {
                params.put(entry.getKey(), entry.getValue());
            }
            if (StringUtil.isNotEmpty(vars)) {
                Map<String, String> varsMap = FileUtil.readProperties(new StringInputStream(vars), StringUtil.UTF8,
                        true);
                for (Entry<String, String> entry : varsMap.entrySet()) {
                    String value = RegexUtils.compile(entry.getValue(), params);
                    params.put(entry.getKey(), value);
                }
            }
            code = ServerPublish.publishServers(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (code != 0) {
            throw new RuntimeException("Failed to publish servers");
        }
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getDisplayName() {
            return "Server Publish";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

    }
}
