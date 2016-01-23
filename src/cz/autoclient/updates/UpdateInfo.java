/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.autoclient.updates;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonArray;
import javax.json.JsonObject;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 *
 * @author Jakub
 */
public class UpdateInfo implements java.io.Serializable {
  private static final long serialVersionUID = 666;
  
  public static String UPDATE_DIR = "updates";
  public UpdateInfo(JsonObject json, File baseDir) {
    JsonArray assets = json.getJsonArray("assets");
    URL tmp = null;
    long sizetmp = -1;
    for (int i=0; i<assets.size(); i++) {
      JsonObject item = assets.getJsonObject(i);
      String contentType = item.getString("content_type");
      if("application/zip".equals(contentType) || "application/x-zip-compressed".equals(contentType)) {
        try {
          tmp = new URL(item.getString("browser_download_url"));
        } catch (MalformedURLException ex) {
          continue;
        }
        sizetmp = item.getInt("size");
        break;
      }
    }
    
    downloadLink = tmp;
    originalSize = tmp==null?-1:sizetmp;
    
    version = new VersionId(json.getString("tag_name"));
    id = json.getInt("id");
    localFile = new File(baseDir, version.toString()+".zip");

    isNew = true;
    prerelease = json.getBoolean("prerelease");
  }

  public void unzip() {
    File destination = new File(localFile.getParentFile(), version.toString());
    destination.mkdirs();
    try {
         ZipFile zipFile = new ZipFile(localFile);
         zipFile.extractAll(destination.getAbsolutePath());
    } catch (ZipException e) {
        e.printStackTrace();
    }
  }
  public boolean isUnzipped() {
    return new File(localFile.getParentFile(), version.toString()).isDirectory();
  }
  /** Replaces all files, then schledules replacement of this jar file. Also creates backup.
   *  Shut down all other threads before calling this function.
   */
  public void install(Progress progress) {
    progress.started();
    File updates = localFile.getParentFile();
    File directory = new File(updates, version.toString());
    File backup = new File(updates, "backup");
    
    File myself;
    try {myself = new File(UpdateInfo.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());}
    catch(Exception e) {
      System.out.println("Some error with getting current jar file path.");
      e.printStackTrace();
      progress.stopped(e);
      return;
    }
    File home = myself.getParentFile();
    /** Part 1: backup **/
    String[] ignoreList = new String[] {"updates","data","LOLResources"};
    /*ArrayList<File> originalFiles = listFileChildren(home, new FileFilter() {
      public boolean accept(File file) {
        if(file.isDirectory())
          return false;
      }
    });*/
    progress.status("Getting list of files.");
    /** Part 2: copy **/
    ArrayList<File> updateFiles = listFileChildren(directory, new FileFilter() {
      @Override
      public boolean accept(File file) {
        return !file.isDirectory();
      }
    });
    progress.status("Copying files.");
    
    int count = updateFiles.size();
    int processed = 0;
    for(File f: updateFiles) {
      progress.process(processed++, count);
      String relative = relativePath(directory, f);
      File target = new File(home,relative);

      if(target.exists()) {
        File backupFile = new File(backup, relative);
        try {
          copyFile(target, backupFile);
        } catch (IOException ex) {
          Logger.getLogger(UpdateInfo.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      if(target.compareTo(myself)==0) {
        System.out.println("Cannot overwrite jar.");
        // Prepare jar file to be copied
        ScriptWithParameters script = new BatchScript("update.bat");
        script.setParameter("AUTO_CLIENT_JAR", myself.getName());
        
        continue;
      }
      try {
        copyFile(f, target);
      } catch (IOException ex) {
        System.out.println("Copy file "+f+" error: "+ex.getMessage());
      }
    }
    
  }
  
  public final URL downloadLink;
  public final File localFile;
  public final VersionId version;
  public final long originalSize;
  public final int id;
  public final boolean prerelease;
  // Whether this release was just downloaded
  boolean isNew;
  // Whether this release was presented to the user
  // this will be set even if the code decides not to actually
  // present it (silent update).
  boolean seen;
  
  public boolean isDownloaded() {
    return localFile.exists(); 
  }
  public boolean validateFile() {
    if(!isDownloaded())
      return false;
    if(localFile.length()!=originalSize) {
      localFile.delete();
      return false;
    }
    return true;
  }
  void downloadFile(Progress process) {
    if(downloadLink==null)
      throw new RuntimeException("Cannot download update, download link is null.");
    HttpURLConnection httpConnection;
    try {
      httpConnection = (HttpURLConnection) (downloadLink.openConnection());
    }
    catch(IOException e) {
      process.stopped(e);
      return;
    }
    //long completeFileSize = httpConnection.getContentLength();
    localFile.getParentFile().mkdirs();
    java.io.FileOutputStream fos;
    try {
      fos = new java.io.FileOutputStream(localFile);
    } catch (FileNotFoundException ex) {
      process.stopped(ex);
      return;
    }
    try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
        java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024)
        ) { 
      process.started();
      byte[] data = new byte[1024];
      long downloadedFileSize = 0;
      process.process((double)downloadedFileSize, originalSize);
      int x = 0;
      while ((x = in.read(data, 0, 1024)) >= 0) {
        downloadedFileSize += x;
        bout.write(data, 0, x);
        process.process((double)downloadedFileSize, originalSize);
      }
      bout.close();
      validateFile();
    }
    catch(Exception e) {
      process.stopped(e);
      localFile.delete();
    }
    process.finished();
  }
  boolean isValid() {
    return downloadLink!=null; 
  }
  boolean isVersion(VersionId v) {
    return v.equals(version); 
  }
  boolean isVersion(String v) {
    return version.equals(new VersionId(v)); 
  }
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + this.version.hashCode();
    hash = 43 * hash + this.id;
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof UpdateInfo)) {
      return false;
    }
    final UpdateInfo other = (UpdateInfo) obj;
    if (!Objects.equals(this.version, other.version)) {
      return false;
    }
    if (this.id != other.id) {
      return false;
    }
    return true;
  }
  String basicInfo() {
    StringBuilder s = new StringBuilder();
    s.append(version);
    if(downloadLink!=null) {
      s.append(" Link:");
      s.append(downloadLink.toString());
    }
    return s.toString();
  }
  public static class Comparator implements java.util.Comparator<UpdateInfo>, java.io.Serializable {
    protected Comparator(){}
    @Override
    public int compare(UpdateInfo o1, UpdateInfo o2) {
      int ret = o1.version.compareTo(o2.version);
      if(ret==0)
        return o1.id-o2.id;
      else
        return ret;
    }
    public static Comparator inst = new Comparator();
  }
  
  /** Adds all files in directory to given list, also returns the list.
   * @param parent parent directory
   * @param f filter that can skip some files, can be null without errors
   * @param list list to add files into. The list must not be null.
   * @return  **/
  public static ArrayList<File> listFileChildren(File parent, FileFilter f, ArrayList<File> list) {
    File[] files = parent.listFiles();
    for(File file:files) {
      if(f!=null && !f.accept(file))
        continue;
      list.add(file);
      if(file.isDirectory())
        listFileChildren(file, f, list);
    }
    return list;
  }
  /** Returns list of all child files, recursively.
   * @param parent parent directory
   * @param f filter that can skip some files, can be null without errors
   * @return  **/
  public static ArrayList<File> listFileChildren(File parent, FileFilter f) {
    return listFileChildren(parent, f, new ArrayList());
  }
  public static String relativePath(File parent, File child) {
    return parent.toURI().relativize(child.toURI()).getPath(); 
  }
  /** From: http://stackoverflow.com/a/115086/607407 **/
  public static void copyFile(File sourceFile, File destFile) throws IOException {
    System.out.println("copy "+sourceFile.getAbsolutePath()+" "+destFile.getAbsolutePath());
    if(true)
      return;

    if(!destFile.exists()) {
      destFile.createNewFile();
    }

    FileChannel source = null;
    FileChannel destination = null;

    try {
      source = new FileInputStream(sourceFile).getChannel();
      destination = new FileOutputStream(destFile).getChannel();
      destination.transferFrom(source, 0, source.size());
    }
    finally {
      if(source != null) {
          source.close();
      }
      if(destination != null) {
          destination.close();
      }
    }

  }
  public static interface FileFilter {
    boolean accept(File file); 
  }
}