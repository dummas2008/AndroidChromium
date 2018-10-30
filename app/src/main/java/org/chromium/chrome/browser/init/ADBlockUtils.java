/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.init;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.chromium.base.Log;
import org.chromium.base.PathUtils;

public class ADBlockUtils {

    public static final String PRIVATE_DATA_DIRECTORY_SUFFIX = "chrome";

    public static final String TRACKING_PROTECTION_URL = "https://s3.amazonaws.com/tracking-protection-data/1/TrackingProtection.dat";
    public static final String TRACKING_PROTECTION_LOCALFILENAME = "TrackingProtection.dat";
    public static final String TRACKING_PROTECTION_LOCALFILENAME_DOWNLOADED = "TrackingProtectionDownloaded.dat";
    public static final String ETAG_PREPEND_TP = "tp";

    public static final String ADBLOCK_URL = "https://adblock-data.s3.brave.com/4/ABPFilterParserData.dat";
    public static final String ADBLOCK_LOCALFILENAME = "ABPFilterParserData.dat";
    public static final String ADBLOCK_LOCALFILENAME_DOWNLOADED = "ABPFilterParserDataDownloaded.dat";
    public static final String ETAG_PREPEND_ADBLOCK = "abp";

    public static final String ADBLOCK_REGIONAL_URL = "https://adblock-data.s3.brave.com/4/";
    public static final String REGIONAL_BLOCKERS_LIST_FILE = "regions.json";
    public static final String ADBLOCK_REGIONAL_LOCALFILENAME_DOWNLOADED = "ABPRegionalDataDownloaded.dat";
    public static final String ETAG_PREPEND_REGIONAL_ADBLOCK = "abp_r";

    public static final String HTTPS_URL_NEW = "https://s3.amazonaws.com/https-everywhere-data/6.0/httpse.leveldb.zip";
    public static final String HTTPS_LOCALFILENAME_NEW = "httpse.leveldb.zip";
    public static final String HTTPS_LEVELDB_FOLDER = "httpse.leveldb";
    public static final String HTTPS_LOCALFILENAME_DOWNLOADED_NEW = "httpse.leveldbDownloaded.zip";
    public static final String HTTPS_LOCALFILENAME = "httpse.sqlite";
    public static final String HTTPS_LOCALFILENAME_DOWNLOADED = "httpseDownloaded.sqlite";
    public static final String ETAG_PREPEND_HTTPS = "rs";

    public static final long MILLISECONDS_IN_A_DAY = 86400 * 1000;
    public static final int BUFFER_TO_READ = 16384;    // 16Kb

    private static final String ETAGS_PREFS_NAME = "EtagsPrefsFile";
    private static final String ETAG_NAME = "Etag";
    private static final String TIME_NAME = "Time";

    public static class RegionalADBlockersSt {
      RegionalADBlockersSt() {
        readData = false;
      }
      public List<String> uuid;
      public boolean readData;
    }

    public static void saveETagInfo(Context context, String prepend, EtagObject etagObject) {
        SharedPreferences sharedPref = context.getSharedPreferences(ETAGS_PREFS_NAME, 0);

        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(prepend + ETAG_NAME, etagObject.mEtag);
        editor.putLong(prepend + TIME_NAME, etagObject.mMilliSeconds);

        editor.apply();
    }

    public static EtagObject getETagInfo(Context context, String prepend) {
        SharedPreferences sharedPref = context.getSharedPreferences(ETAGS_PREFS_NAME, 0);

        EtagObject etagObject = new EtagObject();

        etagObject.mEtag = sharedPref.getString(prepend + ETAG_NAME, "");
        etagObject.mMilliSeconds = sharedPref.getLong(prepend + TIME_NAME, 0);

        return etagObject;
    }

    public static String getDataVerNumber(String url, boolean getFirst) {
        String[] split = url.split("/");
        if (split.length > 2) {
            if (getFirst) {
                return split[split.length - 1];
            }
            return split[split.length - 2];
        }

        return "";
    }

