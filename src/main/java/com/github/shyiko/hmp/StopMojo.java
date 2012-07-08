/*
 * Copyright 2012 Stanley Shyiko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.shyiko.hmp;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import sun.jvmstat.monitor.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @goal stop
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class StopMojo extends AbstractMojo {

    private static String[] HADOOP_CLASSES = {"NameNode", "DataNode", "JobTracker", "TaskTracker"};

    /**
     * True indicates that any output should be suppressed, false otherwise
     * @parameter expression="${hmp.quiet}" default-value="false"
     */
    private boolean quiet;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            List<JavaProcess> processes = listJavaProcesses();
            for (JavaProcess process : processes) {
                String className = process.getClassName();
                if (className.contains("hadoop")) {
                    for (String cls : HADOOP_CLASSES) {
                        if (className.contains(cls)) {
                            int pid = process.getPid();
                            if (getLog().isInfoEnabled()) {
                                getLog().info("Killing " + cls + " using PID " + pid);
                            }
                            kill(pid);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void kill(int pid) throws IOException {
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new ExecutionStreamHandler(quiet));
        executor.execute(CommandLine.parse("kill -15 " + pid));
    }

    @SuppressWarnings("unchecked")
    private List<JavaProcess> listJavaProcesses() throws MonitorException, URISyntaxException {
        List<JavaProcess> processes = new ArrayList<JavaProcess>();
        MonitoredHost host = MonitoredHost.getMonitoredHost(new HostIdentifier((String) null));
        Set<Integer> activeVms = host.activeVms();
        for(Integer pid : activeVms) {
            MonitoredVm vm = host.getMonitoredVm(new VmIdentifier(String.valueOf(pid)));
            String mainClass = MonitoredVmUtil.mainClass(vm, true);
            processes.add(new JavaProcess(pid, mainClass));
        }
        return processes;
    }

    private static class JavaProcess {

        private final int pid;
        private final String className;

        private JavaProcess(int pid, String className) {
            this.pid = pid;
            this.className = className;
        }

        public int getPid() {
            return pid;
        }

        public String getClassName() {
            return className;
        }
    }
}


