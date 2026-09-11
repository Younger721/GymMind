    public String uploadFile(Long userId, File file, String filename) throws IOException {
        ensureBucketExists();

        try (InputStream inputStream = new FileInputStream(file)) {
            String objectName = String.format("user_%d/%s", userId, filename);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(knowledgeBucket)
                            .object(objectName)
                            .stream(inputStream, file.length(), -1)
                            .contentType("text/plain")
                            .build()
            );

            log.info("Uploaded file to MinIO: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new IOException("Failed to upload file", e);
        }
    }