    public static void removeOldVersionFiles(Context context, String fileName) {
        File dataDirPath = new File(PathUtils.getDataDirectory());
        if (null == dataDirPath) {
            return;
        }
        File[] fileList = dataDirPath.listFiles();

        for (File file : fileList) {
            String sFileName = file.getAbsoluteFile().toString();
            if (sFileName.endsWith(fileName) || sFileName.endsWith(fileName + ".tmp")) {
                file.delete();
            } else if (file.isDirectory() &&
                  fileName.equals(ADBlockUtils.HTTPS_LOCALFILENAME_NEW) &&
                  sFileName.endsWith(ADBlockUtils.HTTPS_LEVELDB_FOLDER)) {
                File[] httpsFileList = file.listFiles();
                for (File httpsFile : httpsFileList) {
                    httpsFile.delete();
                }
                file.delete();
            }
        }
    }

    public static RegionalADBlockersSt readRegionalABData(Context context, String eTagPrepend, String verNumber,
                                                  String deviceLanguage) {
        RegionalADBlockersSt result = new RegionalADBlockersSt();
        result.uuid = new ArrayList<String>();

        try {
            JsonReader reader = new JsonReader(new InputStreamReader(context.getAssets().open(REGIONAL_BLOCKERS_LIST_FILE)));
            try {
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    boolean foundLanguage = false;
                    String uuidCurrent = "";
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("uuid")) {
                            uuidCurrent = reader.nextString();
                        } else if (name.equals("lang")) {
                            String[] language = reader.nextString().split(",");
                            for (int i = 0; i < language.length; i++) {
                                if (deviceLanguage.equals(new Locale(language[i]).getLanguage())) {
                                    foundLanguage = true;
                                }
                            }
                        } else {
                            reader.skipValue();
                        }
                    }
                    if (foundLanguage && 0 != uuidCurrent.length()) {
                        result.uuid.add(uuidCurrent);
                    }
                    reader.endObject();
                }
                reader.endArray();
            }
            finally {
                reader.close();
            }
        }
        catch (UnsupportedEncodingException e) {
        }
        catch (IllegalStateException e) {
        }
        catch (IOException e) {
        }
        for (int i = 0; i < result.uuid.size(); i++) {
            boolean res = ADBlockUtils.readData(context,
                result.uuid.get(i) + ".dat",
                ADBlockUtils.ADBLOCK_REGIONAL_URL + result.uuid.get(i) + ".dat",
                ADBlockUtils.ETAG_PREPEND_REGIONAL_ADBLOCK + result.uuid.get(i), verNumber,
                ADBlockUtils.ADBLOCK_REGIONAL_LOCALFILENAME_DOWNLOADED, false);
            if (res) {
              result.readData = res;
            }
        }
        if (0 == result.uuid.size()) {
            File dataPathCreated = new File(
                PathUtils.getDataDirectory(),
                ADBlockUtils.ADBLOCK_REGIONAL_LOCALFILENAME_DOWNLOADED);
            if (null != dataPathCreated && dataPathCreated.exists()) {
                try {
                    dataPathCreated.delete();
                }
                catch (SecurityException exc) {
                }
            }
        }

        return result;
    }

    public static byte[] readLocalFile(File path) {
        byte[] buffer = null;

        FileInputStream inputStream = null;
        try {
            if (!path.exists()) {
                return null;
            }
            inputStream = new FileInputStream(path.getAbsolutePath());
            int size = inputStream.available();
            buffer = new byte[size];
            int n = - 1;
            int bytesOffset = 0;
            byte[] tempBuffer = new byte[ADBlockUtils.BUFFER_TO_READ];
            while ( (n = inputStream.read(tempBuffer)) != -1) {
                System.arraycopy(tempBuffer, 0, buffer, bytesOffset, n);
                bytesOffset += n;
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    public static boolean readData(Context context, String fileName, String urlString, String eTagPrepend, String verNumber,
            String fileNameDownloaded, boolean httpseRequest) {
        File dataPath = new File(PathUtils.getDataDirectory(), verNumber + fileName);
        long oldFileSize = dataPath.length();
        boolean fileAbsent = (0 == oldFileSize);
        if (httpseRequest) {
            File dbPath = new File(PathUtils.getDataDirectory(), verNumber + ADBlockUtils.HTTPS_LEVELDB_FOLDER);
            if (dbPath.exists() && dbPath.isDirectory()) {
                fileAbsent = false;
            }
        }
        EtagObject previousEtag = ADBlockUtils.getETagInfo(context, eTagPrepend);
        long milliSeconds = Calendar.getInstance().getTimeInMillis();
        if (fileAbsent || (milliSeconds - previousEtag.mMilliSeconds >= ADBlockUtils.MILLISECONDS_IN_A_DAY)) {
            return ADBlockUtils.downloadDatFile(context, oldFileSize, previousEtag, milliSeconds, fileName,
                urlString, eTagPrepend, verNumber, !httpseRequest, fileNameDownloaded);
        }

        return false;
    }

    public static boolean downloadDatFile(Context context, long oldFileSize, EtagObject previousEtag, long currentMilliSeconds,
                                       String fileName, String urlString, String eTagPrepend, String verNumber, boolean checkOnSize,
                                       String fileNameDownloaded) {
        byte[] buffer = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        boolean res = false;

        try {
            Log.i("ADB", "Downloading %s", verNumber + fileName);
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            String etag = connection.getHeaderField("ETag");
            int length = connection.getContentLength();
            if (null == etag) {
                etag = "";
            }
            boolean downloadFile = true;
            if ((oldFileSize == length || !checkOnSize) && etag.equals(previousEtag.mEtag)) {
                downloadFile = false;
            }
            previousEtag.mEtag = etag;
            previousEtag.mMilliSeconds = currentMilliSeconds;
            ADBlockUtils.saveETagInfo(context, eTagPrepend, previousEtag);
            if (!downloadFile) {
                return false;
            }
            File dataPathCreated = new File(
                PathUtils.getDataDirectory(),
                fileNameDownloaded);
            if (null != dataPathCreated && dataPathCreated.exists()) {
                try {
                    dataPathCreated.delete();
                }
                catch (SecurityException exc) {
                }
            }
            ADBlockUtils.removeOldVersionFiles(context, fileName);

            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            // Write to .tmp file and rename it to dat if success
            File path = new File(PathUtils.getDataDirectory(), verNumber + fileName + ".tmp");
            FileOutputStream outputStream = new FileOutputStream(path);
            inputStream = connection.getInputStream();
            buffer = new byte[ADBlockUtils.BUFFER_TO_READ];
            int n = - 1;
            int totalReadSize = 0;
            try {
                while ((n = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, n);
                    totalReadSize += n;
                }
            }
            catch (IllegalStateException exc) {
                // Sometimes it gives us that exception, found that we should do that way to avoid it:
                // Each HttpURLConnection instance is used to make a single request but the
                // underlying network connection to the HTTP server may be transparently shared by other instance.
                // But we do that way, so just wrapped it for now and we will redownload the file on next request
            }
            outputStream.close();
            if (length != totalReadSize || length != path.length()) {
                ADBlockUtils.removeOldVersionFiles(context, fileName);
            } else {
              // We downloaded the file with success, rename it now to .dat
              File renameTo = new File(PathUtils.getDataDirectory(), verNumber + fileName);
              if (!path.exists() || !path.renameTo(renameTo)) {
                  ADBlockUtils.removeOldVersionFiles(context, fileName);
              }
              res = true;
              Log.i("ADB", "Downloaded %s", verNumber + fileName);
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if (connection != null)
                connection.disconnect();
        }

        return res;
    }

    public static boolean CreateDownloadedFile(Context context, String fileName,
                                            String verNumber, String fileNameDownloaded,
                                            boolean allowAppend) {
        try {
            Log.i("ADB", "Creating %s", fileNameDownloaded + " from " + verNumber + fileName);
            File dataPath = new File(PathUtils.getDataDirectory(), verNumber + fileName);
            if (null != dataPath && (0 != dataPath.length() || dataPath.isDirectory())) {
               File dataPathCreated = new File(PathUtils.getDataDirectory(), fileNameDownloaded);
               if (null != dataPathCreated) {
                   if (!dataPathCreated.exists()) {
                       dataPathCreated.createNewFile();
                       if (dataPathCreated.exists()) {
                           FileOutputStream fo = new FileOutputStream(dataPathCreated);
                           fo.write((verNumber + fileName).getBytes());
                           fo.close();
                           Log.i("ADB", "Created %s", fileNameDownloaded + " from " + verNumber + fileName);
                       }
                   } else if (allowAppend) {
                       FileOutputStream fo = new FileOutputStream(dataPathCreated, true);
                       fo.write((";" + verNumber + fileName).getBytes());
                       fo.close();
                       Log.i("ADB", "Appended %s", fileNameDownloaded + " from " + verNumber + fileName);
                   } else {
                       return false;
                   }
               }
            }
        }
        catch (NullPointerException exc) {
            // We will try to download the file again on next start
        }
        catch (IOException exc) {
        }

        return true;
    }

    public static String validateFilename(String filename, String intendedDir)
      throws java.io.IOException {
        File f = new File(intendedDir, filename);
        String canonicalPath = f.getCanonicalPath();

        File iD = new File(intendedDir);
        String canonicalID = iD.getCanonicalPath();

        if (canonicalPath.startsWith(canonicalID)) {
            return filename;
        } else {
            throw new IllegalStateException("File is outside extraction target directory.");
        }
    }

    public static boolean UnzipFile(String zipName, String verNumber, boolean removeZipFile) {
        ZipInputStream zis = null;
        List<String> createdFiles = new ArrayList<String>();
        try {
            String dir = PathUtils.getDataDirectory();
            File zipFullName =  new File(dir, verNumber + zipName);
            if (null == zipFullName) {
                return false;
            }
            zis = new ZipInputStream(new FileInputStream(zipFullName));
            if (null == zis) {
                Log.i("ADB", "Open zip file " + verNumber + zipName + " error");

                return false;
            }
            byte[] buffer = new byte[ADBlockUtils.BUFFER_TO_READ];
            if (null == buffer) {
                zis.close();

                return false;
            }
            ZipEntry ze = zis.getNextEntry();
            int readBytes = 0;
            String fileName;

            while (null != ze) {
                fileName = validateFilename(ze.getName(), dir);
                File fmd = new File(dir, verNumber + fileName);
                if (null == fmd) {
                    zis.closeEntry();
                    zis.close();

                    return false;
                }
                if (ze.isDirectory()) {
                    fmd.mkdirs();
                } else {
                    try {
                        FileOutputStream fout = null;
                        fout = new FileOutputStream(fmd);
                        if (null == fout) {
                            zis.closeEntry();
                            zis.close();

                            return false;
                        }

                        int total = 0;
                        while ((readBytes = zis.read(buffer)) != -1) {
                            fout.write(buffer, 0, readBytes);
                            total += readBytes;
                        }
                        createdFiles.add(verNumber + fileName);
                        fout.close();
                    } catch (FileNotFoundException exc) {
                        zis.closeEntry();
                        zis.close();
                        for (int i = 0; i < createdFiles.size(); i++) {
                            File toDelete = new File(dir, createdFiles.get(i));
                            toDelete.delete();
                        }

                        return false;
                    } catch (SecurityException exc) {
                      zis.closeEntry();
                      zis.close();

                      return false;
                    }
                }

                zis.closeEntry();
                ze = zis.getNextEntry();
            }

            zis.close();
            if (removeZipFile) {
                zipFullName.delete();
            }
        } catch (NullPointerException exc) {
            try {
                if (null != zis) {
                    zis.close();
                }
            } catch (IOException ex) {
                return false;
            }

            return false;
        } catch (IOException exc) {
            return false;
        } catch (IllegalStateException exc) {
            return false;
        }

        return true;
    }
}
