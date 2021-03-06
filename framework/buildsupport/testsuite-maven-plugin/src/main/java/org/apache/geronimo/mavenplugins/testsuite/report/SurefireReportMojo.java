/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.apache.geronimo.mavenplugins.testsuite.report;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Creates a nicely formatted Surefire Test Report in html format.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 * @goal generate-surefire-report
 */
public class SurefireReportMojo
    extends AbstractMavenReport
{
    /**
     * Location where generated html will be created.
     *
     * @parameter expression="${project.build.directory}/site"
     */
    private String outputDirectory;

    /**
     * Doxia Site Renderer
     *
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * Maven Project
     *
     * @parameter expression="${project}"
     * @required @readonly
     */
    private MavenProject project;

    /**
     * If set to false, only failures are shown.
     *
     * @parameter expression="${showSuccess}" default-value="true"
     * @required
     */
    private boolean showSuccess;

    /**
     * This directory contains the XML Report files that will be parsed and rendered to HTML format.
     *
     * @parameter expression="${project.build.directory}/surefire-reports"
     * @required
     */
    private File reportsDirectory;

    /**
     * The filename to use for the report.
     *
     * @parameter expression="${outputName}" default-value="surefire-report"
     * @required
     */
    private String outputName;

    /**
     * Location of the Xrefs to link.
     *
     * @parameter default-value="${project.reporting.outputDirectory}/xref-test"
     */
    private File xrefLocation;

    /**
     * Whether to link the XRef if found.
     *
     * @parameter expression="${linkXRef}" default-value="true"
     */
    private boolean linkXRef;

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        /*
        if ( !"pom".equalsIgnoreCase(project.getPackaging()) ) {
            System.out.println("Not a pom packaging.");
            return;
        }
        */

        SurefireReportGenerator report =
            new SurefireReportGenerator( reportsDirectory, locale, showSuccess, determineXrefLocation() );

        report.doGenerateReport( getBundle( locale ), getSink() );

        System.out.println("surefire-report.html generated.");
    }

    private String determineXrefLocation()
    {
        String location = null;

        if ( linkXRef )
        {
            String relativePath = PathTool.getRelativePath( outputDirectory, xrefLocation.getAbsolutePath() );
            if ( StringUtils.isEmpty( relativePath ) )
            {
                relativePath = ".";
            }
            relativePath = relativePath + "/" + xrefLocation.getName();
            if ( xrefLocation.exists() )
            {
                // XRef was already generated by manual execution of a lifecycle binding
                location = relativePath;
            }
            else
            {
                // Not yet generated - check if the report is on its way
                for ( Iterator reports = project.getReportPlugins().iterator(); reports.hasNext(); )
                {
                    ReportPlugin report = (ReportPlugin) reports.next();

                    String artifactId = report.getArtifactId();
                    if ( "maven-jxr-plugin".equals( artifactId ) || "jxr-maven-plugin".equals( artifactId ) )
                    {
                        location = relativePath;
                    }
                }
            }

            if ( location == null )
            {
                getLog().warn( "Unable to locate Test Source XRef to link to - DISABLED" );
            }
        }
        return location;
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.surefire.description" );
    }

    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    public String getOutputName()
    {
        return outputName;
    }

    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "surefire-report", locale, this.getClass().getClassLoader() );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        // Only execute reports for "pom" projects
        return "pom".equalsIgnoreCase(project.getPackaging());
    }
}
