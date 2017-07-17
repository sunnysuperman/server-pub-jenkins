package com.github.sunnysuperman.serverpub.ansible;

import java.io.File;
import java.util.Map;

import com.github.sunnysuperman.commons.utils.FileUtil;
import com.github.sunnysuperman.commons.utils.FormatUtil;
import com.github.sunnysuperman.commons.utils.JSONUtil;
import com.github.sunnysuperman.serverpub.RegexUtils;

public class Ansible {

    public static int execute(Map<String, String> args) throws Exception {
        // host
        String host = FormatUtil.parseString(args.get("host"));
        if (host == null) {
            throw new RuntimeException("No hosts specified");
        }
        String ansibleTmpDir = FormatUtil.parseString(args.get("ansible_tmp_dir"), "/tmp/.ansible-run");
        File ansibleHostFile = new File(ansibleTmpDir, "hosts");
        FileUtil.write(ansibleHostFile, "[servers]" + FileUtil.LINE + host);
        args.put("ansible_host_path", ansibleHostFile.getAbsolutePath());
        // execute
        String template = "cd ${ansible_config_home} && ${ansible_path}ansible-playbook -i ${ansible_host_path} -v ${ansible_task_path}";
        StringBuilder command = new StringBuilder(RegexUtils.compile(template, args));
        {
            command.append(" --extra-vars '");
            command.append(JSONUtil.stringify(args));
            command.append("'");
        }
        int ret = Command.execute(command.toString(), args.get("ansible_tmp_dir"));
        return ret;
    }

}
