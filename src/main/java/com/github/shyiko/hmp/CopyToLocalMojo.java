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
 * @goal copyToLocal
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class CopyToLocalMojo extends AbstractHadoopMojo {

    /**
     * DFS file/directory that needs to be copied to the locale filesystem
     * @parameter expression="${hmp.copyToLocalSource}"
     */
    private String copyToLocalSource;

    /**
     * Target local file/directory
     * @parameter expression="${hmp.copyToLocalTarget}"
     * @required
     */
    private File copyToLocalTarget;

    /**
     * True if target should be cleaned before copyToLocal, false otherwise
     * @parameter expression="${hmp.autoCleanCopyToLocalTarget}" default-value="true"
     */
    private boolean autoCleanCopyToLocalTarget;

    @Override
    protected void execute(HadoopSettings hadoopSettings) throws MojoExecutionException, MojoFailureException {
        if (copyToLocalSource == null || copyToLocalSource.trim().isEmpty()) {
            try {
                copyToLocalSource = copyToLocalTarget.getCanonicalPath();
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to resolve copyToLocalTarget " + copyToLocalTarget, e);
            }
        }
        if (autoCleanCopyToLocalTarget && copyToLocalTarget.exists()) {
            if (getLog().isInfoEnabled()) {
                getLog().info("Cleaning " + copyToLocalTarget);
            }
            delete(copyToLocalTarget);
        }
        try {
            if (getLog().isInfoEnabled()) {
                getLog().info("Copying data from dfs:" + copyToLocalSource + " to " + copyToLocalTarget);
            }
            executeCommand(hadoopSettings, "bin/hadoop fs -copyToLocal " + copyToLocalSource + " " + copyToLocalTarget.getCanonicalPath());
        } catch (IOException e) {
            throw new MojoFailureException("Unable to perform Hadoop copyToLocal: " + e.getMessage());
        }
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        file.delete();
    }
}
