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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;

/**
 * @goal copyFromLocal
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class CopyFromLocalMojo extends AbstractHadoopMojo {

    /**
     * Local file/directory that needs to be copied to the DFS
     * @parameter expression="${hmp.copyFromLocalSource}"
     * @required
     */
    private File copyFromLocalSource;

    /**
     * Target DFS file/directory. Default to copyFromLocalSource
     * @parameter expression="${hmp.copyFromLocalTarget}"
     */
    private String copyFromLocalTarget;

    /**
     * True if target should be cleaned before copyFromLocal, false otherwise
     * @parameter expression="${hmp.autoCleanCopyFromLocalTarget}" default-value="true"
     */
    private boolean autoCleanCopyFromLocalTarget;

    @Override
    protected void execute(HadoopSettings hadoopSettings) throws MojoExecutionException, MojoFailureException {
        if (!copyFromLocalSource.exists()) {
            throw new MojoExecutionException("copyFromLocalSource " + copyFromLocalSource + " doesn't exist");
        }
        String canonicalLocalSource;
        try {
            canonicalLocalSource = copyFromLocalSource.getCanonicalPath();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to resolve copyFromLocalSource " + copyFromLocalSource, e);
        }
        if (copyFromLocalTarget == null || copyFromLocalTarget.trim().isEmpty()) {
            copyFromLocalTarget = canonicalLocalSource;
        }
        if (autoCleanCopyFromLocalTarget) {
            String dfsTargetURI = hadoopSettings.getDFSURI() + copyFromLocalTarget;
            if (getLog().isInfoEnabled()) {
                getLog().info("Cleaning dfs:" + copyFromLocalTarget);
            }
            try {
                executeCommand(hadoopSettings, "bin/hadoop fs -rmr " + dfsTargetURI);
            } catch (IOException e) {
                getLog().warn("Unable to clean dfs:" + dfsTargetURI);
            }
        }
        try {
            if (getLog().isInfoEnabled()) {
                getLog().info("Copying data from " + canonicalLocalSource + " to dfs:" + copyFromLocalTarget);
            }
            executeCommand(hadoopSettings, "bin/hadoop fs -copyFromLocal " + canonicalLocalSource + " " + copyFromLocalTarget);
        } catch (IOException e) {
            throw new MojoFailureException("Unable to perform Hadoop copyFromLocal: " + e.getMessage());
        }
    }
}
