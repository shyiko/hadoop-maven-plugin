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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
class HadoopSettings {

    private static Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");

    private final Map<String, String> environment;
    private final File homeDirectory;
    private final File confDirectory;
    private Map<String, String> configurations;

    public HadoopSettings(File homeDirectory, File confDirectory) throws IOException {
        if (homeDirectory == null) {
            throw new IllegalArgumentException("Hadoop home directory must be specified");
        }
        if (!homeDirectory.exists()) {
            throw new IOException("Hadoop home directory doesn't exist");
        }
        this.homeDirectory = homeDirectory;
        if (confDirectory == null) {
            confDirectory = new File(System.getProperty("java.io.tmpdir"), "hmp.conf");
            confDirectory.mkdir();
            extractResourceDirectory("/pseudo-distributed-mode", confDirectory);
        }
        this.confDirectory = confDirectory;
        this.environment = initEnvironment(homeDirectory, confDirectory);
    }

    private Map<String, String> initEnvironment(File homeDirectory, File confDirectory) throws IOException {
        Map<String, String> environment = new HashMap<String, String>();
        environment.put("JAVA_HOME", System.getProperty("java.home"));
        environment.put("HADOOP_HOME", homeDirectory.getCanonicalPath());
        environment.put("HADOOP_CONF_DIR", confDirectory.getCanonicalPath());
        return environment;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    private void extractResourceDirectory(String path, File targetDirectory) throws IOException {
        URL resource = getClass().getResource(path);
        String protocol = resource.getProtocol();
        if (protocol.equals("file")) {
            extractResourceDirectoryUsingFileProtocol(resource, targetDirectory);
        } else
       if (protocol.equals("jar")) {
           extractResourceDirectoryUsingJarProtocol(path, targetDirectory, resource);
       } else {
           throw new UnsupportedOperationException("Protocol " + protocol);
       }
    }

    private void extractResourceDirectoryUsingFileProtocol(URL resource, File targetDirectory) throws IOException {
        URI uri;
        try {
            uri = resource.toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        File sourceDirectory = new File(uri);
        File[] sourceFiles = sourceDirectory.listFiles();
        if (sourceFiles != null) {
            for (File sourceFile : sourceFiles) {
                if (!sourceFile.isFile()) {
                    continue;
                }
                File targetFile = new File(targetDirectory, sourceFile.getName());
                InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
                try {
                    copyStreamToFile(inputStream, targetFile);
                } finally {
                    inputStream.close();
                }
            }
        }
    }

    private void extractResourceDirectoryUsingJarProtocol(String path, File targetDirectory, URL resource) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
        ZipFile zipFile = new ZipFile(jarPath);
        Enumeration zipFileEntries = zipFile.entries();
        while (zipFileEntries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) zipFileEntries.nextElement();
            String zipEntryName = zipEntry.getName();
            if (!zipEntry.getName().startsWith(path) || zipEntry.isDirectory()) {
                continue;
            }
            String fileName = zipEntryName.substring(path.length() + 1);
            if (fileName.contains("/")) {
                continue;
            }
            File targetFile = new File(targetDirectory, fileName);
            InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
            try {
                copyStreamToFile(inputStream, targetFile);
            } finally {
                inputStream.close();
            }
        }
    }

    private void copyStreamToFile(InputStream inputStream, File targetFile) throws IOException {
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
        try {
            copyStream(inputStream, outputStream);
        } finally {
            outputStream.close();
        }
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int size;
        while ((size = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, size);
        }
    }

    public File getHomeDirectory() {
        return homeDirectory;
    }

    public File getConfDirectory() {
        return confDirectory;
    }

    public String getDFSURI() {
        String fsURI = getConf("fs.default.name", "hdfs://localhost:9000");
        return fsURI.endsWith("/") ? fsURI.substring(0, fsURI.length() - 1) : fsURI;
    }

    public String getMapRedScript() {
        return resolveFile("bin/mapred", "bin/hadoop");
    }

    public String getHDFSScript() {
        return resolveFile("bin/hdfs", "bin/hadoop");
    }

    public String getConf(String key, String defaultValue) {
        String value = getConf(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    private String resolveFile(String executable, String fallback) {
        return new File(homeDirectory, executable).exists() ? executable : fallback;
    }

    public String getConf(String key) {
        if (configurations == null) {
            configurations = new HashMap<String, String>();
            File siteXML = new File(confDirectory, "hdfs-site.xml");
            if (siteXML.exists()) {
                try {
                    addXMLConformingHadoopConfigurationXSL(siteXML);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to parse " + siteXML, e);
                }
            }
        }
        return substitute(configurations.get(key));
    }

    private void addXMLConformingHadoopConfigurationXSL(File file) throws IOException {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setNamespaceAware(true);
            docBuilderFactory.setXIncludeAware(true);
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(file);
            Element root = document.getDocumentElement();
            NodeList properties = root.getElementsByTagName("property");
            int numberOfProperties = properties.getLength();
            for (int i = 0; i < numberOfProperties; i++) {
                Element property = (Element) properties.item(i);
                NodeList names = property.getElementsByTagName("name");
                NodeList values = property.getElementsByTagName("value");
                if (names.getLength() > 0 && values.getLength() > 0) {
                    configurations.put(
                            names.item(0).getTextContent().trim(),
                            values.item(0).getTextContent().trim()
                    );
                }
            }
        } catch (ParserConfigurationException e) {
            throw new IOException();
        } catch (SAXException e) {
            throw new IOException();
        }
    }

    private String substitute(String expr) {
        if (expr == null) {
            return null;
        }
        Set<String> propertiesMet = new HashSet<String>();
        String result = expr;
        Matcher match = VARIABLE_PATTERN.matcher(result);
        while (match.find()) {
            String property = match.group();
            property = property.substring(2, property.length() - 1); // remove ${ .. }
            if (!propertiesMet.add(property)) {
                throw new IllegalArgumentException(expr + " contains cycle or uses same property substitution multiple times");
            }
            String value = resolve(property);
            if (value == null) {
                value = "";
            }
            result = result.substring(0, match.start()) + value + result.substring(match.end()); // substitute
            match.reset(result);
        }
        return result;
    }

    private String resolve(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = configurations.get(key);
        }
        return value;
    }
}
