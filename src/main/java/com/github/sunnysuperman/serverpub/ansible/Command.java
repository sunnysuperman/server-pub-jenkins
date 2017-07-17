package com.github.sunnysuperman.serverpub.ansible;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import com.github.sunnysuperman.commons.model.ObjectId;
import com.github.sunnysuperman.commons.utils.FileUtil;
import com.github.sunnysuperman.serverpub.L;

import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.StreamBuildListener;

public class Command {
    private static AbstractBuild<?, ?> build;
    private static Launcher launcher;
    private static BuildListener listener;

    public static void registerEnv(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        Command.build = build;
        Command.launcher = launcher;
        Command.listener = listener;
    }

    private static File touchExecutableFile(File file) throws IOException {
        FileUtil.delete(file);
        FileUtil.ensureFile(file);
        file.setExecutable(true);
        return file;
    }

    public static int execute(String cmdLine, String tmpDir) throws InterruptedException, IOException {
        File file = touchExecutableFile(new File(tmpDir, "command-" + new ObjectId() + ".sh"));
        try {
            FileUtil.append(file, cmdLine);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamBuildListener sbl = new StreamBuildListener(baos);

            // ArgumentListBuilder args = new ArgumentListBuilder();
            // args.addTokenized(cmdLine);
            final Proc child = launcher.decorateFor(build.getBuiltOn()).launch()
                    .cmdAsSingleString(file.getAbsolutePath()).stdout(sbl).pwd(build.getWorkspace()).start();
            try {
                while (child.isAlive()) {
                    baos.flush();
                    String s = baos.toString();
                    baos.reset();
                    listener.getLogger().print(s);
                    listener.getLogger().flush();
                    Thread.sleep(10);
                }
            } catch (InterruptedException intEx) {
                L.error("Aborted by user");
                child.kill();
                listener.getLogger().println("Aborted by User. Terminated");
                throw new InterruptedException("User Aborted");
            }
            baos.flush();
            listener.getLogger().print(baos.toString());
            listener.getLogger().flush();
            return child.join();
        } finally {
            FileUtil.delete(file);
        }
    }

}
