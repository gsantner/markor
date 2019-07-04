
    // Other libs
    implementation 'com.github.nextcloud:android-library:1.3.5'

        new Thread(){
            @Override
            public void run() {
                x();
            }
        }.start();
    }

    private OwnCloudClient mClient;
    private Handler mHandler;
    private void x(){
        Uri serverUri = Uri.parse("https://cloud.myserver.com");
        mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true);
        mClient.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(
                        "username","PASSWORD"
                )
        );


        File downFolder = new File(_appSettings.getNotebookDirectoryAsStr());
        String remotePath = FileUtils.PATH_SEPARATOR + "Documents" + FileUtils.PATH_SEPARATOR + "todo.txt";
        DownloadFileRemoteOperation downloadOperation = new DownloadFileRemoteOperation(remotePath, downFolder.getAbsolutePath());
        downloadOperation.addDatatransferProgressListener(new OnDatatransferProgressListener() {
            @Override
            public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileAbsoluteName) {

            }
        });
        downloadOperation.execute(mClient);
    }
