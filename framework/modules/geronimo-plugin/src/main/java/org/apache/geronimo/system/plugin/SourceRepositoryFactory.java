/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.geronimo.system.plugin;

import java.io.File;
import java.net.URI;

/**
 * @version $Rev$ $Date$
 */
public class SourceRepositoryFactory {
    private SourceRepositoryFactory() {
    }

    public static SourceRepository getSourceRepository(String repo) {
        return getSourceRepository(repo, null, null);        
    }
    
    public static SourceRepository getSourceRepository(String repo, String user, String pass) {
        if (repo == null) {
            throw new IllegalArgumentException("No repo supplied");
        }
        URI repoURI = PluginRepositoryDownloader.resolveRepository(repo);
        if (repoURI == null) {
            throw new IllegalStateException("Can't locate repo " + repo);
        }
        String scheme = repoURI.getScheme();
        if (scheme.startsWith("http")) {
            return new RemoteSourceRepository(repoURI, user, pass);
        } else if ("file".equals(scheme)) {
            return new LocalSourceRepository(new File(repoURI));
        }
        throw new IllegalStateException("Cannot identify desired repo type for " + repo);
    }
}