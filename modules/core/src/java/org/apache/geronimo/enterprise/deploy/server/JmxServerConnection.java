package org.apache.geronimo.enterprise.deploy.server;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.lang.reflect.UndeclaredThrowableException;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.shared.ModuleType;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.geronimo.kernel.jmx.JMXUtil;

/**
 * Knows how to execute all the relevant JSR-88 operations on a remote
 * Geronimo server via JMX.  Doesn't currently handle clusters.
 *
 * @version $Revision: 1.3 $
 */
public class JmxServerConnection implements ServerConnection {
    private final static ObjectName DEPLOYER_NAME = JMXUtil.getObjectName("geronimo.deployment:role=ApplicationDeployer");
    private MBeanServer server;

    public JmxServerConnection(String hostname) throws URISyntaxException, RemoteException {
        server = RemoteMBeanServerFactory.create(hostname);
        getTargets(); // Make sure the connection works
    }

    public void close() {
        //todo: is there some way to close the BlockingServer, ChannelPool, etc.?
        server = null;
    }

    public Target[] getTargets() throws IllegalStateException, RemoteException {
        try {
            return (Target[]) server.getAttribute(DEPLOYER_NAME, "Targets");
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public TargetModuleID[] getAvailableModules(ModuleType moduleType, Target[] targetList) throws TargetException, IllegalStateException, RemoteException {
        try {
            Object result = server.invoke(DEPLOYER_NAME, "getAvailableModules", new Object[]{new Integer(moduleType.getValue()), targetList},
                                 new String[]{Integer.TYPE.getName(),getArrayType(Target.class.getName())});
            if(result instanceof Exception) {
                throw (Exception)result;
            } else {
                return (TargetModuleID[])result;
            }
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public TargetModuleID[] getNonRunningModules(ModuleType moduleType, Target[] targetList) throws TargetException, IllegalStateException, RemoteException {
        try {
            return (TargetModuleID[]) server.invoke(DEPLOYER_NAME, "getNonRunningModules", new Object[]{new Integer(moduleType.getValue()), targetList},
                                 new String[]{Integer.TYPE.getName(),getArrayType(Target.class.getName())});
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public TargetModuleID[] getRunningModules(ModuleType moduleType, Target[] targetList) throws TargetException, IllegalStateException, RemoteException {
        try {
            return (TargetModuleID[]) server.invoke(DEPLOYER_NAME, "getNonRunningModules", new Object[]{new Integer(moduleType.getValue()), targetList},
                                                    new String[]{Integer.TYPE.getName(),getArrayType(Target.class.getName())});
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public boolean isRedeploySupported() {
        return true;
    }

    public ProgressObject distribute(Target[] targetList, File moduleArchive, File deploymentPlan) throws IllegalStateException, RemoteException {
        //todo: find a way to stream the content to the server
        try {
            //todo: figure out if the targets are all local and place the files and pass URLs via JMX
            Object moduleData = getBytes(new BufferedInputStream(new FileInputStream(moduleArchive)));
            Object ddData = getBytes(new BufferedInputStream(new FileInputStream(deploymentPlan)));
            Object result = server.invoke(DEPLOYER_NAME, "distribute", new Object[]{targetList,
                                                                                        moduleArchive.getName(),
                                                                                        moduleData,
                                                                                        ddData},
                    new String[]{getArrayType(Target.class.getName()), String.class.getName(),
                                 getByteArrayType(), getByteArrayType()});
            if(result instanceof Exception) {
                throw (Exception)result;
            } else {
                return null; //todo: return a proper P.O. based on whatever the server ends up returning
            }
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public ProgressObject distribute(Target[] targetList, InputStream moduleArchive, InputStream deploymentPlan) throws IllegalStateException, RemoteException {
        //todo: find a way to stream the content to the server
        try {
            //todo: figure out if the targets are all local and place the files and pass URLs via JMX
            Object result = server.invoke(DEPLOYER_NAME, "distribute", new Object[]{targetList, getBytes(moduleArchive),getBytes(deploymentPlan)}, new String[]{getArrayType(Target.class.getName()), getByteArrayType(), getByteArrayType()});
            if(result instanceof Exception) {
                throw (Exception)result;
            } else {
                return null; //todo: return a proper P.O. based on whatever the server ends up returning
            }
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public ProgressObject redeploy(TargetModuleID[] moduleIDList, File moduleArchive, File deploymentPlan) throws UnsupportedOperationException, IllegalStateException, RemoteException {
        //todo: find a way to stream the content to the server
        try {
            //todo: figure out if the targets are all local and place the files and pass URLs via JMX
            server.invoke(DEPLOYER_NAME, "redeploy", new Object[]{moduleIDList,
                                                                  getBytes(new BufferedInputStream(new FileInputStream(moduleArchive))),
                                                                  getBytes(new BufferedInputStream(new FileInputStream(deploymentPlan)))},
                          new String[]{getArrayType(TargetModuleID.class.getName()), getByteArrayType(), getByteArrayType()});
            return null; //todo: return a proper P.O. based on whatever the server ends up returning
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public ProgressObject redeploy(TargetModuleID[] moduleIDList, InputStream moduleArchive, InputStream deploymentPlan) throws UnsupportedOperationException, IllegalStateException, RemoteException {
        //todo: find a way to stream the content to the server
        try {
            //todo: figure out if the targets are all local and place the files and pass URLs via JMX
            server.invoke(DEPLOYER_NAME, "redeploy", new Object[]{moduleIDList, getBytes(moduleArchive),getBytes(deploymentPlan)}, new String[]{getArrayType(TargetModuleID.class.getName()), getByteArrayType(), getByteArrayType()});
            return null; //todo: return a proper P.O. based on whatever the server ends up returning
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public ProgressObject start(TargetModuleID[] moduleIDList) throws IllegalStateException, RemoteException {
        try {
            server.invoke(DEPLOYER_NAME, "start", new Object[]{moduleIDList},
                          new String[]{getArrayType(TargetModuleID.class.getName())});
            return null; //todo: return a proper P.O. based on whatever the server ends up returning
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public ProgressObject stop(TargetModuleID[] moduleIDList) throws IllegalStateException, RemoteException {
        try {
            server.invoke(DEPLOYER_NAME, "stop", new Object[]{moduleIDList},
                          new String[]{getArrayType(TargetModuleID.class.getName())});
            return null; //todo: return a proper P.O. based on whatever the server ends up returning
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public ProgressObject undeploy(TargetModuleID[] moduleIDList) throws IllegalStateException, RemoteException {
        try {
            server.invoke(DEPLOYER_NAME, "undeploy", new Object[]{moduleIDList},
                          new String[]{getArrayType(TargetModuleID.class.getName())});
            return null; //todo: return a proper P.O. based on whatever the server ends up returning
        } catch(UndeclaredThrowableException e) {
            throw new RemoteException("Server request failed", e.getCause());
        } catch(Exception e) {
            throw new RemoteException("Server request failed", e);
        }
    }

    public String[] getResourceJndiNames(String resourceClassName) {
        try {
            return (String[]) server.invoke(DEPLOYER_NAME, "getResourceJndiNames", new Object[]{resourceClassName},
                                            new String[]{String.class.getName()});
        } catch(UndeclaredThrowableException e) {
            throw new RuntimeException("Server request failed", e.getCause());
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String[] getSecurityRoleOptions(String securityRealm) {
        try {
            return (String[]) server.invoke(DEPLOYER_NAME, "getResourceJndiNames", new Object[]{securityRealm},
                                            new String[]{String.class.getName()});
        } catch(UndeclaredThrowableException e) {
            throw new RuntimeException("Server request failed", e.getCause());
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object getBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int count;
        while((count = in.read(buf)) > -1) {
            out.write(buf, 0, count);
        }
        in.close();
        out.close();
        return out.toByteArray();
    }

    private String getArrayType(String className) {
        return "[L"+className+";";
    }

    private String getByteArrayType() {
        return "[B";
    }
}
