/*
 * Copyright 2018 Google LLC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package paulscode.android.mupen64plusae.util;


import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;

import androidx.documentfile.provider.DocumentFile;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.DriveFolder;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class DriveServiceHelper {
    private final Drive mDriveService;

    public static String TYPE_AUDIO = "application/vnd.google-apps.audio";
    public static String TYPE_GOOGLE_DOCS = "application/vnd.google-apps.document";
    public static String TYPE_GOOGLE_DRAWING = "application/vnd.google-apps.drawing";
    public static String TYPE_GOOGLE_DRIVE_FILE = "application/vnd.google-apps.file";
    public static String TYPE_GOOGLE_DRIVE_FOLDER = DriveFolder.MIME_TYPE;
    public static String TYPE_GOOGLE_FORMS = "application/vnd.google-apps.form";
    public static String TYPE_GOOGLE_FUSION_TABLES = "application/vnd.google-apps.fusiontable";
    public static String TYPE_GOOGLE_MY_MAPS = "application/vnd.google-apps.map";
    public static String TYPE_PHOTO = "application/vnd.google-apps.photo";
    public static String TYPE_GOOGLE_SLIDES = "application/vnd.google-apps.presentation";
    public static String TYPE_GOOGLE_APPS_SCRIPTS = "application/vnd.google-apps.script";
    public static String TYPE_GOOGLE_SITES = "application/vnd.google-apps.site";
    public static String TYPE_GOOGLE_SHEETS = "application/vnd.google-apps.spreadsheet";
    public static String TYPE_UNKNOWN = "application/vnd.google-apps.unknown";
    public static String TYPE_VIDEO = "application/vnd.google-apps.video";
    public static String TYPE_3_RD_PARTY_SHORTCUT = "application/vnd.google-apps.drive-sdk";


    public static String EXPORT_TYPE_HTML = "text/html";
    public static String EXPORT_TYPE_HTML_ZIPPED = "application/zip";
    public static String EXPORT_TYPE_PLAIN_TEXT = "text/plain";
    public static String EXPORT_TYPE_RICH_TEXT = "application/rtf";
    public static String EXPORT_TYPE_OPEN_OFFICE_DOC = "application/vnd.oasis.opendocument.text";
    public static String EXPORT_TYPE_PDF = "application/pdf";
    public static String EXPORT_TYPE_MS_WORD_DOCUMENT = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static String EXPORT_TYPE_EPUB = "application/epub+zip";
    public static String EXPORT_TYPE_MS_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static String EXPORT_TYPE_OPEN_OFFICE_SHEET = "application/x-vnd.oasis.opendocument.spreadsheet";
    public static String EXPORT_TYPE_CSV = "text/csv";
    public static String EXPORT_TYPE_TSV = "text/tab-separated-values";
    public static String EXPORT_TYPE_JPEG = "application/zip";
    public static String EXPORT_TYPE_PNG = "image/png";
    public static String EXPORT_TYPE_SVG = "image/svg+xml";
    public static String EXPORT_TYPE_MS_POWER_POINT = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static String EXPORT_TYPE_OPEN_OFFICE_PRESENTATION = "application/vnd.oasis.opendocument.presentation";
    public static String EXPORT_TYPE_JSON = "application/vnd.google-apps.script+json";

    private static final String TAG = "DriveServiceHelper";


    public DriveServiceHelper(Drive driveService)
    {
        mDriveService = driveService;
    }

    public static Drive getGoogleDriveService(Context context, GoogleSignInAccount account, String appName)
    {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        return new com.google.api.services.drive.Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName(appName)
                .build();
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public String createFile(final String fileName) throws IOException
    {
        File metadata = new File()
                .setParents(Collections.singletonList("root"))
                .setMimeType("text/plain")
                .setName(fileName);

        File googleFile = mDriveService.files().create(metadata).execute();
        if (googleFile == null) {
            throw new IOException("Null result when requesting file creation.");
        }

        return googleFile.getId();
    }

    public String createFile(final String fileName, @Nullable final String folderId) throws IOException
    {
        List<String> root;
        if (folderId == null) {
            root = Collections.singletonList("root");
        } else {

            root = Collections.singletonList(folderId);
        }

        File metadata = new File()
                .setParents(root)
                .setMimeType("text/plain")
                .setName(fileName);

        File googleFile = mDriveService.files().create(metadata).execute();
        if (googleFile == null) {
            throw new IOException("Null result when requesting file creation.");
        }

        return googleFile.getId();
    }

    public GoogleDriveFileHolder createTextFile(final String fileName, final String content, @Nullable final String folderId) throws IOException
    {
        List<String> root;
        if (folderId == null) {
            root = Collections.singletonList("root");
        } else {

            root = Collections.singletonList(folderId);
        }

        File metadata = new File()
                .setParents(root)
                .setMimeType("text/plain")
                .setName(fileName);
        ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

        File googleFile = mDriveService.files().create(metadata, contentStream).execute();
        if (googleFile == null) {
            throw new IOException("Null result when requesting file creation.");
        }
        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
        googleDriveFileHolder.setId(googleFile.getId());

        return googleDriveFileHolder;
    }

    public GoogleDriveFileHolder createTextFileIfNotExist(final String fileName, final String content, @Nullable final String folderId) throws IOException
    {
        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();

        FileList result = mDriveService.files().list()
                .setQ("mimeType = 'text/plain' and name = '" + fileName + "' ")
                .setSpaces("drive")
                .execute();

        if (result.getFiles().size() > 0) {
            googleDriveFileHolder.setId(result.getFiles().get(0).getId());
            return googleDriveFileHolder;
        } else {

            List<String> root;
            if (folderId == null) {
                root = Collections.singletonList("root");
            } else {

                root = Collections.singletonList(folderId);
            }

            File metadata = new File()
                    .setParents(root)
                    .setMimeType("text/plain")
                    .setName(fileName);
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

            File googleFile = mDriveService.files().create(metadata, contentStream).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            googleDriveFileHolder.setId(googleFile.getId());

            return googleDriveFileHolder;
        }
    }

    public GoogleDriveFileHolder createFolder(final String folderName, @Nullable final String folderId) throws IOException {
        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();

        List<String> root;
        if (folderId == null) {
            root = Collections.singletonList("root");
        } else {

            root = Collections.singletonList(folderId);
        }
        File metadata = new File()
                .setParents(root)
                .setMimeType(DriveFolder.MIME_TYPE)
                .setName(folderName);

        File googleFile = mDriveService.files().create(metadata).execute();
        if (googleFile == null) {
            throw new IOException("Null result when requesting file creation.");
        }
        googleDriveFileHolder.setId(googleFile.getId());
        return googleDriveFileHolder;
    }

    public GoogleDriveFileHolder getExistingFolder(final String folderName, final String folderId) throws IOException
    {
        GoogleDriveFileHolder result = null;

        List<GoogleDriveFileHolder> files = queryFiles(folderId);

        if (files.size() > 0) {
            for ( GoogleDriveFileHolder driveFile : files) {
                if (driveFile.getName().equals(folderName)) {
                    result = driveFile;
                }
            }
        }

        return result;
    }

    public GoogleDriveFileHolder createFolderIfNotExist(final String folderName, @Nullable final String parentFolderId) throws IOException
    {
        GoogleDriveFileHolder foundFolder = getExistingFolder(folderName, parentFolderId);

        if (foundFolder == null) {

            foundFolder = new GoogleDriveFileHolder();

            Log.d(TAG, "createFolderIfNotExist: not found");
            List<String> root;
            if (parentFolderId == null) {
                root = Collections.singletonList("root");
            } else {
                root = Collections.singletonList(parentFolderId);
            }
            File metadata = new File()
                    .setParents(root)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setName(folderName);

            File googleFile = mDriveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            foundFolder.setId(googleFile.getId());
        }

        return foundFolder;
    }

    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Pair<String, String> readFile(final String fileId) throws IOException
    {
        // Retrieve the metadata as a File object.
        File metadata = mDriveService.files().get(fileId).execute();
        String name = metadata.getName();

        // Stream the file contents to a String.
        try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String contents = stringBuilder.toString();

            return Pair.create(name, contents);
        }
    }

    public void deleteFolderFile(final String fileId) throws IOException
    {
        // Retrieve the metadata as a File object.
        if (fileId != null) {
            List<GoogleDriveFileHolder> files = queryFiles(fileId);

            if (files.size() > 0) {
                for ( GoogleDriveFileHolder driveFile : files) {
                    deleteFolderFile(driveFile.getId());
                }
            }

            mDriveService.files().delete(fileId).execute();
        }
    }

    public GoogleDriveFileHolder uploadFile(final File googleDriveFile, final AbstractInputStreamContent content) throws IOException
    {
        // Retrieve the metadata as a File object.
        File fileMeta = mDriveService.files().create(googleDriveFile, content).execute();
        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
        googleDriveFileHolder.setId(fileMeta.getId());
        googleDriveFileHolder.setName(fileMeta.getName());
        return googleDriveFileHolder;
    }

    public GoogleDriveFileHolder uploadFile(Context context, final Uri localFile, final String mimeType, @Nullable final String folderId) throws IOException
    {
        // Retrieve the metadata as a File object.
        List<String> root;
        if (folderId == null) {
            root = Collections.singletonList("root");
        } else {
            root = Collections.singletonList(folderId);
        }

        DocumentFile file = FileUtil.getDocumentFileSingle(context, localFile);

        ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(file.getUri(), "r");

        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();

        InputStream inputData;
        if (parcelFileDescriptor != null) {
            inputData = new FileInputStream(parcelFileDescriptor.getFileDescriptor());

            byte[] inputDataArray = IOUtils.toByteArray(inputData);
            // Convert content to an AbstractInputStreamContent instance.
            ByteArrayContent contentStream = new ByteArrayContent("", inputDataArray);

            File metadata = new File()
                    .setParents(root)
                    .setMimeType(mimeType)
                    .setName(file.getName());

            File fileMeta = mDriveService.files().create(metadata, contentStream).execute();
            googleDriveFileHolder.setId(fileMeta.getId());
            googleDriveFileHolder.setName(fileMeta.getName());
        }

        return googleDriveFileHolder;
    }

    public GoogleDriveFileHolder uploadFolder(Context context, final DocumentFile sourceData, @Nullable final String folderId) throws IOException
    {
        Log.d(TAG, "creating folder " + sourceData.getName() );
        GoogleDriveFileHolder folder = createFolderIfNotExist(sourceData.getName(), folderId);

        DocumentFile[] localFiles = sourceData.listFiles();

        for (DocumentFile file : localFiles) {
            if (file.isDirectory()) {

                Log.d(TAG, "is folder " + file.getName() );

                uploadFolder(context, file, folder.getId());
            } else {
                Log.d(TAG, "is file " + file.getName() );
                uploadFile(context, file.getUri(), "", folder.getId());
            }
        }

        return folder;
    }

    public void uploadFilesThatStartWith(Context context, final DocumentFile sourceData, @Nullable final String folderId, String startsWith) throws IOException
    {
        DocumentFile[] localFiles = sourceData.listFiles();

        for (DocumentFile file : localFiles) {
            if (!file.isDirectory() && file.getName() != null && file.getName().startsWith(startsWith)) {
                Log.d(TAG, "uploading file " + file.getName() );
                uploadFile(context, file.getUri(), "", folderId);
            }
        }
    }

    public boolean downloadFolder(Context context, final DocumentFile destData, GoogleDriveFileHolder folder) throws IOException
    {
        if (folder.isDirectory()) {
            Log.d(TAG, "Downloading folder " + folder.getName());

            List<GoogleDriveFileHolder> files = queryFiles(folder.getId());
            DocumentFile newFolder = FileUtil.createFolderIfNotPresent(context, destData, folder.getName());

            if (newFolder == null) {
                return false;
            }

            for (GoogleDriveFileHolder file : files) {
                if (file.isDirectory()) {
                    downloadFolder(context, newFolder, file);
                } else {
                    downloadFile(context, newFolder, file);
                }
            }
        } else {
            downloadFile(context, destData, folder);
        }
        return true;
    }

    public List<GoogleDriveFileHolder> searchFolder(final String folderName) throws IOException
    {
        List<GoogleDriveFileHolder> googleDriveFileHolderList = new ArrayList<>();
        // Retrive the metadata as a File object.
        FileList result = mDriveService.files().list()
                .setQ("mimeType = '" + DriveFolder.MIME_TYPE + "' and name = '" + folderName + "' ")
                .setSpaces("drive")
                .execute();

        for (int i = 0; i < result.getFiles().size(); i++) {

            GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
            googleDriveFileHolder.setId(result.getFiles().get(i).getId());
            googleDriveFileHolder.setName(result.getFiles().get(i).getName());

            googleDriveFileHolderList.add(googleDriveFileHolder);
        }

        return googleDriveFileHolderList;
    }

    public List<GoogleDriveFileHolder> searchFile(final String fileName, final String mimeType) throws IOException
    {
        List<GoogleDriveFileHolder> googleDriveFileHolderList = new ArrayList<>();
        // Retrive the metadata as a File object.
        FileList result = mDriveService.files().list()
                .setQ("name = '" + fileName + "' and mimeType ='" + mimeType + "'")
                .setSpaces("drive")
                .setFields("files(id, name,size,createdTime,modifiedTime,starred)")
                .execute();


        for (int i = 0; i < result.getFiles().size(); i++) {
            GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
            googleDriveFileHolder.setId(result.getFiles().get(i).getId());
            googleDriveFileHolder.setName(result.getFiles().get(i).getName());
            googleDriveFileHolder.setModifiedTime(result.getFiles().get(i).getModifiedTime());
            googleDriveFileHolder.setSize(result.getFiles().get(i).getSize());
            googleDriveFileHolderList.add(googleDriveFileHolder);

        }

        return googleDriveFileHolderList;
    }

    public void downloadFile(final java.io.File fileSaveLocation, final String fileId) throws IOException
    {
        // Retrieve the metadata as a File object.
        OutputStream outputStream = new FileOutputStream(fileSaveLocation);
        mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
    }

    public boolean downloadFile(Context context, final DocumentFile fileSaveLocation, final GoogleDriveFileHolder file)
    {
        Log.d(TAG, "Downloading file " + file.getName());

        DocumentFile childFile = fileSaveLocation.findFile(file.getName());

        if (childFile == null) {
            childFile = fileSaveLocation.createFile("", file.getName());
        }

        if (childFile == null) {
            return false;
        }

        ParcelFileDescriptor parcelFileDescriptor;

        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(childFile.getUri(), "w");

            if (parcelFileDescriptor != null)
            {
                try (FileOutputStream out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor())) {

                    mDriveService.files().get(file.getId()).executeMediaAndDownloadTo(out);

                } catch (Exception e) {
                    Log.e("copyFile", "Exception: " + e.getMessage());
                }

                parcelFileDescriptor.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public InputStream downloadFile(final String fileId) throws IOException
    {
        // Retrieve the metadata as a File object.
        return mDriveService.files().get(fileId).executeMediaAsInputStream();
    }

    public void exportFile(final java.io.File fileSaveLocation, final String fileId, final String mimeType) throws IOException
    {
        // Retrieve the metadata as a File object.
        OutputStream outputStream = new FileOutputStream(fileSaveLocation);
        mDriveService.files().export(fileId, mimeType).executeMediaAndDownloadTo(outputStream);
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    public void saveFile(final String fileId, final String name, final String content) throws IOException
    {
        // Create a File containing any metadata changes.
        File metadata = new File().setName(name);

        // Convert content to an AbstractInputStreamContent instance.
        ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

        // Update the metadata and contents.
        mDriveService.files().update(fileId, metadata, contentStream).execute();
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    public List<GoogleDriveFileHolder> queryFiles() throws IOException
    {
        List<GoogleDriveFileHolder> googleDriveFileHolderList = new ArrayList<>();

        FileList result = mDriveService.files().list().setFields("files(id, name,size,createdTime,modifiedTime,starred,mimeType)").setSpaces("drive").execute();

        for (int i = 0; i < result.getFiles().size(); i++) {

            GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
            googleDriveFileHolder.setId(result.getFiles().get(i).getId());
            googleDriveFileHolder.setName(result.getFiles().get(i).getName());
            if (result.getFiles().get(i).getSize() != null) {
                googleDriveFileHolder.setSize(result.getFiles().get(i).getSize());
            }

            if (result.getFiles().get(i).getModifiedTime() != null) {
                googleDriveFileHolder.setModifiedTime(result.getFiles().get(i).getModifiedTime());
            }

            if (result.getFiles().get(i).getCreatedTime() != null) {
                googleDriveFileHolder.setCreatedTime(result.getFiles().get(i).getCreatedTime());
            }

            if (result.getFiles().get(i).getStarred() != null) {
                googleDriveFileHolder.setStarred(result.getFiles().get(i).getStarred());
            }

            if (result.getFiles().get(i).getMimeType() != null) {
                googleDriveFileHolder.setMimeType(result.getFiles().get(i).getMimeType());
            }
            googleDriveFileHolderList.add(googleDriveFileHolder);

        }

        return googleDriveFileHolderList;
    }

    public List<GoogleDriveFileHolder> queryFiles(@Nullable final String folderId) throws IOException
    {
        List<GoogleDriveFileHolder> googleDriveFileHolderList = new ArrayList<>();
        String parent = "root";
        if (folderId != null) {
            parent = folderId;
        }

        FileList result = mDriveService.files().list().setQ("'" + parent + "' in parents").setFields("files(id, name,size,createdTime,modifiedTime,starred,mimeType)").setSpaces("drive").execute();

        for (int i = 0; i < result.getFiles().size(); i++) {

            GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
            googleDriveFileHolder.setId(result.getFiles().get(i).getId());
            googleDriveFileHolder.setName(result.getFiles().get(i).getName());
            if (result.getFiles().get(i).getSize() != null) {
                googleDriveFileHolder.setSize(result.getFiles().get(i).getSize());
            }

            if (result.getFiles().get(i).getModifiedTime() != null) {
                googleDriveFileHolder.setModifiedTime(result.getFiles().get(i).getModifiedTime());
            }

            if (result.getFiles().get(i).getCreatedTime() != null) {
                googleDriveFileHolder.setCreatedTime(result.getFiles().get(i).getCreatedTime());
            }

            if (result.getFiles().get(i).getStarred() != null) {
                googleDriveFileHolder.setStarred(result.getFiles().get(i).getStarred());
            }
            if (result.getFiles().get(i).getMimeType() != null) {
                googleDriveFileHolder.setMimeType(result.getFiles().get(i).getMimeType());
            }

            if (!googleDriveFileHolder.getName().equals("null")) {
                googleDriveFileHolderList.add(googleDriveFileHolder);
            }
        }

        return googleDriveFileHolderList;
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        return intent;
    }

    /**
     * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
     * created by {@link #createFilePickerIntent()} using the given {@code contentResolver}.
     */
    @SuppressWarnings("ConstantConditions")
    public Pair<String, String> openFileUsingStorageAccessFramework(
        final ContentResolver contentResolver, final Uri uri) throws IOException
    {
        // Retrieve the document's display name from its metadata.
        String name;
        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                name = cursor.getString(nameIndex);
            } else {
                throw new IOException("Empty cursor returned for file.");
            }
        }

        // Read the document's contents as a String.
        String content;
        try (InputStream is = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            content = stringBuilder.toString();
        }

        return Pair.create(name, content);
    }
}